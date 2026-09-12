package br.com.redesurftank.havalshisuku.managers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

import com.autolink.cluster.ClusterMsgData;
import com.autolink.clusterservice.IClusterCallback;
import com.autolink.clusterservice.IClusterService;
import com.beantechs.inputservice.IInputListener;
import com.beantechs.inputservice.IInputService;
import com.beantechs.intelligentvehiclecontrol.IIntelligentVehicleControlService;
import com.beantechs.intelligentvehiclecontrol.sdk.IListener;
import com.beantechs.voice.adapter.IBinderPool;
import com.beantechs.voice.adapter.IDvr;
import com.beantechs.voice.adapter.IVehicle;
import com.beantechs.voice.adapter.IVehicleModel;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.listeners.IDataChanged;
import br.com.redesurftank.havalshisuku.listeners.IServiceManagerEvent;
import br.com.redesurftank.havalshisuku.models.CarConstants;
import br.com.redesurftank.havalshisuku.models.CarInfo;
import br.com.redesurftank.havalshisuku.models.ServiceManagerEventType;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;
import br.com.redesurftank.havalshisuku.models.SteeringWheelCustomActionType;
import br.com.redesurftank.havalshisuku.models.MainUiManager;
import br.com.redesurftank.havalshisuku.models.screens.Screen;
import br.com.redesurftank.havalshisuku.utils.FridaUtils;
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;

@SuppressLint("PrivateApi")
public class ServiceManager {
    private static final String TAG = "ServiceManager";
    public static final CarConstants[] DEFAULT_KEYS = {
            CarConstants.CAR_BASIC_ACCUMULATED_DIRVETIME,
            CarConstants.CAR_BASIC_GEAR_STATUS,
            CarConstants.CAR_BASIC_DOOR_STATUS,
            CarConstants.CAR_BASIC_DOOR_LOCK_STATUS,
            CarConstants.CAR_BASIC_DRIVING_READY_STATE,
            CarConstants.CAR_BASIC_INSIDE_TEMP,
            CarConstants.CAR_BASIC_MAINTENANCE_WARNING,
            CarConstants.CAR_BASIC_MAINTENANCE_WARNING_MILEAGE,
            CarConstants.CAR_BASIC_OUTSIDE_TEMP,
            CarConstants.CAR_BASIC_STEERING_RESET_REMIND_ENABLE,
            CarConstants.CAR_BASIC_STEERING_WHEEL_ANGLE,
            CarConstants.CAR_BASIC_TOTAL_ODOMETER,
            CarConstants.CAR_BASIC_VEHICLE_SPEED,
            CarConstants.CAR_BASIC_WINDOW_STATUS,
            CarConstants.CAR_DMS_WORK_STATE,
            CarConstants.CAR_EV_SETTING_AVAS_CONFIG,
            CarConstants.CAR_EV_SETTING_AVAS_ENABLE,
            CarConstants.CAR_EV_INFO_CUR_BATTERY_POWER_PERCENTAGE,
            CarConstants.CAR_EV_INFO_ENERGY_OUTPUT_PERCENTAGE,
            CarConstants.CAR_EV_INFO_POWER_BATTERY_VOLTAGE,
            CarConstants.CAR_FRS_SETTING_DISTRACTION_DETECTION_ENABLE,
            CarConstants.CAR_HVAC_AC_ENABLE,
            CarConstants.CAR_HVAC_ANION_ENABLE,
            CarConstants.CAR_HVAC_BLOWER_MODE,
            CarConstants.CAR_HVAC_CYCLE_MODE,
            CarConstants.CAR_HVAC_DRIVER_TEMPERATURE,
            CarConstants.CAR_HVAC_FAN_SPEED,
            CarConstants.CAR_HVAC_FRONT_DEFROST_ENABLE,
            CarConstants.CAR_HVAC_PASS_TEMPERATURE,
            CarConstants.CAR_HVAC_POWER_MODE,
            CarConstants.CAR_HVAC_SYNC_ENABLE,
            CarConstants.CAR_HVAC_AUTO_ENABLE,
            CarConstants.CAR_IPK_SETTING_BRIGHTNESS_CONFIG,
            CarConstants.SYS_AVM_AUTO_PREVIEW_ENABLE,
            CarConstants.SYS_AVM_PREVIEW_STATUS,
            CarConstants.SYS_SETTINGS_AUDIO_MEDIA_VOLUME,
            CarConstants.SYS_SETTINGS_DISPLAY_BACKLIGHT_STATE,
            CarConstants.SYS_SETTINGS_DISPLAY_BRIGHTNESS_LEVEL,
            CarConstants.CAR_DRIVE_SETTING_OUTSIDE_VIEW_MIRROR_FOLD_STATE,
            CarConstants.CAR_BASIC_ENGINE_STATE,
            CarConstants.CAR_DRIVE_SETTING_ESP_ENABLE,
            CarConstants.CAR_EV_SETTING_POWER_MODEL_CONFIG,
            CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE,
            CarConstants.CAR_DRIVE_SETTING_STEERING_WHEEL_ASSIST_MODE,
            CarConstants.CAR_EV_SETTING_ENERGY_RECOVERY_LEVEL,
            CarConstants.CAR_EV_INFO_FUEL_CONSUME_INFO,
            CarConstants.CAR_EV_INFO_CYCLE_FUEL_CONSUME_INFO,
            CarConstants.CAR_CONFIGURE_PEDAL_CONTROL_ENABLE,
            CarConstants.CAR_BASIC_INSTANT_FUEL_CONSUMPTION,
    };

    private static final CarConstants[] KEYS_TO_SAVE = {
            CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE,
            CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE_MEMORY,
            CarConstants.CAR_DRIVE_SETTING_DST_ENABLE,
            CarConstants.CAR_DRIVE_SETTING_ESP_ENABLE,
            CarConstants.CAR_DRIVE_SETTING_FATIGUE_MONITOR_STATE,
            CarConstants.CAR_DRIVE_SETTING_OUTSIDE_VIEW_MIRROR_ASTERN_MODE,
            CarConstants.CAR_DRIVE_SETTING_STEERING_WHEEL_ASSIST_MODE,
            CarConstants.CAR_EV_SETTING_ENERGY_RECOVERY_LEVEL,
            CarConstants.CAR_CONFIGURE_PEDAL_CONTROL_ENABLE,
            CarConstants.CAR_HUD_SETTING_ADAS_DISPLAY_ENABLE,
            CarConstants.CAR_HUD_SETTING_ENABLE_STATE,
            CarConstants.CAR_HUD_SETTING_HEIGHT_CONFIG,
            CarConstants.CAR_HUD_SETTING_NAVIGATION_DISPLAY_ENABLE,
            CarConstants.CAR_HUD_SETTING_ROTATION_ANGLE,
            CarConstants.CAR_HUD_SETTING_ROTATION_DIRECTION,
            CarConstants.CAR_HUD_SETTING_SNOW_MODE_ENABLE,
            CarConstants.CAR_HUD_SETTING_VIBRATION_CORRN_ENABLE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_CRUISING_SPEED_LIMIT,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_EAS_ASSIST_SENSITIVITY,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_EAS_CHANGE_LANE_ASSIST_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_EAS_HIGHWAY_ASSIST_SYSTEM_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_EAS_WARNING_WAY,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_FAS_AUTO_EMERGENCY_TURN,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_FAS_EARLY_WARNING_MODE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_FAS_EARLY_WARNING_SENSITIVITY,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_FAS_FRONT_CROSS_LATERAL_BRAKE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_FAS_FRONT_CROSS_LATERAL_WRANING,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_FAS_INTERSECTION_ASSIST_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_FAS_PCS_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_FAS_PPS_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_LAS_EARLY_WARNING_SENSITIVITY,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_LAS_ELK_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_LAS_ENABLE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_LAS_LCA_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_LAS_LDW_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_LAS_LKA_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_LAS_TSI_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_OVER_SPEED_ALARM_SENSITIVITY,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_OVER_SPEED_WARNING_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_SMART_DODGE_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_SRAS_ALA_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_SRAS_DOOR_OPEN_WARNING,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_SRAS_RCW_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_SRAS_RSA_RSB_STATE,
            CarConstants.CAR_INTELLIGENT_DRIVING_SETTING_SRAS_RSA_RSB_WARNING_STATE,
    };
    private static ServiceManager instance;
    private final List<IDataChanged> dataChangedListeners;
    private final List<IServiceManagerEvent> serviceManagerEventListeners;
    private final Map<String, String> dataCache;
    private SharedPreferences sharedPreferences;
    private Boolean closeWindowDueToeSpeed = false;
    private Boolean closeSunroofDueToeSpeed = false;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private IListener.Stub listener;
    private IInputListener.Stub inputListener;
    private IClusterCallback.Stub clusterCallback;
    private boolean servicesInitialized = false;
    private boolean isFridaInitialized = false;
    private final List<Runnable> pendingTasks = new ArrayList<>();
    private static long timeBootReceived;
    private long timeStartInitialization;
    private long timeInitialized;
    private CarInfo carInfo;
    private IIntelligentVehicleControlService controlService;
    private IVehicle vehicle;
    private IDvr dvr;
    private boolean delayNextAVM = false;
    private IVehicleModel vehicleModel;
    private IClusterService clusterService;
    private ServiceConnection clusterServiceConnection;
    private IInputService inputService;
    private ServiceConnection inputServiceConnection;
    private IConnectivityManager connectivityManager;
    private boolean isClusterHeartbeatRunning = false;
    private int clusterHeartBeatCount = 0;
    private int clusterCardView = 0;
    private final Map<String, String> previousAcState = new HashMap<>();
    private boolean isMaxAcActive = false;
    private Runnable maxAcTimeoutRunnable;
    // v1.8: janela de confirmacao de 3s (ignicao liga com o sensor ainda estabilizando)
    private Runnable maxAcConfirmRunnable;
    // v1.8: histerese de falsos positivos nos primeiros 60s apos disparo
    private long maxAcActivatedAt = 0L;
    // ---------------------------------------------------------------------------
    // v2.7: secagem. Um unico motor, dois sabores (ver DryingKind).
    //
    // INVARIANTE: todo o estado de secagem e lido e escrito APENAS dentro de
    // synchronized (dryingLock), e NENHUMA chamada de binder (getUpdatedData /
    // updateData) nem callback de listener roda com o lock na mao. Os acessores
    // publicos leem os espelhos volatile — ver publishDryingFlags() para o motivo.
    // ---------------------------------------------------------------------------
    private final Object dryingLock = new Object();
    private final Map<String, String> dryingSnapshot = new HashMap<>();  // sob dryingLock
    private DryingKind dryingKind = DryingKind.NONE;                      // sob dryingLock
    private int dryingRemainingSeconds = 0;                               // sob dryingLock
    private long dryingDeadlineElapsed = 0L;                              // sob dryingLock
    private Runnable dryingTickRunnable;                                  // sob dryingLock
    // Espelho do dryingKind para leitura sem lock, num UNICO volatile. Dois booleanos
    // separados deixariam um leitor ver "ativo=true, desligamento=false" — e o ramo OFF
    // trata esse par como "e a manual, pode cancelar", matando um ciclo de desligamento
    // recem-reservado. Uma referencia de enum e escrita de uma vez so: nunca ha par
    // inconsistente.
    private volatile DryingKind dryingKindFlag = DryingKind.NONE;

    private enum DryingKind {
        NONE,
        /** Volante / menu do cluster. Cancelada, restaura o power anterior. */
        MANUAL,
        /** Automatica no desligar. NUNCA restaura o power anterior: o carro esta desligado. */
        SHUTDOWN
    }

    private static final int DRYING_MODE_DURATION_SECONDS = 120;
    private static final int SHUTDOWN_DRYING_DURATION_DEFAULT = 60;
    private static final int SHUTDOWN_DRYING_MIN_SECONDS = 15;
    private static final int SHUTDOWN_DRYING_MAX_SECONDS = 300;
    private static final long SEAT_VENT_SUPPRESS_GRACE_MS = 15_000L;

    // v2.7: deteccao de borda do ready-state. Tocado pelo pool do binder E pela main
    // thread (DispatchAllDatasReceiver) — synchronized curto, so memoria, zero I/O.
    private final Object readyStateLock = new Object();
    private String lastReadyStateValue = null;
    // true = ja vimos uma ignicao ligada NESTA vida do processo. Sem isso, o
    // dispatchAllData() do init (com o carro parado) cairia no ramo OFF e dispararia
    // uma secagem fantasma a cada inicializacao do servico.
    private volatile boolean sawLiveIgnitionThisProcess = false;
    // true = o ciclo de secagem DESTA ignicao ja foi gasto. O barramento pode repetir
    // o OFF a vontade: nada reinicia ate ver um ready-state ON de verdade.
    private volatile boolean shutdownDryingConsumedThisIgnition = false;
    // true = o compressor rodou nesta viagem. So vale como gatilho quem molhou o
    // evaporador; sem compressor nao ha agua para secar.
    private volatile boolean acCompressorRanThisIgnition = false;
    // Janela em que um POWER=1 vindo da secagem NAO deve ligar a ventilacao do banco.
    private volatile long suppressSeatVentBoostUntilElapsed = 0L;
    // Diagnostico H3: so para logar a transicao do engine_state uma vez.
    private volatile String lastEngineStateValue = null;
    // Ciclo abandonado no restart DESTE processo com o perfil ja na rua: guarda o snapshot
    // para reaplicar quando a conexao voltar. De proposito NAO vai para o disco: em disco,
    // uma perda de energia de verdade (o caso que estamos medindo) seria indistinguivel de
    // um restart do servico, e ai a correcao apagaria a evidencia do H5. Em memoria = so o
    // caso que sabemos que aconteceu. Sob dryingLock.
    private Map<String, String> orphanedDryingSnapshot = null;
    private DryingKind orphanedDryingKind = DryingKind.NONE;

    // v1.9: reaplica os defaults de startup apos o modulo HVAC terminar de acordar
    private Runnable startupDefaultsRunnable;
    // v2.6: quantas reaplicacoes ainda cabem neste ciclo de ignicao (ver scheduleStartupDefaults).
    private int startupDefaultsRetriesLeft = 0;
    // v1.9: true = proximo ready-state ON deve aplicar os defaults (processo novo ou
    // apos um OFF real). Evita reaplicar no meio da viagem se o valor oscilar.
    private boolean applyStartupDefaultsOnNextReadyOn = true;


    private ServiceManager() {
        dataChangedListeners = new ArrayList<>();
        dataCache = new HashMap<>();
        serviceManagerEventListeners = new ArrayList<>();
    }

    public static synchronized ServiceManager getInstance() {
        if (instance == null) {
            instance = new ServiceManager();
        }
        return instance;
    }

    public synchronized boolean initializeServices(Context context) {
        try {
            if (controlService != null) {
                if (controlService.asBinder().isBinderAlive()) {
                    try {
                        controlService.unRegisterDataChangedListener(context.getPackageName(), listener);
                        controlService = null;  // Disconnect binder
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            if (vehicle != null) {
                vehicle = null;  // Disconnect binder
            }
            if (dvr != null) {
                dvr = null;  // Disconnect binder
            }
            if (vehicleModel != null) {
                vehicleModel = null;  // Disconnect binder
            }
            if (clusterService != null) {
                if (clusterService.asBinder().isBinderAlive()) {
                    try {
                        clusterService.unregisterCallback(clusterCallback);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (clusterServiceConnection != null) {
                    context.unbindService(clusterServiceConnection);
                }
                clusterService = null;  // Disconnect binder
            }
            if (inputService != null) {
                if (inputService.asBinder().isBinderAlive()) {
                    try {
                        inputService.unregisterKeyEventListener(new int[]{-1}, inputListener);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (inputServiceConnection != null) {
                    context.unbindService(inputServiceConnection);
                }
                inputService = null;  // Disconnect binder
            }
            if (handlerThread != null && handlerThread.isAlive()) {
                handlerThread.quitSafely();
                handlerThread = null;
                backgroundHandler = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during service cleanup", e);
        }
        timeStartInitialization = SystemClock.uptimeMillis();
        Log.w(TAG, "Initializing services with Shizuku");
        sharedPreferences = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
        handlerThread = new HandlerThread("ServiceManagerHandlerThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
        // v2.7: o handler acima ACABOU de ser recriado (quitSafely no cleanup, que roda
        // quando o binder do Shizuku morre sem o processo morrer). O tick da secagem
        // antiga morava no looper velho e nunca mais rodaria: a secagem ficaria "ativa"
        // para a UI, sem ninguem para encerra-la. Zera o motor para o estado nao mentir.
        resetDryingStateIfOrphaned();
        // v2.7: e se a central perdeu energia no meio de um ciclo de desligamento, este
        // e o primeiro lugar onde da para contar isso (H5).
        reportInterruptedShutdownDrying("initializeServices");
        if (!Shizuku.pingBinder()) {
            Log.e(TAG, "Shizuku not available");
            return false;
        }

        try {
            // v2.3: o serviço pode ainda não estar registrado no ServiceManager logo
            // após o boot — getSystemService() devolve null e o wrapper/requireNonNull
            // anterior estourava NPE no main thread (FATAL), derrubando o processo no
            // meio da inicialização. Agora falha limpa e o ForegroundService.restart()
            // tenta de novo em 1s.
            IBinder rawControlBinder = getSystemService("com.beantechs.intelligentvehiclecontrol");
            if (rawControlBinder == null) {
                Log.e(TAG, "IntelligentVehicleControlService not registered yet");
                return false;
            }
            IBinder controlBinder = new ShizukuBinderWrapper(rawControlBinder);
            if (!controlBinder.pingBinder()) {
                Log.e(TAG, "IntelligentVehicleControlService binder not alive");
                return false;
            }
            controlService = IIntelligentVehicleControlService.Stub.asInterface(controlBinder);
            IBinder rawVoiceBinder = getSystemService("com.beantechs.voice.adapter.VoiceAdapterService");
            if (rawVoiceBinder == null) {
                Log.e(TAG, "VoiceAdapterService not registered yet");
                return false;
            }
            IBinder poolBinder = new ShizukuBinderWrapper(rawVoiceBinder);
            if (!poolBinder.pingBinder()) {
                Log.e(TAG, "IBinderPool binder not alive");
                return false;
            }
            IBinderPool pool = IBinderPool.Stub.asInterface(poolBinder);
            IBinder vehicleBinder = pool.queryBinder(6);
            vehicle = IVehicle.Stub.asInterface(new ShizukuBinderWrapper(vehicleBinder));
            IBinder dvrBinder = pool.queryBinder(8);
            dvr = IDvr.Stub.asInterface(new ShizukuBinderWrapper(dvrBinder));
            IBinder vehicleModelBinder = pool.queryBinder(13);
            vehicleModel = IVehicleModel.Stub.asInterface(new ShizukuBinderWrapper(vehicleModelBinder));
            Intent clusterIntent = new Intent();
            clusterIntent.setComponent(new ComponentName("com.autolink.clusterservice", "com.autolink.clusterservice.ClusterService"));
            clusterCallback = new IClusterCallback.Stub() {
                @Override
                public void callbackMsg(int msgId, ClusterMsgData data) {
                    if (msgId == 133) {
                        int whichCard = data.getIntValue();
                        clusterCardView = whichCard;
                        dispatchServiceManagerEvent(ServiceManagerEventType.CLUSTER_CARD_CHANGED, clusterCardView);
                        if (whichCard == 1) {
                            MainUiManager.getInstance().updateScreen();
                        }
                        Log.w(TAG, "Cluster card changed: " + whichCard);
                    } else if (msgId == 134) {
                        if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.getKey(), false)) {
                            int intValue = data.getIntValue();
                            if (intValue == 2) {
                                Log.w(TAG, "Cluster heartbeat reset requested");
                                sendHeartBeatToCluster();
                                startClusterHeartbeat();
                            }
                        }
                    } else if (msgId == 135) {
                        if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.getKey(), false)) {
                            int intValue = data.getIntValue();
                            if (intValue == 1) {
                                Log.w(TAG, "Cluster ready to show");
                                sendClusterIntMsg(135, 1);
                            } else if (intValue == 2) {
                                Log.w(TAG, "Cluster ready to hide");
                                sendClusterIntMsg(135, 2);
                            } else if (intValue == 3 || intValue == 4) {
                                boolean show = (intValue == 3);
                                Log.w(TAG, "Cluster show or hide card: " + show);
                            }
                        }
                    }
                }
            };
            clusterServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    clusterService = IClusterService.Stub.asInterface(service);
                    try {
                        clusterService.registerCallback(clusterCallback);
                    } catch (Exception e) {
                        Log.e(TAG, "Error registering cluster callback", e);
                    }
                    if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.getKey(), false)) {
                        startClusterHeartbeat();
                    }
                    Log.w(TAG, "ClusterService connected successfully");
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    clusterService = null;
                    Log.w(TAG, "ClusterService disconnected");
                }
            };


            // Initialize MainUiManager and respective menu management controls
            MainUiManager.getInstance();

            context.bindService(clusterIntent, clusterServiceConnection, Context.BIND_AUTO_CREATE);
            Intent inputIntent = new Intent("com.beantechs.inputservice.service_init");
            inputIntent.setPackage("com.beantechs.inputservice");
            inputListener = new IInputListener.Stub() {
                @Override
                public void dispatchKeyEvent(KeyEvent keyEvent) {
                    if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS.getKey(), false)) {
                        Log.w(TAG, "Key event received: " + keyEvent);
                        switch (keyEvent.getKeyCode()) {
                            case 517://button 1
                                handleSteeringWheelCustomButton(sharedPreferences.getString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION.getKey(), SteeringWheelCustomActionType.DEFAULT.name()), 1);
                                break;
                            case 1031://button 2
                                handleSteeringWheelCustomButton(sharedPreferences.getString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION.getKey(), SteeringWheelCustomActionType.DEFAULT.name()), 2);
                                break;
                        }
                    }
                    if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_CUSTOM_MENU.getKey(), false)) {
                        if (clusterCardView == 1) {
                            Screen.Key key = null;
                            switch (keyEvent.getKeyCode()) {
                                case 1024: key = Screen.Key.UP; break;
                                case 1025: key = Screen.Key.DOWN; break;
                                case 1028: key = Screen.Key.ENTER; break;
                                case 1029: key = Screen.Key.HOME; break;
                                case 1030: key = Screen.Key.BACK; break;
                                case 1033: key = Screen.Key.UP_LONG; break;
                                case 1034: key = Screen.Key.DOWN_LONG; break;
                                case 1037: key = Screen.Key.ENTER_LONG; break;
                                case 1039: key = Screen.Key.BACK_LONG; break;
                            }
                            if (key != null) MainUiManager.getInstance().handleGeneralKeyEvents(key);
                        }
                    }
                }
            };
            inputServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.w(TAG, "InputService connected");
                    inputService = IInputService.Stub.asInterface(service);
                    try {
                        inputService.registerKeyEventListener(new int[]{-1}, inputListener);
                        Log.w(TAG, "InputService connected and listener registered successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error registering key event listener", e);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    inputService = null;
                    Log.w(TAG, "InputService disconnected");
                }
            };
            context.bindService(inputIntent, inputServiceConnection, Context.BIND_AUTO_CREATE);
            Log.w(TAG, "Services bound successfully");
            listener = new IListener.Stub() {
                @Override
                public void onDataChanged(String key, String value) {
                    OnDataChanged(key, value);
                }
            };
            ShizukuUtils.runCommandAndGetOutput(new String[]{
                    "settings", "put", "secure", "enabled_accessibility_services",
                    "br.com.redesurftank.havalshisuku/.services.AccessibilityService"
            });
            ShizukuUtils.runCommandAndGetOutput(new String[]{
                    "settings", "put", "secure", "accessibility_enabled", "1"
            });
            //enable write secure settings
            ShizukuUtils.runCommandAndGetOutput(new String[]{
                    "pm", "grant", context.getPackageName(), "android.permission.WRITE_SECURE_SETTINGS"
            });
            controlService.registerDataChangedListener(context.getPackageName(), listener);
            Log.w(TAG, "Listener registered successfully");
            controlService.addListenerKey(App.getContext().getPackageName(), getCombinedKeys());
            Log.w(TAG, "Listener keys added successfully");
            IBinder connectivityBinder = new ShizukuBinderWrapper(getSystemService(Context.CONNECTIVITY_SERVICE));
            connectivityManager = IConnectivityManager.Stub.asInterface(connectivityBinder);
            IntentFilter bluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            bluetoothFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() == null) return;
                    String action = intent.getAction();
                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) || action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                        var state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothAdapter.STATE_ON) {
                            var drivingReady = getUpdatedData(CarConstants.CAR_BASIC_DRIVING_READY_STATE.getValue());
                            boolean disableBluetoothWhenPowerOff = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF.getKey(), false);
                            if ((drivingReady.equals("-1") || drivingReady.equals("0")) && disableBluetoothWhenPowerOff) {
                                disableBluetooth();
                            }
                        }
                    }
                }
            }, bluetoothFilter);
            IntentFilter wifiFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() == null) return;
                    String action = intent.getAction();
                    if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                        int state = intent.getIntExtra("wifi_state", 0);
                        if (state == 13) { // WIFI_AP_STATE_ENABLED
                            Log.w(TAG, "Wi-Fi Hotspot turned on");
                            var drivingReady = getUpdatedData(CarConstants.CAR_BASIC_DRIVING_READY_STATE.getValue());
                            boolean disableHotspotWhenPowerOff = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF.getKey(), false);
                            if ((drivingReady.equals("-1") || drivingReady.equals("0")) && disableHotspotWhenPowerOff) {
                                disableWifiTether();
                            }
                        }
                    }
                }
            }, wifiFilter);
            // v2.7: aqui a conexao com o vehicle control esta viva de novo, entao os comandos
            // tem para onde ir. Desfaz o ciclo que ficou orfao no restart — nada sai daqui se
            // nao houver um ciclo abandonado.
            repairOrphanedDrying();
            dispatchAllData();
            // v1.9: volume e AC padrao foram movidos para o handler de ready-state ON
            // (applyStartupDefaults) — o init so roda quando o servico sobe, entao com a
            // central viva entre ignicoes o padrao nunca era reaplicado. O dispatchAllData
            // acima dispara o ramo ON quando o servico inicia com o carro ja ligado.
            boolean isForceDisableMonitoring = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_MONITORING.getKey(), false);
            if (isForceDisableMonitoring) {
                setMonitoringEnabled(false);
                Log.w(TAG, "Distraction detection monitoring disabled by user preference");
            }
            boolean isForceDisableAVAS = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_AVAS.getKey(), false);
            if (isForceDisableAVAS) {
                setAvasEnabled(false);
                Log.w(TAG, "AVAS disabled by user preference");
            }
            if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_AUTO_BRIGHTNESS.getKey(), false)) {
                AutoBrightnessManager.Companion.getInstance().setEnabled(true);
            }
            if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOKS.getKey(), false)) {
                pendingTasks.add(this::initializeFrida);
            }
            if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON.getKey(), false) && getUpdatedData(CarConstants.CAR_HVAC_POWER_MODE.getValue()).equals("1")) {
                updateData(CarConstants.CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL.getValue(), "3");
            }

            ensureSteeringWheelButtonIntegration();
            ensureSystemApps();

            servicesInitialized = true;
            synchronized (pendingTasks) {
                for (Runnable task : pendingTasks) {
                    backgroundHandler.post(task);
                }
                pendingTasks.clear();
            }

            MainUiManager.getInstance().updateScreen();

            timeInitialized = SystemClock.uptimeMillis();
            Log.w(TAG, "Services initialized successfully");
            ProjectorManager.getInstance().initialize();
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Error during initialization", e);
            return false;
        }
    }

    public void ensureSteeringWheelButtonIntegration() {
        if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS.getKey(), false)) {
            var button1Action = sharedPreferences.getString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION.getKey(), SteeringWheelCustomActionType.DEFAULT.getKey());
            var button2Action = sharedPreferences.getString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION.getKey(), SteeringWheelCustomActionType.DEFAULT.getKey());
            Log.w(TAG, "Ensuring steering wheel button integration. Button 1 action: " + button1Action + ", Button 2 action: " + button2Action);
            if (button1Action.equals(SteeringWheelCustomActionType.DEFAULT.getKey())) {
                disableNativeSteeringWheelButton1();
            } else {
                enableSteeringWheelButton1Integration();
            }
            if (button2Action.equals(SteeringWheelCustomActionType.DEFAULT.getKey())) {
                disableNativeSteeringWheelButton2();
            } else {
                enableSteeringWheelButton2Integration();
            }
        } else {
            Log.w(TAG, "Steering wheel button integration disabled, restoring native functions");
            disableNativeSteeringWheelButton1();
            disableNativeSteeringWheelButton2();
        }

    }

    private void handleSteeringWheelCustomButton(String string, int button) {
        SteeringWheelCustomActionType action = SteeringWheelCustomActionType.Companion.fromKey(string);
        if (action == null || action == SteeringWheelCustomActionType.DEFAULT) {
            return;
        }
        switch (action) {
            case CHANGE_POWER_MODE:
                var carEvPowerMode = Integer.parseInt(getUpdatedData(CarConstants.CAR_EV_SETTING_POWER_MODEL_CONFIG.getValue()));
                Log.w(TAG, "Current EV Power Mode: " + carEvPowerMode);
                if (carEvPowerMode == 0) {
                    carEvPowerMode = 1;
                } else if (carEvPowerMode == 1) {
                    carEvPowerMode = 3;
                } else if (carEvPowerMode == 3) {
                    carEvPowerMode = 0;
                }
                updateData(CarConstants.CAR_EV_SETTING_POWER_MODEL_CONFIG.getValue(), String.valueOf(carEvPowerMode));
                Log.w(TAG, "New EV Power Mode: " + carEvPowerMode);
                break;
            case CHANGE_REGENERATION_LEVEL:
                var regenLevel = Integer.parseInt(getUpdatedData(CarConstants.CAR_EV_SETTING_ENERGY_RECOVERY_LEVEL.getValue()));
                Log.w(TAG, "Current Regeneration Level: " + regenLevel);
                //low 2
                //normal 0
                //high 1
                if (regenLevel == 0) {
                    regenLevel = 1;
                } else if (regenLevel == 1) {
                    regenLevel = 2;
                } else if (regenLevel == 2) {
                    regenLevel = 0;
                }
                updateData(CarConstants.CAR_EV_SETTING_ENERGY_RECOVERY_LEVEL.getValue(), String.valueOf(regenLevel));
                Log.w(TAG, "New Regeneration Level: " + regenLevel);
                break;
            case TOGGLE_ANION:
                var anionState = getUpdatedData(CarConstants.CAR_HVAC_ANION_ENABLE.getValue());
                if (anionState != null) {
                    boolean anion = anionState.equals("1");
                    anion = !anion;
                    updateData(CarConstants.CAR_HVAC_ANION_ENABLE.getValue(), anion ? "1" : "0");
                    Log.w(TAG, "Anion state changed to: " + anion);
                }
                break;
            /*case TOGGLE_ESP:
                var espState = getUpdatedData(CarConstants.CAR_DRIVE_SETTING_ESP_ENABLE.getValue());
                if (espState != null) {
                    boolean esp = espState.equals("1");
                    esp = !esp;
                    updateData(CarConstants.CAR_DRIVE_SETTING_ESP_ENABLE.getValue(), esp ? "1" : "0");
                    Log.w(TAG, "ESP state changed to: " + esp);
                }
                break;*/
            case TOGGLE_ONE_PEDAL_DRIVING:
                var onePedalState = getUpdatedData(CarConstants.CAR_CONFIGURE_PEDAL_CONTROL_ENABLE.getValue());
                if (onePedalState != null) {
                    boolean onePedal = onePedalState.equals("1");
                    onePedal = !onePedal;
                    updateData(CarConstants.CAR_CONFIGURE_PEDAL_CONTROL_ENABLE.getValue(), onePedal ? "1" : "0");
                    Log.w(TAG, "One Pedal Driving state changed to: " + onePedal);
                }
                break;
            case OPEN_APP:
                var packageName = sharedPreferences.getString(button == 1 ? SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1.getKey() : SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2.getKey(), "");
                if (!packageName.isEmpty()) {
                    Intent launchIntent = App.getContext().getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        App.getContext().startActivity(launchIntent);
                        Log.w(TAG, "Launching app: " + packageName);
                    } else {
                        Log.e(TAG, "App not found: " + packageName);
                    }
                }
                break;
            case TOGGLE_CAMERA_AVM:
                boolean cameraAVM = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_AVM_CAR_STOPPED.getKey(), false);
                cameraAVM = !cameraAVM;
                sharedPreferences.edit().putBoolean(SharedPreferencesKeys.DISABLE_AVM_CAR_STOPPED.getKey(), cameraAVM).apply();
                Log.w(TAG, "Camera AVM state changed to: " + cameraAVM);
                break;
            case OPEN_AVM_ONCE:
                try {
                    if (getData(CarConstants.SYS_AVM_PREVIEW_STATUS.getValue()).equals("0")) {
                        delayNextAVM = true;
                        dvr.setAVM(1);
                        Log.w(TAG, "Camera AVM temporarily triggered");
                    } else {
                        delayNextAVM = false;
                        dvr.setAVM(0);
                        Log.w(TAG, "Camera AVM closed");
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "Error to launch AVM camera");
                }
                break;
            case APPLY_DEFAULT_AC:
                applyDefaultAcMode();
                Log.w(TAG, "Steering wheel: modo padrão do A/C aplicado");
                break;
            case START_DRYING_MODE:
                // startDryingMode() ja e um toggle: cancela se houver secagem ativa.
                startDryingMode();
                Log.w(TAG, "Drying mode toggled via steering wheel button");
                break;
        }
    }

    public void enableSteeringWheelButton1Integration() {
        try {
            var currentConfig = ShizukuUtils.runCommandAndGetOutput(new String[]{"settings", "get", "system", "bean_sw_custom_key1_config"}).trim();
            Log.w(TAG, "Current steering wheel button 1 config: " + currentConfig);
            sharedPreferences.edit().putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION_ORIGINAL.getKey(), currentConfig).apply();
            ShizukuUtils.runCommandAndGetOutput(new String[]{"settings", "put", "system", "bean_sw_custom_key1_config", "99"});
        } catch (Exception e) {
            Log.e(TAG, "Error disabling native steering wheel custom buttons", e);
        }
    }

    public void enableSteeringWheelButton2Integration() {
        try {
            var currentConfig = ShizukuUtils.runCommandAndGetOutput(new String[]{"settings", "get", "system", "bean_sw_custom_key2_config"}).trim();
            Log.w(TAG, "Current steering wheel button 2 config: " + currentConfig);
            sharedPreferences.edit().putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION_ORIGINAL.getKey(), currentConfig).apply();
            ShizukuUtils.runCommandAndGetOutput(new String[]{"settings", "put", "system", "bean_sw_custom_key2_config", "99"});
        } catch (Exception e) {
            Log.e(TAG, "Error disabling native steering wheel custom buttons", e);
        }
    }

    public void disableNativeSteeringWheelButton1() {
        try {
            var originalConfig = sharedPreferences.getString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION_ORIGINAL.getKey(), "0");
            if (originalConfig.equals("99"))
                return;
            Log.w(TAG, "Restoring steering wheel button 1 config to: " + originalConfig);
            ShizukuUtils.runCommandAndGetOutput(new String[]{"settings", "put", "system", "bean_sw_custom_key1_config", originalConfig});
        } catch (Exception e) {
            Log.e(TAG, "Error restoring native steering wheel custom button 1", e);
        }
    }

    public void disableNativeSteeringWheelButton2() {
        try {
            var originalConfig = sharedPreferences.getString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION_ORIGINAL.getKey(), "0");
            if (originalConfig.equals("99"))
                return;
            Log.w(TAG, "Restoring steering wheel button 2 config to: " + originalConfig);
            ShizukuUtils.runCommandAndGetOutput(new String[]{"settings", "put", "system", "bean_sw_custom_key2_config", originalConfig});
        } catch (Exception e) {
            Log.e(TAG, "Error restoring native steering wheel custom button 2", e);
        }
    }

    private void sendClusterIntMsg(int type, int value) {
        if (clusterService == null) {
            Log.e(TAG, "ClusterService not initialized");
            return;
        }
        ClusterMsgData msg = new ClusterMsgData();
        msg.setIntValue(value);
        try {
            clusterService.setMsg(type, msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error sending message to cluster service", e);
        }
    }

    private void sendAndroidReadyToCluster() {
        try {
            var msg = new ClusterMsgData();
            msg.setIntValue(1);
            clusterService.setMsg(75, msg);
        } catch (Exception e) {
            Log.e(TAG, "Error setting cluster service message", e);
        }
    }

    public synchronized void startClusterHeartbeat() {
        if (isClusterHeartbeatRunning)
            return;
        isClusterHeartbeatRunning = true;
        sendAndroidReadyToCluster();
        backgroundHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.getKey(), false)) {
                    isClusterHeartbeatRunning = false;
                    return;
                }
                sendHeartBeatToCluster();
                backgroundHandler.postDelayed(this, 1000);

            }
        }, 1000);
    }

    private void sendHeartBeatToCluster() {
        if (clusterHeartBeatCount > 32767) {
            clusterHeartBeatCount = 0; // Reset to avoid overflow
        }
        var msg = new ClusterMsgData();
        msg.setIntValue(clusterHeartBeatCount++);
        try {
            clusterService.setMsg(134, msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error sending heartbeat to cluster service", e);
        }
    }

    public void dispatchAllData() {
        if (controlService == null) return;
        try {
            var allKeys = getCombinedKeys();
            String[] currentValues = controlService.fetchDatas(allKeys);
            for (int i = 0; i < currentValues.length; i++) {
                OnDataChanged(allKeys[i], currentValues[i]);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error dispatching data", e);
        }
    }

    public void addDataChangedListener(IDataChanged listener) {
        if (listener == null) {
            Log.e(TAG, "Cannot add null listener");
            return;
        }
        if (!dataChangedListeners.contains(listener)) {
            dataChangedListeners.add(listener);
            Log.w(TAG, "Listener added: " + listener.getClass().getName());
        } else {
            Log.w(TAG, "Listener already exists: " + listener.getClass().getName());
        }
    }

    public void removeDataChangedListener(IDataChanged listener) {
        if (listener == null) {
            Log.e(TAG, "Cannot remove null listener");
            return;
        }
        if (dataChangedListeners.remove(listener)) {
            Log.w(TAG, "Listener removed: " + listener.getClass().getName());
        } else {
            Log.w(TAG, "Listener not found: " + listener.getClass().getName());
        }
    }

    public void addServiceManagerEventListener(IServiceManagerEvent listener) {
        if (listener == null) {
            Log.e(TAG, "Cannot add null service manager event listener");
            return;
        }
        if (!serviceManagerEventListeners.contains(listener)) {
            serviceManagerEventListeners.add(listener);
            Log.w(TAG, "Service manager event listener added: " + listener.getClass().getName());
        } else {
            Log.w(TAG, "Service manager event listener already exists: " + listener.getClass().getName());
        }
    }

    public void removeServiceManagerEventListener(IServiceManagerEvent listener) {
        if (listener == null) {
            Log.e(TAG, "Cannot remove null service manager event listener");
            return;
        }
        if (serviceManagerEventListeners.remove(listener)) {
            Log.w(TAG, "Service manager event listener removed: " + listener.getClass().getName());
        } else {
            Log.w(TAG, "Service manager event listener not found: " + listener.getClass().getName());
        }
    }

    public void dispatchServiceManagerEvent(ServiceManagerEventType event, Object... args) {
        Log.w(TAG, "Dispatching service manager event: " + event);
        for (IServiceManagerEvent listener : new ArrayList<>(serviceManagerEventListeners)) {
            try {
                listener.onEvent(event, args);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying service manager event listener", e);
            }
        }
    }

    public String getData(String key) {
        if (dataCache.containsKey(key)) {
            return dataCache.get(key);
        }
        if (controlService == null) {
            Log.e(TAG, "ControlService not initialized");
            return null;
        }
        try {
            String value = controlService.fetchData(key);
            dataCache.put(key, value);
            return value;
        } catch (RemoteException e) {
            Log.e(TAG, "Error fetching data", e);
            return null;
        }
    }

    public String getUpdatedData(String key) {
        if (controlService == null) {
            Log.e(TAG, "ControlService not initialized");
            return null;
        }
        try {
            String value = controlService.fetchData(key);
            dataCache.put(key, value);
            return value;
        } catch (RemoteException e) {
            Log.e(TAG, "Error fetching data", e);
            return null;
        }
    }

    public void updateData(String key, String value) {
        if (controlService == null) {
            Log.e(TAG, "ControlService not initialized");
            return;
        }
        try {
            controlService.request("cmd.common.request.set", key, value);
        } catch (RemoteException e) {
            Log.e(TAG, "Error updating data", e);
        }
    }

    public Map<String, String> getAllCurrentCachedData() {
        return new HashMap<>(dataCache);
    }

    /**
     * Como updateData, mas devolve se o comando foi REALMENTE entregue ao binder. Existe para
     * a escrita de power que encerra a secagem: e a diferenca entre "mandei o POWER=0" e "o
     * modulo ainda acha que esta secando" — e essa diferenca decide se a flag de pendencia em
     * disco pode ser apagada ou se ela e a unica evidencia que sobra para o diagnostico.
     */
    private boolean updateDataChecked(String key, String value) {
        if (controlService == null) {
            Log.e(TAG, "ControlService not initialized");
            return false;
        }
        try {
            controlService.request("cmd.common.request.set", key, value);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Error updating data", e);
            return false;
        }
    }

    private void OnDataChanged(String key, String value) {
        Intent broadcastIntent = new Intent("android.intent.haval." + key);
        broadcastIntent.putExtra("value", value);
        App.getContext().sendBroadcast(broadcastIntent);
        broadcastIntent = new Intent("android.intent.haval." + key + "_" + value);
        App.getContext().sendBroadcast(broadcastIntent);
        for (IDataChanged listener : new ArrayList<>(dataChangedListeners)) {
            try {
                listener.onDataChanged(key, value);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener", e);
            }
        }
        dataCache.put(key, value);
        try {
            if (key.equals(CarConstants.CAR_FRS_SETTING_DISTRACTION_DETECTION_ENABLE.getValue()) && value.equals("1")) {
                boolean isForceDisableMonitoring = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_MONITORING.getKey(), false);
                if (isForceDisableMonitoring) {
                    setMonitoringEnabled(false);
                    Log.w(TAG, "Distraction detection monitoring disabled by user preference");
                }
            }
            if (key.equals(CarConstants.CAR_EV_SETTING_AVAS_ENABLE.getValue()) && value.equals("1")) {
                boolean isForceDisableAVAS = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_AVAS.getKey(), false);
                if (isForceDisableAVAS) {
                    setAvasEnabled(false);
                    Log.w(TAG, "AVAS disabled by user preference");
                }
            } else if ((key.equals(CarConstants.CAR_DMS_WORK_STATE.getValue()) && value.equals("0"))) {
                boolean closeWindowOnPowerOff = sharedPreferences.getBoolean(SharedPreferencesKeys.CLOSE_WINDOW_ON_POWER_OFF.getKey(), false);
                if (closeWindowOnPowerOff) {
                    closeAllWindow();
                }
                boolean closeSunRoofOnPowerOff = sharedPreferences.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_POWER_OFF.getKey(), false);
                if (closeSunRoofOnPowerOff) {
                    closeSunRoof(true);
                }
            } else if ((key.equals(CarConstants.CAR_DRIVE_SETTING_OUTSIDE_VIEW_MIRROR_FOLD_STATE.getValue()) && value.equals("0"))) {
                var speedValue = Float.parseFloat(getUpdatedData(CarConstants.CAR_BASIC_VEHICLE_SPEED.getValue()));
                var currentGear = getUpdatedData(CarConstants.CAR_BASIC_GEAR_STATUS.getValue());
                if (speedValue > 0 || !currentGear.equals("3")) {
                    Log.w(TAG, "Ignoring mirror fold event due to speed or gear state");
                    return;
                }
                boolean closeWindowOnFoldMirror = sharedPreferences.getBoolean(SharedPreferencesKeys.CLOSE_WINDOW_ON_FOLD_MIRROR.getKey(), false);
                if (closeWindowOnFoldMirror) {
                    closeAllWindow();
                }
                boolean closeSunRoofOnFoldMirror = sharedPreferences.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_FOLD_MIRROR.getKey(), false);
                if (closeSunRoofOnFoldMirror) {
                    closeSunRoof(true);
                }
            } else if (key.equals(CarConstants.CAR_BASIC_VEHICLE_SPEED.getValue())) {
                float currentSpeed = Float.parseFloat(value);
                boolean closeWindowOnSpeed = sharedPreferences.getBoolean(SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED.getKey(), false);
                boolean closeSunRoofOnSpeed = sharedPreferences.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED.getKey(), false);
                if (currentSpeed > sharedPreferences.getFloat(SharedPreferencesKeys.SPEED_THRESHOLD.getKey(), 15f)) {
                    if (!closeWindowDueToeSpeed) {
                        if (closeWindowOnSpeed) {
                            closeAllWindow();
                        }
                        closeWindowDueToeSpeed = true;
                    }
                }
                if (currentSpeed > sharedPreferences.getFloat(SharedPreferencesKeys.SUNROOF_SPEED_THRESHOLD.getKey(), 15f)) {
                    if (!closeSunroofDueToeSpeed) {
                        if (closeSunRoofOnSpeed) {
                            closeSunRoof(false);
                        }
                        closeSunroofDueToeSpeed = true;
                    }
                }
                if (currentSpeed <= 10 && (closeWindowDueToeSpeed || closeSunroofDueToeSpeed)) {
                    closeWindowDueToeSpeed = false;
                    closeSunroofDueToeSpeed = false;
                }
                if (currentSpeed <= 0 & sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_AVM_CAR_STOPPED.getKey(), false) && !getData(CarConstants.CAR_BASIC_GEAR_STATUS.getValue()).equals("4")) {
                    if (!delayNextAVM) dvr.setAVM(0);
                }
            } else if (key.equals(CarConstants.SYS_AVM_PREVIEW_STATUS.getValue()) && sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_AVM_CAR_STOPPED.getKey(), false) && Float.parseFloat(getData(CarConstants.CAR_BASIC_VEHICLE_SPEED.getValue())) <= 0f && !getData(CarConstants.CAR_BASIC_GEAR_STATUS.getValue()).equals("4")) {
                if (value.equals("1")) {
                    if (!delayNextAVM) dvr.setAVM(0);
                } else {
                    delayNextAVM = false;
                }
            } else if (key.equals(CarConstants.CAR_HVAC_AC_ENABLE.getValue())) {
                // v2.7: so o compressor molha o evaporador. A flag e pegajosa pela viagem
                // inteira — o compressor liga e desliga varias vezes numa mesma viagem, e
                // qualquer uma delas basta para ter agua para secar. Ela so e zerada no fim
                // da viagem (ramo OFF do ready-state), nunca no meio dela.
                //
                // E so ARMA com a ignicao ligada. Com o carro desligado o modulo continua
                // reportando o estado do botao do A/C (esse estado nao zera ao desligar), e
                // armar ali faria a viagem seguinte — sem A/C nenhum — secar por causa de uma
                // notificacao do desligamento anterior.
                if (value.equals("1") && !isReadyStateOffNow()) {
                    acCompressorRanThisIgnition = true;
                    Log.w(TAG, "[SECAGEM] compressor ligou nesta viagem — secagem ao desligar fica armada");
                }
            } else if (key.equals(CarConstants.CAR_BASIC_ENGINE_STATE.getValue())) {
                // v2.7 (diagnostico H3): qual dos dois chega primeiro no desligamento, o
                // ready-state ou o engine_state? So loga transicao, para nao virar spam.
                if (!value.equals(lastEngineStateValue)) {
                    lastEngineStateValue = value;
                    Log.w(TAG, "[DESLIG] engine_state=" + value + " elapsed=" + SystemClock.elapsedRealtime());
                }
            } else if (key.equals(CarConstants.CAR_BASIC_DRIVING_READY_STATE.getValue())) {
                final boolean readyOff = isReadyStateOff(value);
                final boolean edge;
                String previous;
                // synchronized curto, so memoria, zero I/O: este ramo roda na thread de
                // callback do vehicle control E na main thread (DispatchAllDatasReceiver),
                // as vezes ao mesmo tempo. Sem deteccao de borda, o "-1" seguido de "0" do
                // MESMO desligamento dispararia dois ciclos.
                synchronized (readyStateLock) {
                    previous = lastReadyStateValue;
                    edge = (previous == null) || (isReadyStateOff(previous) != readyOff);
                    lastReadyStateValue = value;
                }
                if (readyOff) {
                    // v1.8: carro desligou — cancela qualquer confirmacao de Max AC pendente
                    cancelPendingMaxAcConfirmation();
                    // v1.9: automacoes de HVAC encerram no desligar. Restaurar agora, com o
                    // modulo ainda acordado, evita o residual no proximo start (ex.: secagem
                    // interrompida deixava o AC em 32°C ao religar — o restore so rodava com
                    // o carro desligado e o comando se perdia).
                    //
                    // v2.7: isso continua valendo para a secagem MANUAL, mas nao para a de
                    // desligamento — o trabalho dela e justamente rodar DAQUI PARA FRENTE.
                    // A manual e encerrada dentro do startShutdownDrying, ja fora desta thread.
                    if (isDryingModeActive() && !isShutdownDryingActive()) {
                        cancelDryingMode();
                    }
                    if (isMaxAcActive) {
                        cancelMaxAcMode();
                    }
                    cancelStartupDefaultsRetries();
                    applyStartupDefaultsOnNextReadyOn = true;
                    // v2.7: posta a secagem ANTES do bluetooth/hotspot de proposito. Aqui
                    // so decide e posta (nenhum comando de HVAC sai desta thread), mas cada
                    // segundo conta: a intencao do v1.9 e falar com o modulo HVAC enquanto
                    // ele ainda esta acordado, e desligar bluetooth/ponto de acesso via
                    // Shizuku pode levar bons segundos.
                    maybeStartShutdownDrying(edge, value);
                    // A memoria do compressor e POR VIAGEM, e o fim da viagem e AQUI — nao na
                    // borda ON. O maybeStartShutdownDrying acima ja leu a flag para decidir;
                    // daqui para frente ela pertence a proxima viagem.
                    //
                    // Zerar na borda ON seria uma corrida contra a ordem de entrega das
                    // chaves: o modulo empurra o estado chave por chave (IListener.onDataChanged)
                    // na ordem que ele quiser, entao numa viagem que comeca com o A/C JA ligado
                    // o ac_enable=1 pode chegar ANTES do ready-state, e o zero cego apagaria a
                    // viagem inteira — a secagem nunca dispararia. Zerando no OFF, qualquer
                    // ordem funciona: a flag so e armada depois, durante a viagem seguinte.
                    if (edge) {
                        acCompressorRanThisIgnition = false;
                    }
                    boolean disableBluetoothOnPowerOff = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF.getKey(), false);
                    boolean currentBluetoothState = currentBluetoothState();
                    if (currentBluetoothState && disableBluetoothOnPowerOff) {
                        sharedPreferences.edit().putBoolean(SharedPreferencesKeys.BLUETOOTH_STATE_ON_POWER_OFF.getKey(), true).apply();
                        disableBluetooth();
                    }
                    boolean disableHotspotOnPowerOff = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF.getKey(), false);
                    if (disableHotspotOnPowerOff) {
                        disableWifiTether();
                    }
                } else {
                    // v2.7: carro religou. Encerra a secagem de desligamento ANTES dos
                    // defaults de startup, para a ordem ser determinística: o POWER=0 da
                    // secagem sai primeiro e o "A/C ao ligar" / Max AC passam a ser donos
                    // do HVAC. Os envios aqui sao fire-and-forget, entao nao custa nada
                    // estar na thread do binder.
                    if (isShutdownDryingActive()) {
                        Log.w(TAG, "[SECAGEM] carro religou — encerrando a secagem de desligamento");
                        requestFinish(false, "carro religou");
                    }
                    // Desta borda em diante existe uma ignicao viva neste processo, e o
                    // ciclo de secagem dela esta por gastar.
                    //
                    // Tudo aqui e por BORDA: este ramo roda a CADA entrega do ready-state
                    // (todo dispatchAllData reenvia a chave), nao so quando o carro liga de
                    // fato. Sem a guarda, uma entrega repetida no meio da viagem re-armaria o
                    // ciclo de secagem ja gasto desta ignicao e ele dispararia duas vezes.
                    //
                    // A flag do compressor e RE-DERIVADA aqui, e nao zerada: e a borda que
                    // define a viagem nova, e o valor que interessa e o de agora. Zerar
                    // apagaria o ac_enable=1 que o modulo pode ter empurrado um instante ANTES
                    // desta borda (ele escolhe a ordem dos pushes) — e a viagem inteira ficaria
                    // sem memoria de compressor, que e o defeito que o usuario ve como "nao
                    // seca nunca". No meio da viagem a flag continua so sendo ARMADA (a borda
                    // impede re-derivacao), porque o compressor liga e desliga varias vezes e
                    // desarmar no meio perderia a agua que ja esta no evaporador.
                    if (edge) {
                        sawLiveIgnitionThisProcess = true;
                        shutdownDryingConsumedThisIgnition = false;
                        rederiveCompressorFlag();
                    }
                    boolean disableBluetoothOnPowerOff = sharedPreferences.getBoolean(SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF.getKey(), false);
                    boolean bluetoothStateOnPowerOff = sharedPreferences.getBoolean(SharedPreferencesKeys.BLUETOOTH_STATE_ON_POWER_OFF.getKey(), false);
                    if (disableBluetoothOnPowerOff && bluetoothStateOnPowerOff && !currentBluetoothState()) {
                        enableBluetooth();
                    }
                    // v1.9: defaults de startup agora rodam em CADA ciclo de ignicao (nao
                    // so quando o servico inicia). Aplica imediato e reaplica em 4s — o
                    // modulo HVAC pode demorar a acordar e engolir o primeiro envio.
                    if (applyStartupDefaultsOnNextReadyOn) {
                        applyStartupDefaultsOnNextReadyOn = false;
                        scheduleStartupDefaults();
                    }
                    if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.getKey(), true)) {
                        if (!isMaxAcActive) enableMaxAcOn();
                    }
                }
            } else if (key.equals(CarConstants.CAR_HVAC_POWER_MODE.getValue()) && value.equals("1") && sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON.getKey(), false)) {
                // v2.7: escrever power_mode=1 e o que liga a secagem. Sem esta guarda, o
                // banco do motorista iria para o nivel 3 com o carro desligado, por um
                // minuto, por um motivo que nao tem nada a ver com secar o evaporador.
                // A janela e por TEMPO e nao por dryingKind de proposito: esta notificacao
                // e assincrona e pode chegar depois de o ciclo terminar, quando o kind ja e
                // NONE — uma guarda por kind deixaria passar um vent-3 orfao, e a janela
                // por tempo pega os dois casos. O lado do POWER=0 NAO e suprimido: e a rede
                // que garante a volta do banco ao zero.
                if (SystemClock.elapsedRealtime() < suppressSeatVentBoostUntilElapsed) {
                    Log.w(TAG, "[SECAGEM] ventilacao do banco nao ligada (POWER=1 da secagem)");
                } else {
                    updateData(CarConstants.CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL.getValue(), "3");
                }
            } else if (key.equals(CarConstants.CAR_HVAC_POWER_MODE.getValue()) && value.equals("0") && sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON.getKey(), false)) {
                updateData(CarConstants.CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL.getValue(), "0");
            } else if (key.equals(CarConstants.CAR_BASIC_INSIDE_TEMP.getValue()) && sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.getKey(), true)) {
                if (isMaxAcActive) updateMaxAcSmoothing();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in OnDataChanged", e);
        }
    }

    // v1.9: defaults de volume/AC a cada ready-state ON. Antes viviam no initializeServices
    // e so rodavam quando o servico subia — com a central viva entre ignicoes, o padrao
    // nunca era reaplicado (ex.: secagem interrompida deixava o HVAC em 32°C ao religar).
    private void scheduleStartupDefaults() {
        try {
            cancelStartupDefaultsRetries();
            // Aplica imediato e reaplica 2x (4s e 9s): o modulo HVAC pode ainda estar
            // dormindo no ready-state ON e engolir os primeiros envios em silencio.
            // Sem os retries, um unico envio perdido = "AC ao ligar" nao faz nada.
            applyStartupDefaults();
            startupDefaultsRetriesLeft = 2;
            postStartupDefaultsRetry(4000L);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling startup defaults", e);
        }
    }

    private void postStartupDefaultsRetry(long delayMs) {
        if (startupDefaultsRetriesLeft <= 0) return;
        startupDefaultsRetriesLeft--;
        startupDefaultsRunnable = () -> {
            startupDefaultsRunnable = null;
            applyStartupDefaults();
            postStartupDefaultsRetry(5000L);
        };
        backgroundHandler.postDelayed(startupDefaultsRunnable, delayMs);
    }

    private void cancelStartupDefaultsRetries() {
        startupDefaultsRetriesLeft = 0;
        if (startupDefaultsRunnable != null) {
            backgroundHandler.removeCallbacks(startupDefaultsRunnable);
            startupDefaultsRunnable = null;
        }
    }

    private void applyStartupDefaults() {
        if (sharedPreferences.getBoolean(SharedPreferencesKeys.SET_STARTUP_VOLUME.getKey(), false)) {
            int startupVolume = sharedPreferences.getInt(SharedPreferencesKeys.STARTUP_VOLUME.getKey(), -1);
            if (startupVolume != -1) {
                try {
                    controlService.request("cmd.common.request.set", CarConstants.SYS_SETTINGS_AUDIO_MEDIA_VOLUME.getValue(), String.valueOf(startupVolume));
                    Log.w(TAG, "Startup volume set to: " + startupVolume);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting startup volume", e);
                }
            }
        }
        if (!sharedPreferences.getBoolean(SharedPreferencesKeys.SET_STARTUP_AC.getKey(), false)) return;
        // v2.7: a secagem de desligamento tem prioridade absoluta sobre automacao. Nao e
        // simetria com o Max AC abaixo: e uma corrida real e pre-existente. As
        // retentativas do startup sao postadas em 4s e 9s (postStartupDefaultsRetry), e o
        // `startupDefaultsRetriesLeft--` acontece ANTES da atribuicao do runnable — entao
        // um cancelStartupDefaultsRetries() vindo do ramo OFF (outra thread) pode cancelar
        // o runnable antigo e o novo ser postado assim mesmo. Uma retentativa sobrevivente
        // ao desligamento sobrescreveria FAN/TEMPS por cima da secagem. A guarda torna a
        // corrida inofensiva sem precisar consertar a ordem das duas linhas.
        if (isShutdownDryingActive()) {
            Log.w(TAG, "Startup AC ignorado: secagem de desligamento em andamento");
            return;
        }
        // Max AC em andamento domina o HVAC — nao briga com ele; o smoothing do Max AC
        // restaura o snapshot do usuario ao terminar. Log em WARN de proposito: o head
        // unit filtra INFO, e sem isso o "AC ao ligar" parecia simplesmente nao existir
        // quando o Max AC (ligado por padrao) assumia o controle 3s depois da ignicao.
        if (isMaxAcActive) {
            Log.w(TAG, "Startup AC ignorado: Max AC esta ativo e domina o HVAC");
            return;
        }
        // Default HVAC state applied every time the car turns on: driver/passenger
        // temperature, vent direction, fan speed and circulation mode. POWER so e
        // tocado se o usuario pediu (STARTUP_AC_POWER) — o padrao continua sendo so
        // ajustar os valores sem forcar o ar ligado.
        boolean startupAcPower = sharedPreferences.getBoolean(SharedPreferencesKeys.STARTUP_AC_POWER.getKey(), false);
        boolean startupAcCompressor = sharedPreferences.getBoolean(SharedPreferencesKeys.STARTUP_AC_COMPRESSOR.getKey(), true);
        String startupAcTemp = sharedPreferences.getString(SharedPreferencesKeys.STARTUP_AC_TEMPERATURE.getKey(), "22.0");
        String startupAcBlower = sharedPreferences.getString(SharedPreferencesKeys.STARTUP_AC_BLOWER_MODE.getKey(), null);
        String startupAcCycle = sharedPreferences.getString(SharedPreferencesKeys.STARTUP_AC_CYCLE_MODE.getKey(), "1"); // 1 = externa no H6
        int startupAcFan = sharedPreferences.getInt(SharedPreferencesKeys.STARTUP_AC_FAN_SPEED.getKey(), 0);
        if (startupAcPower) {
            updateData(CarConstants.CAR_HVAC_POWER_MODE.getValue(), "1");
            // So mexe no compressor quando o ar vai ser ligado — desligado com o ar
            // desligado nao significa nada na tela do carro.
            updateData(CarConstants.CAR_HVAC_AC_ENABLE.getValue(), startupAcCompressor ? "1" : "0");
        }
        updateData(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue(), startupAcTemp);
        updateData(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue(), startupAcTemp);
        updateData(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue(), "1");
        if (startupAcBlower != null && !startupAcBlower.isEmpty()) {
            updateData(CarConstants.CAR_HVAC_BLOWER_MODE.getValue(), startupAcBlower);
        }
        // v1.8: velocidade da ventilacao ao ligar (1-7); 0 = nao alterar
        if (startupAcFan > 0) {
            updateData(CarConstants.CAR_HVAC_FAN_SPEED.getValue(), String.valueOf(startupAcFan));
        }
        updateData(CarConstants.CAR_HVAC_CYCLE_MODE.getValue(), startupAcCycle);
        updateData(CarConstants.CAR_HVAC_AQS_ENABLE.getValue(), "0"); // AQS could force internal circulation back
        Log.w(TAG, "Startup AC set: temp=" + startupAcTemp + " blower=" + startupAcBlower + " fan=" + startupAcFan
                + " cycle=" + startupAcCycle + " power=" + startupAcPower + " compressor=" + startupAcCompressor);
    }

    // "Modo padrão do A/C" (v2.0): aplica o perfil salvo no app — temperaturas dos
    // dois lados de forma independente, ventilação, circulação e direção do ar —
    // por cima de qualquer estado atual do HVAC, ligando o ar no processo.
    // Acionado pelo botão do app ou pela zona "Modo padrão" da tela de A/C do
    // cluster (logo após a circulação no ciclo de ENTER).
    public void applyDefaultAcMode() {
        try {
            // Automations (Max AC / secagem) tomam conta do HVAC: derruba antes de
            // aplicar o perfil. Os cancelamentos restauram o snapshot via updateData
            // (fila assíncrona FIFO), então saem antes dos comandos do perfil abaixo.
            cancelMaxAcMode();
            cancelDryingMode();

            String driverTemp = sharedPreferences.getString(SharedPreferencesKeys.DEFAULT_AC_TEMPERATURE_DRIVER.getKey(), "22.0");
            String passTemp = sharedPreferences.getString(SharedPreferencesKeys.DEFAULT_AC_TEMPERATURE_PASSENGER.getKey(), "22.0");
            int fan = sharedPreferences.getInt(SharedPreferencesKeys.DEFAULT_AC_FAN_SPEED.getKey(), 3);
            String cycle = sharedPreferences.getString(SharedPreferencesKeys.DEFAULT_AC_CYCLE_MODE.getKey(), "1"); // H6: 1 = externa
            String blower = sharedPreferences.getString(SharedPreferencesKeys.DEFAULT_AC_BLOWER_MODE.getKey(), "");
            boolean compressor = sharedPreferences.getBoolean(SharedPreferencesKeys.DEFAULT_AC_COMPRESSOR.getKey(), true);

            updateData(CarConstants.CAR_HVAC_POWER_MODE.getValue(), "1"); // perfil = AC ligado, independente do estado anterior
            // Compressor (botão "A/C", distinto de power_mode = sistema ligado). Vai logo
            // depois do power para o módulo já estar acordado quando ele chega.
            updateData(CarConstants.CAR_HVAC_AC_ENABLE.getValue(), compressor ? "1" : "0");
            updateData(CarConstants.CAR_HVAC_AUTO_ENABLE.getValue(), "0"); // velocidade explícita, sem AUTO interferir
            updateData(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue(), driverTemp);
            updateData(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue(), passTemp);
            // Sync só quando os dois lados pedem a mesma temperatura; senão o painel do
            // carro igualaria o lado do passageiro ao do motorista ao apertar SYNC.
            updateData(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue(), driverTemp.equals(passTemp) ? "1" : "0");
            if (fan > 0 && fan <= 7) {
                updateData(CarConstants.CAR_HVAC_FAN_SPEED.getValue(), String.valueOf(fan));
            }
            updateData(CarConstants.CAR_HVAC_CYCLE_MODE.getValue(), cycle);
            if (blower != null && !blower.isEmpty()) {
                updateData(CarConstants.CAR_HVAC_BLOWER_MODE.getValue(), blower);
            }
            updateData(CarConstants.CAR_HVAC_AQS_ENABLE.getValue(), "0"); // AQS could force internal circulation back
            Log.w(TAG, "Modo padrão AC aplicado: driver=" + driverTemp + " pass=" + passTemp
                    + " fan=" + fan + " cycle=" + cycle + " blower=" + blower + " compressor=" + compressor);
        } catch (Exception e) {
            Log.e(TAG, "Error applying default AC mode", e);
        }
    }

    public boolean closeAllWindow() {
        try {
            int[] windowsStatus = vehicle.getWindowsStatus(0);
            for (int i = 0; i < windowsStatus.length; i++) {
                if (windowsStatus[i] != 1) {
                    vehicle.setWindowStatus(i, 1);
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error closing all windows", e);
            return false;
        }
    }

    public void closeSunRoof(boolean checkCloseShade) {
        try {
            var sunRoofStatus = vehicle.getSkylightLevel(0);
            if (sunRoofStatus != 0) {
                vehicle.setSkylightLevel(0);
            }
            if (checkCloseShade && sharedPreferences.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF.getKey(), false)) {
                backgroundHandler.postDelayed(this::closeSunRoofShade, 5000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing sunroof", e);
        }
    }

    public void closeSunRoofShade() {
        try {
            var sunRoofBlockStatus = vehicle.getShadeScreensLevel(0);
            if (sunRoofBlockStatus != 0) {
                vehicle.setShadeScreensLevel(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing shade screens", e);
        }
    }

    public boolean isTurnLightOn() {
        var leftTurnLight = getData(CarConstants.CAR_BASIC_LEFT_TURN_SWITCH_STATUS.getValue());
        var rightTurnLight = getData(CarConstants.CAR_BASIC_RIGHT_TURN_SWITCH_STATUS.getValue());
        return (leftTurnLight != null && leftTurnLight.equals("1")) || (rightTurnLight != null && rightTurnLight.equals("1"));
    }

    public void setMonitoringEnabled(boolean b) {
        if (controlService == null) {
            Log.e(TAG, "ControlService not initialized");
            return;
        }
        try {
            controlService.request("cmd.common.request.set", CarConstants.CAR_FRS_SETTING_DISTRACTION_DETECTION_ENABLE.getValue(), b ? "1" : "0");
            Log.w(TAG, "Distraction detection monitoring set to: " + b);
        } catch (RemoteException e) {
            Log.e(TAG, "Error setting monitoring", e);
        }
    }

    public void setAvasEnabled(boolean b) {
        if (controlService == null) {
            Log.e(TAG, "ControlService not initialized");
            return;
        }
        try {
            controlService.request("cmd.common.request.set", CarConstants.CAR_EV_SETTING_AVAS_ENABLE.getValue(), b ? "1" : "0");
            Log.w(TAG, "AVAS enabled: " + b);
        } catch (RemoteException e) {
            Log.e(TAG, "Error setting AVAS", e);
        }
    }

    private boolean currentBluetoothState() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Error checking Bluetooth state", e);
            return false;
        }
    }

    public void disableBluetooth() {
        try {
            ShizukuUtils.runCommandAndGetOutput(new String[]{"svc", "bluetooth", "disable"});
        } catch (Exception e) {
            Log.e(TAG, "Error disabling Bluetooth", e);
        }
    }

    public void enableBluetooth() {
        try {
            ShizukuUtils.runCommandAndGetOutput(new String[]{"svc", "bluetooth", "enable"});
        } catch (Exception e) {
            Log.e(TAG, "Error enabling Bluetooth", e);
        }
    }

    public void disableWifiTether() {
        try {
            connectivityManager.stopTethering(0, "br.com.redesurftank.havalshisuku");
        } catch (Exception e) {
            Log.e(TAG, "Error disabling Wi-Fi", e);
        }
    }

    public void enableWifiTether() {
        try {
            var receiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (resultCode == 0) {
                        Log.w(TAG, "Wi-Fi tethering started successfully");
                    } else {
                        Log.e(TAG, "Failed to start Wi-Fi tethering with result code: " + resultCode);
                    }
                }
            };
            connectivityManager.startTethering(0, receiver, false, "br.com.redesurftank.havalshisuku");
        } catch (Exception e) {
            Log.e(TAG, "Error enabling Wi-Fi", e);
        }
    }

    public void cancelMaxAcMode() {

        if (!isMaxAcActive) return;

        for (Map.Entry<String, String> entry : previousAcState.entrySet()) {
            if (entry.getValue() != null) {
                updateData(entry.getKey(), entry.getValue());
            }
        }
        isMaxAcActive = false;
        previousAcState.clear();
        // v1.8: zera a janela de histerese e cancela confirmacao pendente ao desligar
        maxAcActivatedAt = 0L;
        cancelPendingMaxAcConfirmation();
        if (maxAcTimeoutRunnable != null) {
            backgroundHandler.removeCallbacks(maxAcTimeoutRunnable);
            maxAcTimeoutRunnable = null;
        }
        dispatchServiceManagerEvent(ServiceManagerEventType.MAX_AUTO_AC_STATUS_CHANGED, 0);

    }

    /** "Desligado" no ready-state: tanto o -1 (tela apagando) quanto o 0. */
    private static boolean isReadyStateOff(String value) {
        return "-1".equals(value) || "0".equals(value);
    }

    /**
     * Zera o motor sem tocar no HVAC. Usado quando o handler antigo morreu e o servico
     * esta subindo de novo: o cleanup roda antes de tudo, inclusive de o veiculo
     * reconectar, entao aqui um comando de HVAC nao teria para onde ir.
     *
     * O ciclo e guardado inteiro (snapshot + kind) para ser desfeito depois, com a conexao
     * viva: ele ja tinha mandado POWER=1 / fan 7 / 32 C e ninguem mais vai mandar o fim.
     * Sao os DOIS sabores — a manual tambem pode ter comecado com o carro ja desligado
     * (volante / menu do cluster na central acordada em acessorio), e nesse caso abandonar
     * sem desfazer deixaria a ventilacao ligada sem ignicao, para sempre.
     *
     * Isto NAO se confunde com perda de energia de verdade: se a central morreu, o processo
     * morre junto e este codigo nunca roda — a proxima subida encontra dryingKind == NONE e
     * so a flag em disco, que vira log (ver reportInterruptedShutdownDrying). Essa medicao
     * continua limpa: so o abandono DENTRO de um processo vivo dispara a correcao.
     */
    private void resetDryingStateIfOrphaned() {
        synchronized (dryingLock) {
            if (dryingKind == DryingKind.NONE) return;
            Log.w(TAG, "[SECAGEM] " + dryingKind
                    + " orfa no restart do servico — motor zerado, estado sera reaplicado"
                    + " quando a conexao voltar");
            orphanedDryingSnapshot = new HashMap<>(dryingSnapshot);
            orphanedDryingKind = dryingKind;
            clearDryingStateLocked();
        }
    }

    /**
     * Desfaz, com a conexao ja viva, o ciclo abandonado no restart. Roda no
     * initializeServices logo ANTES do dispatchAllData: o ready-state ON que ele despacha
     * pode chamar applyStartupDefaults, e o "A/C ao ligar" deve ser o ultimo a escrever
     * nesta ignicao, nao a correcao.
     */
    private void repairOrphanedDrying() {
        final Map<String, String> snapshot;
        final DryingKind kind;
        synchronized (dryingLock) {
            if (orphanedDryingSnapshot == null) return;
            snapshot = orphanedDryingSnapshot;
            kind = orphanedDryingKind;
            orphanedDryingSnapshot = null;
            orphanedDryingKind = DryingKind.NONE;
        }
        // Com o carro DESLIGADO o power tem de terminar em 0: deixar a ventilacao ligada sem
        // ignicao e o unico desfecho inaceitavel. Com o carro ligado, o estado anterior e
        // legitimo (a secagem manual cancela restaurando o que estava antes).
        final boolean vehicleOff = isReadyStateOffNow();
        Log.w(TAG, "[SECAGEM] reaplicando estado por cima do ciclo " + kind + " orfao (carro "
                + (vehicleOff ? "desligado" : "ligado") + ") elapsed=" + SystemClock.elapsedRealtime());
        writeSnapshotAndPower(snapshot, kind, (kind == DryingKind.MANUAL) && !vehicleOff);
    }

    /** Zera o motor. O chamador DEVE estar sob dryingLock. Nao toca no HVAC. */
    private void clearDryingStateLocked() {
        dryingKind = DryingKind.NONE;
        dryingSnapshot.clear();
        dryingRemainingSeconds = 0;
        dryingDeadlineElapsed = 0L;
        // O looper dela ja morreu nos casos de restart; removeCallbacks seria no-op.
        dryingTickRunnable = null;
        publishDryingFlags();
    }

    /**
     * O ULTIMO ready-state entregue diz "desligado". Sem ready-state nenhum ainda, isto e
     * false — tratamos "nao sei" como "nao desligado", que e o lado seguro nos dois usos:
     * nao iniciar secagem por engano, e nao deixar de armar o compressor.
     * Leitura so de memoria, sob o lock curto.
     */
    private boolean isReadyStateOffNow() {
        synchronized (readyStateLock) {
            return lastReadyStateValue != null && isReadyStateOff(lastReadyStateValue);
        }
    }

    private String currentReadyStateValue() {
        synchronized (readyStateLock) {
            return lastReadyStateValue;
        }
    }

    /**
     * v2.7: decide e POSTA a secagem de desligamento. Nada de HVAC acontece aqui.
     *
     * Este metodo roda na thread de callback do vehicle control — e, via
     * DispatchAllDatasReceiver, na MAIN thread. Iniciar a secagem inline aqui seriam 10
     * fetchData sincronos (two-way, sem timeout, dentro de uma transacao binder) no
     * exato momento em que o carro esta indo embora, com ANR no caminho da main thread.
     * Por isso: decide-se aqui, executa-se no backgroundHandler.
     *
     * Quatro guardas, e todas precisam passar:
     *   1. borda  — o desligamento manda -1 e depois 0; sem borda, dois ciclos.
     *   2. ligada — o card fica na secao "Ao Desligar" e nasce ligado.
     *   3. armada — so depois de ver uma ignicao viva NESTE processo. Sem isso, o
     *      dispatchAllData() da inicializacao (com o carro parado) cairia aqui e
     *      dispararia uma secagem fantasma a cada subida do servico.
     *   4. compressor — sem compressor nao ha agua no evaporador para secar.
     */
    private void maybeStartShutdownDrying(boolean edge, String value) {
        final boolean enabled = sharedPreferences.getBoolean(
                SharedPreferencesKeys.ENABLE_SHUTDOWN_DRYING.getKey(), true);
        final boolean armed = sawLiveIgnitionThisProcess;
        final boolean spent = shutdownDryingConsumedThisIgnition;
        final boolean acUsed = acCompressorRanThisIgnition;
        if (!edge || !enabled || !armed || spent || !acUsed) {
            // Log sempre: e esta linha que diz QUAL guarda barrou quando no carro nao
            // acontecer nada.
            Log.w(TAG, "[DESLIG] secagem ao desligar NAO disparada ready=" + value
                    + " borda=" + edge + " ligada=" + enabled + " armada=" + armed
                    + " jaGasta=" + spent + " compressorRodou=" + acUsed);
            return;
        }
        // Consome a borda JA, na thread do callback. O barramento repete o OFF: a
        // marcacao tem de existir antes de a repeticao ser entregue, senao a guarda 3
        // deixa passar o segundo ciclo (o consuming no backgroundHandler chegaria tarde).
        shutdownDryingConsumedThisIgnition = true;
        Log.w(TAG, "[DESLIG] secagem ao desligar armada — postando no backgroundHandler elapsed="
                + SystemClock.elapsedRealtime());
        backgroundHandler.post(this::startShutdownDrying);
    }

    /**
     * Secagem manual (volante / menu do cluster): so ventilador, compressor
     * desligado, temperatura HI, ventilacao no maximo e ar externo, por 2 minutos.
     * Usada para secar o evaporador e evitar mofo depois de usar o A/C.
     */
    public void startDryingMode() {
        if (isDryingModeActive()) {
            cancelDryingMode();
            return;
        }
        // Nao roda junto com o Max AC (comportamento historico: o snapshot pega o
        // estado que o Max AC restaurou ao ser derrubado).
        cancelMaxAcMode();
        startDrying(DryingKind.MANUAL, DRYING_MODE_DURATION_SECONDS);
    }

    /**
     * v2.7: secagem automatica no desligamento. Roda SEMPRE no backgroundHandler —
     * nunca inline no ramo do ready-state, que executa na thread de callback do
     * vehicle control e, via DispatchAllDatasReceiver, tambem na MAIN thread.
     * Iniciar inline ali seriam 10 fetchData sincronos (chamada two-way dentro de
     * uma transacao binder, sem timeout) no exato momento em que o carro vai embora.
     */
    private void startShutdownDrying() {
        // Guarda de re-leitura, especifica do fato de isto rodar POSTADO: entre o disparo
        // e este runnable o carro pode ter sido religado. Nesse caso o ramo ON nao viu
        // secagem nenhuma para encerrar (ela ainda nao existia) e a secagem nasceria com o
        // carro ligado. Em vez de confiar na memoria do disparo, olha o ultimo ready-state.
        synchronized (readyStateLock) {
            if (lastReadyStateValue == null || !isReadyStateOff(lastReadyStateValue)) {
                Log.w(TAG, "[SECAGEM] desligamento abortado: ready-state atual ja e "
                        + lastReadyStateValue + " (o carro religou antes de comecar)");
                return;
            }
        }
        synchronized (dryingLock) {
            if (dryingKind == DryingKind.SHUTDOWN) {
                Log.w(TAG, "[SECAGEM] ja existe uma secagem de desligamento ativa — nao sobrepoe");
                return;
            }
        }
        // Uma secagem MANUAL em andamento e encerrada antes. Era o que o ramo OFF
        // fazia inline; agora acontece aqui, fora da thread do binder, preservando a
        // intencao do v1.9 (restaurar com o modulo HVAC ainda acordado).
        if (isDryingModeActive()) {
            Log.w(TAG, "[SECAGEM] encerrando a secagem manual no desligamento");
            requestFinish(true, "desligamento encerrou a manual");
        }
        cancelMaxAcMode();
        int seconds = sharedPreferences.getInt(
                SharedPreferencesKeys.SHUTDOWN_DRYING_DURATION.getKey(), SHUTDOWN_DRYING_DURATION_DEFAULT);
        if (seconds < SHUTDOWN_DRYING_MIN_SECONDS) seconds = SHUTDOWN_DRYING_MIN_SECONDS;
        if (seconds > SHUTDOWN_DRYING_MAX_SECONDS) seconds = SHUTDOWN_DRYING_MAX_SECONDS;
        startDrying(DryingKind.SHUTDOWN, seconds);
    }

    /**
     * Caminho comum de inicio, em quatro tempos, exatamente para que NENHUMA chamada
     * de binder aconteca com dryingLock na mao:
     *
     *   ler (binder, sem lock) -> reservar (lock, so memoria) -> comandar (binder, sem
     *   lock) -> instalar (lock, so memoria + post).
     *
     * O preco e que existe uma janela entre reservar e instalar em que o ciclo conta
     * como ativo mas o tick ainda nao subiu. Nessa janela um requestFinish e legitimo
     * (carro religou, usuario cancelou) e simplesmente vence: installDrying ve o kind
     * trocado, nao instala, e o startDrying desfaz o que ja mandou.
     */
    private void startDrying(DryingKind kind, int durationSeconds) {
        final boolean isShutdown = (kind == DryingKind.SHUTDOWN);
        final Map<String, String> snapshot = readDryingSnapshot();
        if (!reserveDrying(kind, snapshot)) {
            Log.w(TAG, "[SECAGEM] " + kind + " ignorada: o motor de secagem ja esta ocupado");
            return;
        }

        // Ultima re-leitura, no ponto mais perto possivel do comando. Entre a guarda do
        // topo de startShutdownDrying e aqui passaram dezenas a centenas de ms (11 leituras
        // de binder + um commit em disco) — e nesse intervalo o carro pode ter sido
        // religado, caso em que o ciclo nasceria com o carro LIGADO e as guardas de startup
        // e de Max AC veriam "secagem de desligamento ativa" e se afastariam. Abortar AQUI
        // nao custa comando nenhum: nada foi enviado ainda.
        if (isShutdown && !isReadyStateOffNow()) {
            Log.w(TAG, "[SECAGEM] desligamento abortado antes do primeiro comando — ready-state"
                    + " atual ja e " + currentReadyStateValue());
            synchronized (dryingLock) {
                if (dryingKind == kind) clearDryingStateLocked();
            }
            return;
        }

        // Janela do banco armada AQUI, antes do primeiro comando — e nao em installDrying.
        // O POWER=1 sai em sendDryingProfile logo abaixo, e a notificacao dele pode chegar
        // enquanto este thread ainda esta mandando o resto do perfil: armar depois deixava
        // passar justamente o evento que a janela existe para suprimir, e o banco ia para o
        // nivel 3 num carro desligado. Prazo contado daqui: ciclo + carencia.
        //
        // DEPOIS do aborto acima, nunca antes: o aborto sai sem mandar comando nenhum e sem
        // chamar requestFinish, entao uma janela armada ali sobreviveria ao ciclo que nunca
        // existiu — e blindaria a ventilacao do banco por ate 60s+15s da viagem seguinte,
        // que comecou com o carro ligando.
        if (isShutdown) {
            suppressSeatVentBoostUntilElapsed = SystemClock.elapsedRealtime()
                    + durationSeconds * 1000L + SEAT_VENT_SUPPRESS_GRACE_MS;
        }

        final String prevPower = snapshot.get(CarConstants.CAR_HVAC_POWER_MODE.getValue());
        final String prevAc = snapshot.get(CarConstants.CAR_HVAC_AC_ENABLE.getValue());
        final String prevBlower = snapshot.get(CarConstants.CAR_HVAC_BLOWER_MODE.getValue());
        boolean installed = false;
        try {
            // Grava a intencao ANTES do primeiro comando de HVAC. Se a central perder
            // energia agora, e isto que conta a historia na proxima ignicao — e o log
            // registra o prevPower/prevAc lidos antes de escrevermos qualquer coisa,
            // que e o dado que responde se o estado do HVAC sobrevive entre ignicoes.
            if (isShutdown) {
                markShutdownDryingPending(prevPower, prevAc);
            }

            if (sendDryingProfile(kind, prevBlower)) {
                installed = installDrying(kind, durationSeconds);
            }
        } catch (Exception e) {
            Log.e(TAG, "[SECAGEM] erro ao iniciar", e);
        }

        if (!installed) {
            // Duas causas possiveis, e as duas querem a mesma resposta: excecao no meio
            // do perfil, ou alguem encerrou o ciclo enquanto comandavamos. Parte do
            // perfil ja saiu, e nao pode sobrar sem tick para corrigir.
            Log.w(TAG, "[SECAGEM] " + kind + " NAO instalada — desfazendo o que ja saiu");
            // O teste e stillDrying(kind), NAO o espelho global: com o espelho, uma
            // instalacao que falhou podia encontrar o ciclo de OUTRO dono ativo (outra
            // thread reservou no meio-tempo) e encerrar um ciclo saudavel alheio.
            if (stillDrying(kind)) {
                requestFinish(false, "nao instalada");
            } else if (isShutdown) {
                // Alguem JA encerrou e o carro esta desligado: o nosso proprio POWER=1 pode
                // ter saido depois do restore dele, e deixar a ventilacao ligada sem ignicao
                // e o unico desfecho inaceitavel (temperatura/ventilacao a ignicao seguinte
                // reaplica se estiver configurado).
                updateData(CarConstants.CAR_HVAC_POWER_MODE.getValue(), "0");
            } else {
                // MANUAL com o ciclo ja encerrado por outro: quem cancelou restaurou a
                // intencao DELE (o A/C do usuario, tipicamente) e um POWER=0 cego aqui
                // desligaria esse A/C — dois toques rapidos no botao da secagem bastavam para
                // isso. Nao escrevemos nada: sobrescrever a decisao de quem cancelou seria
                // pior do que o rastro que o nosso perfil parcial possa ter deixado, e o
                // restore do cancelamento passa por cima dele quando chega depois.
                Log.w(TAG, "[SECAGEM] MANUAL nao instalada com o ciclo ja encerrado por outro"
                        + " — nao sobrescreve o power");
            }
            return;
        }

        Log.w(TAG, "[SECAGEM] " + kind + " iniciada dur=" + durationSeconds
                + "s fan=7 temp=32.0 cycle=1(externa) aqs=0 ac=0 prevPower=" + prevPower
                + " prevAc=" + prevAc + " elapsed=" + SystemClock.elapsedRealtime());
    }

    /** As 10 leituras do estado atual do HVAC. Binder two-way: NUNCA sob dryingLock. */
    private Map<String, String> readDryingSnapshot() {
        Map<String, String> snapshot = new HashMap<>();
        snapshot.put(CarConstants.CAR_HVAC_POWER_MODE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_POWER_MODE.getValue()));
        snapshot.put(CarConstants.CAR_HVAC_AC_ENABLE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_AC_ENABLE.getValue()));
        snapshot.put(CarConstants.CAR_HVAC_FAN_SPEED.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_FAN_SPEED.getValue()));
        snapshot.put(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue()));
        snapshot.put(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue()));
        snapshot.put(CarConstants.CAR_HVAC_AUTO_ENABLE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_AUTO_ENABLE.getValue()));
        snapshot.put(CarConstants.CAR_HVAC_CYCLE_MODE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_CYCLE_MODE.getValue()));
        snapshot.put(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue()));
        // Vent direction (windshield / face / feet...) and AQS: keep them so drying
        // can restore them and so AQS cannot silently re-enable internal circulation.
        snapshot.put(CarConstants.CAR_HVAC_BLOWER_MODE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_BLOWER_MODE.getValue()));
        snapshot.put(CarConstants.CAR_HVAC_AQS_ENABLE.getValue(),
                getUpdatedData(CarConstants.CAR_HVAC_AQS_ENABLE.getValue()));
        return snapshot;
    }

    /**
     * Toma posse do motor. O snapshot entra aqui, ainda sem nenhum comando enviado,
     * justamente para que um requestFinish que chegue durante os comandos tenha o que
     * restaurar. Devolve false se outro ciclo chegou primeiro.
     */
    private boolean reserveDrying(DryingKind kind, Map<String, String> snapshot) {
        synchronized (dryingLock) {
            if (dryingKind != DryingKind.NONE) return false;
            dryingKind = kind;
            dryingSnapshot.clear();
            dryingSnapshot.putAll(snapshot);
            dryingRemainingSeconds = 0; // instalado de verdade em installDrying
            dryingDeadlineElapsed = 0L;
            // A UI ja ve a secagem como ativa a partir daqui — e o que faz um segundo
            // toque no botao do volante cancelar em vez de empilhar um ciclo.
            publishDryingFlags();
            return true;
        }
    }

    /**
     * Publica o prazo e sobe o tick, depois dos comandos. Devolve false se o ciclo foi
     * encerrado no meio-tempo (nao instala nada).
     */
    private boolean installDrying(DryingKind kind, int durationSeconds) {
        synchronized (dryingLock) {
            if (dryingKind != kind) return false;
            long now = SystemClock.elapsedRealtime();
            dryingRemainingSeconds = durationSeconds;
            // Prazo ABSOLUTO calculado UMA vez. Nenhuma notificacao futura o altera —
            // e isto que impede o barramento, repetindo o OFF, de esticar o ciclo para
            // sempre. E substitui o antigo dryingModeTimeoutRunnable, que era falso
            // seguro: vivia no MESMO looper do tick, entao travava junto com ele.
            dryingDeadlineElapsed = now + durationSeconds * 1000L;
            dryingTickRunnable = buildDryingTick();
            // O handler e lido UMA vez, num local: initializeServices anula o campo no
            // cleanup, e ler duas vezes poderia pegar dois handlers diferentes.
            final Handler handler = backgroundHandler;
            if (handler == null) {
                Log.w(TAG, "[SECAGEM] sem backgroundHandler (servico reiniciando) — abortando o ciclo");
                dryingTickRunnable = null;
                return false;
            }
            // O retorno IMPORTA. Um ciclo pode ser instalado justo quando o looper esta
            // morrendo: initializeServices faz quitSafely() no handler antigo e cria outro,
            // e quit(true) apaga toda mensagem futura — o tick de 1s entra nessa. Se o
            // post fosse aceito sem ser entregue, sobraria um ciclo "ativo" sem ninguem
            // para encerra-lo: POWER=1 / fan 7 / 32 C num carro desligado, para sempre.
            // Recusado (looper morto) => devolve false e o startDrying desfaz o perfil.
            if (!handler.postDelayed(dryingTickRunnable, 1000L)) {
                Log.w(TAG, "[SECAGEM] handler recusou o tick (looper morto?) — abortando o ciclo");
                dryingTickRunnable = null;
                return false;
            }
            return true;
        }
    }

    /**
     * Manda o perfil de secagem inteiro. Os dois pontos de saida existem para nao
     * continuar escrevendo por cima do que quem encerrou o ciclo acabou de restaurar —
     * a janela fica do tamanho de um lote de comandos, nao do perfil todo.
     * Devolve false se o ciclo deixou de ser dono no meio.
     */
    private boolean sendDryingProfile(DryingKind kind, String prevBlower) {
        updateData(CarConstants.CAR_HVAC_POWER_MODE.getValue(), "1");
        updateData(CarConstants.CAR_HVAC_AUTO_ENABLE.getValue(), "0");
        // Turning AUTO off can make the module snap the vent direction to its own
        // default — re-apply the user's direction so drying keeps it untouched.
        if (prevBlower != null) {
            updateData(CarConstants.CAR_HVAC_BLOWER_MODE.getValue(), prevBlower);
        }
        updateData(CarConstants.CAR_HVAC_AC_ENABLE.getValue(), "0");
        updateData(CarConstants.CAR_HVAC_AQS_ENABLE.getValue(), "0"); // AQS could force internal circulation back
        if (!stillDrying(kind)) return false;
        updateData(CarConstants.CAR_HVAC_FAN_SPEED.getValue(), "7");
        updateData(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue(), "32.0");
        updateData(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue(), "32.0");
        updateData(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue(), "1");
        updateData(CarConstants.CAR_HVAC_CYCLE_MODE.getValue(), "1"); // H6: 1 = fresh air / outside circulation (propriedade invertida vs AOSP), sent LAST so no later command overrides it
        return true;
    }

    private boolean stillDrying(DryingKind kind) {
        synchronized (dryingLock) {
            return dryingKind == kind;
        }
    }

    private Runnable buildDryingTick() {
        return new Runnable() {
            @Override
            public void run() {
                final int remaining;
                final boolean shutdown;
                final boolean expired;
                synchronized (dryingLock) {
                    // A checagem e de IDENTIDADE, nao so do kind. Entre o fim de um ciclo e o
                    // comeco de outro este mesmo Runnable pode continuar na fila (encerrar e
                    // recomecar dentro de 1s). Sem isso, o runnable velho veria o kind NOVO,
                    // decrementaria o contador por fora e se re-postaria — dois runnables
                    // vivos, contagem andando em dobro, e um deles imortal enquanto a
                    // secagem durasse.
                    if (dryingTickRunnable != this) return; // morta: sai sem re-postar
                    dryingRemainingSeconds--;
                    remaining = dryingRemainingSeconds;
                    shutdown = (dryingKind == DryingKind.SHUTDOWN);
                    // Rede de seguranca dentro do proprio tick: mesmo que a contagem
                    // dessincronize, o prazo absoluto manda.
                    expired = SystemClock.elapsedRealtime() >= dryingDeadlineElapsed;
                }

                if (remaining <= 0 || expired) {
                    Log.w(TAG, "[SECAGEM] fim por " + (expired ? "prazo absoluto" : "contagem")
                            + " (" + (shutdown ? "desligamento" : "manual") + ")");
                    requestFinish(false, expired ? "prazo absoluto" : "contagem esgotada");
                    return;
                }

                // Os comandos de reafirmacao so saem se ainda formos o tick vigente: sao
                // envios cegos para o modulo e escreveriam por cima de um restore que
                // acabou de rodar em outra thread.
                boolean reassert = isCurrentTick(this);
                if (reassert && shutdown) {
                    // Reafirmacao CEGA de proposito. A secagem manual faz read-before-write
                    // desde a v2.1 para nao popar o painel nativo do A/C por cima do GPS —
                    // mas aqui o carro esta desligado, nao ha painel para popar, e um
                    // fetchData pode travar segundos com o modulo HVAC dormindo. E o tick
                    // justamente o que nao podemos perder. Reafirmar fan/temp tambem cobre
                    // o modulo ter engolido os primeiros envios enquanto acordava.
                    if (remaining % 5 == 0) {
                        reassertBatch(this);
                        Log.w(TAG, "[SECAGEM] reafirmando (cego) t=" + remaining
                                + "s elapsed=" + SystemClock.elapsedRealtime());
                    }
                } else if (reassert && remaining % 3 == 0) {
                    // Re-assert fresh-air circulation (CYCLE=1 on the H6) + AQS off every
                    // 3 seconds so the drying really dries with outside air, as requested.
                    // v2.1: read-before-write — reescrever cegamente a cada 3s mandava ~80
                    // comandos externos por secagem e cada escrita popava o painel nativo do
                    // A/C na central por cima do GPS. Ler primeiro e escrever SÓ quando o
                    // módulo derivou mantém a secagem silenciosa e ainda briga com a deriva.
                    String aqs = getUpdatedData(CarConstants.CAR_HVAC_AQS_ENABLE.getValue());
                    if (aqs == null || !aqs.equals("0")) {
                        updateData(CarConstants.CAR_HVAC_AQS_ENABLE.getValue(), "0"); // AQS could force internal circulation back
                    }
                    String cycle = getUpdatedData(CarConstants.CAR_HVAC_CYCLE_MODE.getValue());
                    if (cycle == null || !cycle.equals("1")) {
                        updateData(CarConstants.CAR_HVAC_CYCLE_MODE.getValue(), "1"); // H6: 1 = fresh air / outside circulation
                    }
                }

                // dispatchServiceManagerEvent ja loga em WARN: o tick vira um heartbeat
                // de 1 Hz no gist de diagnostico sem custo de log extra.
                dispatchServiceManagerEvent(ServiceManagerEventType.DRYING_MODE_STATUS_CHANGED, remaining);
                // Re-posta so se continuarmos sendo o tick vigente (checagem fora do lock:
                // o pior caso de uma corrida aqui e UM no-op a mais, que a checagem de
                // identidade do proximo run descarta).
                if (isCurrentTick(this)) {
                    final Handler handler = backgroundHandler;
                    // Sem looper nao existe proximo tick — e um ciclo ativo sem tick e
                    // exatamente o desfecho que nao pode acontecer. Encerra aqui: o POWER=0
                    // do fim sai por updateData, que nao depende do handler.
                    if (handler == null || !handler.postDelayed(this, 1000L)) {
                        Log.w(TAG, "[SECAGEM] nao consegui repostar o tick — encerrando o ciclo agora");
                        requestFinish(false, "sem handler para o proximo tick");
                    }
                }
            }
        };
    }

    /** Este Runnable ainda e o tick vigente do motor de secagem? */
    private boolean isCurrentTick(Runnable tick) {
        synchronized (dryingLock) {
            return dryingTickRunnable == tick;
        }
    }

    /**
     * Re-deriva a memoria do compressor na borda de subida do ready-state. Aqui a flag pode
     * ser DESARMADA — e a unica hora em que isso e verdade: a viagem nova se define por este
     * instante, e uma notificacao do desligamento anterior nao pode sobreviver para a viagem
     * seguinte e fazer secar quem nao usou A/C.
     *
     * Pergunta ao MODULO, com uma leitura fresca, em vez de ler o cache: o cache guarda toda
     * entrega, inclusive a que chegou com o carro desligado (o modulo segue reportando o
     * botao do A/C depois de desligar), e re-derivar daquele valor ressuscitaria exatamente o
     * falso-positivo. A resposta do modulo no instante em que a ignicao sobe e a verdade da
     * viagem nova — e se ele reportar o valor novo so um instante depois, o push seguinte
     * arma a flag do mesmo jeito, porque no meio da viagem ela so e armada.
     *
     * Vai para o backgroundHandler porque este ramo roda tambem na MAIN thread e getUpdatedData
     * e um fetchData two-way sem timeout.
     */
    private void rederiveCompressorFlag() {
        final String key = CarConstants.CAR_HVAC_AC_ENABLE.getValue();
        final Handler handler = backgroundHandler;
        if (handler == null) return;
        handler.post(() -> {
            final String current = getUpdatedData(key);
            if (current == null) return; // binder fora: mantem o que ja havia
            final boolean compressorOn = "1".equals(current);
            acCompressorRanThisIgnition = compressorOn;
            Log.w(TAG, "[SECAGEM] viagem nova: compressorRodou=" + compressorOn
                    + " (ac_enable=" + current + " lido do modulo)");
        });
    }

    /**
     * Lote de reafirmacao cega da secagem de desligamento.
     *
     * POWER e AC entram no lote porque sao justamente as duas chaves que DEFINEM a
     * funcionalidade, e as duas que o modulo tem motivo proprio para derrubar: o power-off
     * dele mesmo desliga a ventilacao, e o compressor so voltaria se alguem o religasse.
     * Reafirmar so fan/temp deixaria "secando" com o ventilador parado — o defeito
     * silencioso, que parece funcionar no log e nao seca nada.
     *
     * CADA escrita reconfere a posse, e nao so o lote. `isCurrentTick` e check-then-act: o
     * encerramento pode entrar entre a checagem e as escritas (usuario mexendo na central,
     * carro religando), e um POWER=1 nosso saindo DEPOIS do POWER=0 do encerramento deixaria
     * o ar ligado num carro desligado sem tick nenhum para corrigir. A janela entre a
     * checagem e a escrita continua existindo — um par de instrucoes, nao o lote inteiro.
     */
    private void reassertBatch(Runnable tick) {
        reassertOwned(tick, CarConstants.CAR_HVAC_POWER_MODE.getValue(), "1");
        reassertOwned(tick, CarConstants.CAR_HVAC_AC_ENABLE.getValue(), "0");
        reassertOwned(tick, CarConstants.CAR_HVAC_FAN_SPEED.getValue(), "7");
        reassertOwned(tick, CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue(), "32.0");
        reassertOwned(tick, CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue(), "32.0");
        reassertOwned(tick, CarConstants.CAR_HVAC_AQS_ENABLE.getValue(), "0");
        // CYCLE por ULTIMO, o mesmo cuidado do perfil inicial: e a chave que define "ar de
        // fora" e nada depois dela pode reverter a circulacao.
        reassertOwned(tick, CarConstants.CAR_HVAC_CYCLE_MODE.getValue(), "1");
    }

    private void reassertOwned(Runnable tick, String key, String value) {
        if (isCurrentTick(tick)) {
            updateData(key, value);
        }
    }

    public void cancelDryingMode() {
        requestFinish(true, "cancelada pelo usuario");
    }

    public boolean isDryingModeActive() {
        return dryingKindFlag != DryingKind.NONE;
    }

    public boolean isShutdownDryingActive() {
        return dryingKindFlag == DryingKind.SHUTDOWN;
    }

    /**
     * Espelhos volatile publicados SOB dryingLock. Os acessores publicos NUNCA pegam
     * dryingLock de proposito: requestFinish libera o lock e depois chama
     * dispatchServiceManagerEvent, e os listeners (MainMenu, InstrumentProjector2)
     * chamam isDryingModeActive() de volta. Se o acessor pegasse o lock, seria
     * T1 segura o lock -> dispara evento -> listener em T2 bloqueia -> T1 espera T2.
     * Monitores Java sao reentrantes, entao o caso mesma-thread se salva; o caso
     * cross-thread e deadlock de verdade.
     */
    private void publishDryingFlags() {
        dryingKindFlag = dryingKind;
    }

    /**
     * Encerra a secagem. Transicao de estado sob o lock, I/O FORA dele — e isso que
     * torna a analise de deadlock trivial: ninguem bloqueia segurando o lock.
     */
    private void requestFinish(boolean restorePower, String reason) {
        final DryingKind kind;
        final Map<String, String> snapshot;
        synchronized (dryingLock) {
            if (dryingKind == DryingKind.NONE) return; // exatamente-uma-vez
            kind = dryingKind;
            dryingKind = DryingKind.NONE; // o tick e qualquer cancelador viram no-op ja
            snapshot = new HashMap<>(dryingSnapshot); // copia: os envios fogem do lock
            dryingSnapshot.clear();
            dryingRemainingSeconds = 0;
            dryingDeadlineElapsed = 0L;
            // backgroundHandler pode ja ter sido anulado pelo cleanup do initializeServices
            // enquanto este requestFinish roda (o tick vive no looper antigo). Sem a guarda,
            // um NPE aqui subiria pela pilha do HandlerThread e mataria a thread — deixando o
            // app inteiro sem background sem nenhum sinal claro do porque.
            final Handler handler = backgroundHandler;
            if (dryingTickRunnable != null) {
                if (handler != null) handler.removeCallbacks(dryingTickRunnable);
                dryingTickRunnable = null;
            }
            // Janela do banco encurtada, nao zerada: a notificacao do NOSSO ultimo POWER=1
            // pode estar em voo agora, e zerar aqui deixaria esse eco passar pelo ramo de
            // ventilacao, ligando o banco logo depois de o ciclo ter terminado. 3s cobrem o
            // eco e nao atrapalham nada depois: o POWER=0 daqui de baixo ja vai reenviar a
            // notificacao e zerar o banco de qualquer forma.
            if (kind == DryingKind.SHUTDOWN) {
                suppressSeatVentBoostUntilElapsed = SystemClock.elapsedRealtime() + 3_000L;
            }
            publishDryingFlags();
        }
        // ---- sem lock daqui para baixo ----
        try {
            // O POWER=0 do desligamento e a escrita que NAO pode se perder: sem ela o modulo
            // continua achando que esta secando. Se ela falhar (controlService morto, ou
            // RemoteException no meio), a flag de pendencia em disco NAO e apagada — ela vira
            // a unica evidencia de que um ciclo terminou sem o comando chegar, que e
            // exatamente o que o diagnostico do H5 precisa distinguir de "perda de energia".
            final boolean terminalPowerSent = writeSnapshotAndPower(snapshot, kind, restorePower);
            Log.w(TAG, "[SECAGEM] " + kind + " encerrada (" + reason + ") power="
                    + (kind == DryingKind.SHUTDOWN ? "0"
                       : (restorePower ? "restaurado(" + snapshot.get(CarConstants.CAR_HVAC_POWER_MODE.getValue()) + ")" : "0"))
                    + " entregue=" + terminalPowerSent
                    + " elapsed=" + SystemClock.elapsedRealtime());
            if (kind == DryingKind.SHUTDOWN && terminalPowerSent) {
                clearShutdownDryingPending();
            } else if (kind == DryingKind.SHUTDOWN) {
                Log.w(TAG, "[SECAGEM] POWER=0 NAO foi entregue — mantendo a pendencia em disco"
                        + " para a proxima subida contar (o modulo pode ter ficado em POWER=1)");
            }
        } catch (Exception e) {
            Log.e(TAG, "[SECAGEM] erro ao encerrar", e);
        } finally {
            dispatchServiceManagerEvent(ServiceManagerEventType.DRYING_MODE_STATUS_CHANGED, 0);
        }
    }

    /**
     * Desfaz o perfil: escreve o snapshot inteiro (menos o power) e o power por ULTIMO.
     * Devolve se o comando de power foi de fato entregue ao binder.
     *
     * O power vai por ultimo de proposito: o snapshot e um HashMap, entao iterar direto
     * manda os comandos em ordem arbitraria — e se o POWER=0 saisse antes do FAN=anterior,
     * o modulo reacordava a ventilacao no comando de fan, deixando o ar ligado depois de a
     * interface ja mostrar "desligado".
     *
     * `restorePreviousPower=false` forca o power a "0" (desligamento do veiculo);
     * `true` devolve o valor que estava antes da secagem (cancelamento manual).
     */
    private boolean writeSnapshotAndPower(Map<String, String> snapshot, DryingKind kind, boolean restorePreviousPower) {
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            if (entry.getValue() == null) continue;
            if (entry.getKey().equals(CarConstants.CAR_HVAC_POWER_MODE.getValue())) continue;
            updateData(entry.getKey(), entry.getValue());
        }
        final String previousPower = snapshot.get(CarConstants.CAR_HVAC_POWER_MODE.getValue());
        if (kind == DryingKind.SHUTDOWN) {
            // NUNCA restaura o power anterior: o estado anterior e da ignicao que acabou, e
            // ligar a ventilacao com o carro desligado a deixaria ligada para sempre. Os
            // valores (temp/fan/circulacao) voltam ao que eram.
            return updateDataChecked(CarConstants.CAR_HVAC_POWER_MODE.getValue(), "0");
        }
        // Sem teste de null no caminho: se a leitura do snapshot falhou no start (binder
        // morto naquele instante), previousPower e null e a versao anterior simplesmente nao
        // mandava comando NENHUM — deixando POWER=1 num ciclo que acabou de anunciar que
        // terminou. Na duvida, desligar e o lado seguro. Natural completion leaves the HVAC
        // off; manual cancel restores the previous power state.
        return updateDataChecked(CarConstants.CAR_HVAC_POWER_MODE.getValue(),
                (restorePreviousPower && previousPower != null) ? previousPower : "0");
    }

    /**
     * Marca que um ciclo de desligamento comecou. commit() e nao apply(): isto precisa
     * estar NO DISCO antes do primeiro comando de HVAC ir para a rua — apply() e
     * assincrono e uma queda de energia nos proximos milissegundos perderia a flag,
     * que e exatamente o que queremos registrar. Rodamos no backgroundHandler.
     */
    private void markShutdownDryingPending(String prevPower, String prevAc) {
        try {
            sharedPreferences.edit()
                    .putBoolean(SharedPreferencesKeys.SHUTDOWN_DRYING_PENDING.getKey(), true)
                    .putLong(SharedPreferencesKeys.SHUTDOWN_DRYING_STARTED_AT.getKey(), System.currentTimeMillis())
                    .commit();
            Log.w(TAG, "[SECAGEM] pendencia gravada prevPower=" + prevPower + " prevAc=" + prevAc);
        } catch (Exception e) {
            Log.e(TAG, "[SECAGEM] erro ao gravar pendencia", e);
        }
    }

    private void clearShutdownDryingPending() {
        sharedPreferences.edit().putBoolean(SharedPreferencesKeys.SHUTDOWN_DRYING_PENDING.getKey(), false).apply();
    }

    /**
     * v2.7: a central pode perder energia no meio da secagem — o processo morre, o tick
     * nunca roda e o HVAC fica em POWER=1 / 32 C / fan 7. Aqui a gente registra o fato:
     * e ele que explica o residual no log e responde se o estado do HVAC sobrevive
     * entre ignicoes. NADA e forcado aqui de proposito — so depois de medir.
     */
    private void reportInterruptedShutdownDrying(String where) {
        if (!sharedPreferences.getBoolean(SharedPreferencesKeys.SHUTDOWN_DRYING_PENDING.getKey(), false)) return;
        long startedAt = sharedPreferences.getLong(SharedPreferencesKeys.SHUTDOWN_DRYING_STARTED_AT.getKey(), 0L);
        Log.w(TAG, "[SECAGEM] ciclo de desligamento NAO terminou — onde=" + where
                + " idade=" + (startedAt == 0L ? "?" : (System.currentTimeMillis() - startedAt) + "ms")
                + " (o HVAC pode ter ficado em POWER=1 / 32C / fan 7)");
        clearShutdownDryingPending();
    }

    public boolean isMaxAcActive() {
        return isMaxAcActive;
    }

    // v2.5: superfície de sondagem do DVR (card "Teste DVR" na aba Diagnóstico).
    // A interface IDvr já era obtida no initializeServices (queryBinder 8) para o AVM;
    // estes wrappers expõem estado e comandos que ainda não têm consumidor. Os
    // parâmetros de capturePhoto/captureVideo são desconhecidos — o card permite
    // testar valores no carro; cada chamada vai para o logcat em WARN (o head unit
    // filtra INFO) e aparece no envio de logs do Diagnóstico.
    public String dvrStatusSnapshot() {
        try {
            if (dvr == null) {
                Log.w(TAG, "[DVR] binder IDvr não disponível (queryBinder 8 falhou?)");
                return "binder DVR não disponível";
            }
            String snapshot = "isDvrSupported=" + dvr.isDvrSupported()
                + " | captureStatus=" + dvr.getCaptureStatus()
                + " | currentMode=" + dvr.getCurrentMode()
                + " | inDvrApp=" + dvr.isInDvrApp()
                + " | inPreviewView=" + dvr.isInPreviewView();
            Log.w(TAG, "[DVR] estado: " + snapshot);
            return snapshot;
        } catch (RemoteException e) {
            Log.w(TAG, "[DVR] falha ao ler estado", e);
            return "erro ao ler estado: " + e.getMessage();
        }
    }

    public String dvrOpenApp() {
        try {
            if (dvr == null) return "binder DVR não disponível";
            dvr.startBeanDvr();
            Log.w(TAG, "[DVR] startBeanDvr() enviado");
            return "startBeanDvr enviado — o app DVR deve abrir na tela";
        } catch (RemoteException e) {
            Log.w(TAG, "[DVR] falha no startBeanDvr", e);
            return "erro: " + e.getMessage();
        }
    }

    public String dvrCloseApp() {
        try {
            if (dvr == null) return "binder DVR não disponível";
            dvr.closeBeanDvr();
            Log.w(TAG, "[DVR] closeBeanDvr() enviado");
            return "closeBeanDvr enviado";
        } catch (RemoteException e) {
            Log.w(TAG, "[DVR] falha no closeBeanDvr", e);
            return "erro: " + e.getMessage();
        }
    }

    public String dvrCapturePhoto(int paramA, int paramB) {
        try {
            if (dvr == null) return "binder DVR não disponível";
            dvr.capturePhoto(paramA, paramB);
            Log.w(TAG, "[DVR] capturePhoto(" + paramA + ", " + paramB + ") enviado");
            return "capturePhoto(" + paramA + ", " + paramB + ") enviado";
        } catch (RemoteException e) {
            Log.w(TAG, "[DVR] falha no capturePhoto(" + paramA + ", " + paramB + ")", e);
            return "erro: " + e.getMessage();
        }
    }

    public String dvrCaptureVideo(int paramA, int paramB) {
        try {
            if (dvr == null) return "binder DVR não disponível";
            dvr.captureVideo(paramA, paramB);
            Log.w(TAG, "[DVR] captureVideo(" + paramA + ", " + paramB + ") enviado");
            return "captureVideo(" + paramA + ", " + paramB + ") enviado";
        } catch (RemoteException e) {
            Log.w(TAG, "[DVR] falha no captureVideo(" + paramA + ", " + paramB + ")", e);
            return "erro: " + e.getMessage();
        }
    }

    // v1.8: nao dispara mais direto ao ligar a ignicao — aguarda 3s e confirma a
    // leitura (o sensor pode estar estabilizando), com clamp de plausibilidade.
    private void enableMaxAcOn() {
        try {
            if (isMaxAcActive) return;
            if (maxAcConfirmRunnable != null) {
                backgroundHandler.removeCallbacks(maxAcConfirmRunnable);
            }
            maxAcConfirmRunnable = () -> {
                maxAcConfirmRunnable = null;
                confirmAndActivateMaxAc();
            };
            backgroundHandler.postDelayed(maxAcConfirmRunnable, 3000);
            Log.d(TAG, "Max AC: confirmacao de 3s agendada apos ready-state on");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling Max AC confirmation", e);
        }
    }

    private void cancelPendingMaxAcConfirmation() {
        if (maxAcConfirmRunnable != null) {
            backgroundHandler.removeCallbacks(maxAcConfirmRunnable);
            maxAcConfirmRunnable = null;
            Log.d(TAG, "Max AC: confirmacao pendente cancelada (carro desligou)");
        }
    }

    private void confirmAndActivateMaxAc() {
        try {
            if (isMaxAcActive) return;
            String tempStr = getUpdatedData(CarConstants.CAR_BASIC_INSIDE_TEMP.getValue());
            if (tempStr == null) {
                Log.w(TAG, "Max AC: temperatura indisponivel na confirmacao, nao dispara");
                return;
            }
            float currentTemp;
            try {
                currentTemp = Float.parseFloat(tempStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Max AC: leitura de temperatura invalida (\"" + tempStr + "\"), nao dispara");
                return;
            }
            // Clamp de plausibilidade: fora de 10..70°C a leitura nao e confiavel
            // (sensor inicializando / dados sujos) — nao dispara.
            if (currentTemp < 10f || currentTemp > 70f) {
                Log.w(TAG, "Max AC: temperatura fora do intervalo plausivel (" + currentTemp + "°C), nao dispara");
                return;
            }
            float threshold = sharedPreferences.getFloat(SharedPreferencesKeys.MAX_AC_ON_UNLOCK_THRESHOLD.getKey(), 35.0f);
            if (currentTemp < threshold) {
                Log.d(TAG, "Max AC: temperatura " + currentTemp + "°C abaixo do trigger " + threshold + "°C apos confirmacao, nao dispara");
                return;
            }
            Log.w(TAG, "Max AC: trigger confirmado (" + currentTemp + "°C >= " + threshold + "°C), ativando");
            activateMaxAc();
        } catch (Exception e) {
            Log.e(TAG, "Error in Max AC confirmation logic", e);
        }
    }

    private void activateMaxAc() {
        try {
            if (isMaxAcActive) return;
            // v2.7: a secagem de desligamento nao e atropelavel por automacao. Aqui a
            // consequencia de deixar passar e concreta: 16 C com o compressor ligado num
            // carro desligado. A secagem MANUAL continua sendo cancelada, como sempre foi.
            if (isShutdownDryingActive()) {
                Log.w(TAG, "Max AC ignorado: secagem de desligamento em andamento");
                return;
            }
            cancelDryingMode();
            String prevPower = getUpdatedData(CarConstants.CAR_HVAC_POWER_MODE.getValue());
            String prevEnabled = getUpdatedData(CarConstants.CAR_HVAC_AC_ENABLE.getValue());
            String prevFan = getUpdatedData(CarConstants.CAR_HVAC_FAN_SPEED.getValue());
            String prevDriverTemp = getUpdatedData(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue());
            String prevPassTemp = getUpdatedData(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue());
            String prevAuto = getUpdatedData(CarConstants.CAR_HVAC_AUTO_ENABLE.getValue());
            String prevAnion = getUpdatedData(CarConstants.CAR_HVAC_ANION_ENABLE.getValue());
            String prevAQS = getUpdatedData(CarConstants.CAR_HVAC_AQS_ENABLE.getValue());
            String prevSync = getUpdatedData(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue());
            String prevSeatVent = getUpdatedData(CarConstants.CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL.getValue());
            String prevBlower = getUpdatedData(CarConstants.CAR_HVAC_BLOWER_MODE.getValue());

            previousAcState.put(CarConstants.CAR_HVAC_POWER_MODE.getValue(), prevPower);
            previousAcState.put(CarConstants.CAR_HVAC_AC_ENABLE.getValue(), prevEnabled);
            previousAcState.put(CarConstants.CAR_HVAC_FAN_SPEED.getValue(), prevFan);
            previousAcState.put(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue(), prevDriverTemp);
            previousAcState.put(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue(), prevPassTemp);
            previousAcState.put(CarConstants.CAR_HVAC_AUTO_ENABLE.getValue(), prevAuto);
            previousAcState.put(CarConstants.CAR_HVAC_ANION_ENABLE.getValue(), prevAnion);
            previousAcState.put(CarConstants.CAR_HVAC_AQS_ENABLE.getValue(), prevAQS);
            previousAcState.put(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue(), prevSync);
            previousAcState.put(CarConstants.CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL.getValue(), prevSeatVent);
            previousAcState.put(CarConstants.CAR_HVAC_BLOWER_MODE.getValue(), prevBlower);

            updateData(CarConstants.CAR_HVAC_POWER_MODE.getValue(), "1");
            updateData(CarConstants.CAR_HVAC_AUTO_ENABLE.getValue(), "0");
            updateData(CarConstants.CAR_HVAC_FAN_SPEED.getValue(), "7");
            updateData(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue(), "16.0");
            updateData(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue(), "16.0");
            updateData(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue(), "1");
            updateData(CarConstants.CAR_HVAC_AC_ENABLE.getValue(), "1");
            // Opções do Max AC: ventilação do banco (motorista) e direção do ar.
            if (sharedPreferences.getBoolean(SharedPreferencesKeys.MAX_AC_SEAT_VENTILATION.getKey(), false)) {
                updateData(CarConstants.CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL.getValue(), "3");
            }
            String maxAcBlower = sharedPreferences.getString(SharedPreferencesKeys.MAX_AC_BLOWER_MODE.getKey(), "");
            if (maxAcBlower != null && !maxAcBlower.isEmpty()) {
                updateData(CarConstants.CAR_HVAC_BLOWER_MODE.getValue(), maxAcBlower);
            }

            isMaxAcActive = true;
            // v1.8: marca o momento do disparo para a janela de histerese
            maxAcActivatedAt = System.currentTimeMillis();

            int timeoutMinutes = sharedPreferences.getInt(SharedPreferencesKeys.MAX_AC_TIMEOUT.getKey(), 0);
            if (timeoutMinutes > 0) {
                if (maxAcTimeoutRunnable != null) {
                    backgroundHandler.removeCallbacks(maxAcTimeoutRunnable);
                }
                maxAcTimeoutRunnable = () -> {
                    Log.w(TAG, "Max AC timeout reached, aborting");
                    cancelMaxAcMode();
                };
                backgroundHandler.postDelayed(maxAcTimeoutRunnable, timeoutMinutes * 60 * 1000L);
                Log.w(TAG, "Max AC timeout scheduled for " + timeoutMinutes + " minutes");
            }

            Log.w(TAG, "Max AC activated (power on, high temp)");
        } catch (Exception e) {
            Log.e(TAG, "Error in Max AC Activation logic", e);
        }
    }

    private void updateMaxAcSmoothing() {
        if (!isMaxAcActive) return;
        try {
            String tempStr = getUpdatedData(CarConstants.CAR_BASIC_INSIDE_TEMP.getValue());
            if (tempStr == null) return;
            float currentTemp = Float.parseFloat(tempStr);
            float targetTemp = sharedPreferences.getFloat(SharedPreferencesKeys.MAX_AC_TARGET_TEMP.getKey(), 28.0f);
            float smoothingRange = 2.0f;
            float startSmoothingTemp = targetTemp + smoothingRange;

            // v1.8: janela de histerese de 60s — se o disparo foi espurio (leitura de
            // boot acima do trigger que depois se estabilizou), desliga ao cair abaixo
            // do trigger em vez de esperar chegar na temperatura alvo.
            float threshold = sharedPreferences.getFloat(SharedPreferencesKeys.MAX_AC_ON_UNLOCK_THRESHOLD.getKey(), 35.0f);
            boolean withinHysteresisWindow = maxAcActivatedAt > 0L
                    && (System.currentTimeMillis() - maxAcActivatedAt) < 60_000L;
            float earlyCancelTemp = Math.max(targetTemp, threshold - 1f);

            if (currentTemp <= targetTemp) {
                cancelMaxAcMode();
                Log.w(TAG, "Max AC deactivated, temperature reached target: " + targetTemp);
            } else if (withinHysteresisWindow && currentTemp <= earlyCancelTemp) {
                cancelMaxAcMode();
                Log.w(TAG, "Max AC deactivated within 60s hysteresis window (falso positivo), temp "
                        + currentTemp + "°C abaixo do trigger " + threshold + "°C");
            } else if (currentTemp < startSmoothingTemp) {
                float factor = (currentTemp - targetTemp) / smoothingRange;
                factor = Math.max(0f, Math.min(1f, factor));

                String prevFanStr = previousAcState.get(CarConstants.CAR_HVAC_FAN_SPEED.getValue());
                int prevFan = (prevFanStr != null) ? Integer.parseInt(prevFanStr) : 3;
                int maxFan = 7;
                int newFan = prevFan + Math.round((maxFan - prevFan) * factor);
                newFan = Math.max(3, newFan);

                String prevDriverKey = CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue();
                float minTemp = 16.0f;
                float prevDriverTemp = (previousAcState.get(prevDriverKey) != null) ? Float.parseFloat(previousAcState.get(prevDriverKey)) : 22.0f;
                float newDriverTemp = prevDriverTemp - ((prevDriverTemp - minTemp) * factor);
                newDriverTemp = Math.min(20, newDriverTemp);

                updateData(CarConstants.CAR_HVAC_FAN_SPEED.getValue(), String.valueOf(newFan));
                updateData(prevDriverKey, String.format(java.util.Locale.US, "%.1f", newDriverTemp));

                Log.d(TAG, "Max AC Smoothing: Temp=" + currentTemp + ", Factor=" + factor + ", Fan=" + newFan + ", DriverTemp=" + newDriverTemp);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in Max AC Smoothing logic", e);
        }
    }

    public void executeWithServicesRunning(Runnable task) {
        Runnable wrapperWithCatch = () -> {
            try {
                task.run();
            } catch (Exception e) {
                Log.e(TAG, "Error executing task", e);
            }
        };
        if (servicesInitialized) {
            wrapperWithCatch.run();
        } else {
            synchronized (pendingTasks) {
                pendingTasks.add(wrapperWithCatch);
            }
        }
    }

    public void switchUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Invalid user ID provided for switchUser");
            return;
        }
        executeWithServicesRunning(() -> {
            var currentUser = sharedPreferences.getString(SharedPreferencesKeys.CURRENT_USER.getKey(), "");
            if (currentUser.equals(userId)) {
                Log.w(TAG, "Current user is already: " + userId);
                return;
            }

            Log.w(TAG, "Switching user to: " + userId);

            try {
                saveCarSettingsForUser(currentUser);
            } catch (Exception e) {
                Log.e(TAG, "Error saving settings for user: " + currentUser, e);
            }

            try {
                restoreCarSettingsForUser(userId);
            } catch (Exception e) {
                Log.e(TAG, "Error restoring settings for user: " + userId, e);
            }

            sharedPreferences.edit()
                    .putString(SharedPreferencesKeys.CURRENT_USER.getKey(), userId)
                    .apply();
        });
    }

    private void restoreCarSettingsForUser(String userId) {
        File file = new File(App.getContext().getFilesDir(), userId + ".settings.json");
        if (!file.exists()) {
            Log.w(TAG, "No saved settings found for user: " + userId);
            return;
        }
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            JsonElement userSettingsMap = gson.fromJson(reader, JsonElement.class);
            if (!(userSettingsMap instanceof JsonObject)) {
                Log.e(TAG, "Error parsing user settings JSON for user: " + userId);
                return;
            }

            JsonObject userSettings = (JsonObject) userSettingsMap;
            for (Map.Entry<String, JsonElement> entry : userSettings.entrySet()) {
                String key = entry.getKey();
                if (Arrays.stream(KEYS_TO_SAVE).noneMatch(k -> k.getValue().equals(key))) {
                    continue;
                }
                String value = entry.getValue().getAsString();
                if (value.isEmpty()) {
                    Log.w(TAG, "Skipping empty value for key: " + key);
                    continue;
                }
                try {
                    updateData(key, value);
                    Log.w(TAG, "Restored setting for user " + userId + ": " + key + " = " + value);
                } catch (Exception e) {
                    Log.e(TAG, "Error restoring setting for user " + userId + ": " + key, e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading settings file for user: " + userId, e);
        }
    }

    private void saveCarSettingsForUser(String userId) {
        Map<String, String> settingsToSave = new HashMap<>();

        for (CarConstants key : KEYS_TO_SAVE) {
            String value = getUpdatedData(key.getValue());
            settingsToSave.put(key.getValue(), value);
        }

        if (settingsToSave.isEmpty()) {
            Log.w(TAG, "No settings to save for user: " + userId);
            return;
        }

        Gson gson = new Gson();
        JsonObject jsonObject = new com.google.gson.JsonObject();
        for (Map.Entry<String, String> entry : settingsToSave.entrySet()) {
            jsonObject.addProperty(entry.getKey(), entry.getValue());
        }

        File file = new File(App.getContext().getFilesDir(), userId + ".settings.json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(jsonObject, writer);
            Log.w(TAG, "Saved settings for user: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error writing settings file for user: " + userId, e);
        }
    }

    public int getTotalOdometer() {
        var totalOdometer = getData(CarConstants.CAR_BASIC_TOTAL_ODOMETER.getValue());
        if (totalOdometer == null || totalOdometer.isEmpty()) {
            Log.w(TAG, "Total odometer data is not available");
            return 0;
        }
        try {
            return Integer.parseInt(totalOdometer);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing total odometer value: " + totalOdometer, e);
            return 0;
        }
    }

    public void updateMonitoringProperties() {
        executeWithServicesRunning(() -> {
            try {
                var allKeys = getCombinedKeys();
                controlService.addListenerKey(App.getContext().getPackageName(), allKeys);
                for (String s : new HashSet<>(dataCache.keySet())) {
                    dataCache.remove(s);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error updating monitoring properties", e);
            }
            dispatchAllData();
        });
    }

    public String[] getCombinedKeys() {
        List<String> keys = new ArrayList<>();
        keys.addAll(List.of(CarConstants.FromArray(DEFAULT_KEYS)));
        keys.addAll(sharedPreferences.getStringSet(SharedPreferencesKeys.CAR_MONITOR_PROPERTIES.getKey(), new HashSet<>()));
        return keys.toArray(new String[0]);
    }

    public void initializeFrida() {
        if (isFridaInitialized)
            return;
        isFridaInitialized = true;

        if (!tryInitializeFrida()) {
            sharedPreferences.edit()
                    .putBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOKS.getKey(), false)
                    .apply();
            Log.e(TAG, "Frida initialization failed, disabling Frida hooks");
        }
    }

    private boolean tryInitializeFrida() {
        try {
            if (!FridaUtils.ensureFridaServerRunning()) {
                Log.e(TAG, "Failed to ensure Frida server is running");
                return false;
            }
            Log.w(TAG, "Frida server is running, injecting scripts...");
            if (!FridaUtils.injectAllScripts())
                return false;
            if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOK_SYSTEM_SERVER.getKey(), false)) {
                backgroundHandler.postDelayed(() -> {
                    try {
                        FridaUtils.injectSystemServer();
                    } catch (Exception e) {
                        Log.e(TAG, "Error injecting into system_server", e);
                    }
                }, 10000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during Frida script injection", e);
            return false;
        }

        Log.w(TAG, "Frida initialization completed successfully");
        return true;
    }

    public void ensureSystemApps() {
        if (sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_PROJECTOR.getKey(), false) && sharedPreferences.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.getKey(), false)) {
            disableSystemApp("com.beantechs.multidisplay");
        } else {
            enableSystemApp("com.beantechs.multidisplay");
        }
    }

    public void disableSystemApp(String packageName) {
        try {
            ShizukuUtils.runCommandAndGetOutput(new String[]{"pm", "uninstall", "--user", "0", packageName});
            ShizukuUtils.runCommandAndGetOutput(new String[]{"pkill", "-9", "-f", packageName});
        } catch (Exception e) {
            Log.e(TAG, "Error disabling system app: " + packageName, e);
        }
    }

    public void enableSystemApp(String packageName) {
        try {
            ShizukuUtils.runCommandAndGetOutput(new String[]{"pm", "install-existing", packageName});
        } catch (Exception e) {
            Log.e(TAG, "Error enabling system app: " + packageName, e);
        }
    }

    public boolean isServicesInitialized() {
        return servicesInitialized;
    }

    public void setTimeBootReceived(long l) {
        if (timeBootReceived != 0)
            return;
        timeBootReceived = l;
    }

    public long getTimeInitialized() {
        return timeInitialized;
    }

    public long getTimeBootReceived() {
        return timeBootReceived;
    }

    public long getTimeStartInitialization() {
        return timeStartInitialization;
    }

    public boolean isMainScreenOn() {
        try {
            var engineState = getData(CarConstants.CAR_BASIC_ENGINE_STATE.getValue());
            return engineState != null && !engineState.equals("-1") && !engineState.equals("15");
        } catch (Exception e) {
            return false;
        }
    }

    public CarInfo getCarInfo() {
        try {
            if (carInfo == null) {
                carInfo = new CarInfo(vehicleModel.getCarBrand(), vehicleModel.getVehicleModel(), vehicleModel.getVehicleType());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting car info", e);
            return new CarInfo("Unknown", "Unknown", "Unknown");
        }

        return carInfo;
    }

    public int getClusterCardView() {
        return clusterCardView;
    }

    private static IBinder getSystemService(String serviceName) {
        try {
            // Pode retornar null quando o serviço ainda não foi registrado no boot —
            // quem chama decide como reagir.
            return (IBinder) getService.invoke(null, serviceName);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Log.e(TAG, "Error getting system service: " + serviceName, e);
            throw new RuntimeException(e);
        }
    }

    private static Method getService;

    static {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            getService = sm.getMethod("getService", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Log.w(TAG, Log.getStackTraceString(e));
        }
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}