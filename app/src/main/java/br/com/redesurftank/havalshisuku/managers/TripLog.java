package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Historico de viagens: uma linha por ciclo liga->desliga, gravada quando o carro desliga.
 *
 * Fica num JSON em getFilesDir() seguindo o mesmo caminho do <userId>.settings.json do
 * ServiceManager (Gson + File), e nao em SharedPreferences: e uma lista que cresce, e
 * SharedPreferences nao e lugar de dado que cresce.
 *
 * GRAVACAO ATOMICA, e isso nao e preciosismo: a central corta a energia de repente — e
 * literalmente por isso que existem as chaves SHUTDOWN_DRYING_PENDING/STARTED_AT no
 * SharedPreferencesKeys. Escrever direto no arquivo final deixaria um JSON truncado no
 * disco se a energia caisse no meio da escrita, e o historico INTEIRO se perderia. Com
 * tmp + rename, o pior caso e perder a ultima viagem; o que ja estava gravado sobrevive.
 */
public final class TripLog {
    private static final String TAG = "TripLog";
    private static final String FILE_NAME = "trips.json";
    private static final String TMP_NAME = "trips.json.tmp";

    /** Teto do historico. ~500 viagens dao ~50 KB — o mais antigo sai quando estoura. */
    public static final int MAX_TRIPS = 500;

    private TripLog() {
    }

    /**
     * Uma viagem. Os campos sao simples de proposito: e o Gson que escreve e le, sem
     * adaptador proprio — assim o arquivo continua legivel a mao no run-as/adb.
     */
    public static final class Trip {
        public long startedAt;
        public long endedAt;
        public double distanceKm;
        /**
         * null = o carro NAO informou o consumo desta viagem. Fica null em vez de 0.0
         * de proposito: um zero desenharia um ponto no grafico e uma linha na tabela
         * com cara de medicao real, e nao e. A tabela mostra "—" neste caso.
         */
        public Double kmPerLiter;
        /**
         * true = a distancia veio do delta do odometro total, nao da chave de viagem.
         * Fica gravado para dar para distinguir, depois, uma viagem medida de uma
         * derivada — e para saber se a sonda do H6 respondeu.
         */
        public boolean distanceFromOdometer;

        public Trip() {
        }

        public Trip(long startedAt, long endedAt, double distanceKm, Double kmPerLiter,
                    boolean distanceFromOdometer) {
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.distanceKm = distanceKm;
            this.kmPerLiter = kmPerLiter;
            this.distanceFromOdometer = distanceFromOdometer;
        }
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    private static File tmpFile(Context context) {
        return new File(context.getFilesDir(), TMP_NAME);
    }

    /** Nunca lanca e nunca devolve null: arquivo ausente/corrompido vira lista vazia. */
    public static List<Trip> load(Context context) {
        List<Trip> trips = new ArrayList<>();
        File f = file(context);
        if (!f.exists()) {
            return trips;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<Trip>>() {
        }.getType();
        try (FileReader reader = new FileReader(f)) {
            List<Trip> read = gson.fromJson(reader, type);
            if (read != null) {
                // Um arquivo de uma versao futura pode trazer entradas nulas; a UI
                // nao deve ter que se defender disso.
                for (Trip t : read) {
                    if (t != null) {
                        trips.add(t);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Falha ao ler o historico de viagens; comecando vazio", e);
            return new ArrayList<>();
        }
        return trips;
    }

    /**
     * Acrescenta uma viagem e grava. SINCRONO de proposito: e chamado no desligamento,
     * e o dado precisa estar no disco antes de a energia cair.
     */
    public static void append(Context context, Trip trip) {
        if (trip == null) {
            return;
        }
        List<Trip> trips = load(context);
        trips.add(trip);
        while (trips.size() > MAX_TRIPS) {
            trips.remove(0);
        }
        save(context, trips);
    }

    private static void save(Context context, List<Trip> trips) {
        Gson gson = new Gson();
        File tmp = tmpFile(context);
        File dst = file(context);
        try (FileWriter writer = new FileWriter(tmp)) {
            gson.toJson(trips, writer);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao escrever o historico de viagens", e);
            // Nao deixa um tmp pela metade para tras: o proximo append tentaria
            // sobrescreve-lo de qualquer forma, mas um tmp orfao confunde o run-as.
            if (tmp.exists() && !tmp.delete()) {
                Log.w(TAG, "Nao consegui apagar o arquivo temporario de viagens");
            }
            return;
        }
        if (!tmp.renameTo(dst)) {
            // renameTo nao sobrescreve em alguns sistemas de arquivos: apagar o
            // destino e tentar de novo e mais seguro que ficar com o historico velho
            // em silencio enquanto o novo se perde no tmp.
            if (dst.exists() && dst.delete() && tmp.renameTo(dst)) {
                return;
            }
            Log.e(TAG, "Nao consegui promover o arquivo temporario de viagens");
        }
    }

    /** Apaga o historico. Usado pelo botao "Limpar historico" antes de uma viagem de teste. */
    public static void clear(Context context) {
        File f = file(context);
        if (f.exists() && !f.delete()) {
            Log.w(TAG, "Nao consegui apagar o historico de viagens");
        }
        File tmp = tmpFile(context);
        if (tmp.exists() && !tmp.delete()) {
            Log.w(TAG, "Nao consegui apagar o temporario de viagens");
        }
    }
}
