package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumo por tanque: um registro por trecho entre dois abastecimentos (ou desde o
 * inicio do acompanhamento ate o primeiro abastecimento detectado).
 *
 * O H6 nao publica litros abastecidos — so o nivel em porcentagem
 * (car.basic.remain_fuel_percentage) e a autonomia (car.basic.remain_odometer).
 * Por isso o volume e ESTIMADO: variacao do nivel x capacidade do tanque. O numero
 * nao substitui a bomba do posto, mas todos os tanques usam a mesma regua, que e o
 * que importa para comparar um posto com outro.
 *
 * O abastecimento e detectado por um SALTO para cima no nivel. O limiar existe para
 * nao confundir a oscilacao normal do medidor (ladeira, curva) com abastecimento, e
 * o intervalo minimo entre dois abastecimentos evita fechar um tanque de poucos
 * metros quando o motorista completa o tanque em duas etapas.
 *
 * Gravacao atomica (tmp + rename), pelo mesmo motivo do TripLog: a central pode
 * perder energia no meio da escrita, e o historico inteiro nao pode se perder por
 * causa do ultimo registro.
 */
public final class TankLog {
    private static final String TAG = "TankLog";
    private static final String FILE_NAME = "tanks.json";
    private static final String TMP_NAME = "tanks.json.tmp";

    private static final Object LOCK = new Object();

    /**
     * Capacidade oficial do Haval H6 HEV no Brasil: 60 litros. So e usada para
     * converter a variacao percentual em litros; se o carro do usuario tiver outra
     * capacidade, este e o unico numero a mudar.
     */
    public static final double TANK_CAPACITY_LITERS = 60.0;

    /** Salto minimo do nivel, em pontos percentuais, para tratar como abastecimento. */
    private static final double REFUEL_MIN_DELTA_PERCENT = 5.0;

    /**
     * Dois abastecimentos mais proximos que isso viram um so. Cobre o habito de
     * completar o tanque em duas etapas e o medidor acomodando logo depois da bomba.
     */
    private static final long REFUEL_COOLDOWN_MS = 10 * 60 * 1000L;

    /**
     * Janela em que uma atualizacao de autonomia ainda pode preencher a autonomia
     * inicial do tanque recem-criado. Serve para a ordem de entrega das chaves: o
     * modulo pode mandar o nivel antes da autonomia.
     */
    private static final long RANGE_CAPTURE_WINDOW_MS = 2 * 60 * 1000L;

    /**
     * Janela de estabilizacao do primeiro tanque. O modulo pode publicar um nivel
     * transitorio ao acordar; se o nivel "pular" nesses primeiros minutos sem o carro
     * ter andado, isso e ajuste de leitura, nao abastecimento.
     */
    private static final long INIT_SETTLE_MS = 2 * 60 * 1000L;
    private static final double INIT_SETTLE_MAX_DISTANCE_KM = 1.0;

    /** Teto do historico. ~300 tanques dao algumas dezenas de KB. */
    public static final int MAX_TANKS = 300;

    /** Otimizacao para o ramo de autonomia: fora da janela, nem le o arquivo. */
    private static volatile long rangeCaptureUntil = 0L;

    private TankLog() {
    }

    /**
     * Estado persistido. `hasFuel` separa "nunca vimos o nivel" de "o nivel e 0":
     * enquanto for false, a primeira amostra inicia o acompanhamento em vez de ser
     * interpretada como um salto de 0 para o nivel atual.
     */
    public static final class State {
        public int version = 1;
        public boolean hasFuel;
        public double lastFuelPercent;
        public long lastRefuelAt;
        public List<Tank> tanks = new ArrayList<>();

        /** O tanque aberto e sempre o ultimo; nunca ha dois ao mesmo tempo. */
        public Tank openTank() {
            if (tanks == null) {
                return null;
            }
            for (int i = tanks.size() - 1; i >= 0; i--) {
                Tank tank = tanks.get(i);
                if (tank != null && tank.endedAt == 0L) {
                    return tank;
                }
            }
            return null;
        }
    }

    /**
     * Um tanque. Os campos sao simples de proposito: e o Gson que escreve e le, sem
     * adaptador proprio — o arquivo continua legivel a mao no run-as/adb.
     *
     * `station` e o posto do abastecimento que ABRIU este tanque. O consumo medido
     * aqui e o do combustivel comprado nele, e nao o do proximo abastecimento.
     */
    public static final class Tank {
        public long startedAt;
        /** 0 = ainda aberto (tanque atual). */
        public long endedAt;
        public int startOdometer;
        public int endOdometer;
        public double startFuelPercent;
        /** Ultimo nivel visto antes do abastecimento que fechou o tanque. */
        public double endFuelPercent;
        /** Ultimo nivel visto; acompanha o tanque aberto. */
        public double currentFuelPercent;
        /** Autonomia informada pelo carro no inicio (-1/0 = nao informada). */
        public int startRangeKm;
        /** Autonomia informada pelo carro antes do abastecimento que fechou o tanque. */
        public int endRangeKm;
        public double distanceKm;
        /** Litros que entraram no abastecimento que abriu o tanque (estimados). */
        public Double litersAdded;
        /**
         * Nivel antes do abastecimento. Guardado para recalcular os litros enquanto
         * a bomba ainda esta enchendo: o modulo publica o nivel em passos, e o
         * primeiro salto de 5 pontos pode acontecer no meio do abastecimento.
         */
        public Double fuelBeforeRefuelPercent;
        /** Litros consumidos no trecho (estimados pela variacao do nivel). */
        public Double litersUsed;
        public Double kmPerLiter;
        /** Posto do abastecimento que abriu o tanque. Vazio = nao definido. */
        public String station = "";
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    private static File tmpFile(Context context) {
        return new File(context.getFilesDir(), TMP_NAME);
    }

    /** Arquivo ausente/corrompido vira estado vazio; nunca devolve null. */
    public static State loadState(Context context) {
        File f = file(context);
        if (!f.exists()) {
            return new State();
        }
        try (FileReader reader = new FileReader(f)) {
            State state = new Gson().fromJson(reader, State.class);
            return normalize(state);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao ler o historico de tanques; comecando vazio", e);
            return new State();
        }
    }

    /**
     * Gson nao roda inicializadores de campo (usa Unsafe), entao tudo que o arquivo
     * pode nao trazer precisa ser normalizado aqui antes de a UI ver.
     */
    private static State normalize(State state) {
        if (state == null) {
            return new State();
        }
        state.version = 1;
        if (state.tanks == null) {
            state.tanks = new ArrayList<>();
        }
        for (int i = state.tanks.size() - 1; i >= 0; i--) {
            Tank tank = state.tanks.get(i);
            if (tank == null) {
                state.tanks.remove(i);
                continue;
            }
            if (tank.station == null) {
                tank.station = "";
            }
        }
        return state;
    }

    /** Ultima modificacao do arquivo, para a aba evitar reler o JSON a cada segundo. */
    public static long lastModified(Context context) {
        File f = file(context);
        return f.exists() ? f.lastModified() : 0L;
    }

    /**
     * Uma leitura nova do nivel de combustivel. Chamado pelo ServiceManager a cada
     * entrega de car.basic.remain_fuel_percentage.
     *
     * `now` e `odometer` vem de fora para o registro ser facil de testar e para a
     * distancia ser sempre a do mesmo instante da amostra.
     */
    public static void onFuelSample(Context context, double percent, int odometer,
                                    int rangeKm, long now) {
        // 0 e sentinela de "modulo ainda acordando" em muitos barramentos. Aceitar
        // esse zero faria a amostra seguinte (o nivel de verdade) parecer um
        // abastecimento de tanque cheio.
        if (!isValidPercent(percent)) {
            return;
        }

        synchronized (LOCK) {
            State state = loadState(context);
            Tank open = state.openTank();

            // O barramento pode republicar o mesmo nivel com frequencia. Sem esta
            // saida, cada republicacao releria e regravaria o historico inteiro.
            if (state.hasFuel && open != null
                    && Double.compare(percent, state.lastFuelPercent) == 0) {
                return;
            }

            if (!state.hasFuel) {
                state.hasFuel = true;
                state.lastFuelPercent = percent;
                if (open == null) {
                    open = newTank(now, percent, odometer, rangeKm);
                    state.tanks.add(open);
                    armRangeCapture(now);
                    Log.w(TAG, "[TANQUE] acompanhamento iniciado em " + percent
                            + "% (odometro=" + odometer + ", autonomia=" + rangeKm + " km)");
                } else {
                    open.currentFuelPercent = percent;
                }
                save(context, state);
                return;
            }

            double fuelBefore = state.lastFuelPercent;
            double delta = percent - fuelBefore;
            boolean withinRefuelWindow = open != null && open.litersAdded != null
                    && now - state.lastRefuelAt <= REFUEL_COOLDOWN_MS;
            boolean refuel = delta >= REFUEL_MIN_DELTA_PERCENT
                    && now - state.lastRefuelAt >= REFUEL_COOLDOWN_MS;

            // Primeiro tanque recem-criado e o carro praticamente nao andou: o salto
            // e a leitura acomodando, nao combustivel entrando. Sem esta guarda, um
            // 1% transitorio no acordar do modulo viraria um "abastecimento" de 49%.
            boolean initialSettle = refuel && open != null && open.litersAdded == null
                    && now - open.startedAt < INIT_SETTLE_MS
                    && (odometer <= 0 || open.startOdometer <= 0
                        || odometer - open.startOdometer < INIT_SETTLE_MAX_DISTANCE_KM);

            if (initialSettle) {
                open.startFuelPercent = percent;
                open.currentFuelPercent = percent;
                if (odometer > 0) {
                    open.startOdometer = odometer;
                }
                if (rangeKm > open.startRangeKm) {
                    open.startRangeKm = rangeKm;
                }
                armRangeCapture(now);
                state.lastFuelPercent = percent;
                Log.w(TAG, "[TANQUE] ajuste de estabilizacao inicial: " + percent
                        + "% (odometro=" + odometer + ", autonomia=" + rangeKm + " km)");
            } else if (refuel) {
                if (open != null) {
                    closeTank(open, now, odometer, fuelBefore, rangeKm);
                    Log.w(TAG, "[TANQUE] fechado: distancia=" + round1(open.distanceKm)
                            + " km, litros=" + (open.litersUsed == null ? "?" : round1(open.litersUsed))
                            + ", consumo=" + (open.kmPerLiter == null ? "?" : round1(open.kmPerLiter))
                            + " km/L, posto=" + (open.station.isEmpty() ? "(nao definido)" : open.station));
                }

                Tank tank = newTank(now, percent, odometer, rangeKm);
                tank.fuelBeforeRefuelPercent = fuelBefore;
                tank.litersAdded = round2(delta * TANK_CAPACITY_LITERS / 100.0);
                state.tanks.add(tank);
                armRangeCapture(now);
                state.lastFuelPercent = percent;
                state.lastRefuelAt = now;
                Log.w(TAG, "[TANQUE] abastecimento detectado: " + fuelBefore
                        + "% -> " + percent + "% (~" + round1(tank.litersAdded) + " L)"
                        + ", odometro=" + odometer + ", autonomia=" + rangeKm + " km");
            } else if (withinRefuelWindow && percent > open.startFuelPercent) {
                // A bomba continua enchendo depois do primeiro salto: o nivel sobe em
                // passos e o tanque ainda nao pode comecar no meio do abastecimento.
                // So corrige para CIMA; uma queda a partir daqui ja e consumo.
                open.startFuelPercent = percent;
                open.currentFuelPercent = percent;
                if (open.fuelBeforeRefuelPercent != null) {
                    open.litersAdded = round2((percent - open.fuelBeforeRefuelPercent)
                            * TANK_CAPACITY_LITERS / 100.0);
                } else {
                    open.litersAdded = round2((open.litersAdded == null ? 0.0 : open.litersAdded)
                            + delta * TANK_CAPACITY_LITERS / 100.0);
                }
                if (rangeKm > open.startRangeKm) {
                    open.startRangeKm = rangeKm;
                }
                if (open.startOdometer <= 0 && odometer > 0) {
                    open.startOdometer = odometer;
                }
                armRangeCapture(now);
                state.lastFuelPercent = percent;
                // Nao estende lastRefuelAt: a janela de abastecimento continua contada
                // a partir do primeiro salto. Assim uma oscilacao lenta do medidor,
                // minutos depois, nao vira "abastecimento em andamento" para sempre.
                Log.w(TAG, "[TANQUE] abastecimento em andamento: " + percent
                        + "% (~" + round1(open.litersAdded) + " L no total)");
            } else {
                state.lastFuelPercent = percent;
                if (open == null) {
                    // Estado antigo sem tanque aberto (ex.: historico limpo): recomeca
                    // daqui, sem inventar um trecho para tras.
                    open = newTank(now, percent, odometer, rangeKm);
                    state.tanks.add(open);
                    armRangeCapture(now);
                } else {
                    open.currentFuelPercent = percent;
                    if (open.startRangeKm <= 0 && rangeKm > 0) {
                        open.startRangeKm = rangeKm;
                    }
                    // Odometro indisponivel na abertura do tanque: aproveita a primeira
                    // leitura valida, senao a distancia do trecho fica zerada para sempre.
                    if (open.startOdometer <= 0 && odometer > 0) {
                        open.startOdometer = odometer;
                    }
                }
            }

            save(context, state);
        }
    }

    /**
     * A autonomia chegou DEPOIS do nivel que abriu o tanque. A cache do
     * ServiceManager pode estar com a autonomia anterior ao abastecimento quando o
     * nivel e processado primeiro; dentro da janela, so aceita um valor MAIOR que o
     * ja registrado — isso corrige a leitura velha sem trocar a autonomia do
     * abastecimento por uma ja gasta pelo uso.
     */
    public static void onRangeSample(Context context, int rangeKm, long now) {
        if (rangeKm <= 0 || now > rangeCaptureUntil) {
            return;
        }
        synchronized (LOCK) {
            State state = loadState(context);
            Tank open = state.openTank();
            if (open == null || now - open.startedAt > RANGE_CAPTURE_WINDOW_MS) {
                return;
            }
            if (open.startRangeKm > 0 && rangeKm <= open.startRangeKm) {
                return;
            }
            open.startRangeKm = rangeKm;
            // Nao desarma a janela: a autonomia tambem pode subir em passos; cada
            // valor maior corrige o anterior ate a janela de 2 min fechar sozinha.
            save(context, state);
            Log.w(TAG, "[TANQUE] autonomia inicial capturada: " + rangeKm + " km");
        }
    }

    /** Define/edita o posto do abastecimento que abriu o tanque. */
    public static boolean setStation(Context context, long startedAt, String station) {
        synchronized (LOCK) {
            State state = loadState(context);
            Tank found = null;
            for (Tank tank : state.tanks) {
                if (tank != null && tank.startedAt == startedAt) {
                    found = tank;
                    break;
                }
            }
            if (found == null) {
                return false;
            }
            String cleaned = normalizeStation(station);
            // Reaproveita a grafia ja usada em outro tanque. Sem isso, "Shell Centro"
            // e "shell centro" viram postos diferentes no comparativo.
            if (!cleaned.isEmpty()) {
                for (Tank tank : state.tanks) {
                    if (tank == null || tank == found || tank.station == null) {
                        continue;
                    }
                    String existing = tank.station.trim();
                    if (!existing.isEmpty() && existing.equalsIgnoreCase(cleaned)) {
                        cleaned = existing;
                        break;
                    }
                }
            }
            found.station = cleaned;
            save(context, state);
            Log.w(TAG, "[TANQUE] posto definido: " + (found.station.isEmpty() ? "(vazio)" : found.station));
            return true;
        }
    }

    /** Apaga o historico inteiro. A proxima leitura de nivel recomeca o acompanhamento. */
    public static void clear(Context context) {
        synchronized (LOCK) {
            rangeCaptureUntil = 0L;
            File f = file(context);
            if (f.exists() && !f.delete()) {
                Log.w(TAG, "Nao consegui apagar o historico de tanques");
            }
            File tmp = tmpFile(context);
            if (tmp.exists() && !tmp.delete()) {
                Log.w(TAG, "Nao consegui apagar o temporario de tanques");
            }
        }
    }

    private static boolean isValidPercent(double percent) {
        // 0 fica de fora de proposito: ver comentario em onFuelSample.
        return !Double.isNaN(percent) && !Double.isInfinite(percent)
                && percent > 0.0 && percent <= 100.0;
    }

    private static Tank newTank(long now, double percent, int odometer, int rangeKm) {
        Tank tank = new Tank();
        tank.startedAt = now;
        tank.endedAt = 0L;
        tank.startOdometer = odometer;
        tank.startFuelPercent = percent;
        tank.currentFuelPercent = percent;
        tank.startRangeKm = Math.max(0, rangeKm);
        tank.station = "";
        return tank;
    }

    private static void closeTank(Tank tank, long now, int odometer, double endPercent,
                                  int endRangeKm) {
        tank.endedAt = now;
        tank.endOdometer = odometer;
        tank.endFuelPercent = endPercent;
        tank.currentFuelPercent = endPercent;
        tank.endRangeKm = Math.max(0, endRangeKm);
        tank.distanceKm = (tank.startOdometer > 0 && odometer >= tank.startOdometer)
                ? odometer - tank.startOdometer
                : 0.0;

        double liters = (tank.startFuelPercent - endPercent) * TANK_CAPACITY_LITERS / 100.0;
        if (liters > 0.05) {
            tank.litersUsed = round2(liters);
            tank.kmPerLiter = (tank.distanceKm > 0.0)
                    ? round2(tank.distanceKm / liters)
                    : null;
        } else {
            tank.litersUsed = null;
            tank.kmPerLiter = null;
        }
    }

    /**
     * Sempre arma a janela: a autonomia passada junto com o nivel pode ter vindo da
     * cache antes do abastecimento, e o valor novo chega poucos instantes depois.
     * Um valor menor que o atual nunca substitui o do inicio do tanque.
     */
    private static void armRangeCapture(long now) {
        rangeCaptureUntil = now + RANGE_CAPTURE_WINDOW_MS;
    }

    private static String normalizeStation(String station) {
        if (station == null) {
            return "";
        }
        String trimmed = station.trim();
        if (trimmed.length() > 60) {
            trimmed = trimmed.substring(0, 60);
        }
        return trimmed;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void save(Context context, State state) {
        while (state.tanks.size() > MAX_TANKS) {
            state.tanks.remove(0);
        }
        Gson gson = new Gson();
        File tmp = tmpFile(context);
        File dst = file(context);
        try (FileWriter writer = new FileWriter(tmp)) {
            gson.toJson(state, writer);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao escrever o historico de tanques", e);
            if (tmp.exists() && !tmp.delete()) {
                Log.w(TAG, "Nao consegui apagar o arquivo temporario de tanques");
            }
            return;
        }
        if (!tmp.renameTo(dst)) {
            if (dst.exists() && dst.delete() && tmp.renameTo(dst)) {
                return;
            }
            Log.e(TAG, "Nao consegui promover o arquivo temporario de tanques");
        }
    }
}
