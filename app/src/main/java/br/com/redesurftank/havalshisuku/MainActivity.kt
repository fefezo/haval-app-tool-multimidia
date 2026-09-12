package br.com.redesurftank.havalshisuku

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Window
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.core.content.FileProvider
import androidx.core.content.edit
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.listeners.IDataChanged
import br.com.redesurftank.havalshisuku.managers.AutoBrightnessManager
import br.com.redesurftank.havalshisuku.managers.ServiceManager
import br.com.redesurftank.havalshisuku.models.AppInfo
import br.com.redesurftank.havalshisuku.models.CarConstants
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys
import br.com.redesurftank.havalshisuku.models.SteeringWheelCustomActionType
import br.com.redesurftank.havalshisuku.ui.components.AppColors
import br.com.redesurftank.havalshisuku.ui.components.AppDimensions
import br.com.redesurftank.havalshisuku.ui.components.SettingCard
import br.com.redesurftank.havalshisuku.ui.components.SettingItem
import br.com.redesurftank.havalshisuku.ui.components.StyledCard
import br.com.redesurftank.havalshisuku.ui.components.TwoColumnSettingsLayout
import br.com.redesurftank.havalshisuku.ui.theme.HavalShisukuTheme
import br.com.redesurftank.havalshisuku.utils.DiagnosticsCollector
import br.com.redesurftank.havalshisuku.utils.FridaUtils
import br.com.redesurftank.havalshisuku.utils.GistUploader
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils
import br.com.redesurftank.havalshisuku.utils.TelnetClientWrapper
import rikka.shizuku.Shizuku
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min
import androidx.compose.foundation.lazy.grid.items as gridItems

const val TAG = "HavalShisuku"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HavalShisukuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    val advancedUse = prefs.getBoolean(SharedPreferencesKeys.ADVANCE_USE.key, false)
    val selfInstallationCheck = prefs.getBoolean(SharedPreferencesKeys.SELF_INSTALLATION_INTEGRITY_CHECK.key, false)

    // Seções de configurações: cada uma vira um item do menu lateral
    val settingsSections = buildList {
        add(DrawerMenuItem("Clima", Icons.Default.AcUnit))
        add(DrawerMenuItem("Janelas e Teto", Icons.Default.Window))
        add(DrawerMenuItem("Som", Icons.Default.VolumeUp))
        add(DrawerMenuItem("Noite e Brilho", Icons.Default.DarkMode))
        add(DrawerMenuItem("Conforto", Icons.Default.Weekend))
        add(DrawerMenuItem("Ao Desligar", Icons.Default.PowerSettingsNew))
        // Só aparece quando tem conteúdo (o item de bypass exige uso avançado)
        if (advancedUse && !selfInstallationCheck) {
            add(DrawerMenuItem("Avançado", Icons.Default.Security))
        }
    }
    val tabItems = buildList {
        add(DrawerMenuItem("Telas", Icons.Default.SmartDisplay))
        add(DrawerMenuItem("Valores Atuais", Icons.Default.DeveloperMode))
        add(DrawerMenuItem("Instalar Apps", Icons.Default.ShoppingCart))
        add(DrawerMenuItem("Informações", Icons.Default.Info))
        add(DrawerMenuItem("Diagnóstico", Icons.Default.BugReport))
        if (advancedUse) {
            add(DrawerMenuItem("Frida Hooks", Icons.Default.Build))
        }
    }
    val menuItems = settingsSections + tabItems

    var selectedItem by remember { mutableStateOf(0) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // Fixed Side Menu
        Surface(
            modifier = Modifier
                .width(AppDimensions.MenuWidth)
                .fillMaxHeight(),
            color = Color(0xFF13151A),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()) // 12+ itens: menu rolável
            ) {
                menuItems.forEachIndexed { index, item ->
                    // Divisor visual entre as seções de configurações e as abas
                    if (index == settingsSections.size) {
                        HorizontalDivider(color = Color(0xFF2A2F37), thickness = 1.dp)
                    }
                    val animatedWidth by animateFloatAsState(
                        targetValue = if (selectedItem == index) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing
                        ),
                        label = "backgroundWidth"
                    )

                    val borderAlpha by animateFloatAsState(
                        targetValue = if (selectedItem == index) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 0,
                            delayMillis = 0
                        ),
                        label = "borderAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimensions.MenuItemHeight)
                            .clickable { selectedItem = index },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Animated background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedWidth)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF152031),
                                            Color(0xFF13151A)
                                        )
                                    )
                                )
                                .drawBehind {
                                    drawLine(
                                        color = Color(0xFF0B84FF).copy(alpha = borderAlpha),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = 10.dp.toPx()
                                    )
                                }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (selectedItem == index) AppColors.MenuSelectedIcon else AppColors.MenuUnselectedIcon,
                                modifier = Modifier.size(AppDimensions.IconSize)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                item.title,
                                color = if (selectedItem == index) AppColors.TextPrimary else AppColors.MenuUnselectedText,
                                fontSize = 20.sp,
                                fontWeight = if (selectedItem == index) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(AppColors.Background)
        ) {
            // Content Area
            ContentArea {
                when (selectedItem) {
                    in settingsSections.indices -> BasicSettingsTab(settingsSections[selectedItem].title)
                    else -> when (selectedItem - settingsSections.size) {
                        0 -> TelasTab()
                        1 -> CurrentValuesTab()
                        2 -> InstallAppsTab()
                        3 -> InformacoesTab()
                        4 -> DiagnosticsTab()
                        5 -> FridaHooksTab()
                    }
                }
            }
        }
    }
}

data class DrawerMenuItem(
    val title: String,
    val icon: ImageVector
)

// Direção do ar (car.hvac.blower_mode): valores do módulo GWM. Combinações somam
// os bits (1 = para-brisa, 2 = corpo, 4 = pés). O valor vazio mantém a direção atual.
private val startupAcBlowerOptions = listOf(
    "" to "Não mudar",
    "1" to "Para-brisa",
    "2" to "Corpo",
    "4" to "Pés",
    "3" to "Corpo + brisa",
    "5" to "Pés + brisa",
    "6" to "Pés + corpo",
    "7" to "Todos"
)

// Zonas da direção do ar (bits de car.hvac.blower_mode): 1 = para-brisa, 2 = corpo, 4 = pés.
// Marcar/desmarcar as zonas monta o bitmask; vazio = o Max AC não muda a direção.
private val blowerZoneOptions = listOf(
    1 to "Para-brisa",
    2 to "Corpo",
    4 to "Pés"
)

@Composable
private fun RowScope.StartupAcOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AppColors.Primary else AppColors.SurfaceVariant,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontSize = 12.sp, maxLines = 1)
    }
}

// Slider de temperatura em 0,5 °C (valor do slider = °C × 2, 32..64 = 16..32 °C),
// mesmo padrão visual do restante do app. Usado no card do modo padrão do A/C.
@Composable
private fun AcTempSlider(
    label: String,
    sliderValue: Int,
    onValueChange: (Int) -> Unit
) {
    Text(label, color = Color.White, fontSize = 15.sp)
    Spacer(modifier = Modifier.height(4.dp))
    Slider(
        value = sliderValue.toFloat(),
        onValueChange = { onValueChange(it.toInt()) },
        valueRange = 32f..64f,
        steps = 31,
        colors = SliderDefaults.colors(
            thumbColor = AppColors.Primary,
            activeTrackColor = AppColors.Primary,
            inactiveTrackColor = Color(0xFF2C3139),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        )
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicSettingsTab(section: String) {
    val context = LocalContext.current
    val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    var isAdvancedUse by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ADVANCE_USE.key, false)) }
    var selfInstallationCheck by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.SELF_INSTALLATION_INTEGRITY_CHECK.key, false)) }
    var bypassSelfInstallationCheck by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK.key, false)) }
    var disableMonitoring by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.DISABLE_MONITORING.key, false)) }
    var disableAvas by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.DISABLE_AVAS.key, false)) }
    var disableAvmCarStopped by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.DISABLE_AVM_CAR_STOPPED.key, false)) }
    var closeWindowOnPowerOff by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.CLOSE_WINDOW_ON_POWER_OFF.key, false)) }
    var closeWindowOnFoldMirror by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.CLOSE_WINDOW_ON_FOLD_MIRROR.key, false)) }
    var closeSunroofOnPowerOff by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_POWER_OFF.key, false)) }
    var closeSunroofOnFoldMirror by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_FOLD_MIRROR.key, false)) }
    var closeSunroofSunShadeOnCloseSunroof by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF.key, false)) }
    var enableCustomMenu by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_CUSTOM_MENU.key, false)) }
    var setStartupVolume by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.SET_STARTUP_VOLUME.key, false)) }
    var volume by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.STARTUP_VOLUME.key, 1)) }
    var setStartupAc by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.SET_STARTUP_AC.key, false)) }
    // Temperatura armazenada em unidades de 0,5 °C (16.0 °C = 32 ... 32.0 °C = 64) para o slider inteiro
    var startupAcTemp by remember { mutableIntStateOf((prefs.getString(SharedPreferencesKeys.STARTUP_AC_TEMPERATURE.key, "22.0")?.toFloatOrNull()?.times(2)?.toInt() ?: 44).coerceIn(32, 64)) }
    var startupAcBlower by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.STARTUP_AC_BLOWER_MODE.key, "") ?: "") }
    // H6: cycle_mode 1 = ar externo, 0 = interna (invertido vs padrão AOSP)
    var startupAcCycle by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.STARTUP_AC_CYCLE_MODE.key, "1") ?: "1") }
    var startupAcFan by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.STARTUP_AC_FAN_SPEED.key, 0)) }
    // v2.6: ligar o ar ao dar partida e estado do compressor. Antes o card so ajustava
    // os valores — com o ar desligado na partida nada mudava na tela do carro.
    var startupAcPower by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.STARTUP_AC_POWER.key, false)) }
    var startupAcCompressor by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.STARTUP_AC_COMPRESSOR.key, true)) }
    // Perfil "Modo padrão do A/C" (v2.0): temperaturas independentes por lado,
    // ventilação, circulação e direção do ar. Mesmo armazenamento do AC de partida
    // (0,5 °C por ponto de slider, string "22.0").
    var defaultAcDriverTemp by remember { mutableIntStateOf((prefs.getString(SharedPreferencesKeys.DEFAULT_AC_TEMPERATURE_DRIVER.key, "22.0")?.toFloatOrNull()?.times(2)?.toInt() ?: 44).coerceIn(32, 64)) }
    var defaultAcPassTemp by remember { mutableIntStateOf((prefs.getString(SharedPreferencesKeys.DEFAULT_AC_TEMPERATURE_PASSENGER.key, "22.0")?.toFloatOrNull()?.times(2)?.toInt() ?: 44).coerceIn(32, 64)) }
    var defaultAcFan by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.DEFAULT_AC_FAN_SPEED.key, 3).coerceIn(1, 7)) }
    var defaultAcCycle by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.DEFAULT_AC_CYCLE_MODE.key, "1") ?: "1") }
    var defaultAcBlower by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.DEFAULT_AC_BLOWER_MODE.key, "") ?: "") }
    var defaultAcCompressor by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.DEFAULT_AC_COMPRESSOR.key, true)) }
    var closeWindowsOnSpeed by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED.key, false)) }
    var closeSunroofOnSpeed by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED.key, false)) }
    var speedThreshold by remember { mutableFloatStateOf(prefs.getFloat(SharedPreferencesKeys.SPEED_THRESHOLD.key, 15f)) }
    var closeSunroofSpeedThreshold by remember { mutableFloatStateOf(prefs.getFloat(SharedPreferencesKeys.SUNROOF_SPEED_THRESHOLD.key, 15f)) }
    var enableMaxAcOnUnlock by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.key, true)) }
    var maxAcOnUnlockThreshold by remember { mutableFloatStateOf(prefs.getFloat(SharedPreferencesKeys.MAX_AC_ON_UNLOCK_THRESHOLD.key, 34f)) }
    var maxAcTargetTemp by remember { mutableFloatStateOf(prefs.getFloat(SharedPreferencesKeys.MAX_AC_TARGET_TEMP.key, 28f)) }
    var maxAcTimeout by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.MAX_AC_TIMEOUT.key, 0)) }
    var maxAcSeatVentilation by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.MAX_AC_SEAT_VENTILATION.key, false)) }
    var maxAcBlower by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.MAX_AC_BLOWER_MODE.key, "") ?: "") }
    var enableAutoBrightness by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_AUTO_BRIGHTNESS.key, false)) }
    var nightStartHour by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_START_HOUR.key, 20)) }
    var nightStartMinute by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_START_MINUTE.key, 0)) }
    var nightEndHour by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_END_HOUR.key, 6)) }
    var nightEndMinute by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_END_MINUTE.key, 0)) }
    var disableBluetoothOnPowerOff by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF.key, false)) }
    var disableHotspotOnPowerOff by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF.key, false)) }
    // v2.7: secagem ao desligar. Nasce ligada — o card existe para poder desligar.
    var enableShutdownDrying by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_SHUTDOWN_DRYING.key, true)) }
    var shutdownDryingDuration by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.SHUTDOWN_DRYING_DURATION.key, 60)) }
    var nightBrightnessLevel by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.AUTO_BRIGHTNESS_LEVEL_NIGHT.key, 1)) }
    var dayBrightnessLevel by remember { mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.AUTO_BRIGHTNESS_LEVEL_DAY.key, 10)) }
    var enableSeatVentilationOnAcOn by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON.key, false)) }
    var enableCustomSteeringWheelButtons by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS.key, false)) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Listas por seção (menu lateral): cada seção renderiza só os seus itens
    val advancedList = mutableListOf<SettingItem>()
    val windowsList = mutableListOf<SettingItem>()
    val climateList = mutableListOf<SettingItem>()
    val comfortList = mutableListOf<SettingItem>()
    val powerOffList = mutableListOf<SettingItem>()
    val nightList = mutableListOf<SettingItem>()
    val soundList = mutableListOf<SettingItem>()

    if (isAdvancedUse && !selfInstallationCheck) {
        advancedList.add(
            SettingItem(
                title = "Bypass de Verificação",
                description = SharedPreferencesKeys.BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK.description,
                checked = bypassSelfInstallationCheck,
                onCheckedChange = {
                    bypassSelfInstallationCheck = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK.key, it) }
                }
            )
        )
    }

    windowsList.addAll(
        listOfNotNull(
            SettingItem(
                title = "Fechar janela ao desligar o veículo",
                description = "Fecha automaticamente as janelas quando o motor é desligado",
                checked = closeWindowOnPowerOff,
                onCheckedChange = {
                    closeWindowOnPowerOff = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.CLOSE_WINDOW_ON_POWER_OFF.key, it) }
                }
            ),
            SettingItem(
                title = "Fechar janela ao recolher retrovisores",
                description = "Sincroniza fechamento das janelas com o recolhimento dos retrovisores",
                checked = closeWindowOnFoldMirror,
                onCheckedChange = {
                    closeWindowOnFoldMirror = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.CLOSE_WINDOW_ON_FOLD_MIRROR.key, it) }
                }
            ),
            SettingItem(
                title = "Fechar teto solar ao desligar",
                description = SharedPreferencesKeys.CLOSE_SUNROOF_ON_POWER_OFF.description,
                checked = closeSunroofOnPowerOff,
                onCheckedChange = {
                    closeSunroofOnPowerOff = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_POWER_OFF.key, it) }
                }
            ),
            SettingItem(
                title = "Fechar teto solar ao recolher retrovisores",
                description = SharedPreferencesKeys.CLOSE_SUNROOF_ON_FOLD_MIRROR.description,
                checked = closeSunroofOnFoldMirror,
                onCheckedChange = {
                    closeSunroofOnFoldMirror = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_FOLD_MIRROR.key, it) }
                }
            ),
            SettingItem(
                title = "Fechar cortina do teto solar",
                description = SharedPreferencesKeys.CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF.description,
                checked = closeSunroofSunShadeOnCloseSunroof,
                onCheckedChange = {
                    closeSunroofSunShadeOnCloseSunroof = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF.key, it) }
                }
            ),
            SettingItem(
                title = "Fechar janelas com velocidade",
                description = SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED.description,
                checked = closeWindowsOnSpeed,
                onCheckedChange = {
                    closeWindowsOnSpeed = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED.key, it) }
                },
                sliderValue = speedThreshold.toInt(),
                sliderRange = 10..120,
                onSliderChange = { newSpeed ->
                    speedThreshold = newSpeed.toFloat()
                    prefs.edit { putFloat(SharedPreferencesKeys.SPEED_THRESHOLD.key, newSpeed.toFloat()) }
                },
                sliderLabel = "Velocidade: $speedThreshold km/h"
            ),
            SettingItem(
                title = "Fechar teto solar com velocidade",
                description = SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED.description,
                checked = closeSunroofOnSpeed,
                onCheckedChange = {
                    closeSunroofOnSpeed = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED.key, it) }
                },
                sliderValue = closeSunroofSpeedThreshold.toInt(),
                sliderRange = 10..120,
                onSliderChange = { newSpeed ->
                    closeSunroofSpeedThreshold = newSpeed.toFloat()
                    prefs.edit { putFloat(SharedPreferencesKeys.SUNROOF_SPEED_THRESHOLD.key, newSpeed.toFloat()) }
                },
                sliderLabel = "Velocidade: ${closeSunroofSpeedThreshold.toInt()} km/h"
            )
        )
    )

    climateList.addAll(
        listOfNotNull(
            SettingItem(
                title = "A/C no máximo ao ligar o carro",
                description = SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.description,
                checked = enableMaxAcOnUnlock,
                onCheckedChange = {
                    enableMaxAcOnUnlock = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.key, it) }
                },
                sliderValue = maxAcOnUnlockThreshold.toInt(),
                sliderRange = 20..38,
                sliderStep = 1,
                onSliderChange = { newTemp ->
                    maxAcOnUnlockThreshold = newTemp.toFloat()
                    prefs.edit { putFloat(SharedPreferencesKeys.MAX_AC_ON_UNLOCK_THRESHOLD.key, newTemp.toFloat()) }
                },
                sliderLabel = "Temperatura de disparo: ${maxAcOnUnlockThreshold.toInt()}°C",
                customContent = if (enableMaxAcOnUnlock) {
                    {
                        val timeOptions = mapOf(0 to "Sem limite", 1 to "1 minuto", 3 to "3 minutos", 5 to "5 minutos")
                        var expanded by remember { mutableStateOf(false) }

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text(
                                    text = "Temperatura alvo: ${maxAcTargetTemp.toInt()}°C",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Slider(
                                    value = maxAcTargetTemp,
                                    onValueChange = { newTemp ->
                                        maxAcTargetTemp = newTemp
                                        prefs.edit { putFloat(SharedPreferencesKeys.MAX_AC_TARGET_TEMP.key, newTemp) }
                                    },
                                    valueRange = 18f..34f,
                                    steps = 15,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AppColors.Primary,
                                        activeTrackColor = AppColors.Primary,
                                        inactiveTrackColor = Color(0xFF2C3139),
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    )
                                )
                            }
                            Column {
                                Text(
                                    text = SharedPreferencesKeys.MAX_AC_TIMEOUT.description,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box {
                                    Text(
                                        text = timeOptions[maxAcTimeout] ?: "Sem limite",
                                        color = Color(0xFF4A9EFF),
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .background(Color(0xFF2A2F37), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .clickable { expanded = true }
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.background(Color(0xFF2A2F37))
                                    ) {
                                        timeOptions.forEach { (value, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label, color = Color.White) },
                                                onClick = {
                                                    maxAcTimeout = value
                                                    prefs.edit { putInt(SharedPreferencesKeys.MAX_AC_TIMEOUT.key, value) }
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Column {
                                Text(
                                    text = "Ventilação no banco do motorista",
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { maxAcSeatVentilation = !maxAcSeatVentilation },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = maxAcSeatVentilation,
                                        onCheckedChange = {
                                            maxAcSeatVentilation = it
                                            prefs.edit { putBoolean(SharedPreferencesKeys.MAX_AC_SEAT_VENTILATION.key, it) }
                                        }
                                    )
                                    Text(
                                        text = if (maxAcSeatVentilation) "Ventilação ligada (nível máximo)" else "Ventilação desligada",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Direção do ar (ligar ou desligar as saídas)",
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                blowerZoneOptions.forEach { (bit, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val current = maxAcBlower.toIntOrNull() ?: 0
                                                val checked = (current and bit) != 0
                                                val newValue = if (checked) current and bit.inv() else current or bit
                                                maxAcBlower = if (newValue == 0) "" else newValue.toString()
                                                prefs.edit { putString(SharedPreferencesKeys.MAX_AC_BLOWER_MODE.key, maxAcBlower) }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = ((maxAcBlower.toIntOrNull() ?: 0) and bit) != 0,
                                            onCheckedChange = {
                                                val current = maxAcBlower.toIntOrNull() ?: 0
                                                val checked = (current and bit) != 0
                                                val newValue = if (checked) current and bit.inv() else current or bit
                                                maxAcBlower = if (newValue == 0) "" else newValue.toString()
                                                prefs.edit { putString(SharedPreferencesKeys.MAX_AC_BLOWER_MODE.key, maxAcBlower) }
                                            }
                                        )
                                        Text(label, fontSize = 14.sp, color = Color.White)
                                    }
                                }
                                Text(
                                    text = if (maxAcBlower.isEmpty()) "Nenhuma selecionada — o Max AC não muda a direção do ar" else "Valor: $maxAcBlower",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                } else null
            )
        )
    )

    comfortList.addAll(
        listOfNotNull(
            SettingItem(
                title = "Manter desativado monitoramento de distrações",
                description = "Desabilita alertas de distração durante a condução",
                checked = disableMonitoring,
                onCheckedChange = {
                    disableMonitoring = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.DISABLE_MONITORING.key, it) }
                    ServiceManager.getInstance().setMonitoringEnabled(!it)
                }
            ),
            SettingItem(
                title = "Desativar AVAS",
                description = "Sistema de alerta de veículo silencioso",
                checked = disableAvas,
                onCheckedChange = {
                    disableAvas = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.DISABLE_AVAS.key, it) }
                    ServiceManager.getInstance().setAvasEnabled(!it)
                }
            ),
            SettingItem(
                title = "Desativar câmera AVM quando parado",
                description = "Desliga câmera de visão 360° quando o veículo está parado",
                checked = disableAvmCarStopped,
                onCheckedChange = {
                    disableAvmCarStopped = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.DISABLE_AVM_CAR_STOPPED.key, it) }
                }
            ),
            SettingItem(
                title = "Habilitar menu customizado no cluster",
                description = SharedPreferencesKeys.ENABLE_CUSTOM_MENU.description,
                checked = enableCustomMenu,
                onCheckedChange = {
                    enableCustomMenu = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_CUSTOM_MENU.key, it) }
                }
            )
        )
    )

    climateList.addAll(
        listOfNotNull(
            SettingItem(
                title = "Ligar ventilação do banco do motorisca com A/C ligado",
                description = SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON.description,
                checked = enableSeatVentilationOnAcOn,
                onCheckedChange = {
                    enableSeatVentilationOnAcOn = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON.key, it) }
                }
            )
        )
    )

    powerOffList.addAll(
        listOfNotNull(
            SettingItem(
                title = "Desligar bluetooth ao desligar",
                description = SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF.description,
                checked = disableBluetoothOnPowerOff,
                onCheckedChange = {
                    disableBluetoothOnPowerOff = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF.key, it) }
                }
            ),
            SettingItem(
                title = "Desligar ponto de acesso ao desligar",
                description = SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF.description,
                checked = disableHotspotOnPowerOff,
                onCheckedChange = {
                    disableHotspotOnPowerOff = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF.key, it) }
                }
            ),
            SettingItem(
                title = "Secar o ar-condicionado ao desligar",
                description = "Ao desligar o veículo, mantém a ventilação no máximo em temperatura máxima e com ar de fora, " +
                    "por alguns segundos, para secar o evaporador e evitar mofo e cheiro de umidade. " +
                    "Apenas ventilação: o compressor fica desligado (ligado ele voltaria a condensar água). " +
                    "Dispara só quando o A/C foi realmente usado naquela viagem.",
                checked = enableShutdownDrying,
                onCheckedChange = {
                    enableShutdownDrying = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_SHUTDOWN_DRYING.key, it) }
                },
                customContent = if (enableShutdownDrying) {
                    {
                        Text(
                            "Duração: ${shutdownDryingDuration}s",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StartupAcOptionButton(
                                label = "30s",
                                selected = shutdownDryingDuration == 30,
                                onClick = {
                                    shutdownDryingDuration = 30
                                    prefs.edit { putInt(SharedPreferencesKeys.SHUTDOWN_DRYING_DURATION.key, 30) }
                                }
                            )
                            StartupAcOptionButton(
                                label = "60s",
                                selected = shutdownDryingDuration == 60,
                                onClick = {
                                    shutdownDryingDuration = 60
                                    prefs.edit { putInt(SharedPreferencesKeys.SHUTDOWN_DRYING_DURATION.key, 60) }
                                }
                            )
                            StartupAcOptionButton(
                                label = "2 min",
                                selected = shutdownDryingDuration == 120,
                                onClick = {
                                    shutdownDryingDuration = 120
                                    prefs.edit { putInt(SharedPreferencesKeys.SHUTDOWN_DRYING_DURATION.key, 120) }
                                }
                            )
                            StartupAcOptionButton(
                                label = "3 min",
                                selected = shutdownDryingDuration == 180,
                                onClick = {
                                    shutdownDryingDuration = 180
                                    prefs.edit { putInt(SharedPreferencesKeys.SHUTDOWN_DRYING_DURATION.key, 180) }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Precisa do app em execução com a central ainda energizada depois de desligar. " +
                                "Se a central cortar a energia junto com a ignição, o ciclo termina onde parou — " +
                                "o log de diagnóstico mostra até onde foi.",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else null
            )
        )
    )

    comfortList.addAll(
        listOfNotNull(
            SettingItem(
                title = "Habilitar botões personalizados no volante",
                description = SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS.description,
                checked = enableCustomSteeringWheelButtons,
                onCheckedChange = {
                    enableCustomSteeringWheelButtons = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS.key, it) }
                    ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                },
                customContent = if (enableCustomSteeringWheelButtons) {
                    {
                        var action1 by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION.key, SteeringWheelCustomActionType.DEFAULT.key)!!) }
                        var action2 by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION.key, SteeringWheelCustomActionType.DEFAULT.key)!!) }
                        var package1 by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1.key, "")!!) }
                        var package2 by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2.key, "")!!) }
                        var expanded1 by remember { mutableStateOf(false) }
                        var expanded2 by remember { mutableStateOf(false) }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = Color(0xFF3A3F47), thickness = 1.dp)

                            Text("Botão 1", color = Color.White, fontSize = 16.sp)
                            ExposedDropdownMenuBox(
                                expanded = expanded1,
                                onExpandedChange = { expanded1 = !expanded1 }
                            ) {
                                TextField(
                                    value = SteeringWheelCustomActionType.entries.find { it.key == action1 }?.description ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tipo de Ação") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded1) },
                                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded1,
                                    onDismissRequest = { expanded1 = false }
                                ) {
                                    SteeringWheelCustomActionType.entries.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type.description) },
                                            onClick = {
                                                action1 = type.key
                                                prefs.edit { putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION.key, type.key) }
                                                expanded1 = false
                                                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                                            }
                                        )
                                    }
                                }
                            }
                            if (action1 == SteeringWheelCustomActionType.OPEN_APP.key) {
                                TextField(
                                    value = package1,
                                    onValueChange = { newPkg ->
                                        package1 = newPkg
                                        prefs.edit { putString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1.key, newPkg) }
                                    },
                                    label = { Text("Pacote do App") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF2A2F37),
                                        unfocusedContainerColor = Color(0xFF2A2F37),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color(0xFFB0B8C4),
                                        focusedIndicatorColor = Color(0xFF4A9EFF),
                                        unfocusedIndicatorColor = Color(0xFF3A3F47)
                                    )
                                )
                            }

                            HorizontalDivider(color = Color(0xFF3A3F47), thickness = 1.dp)

                            Text("Botão 2", color = Color.White, fontSize = 16.sp)
                            ExposedDropdownMenuBox(
                                expanded = expanded2,
                                onExpandedChange = { expanded2 = !expanded2 }
                            ) {
                                TextField(
                                    value = SteeringWheelCustomActionType.entries.find { it.key == action2 }?.description ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tipo de Ação") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded2) },
                                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded2,
                                    onDismissRequest = { expanded2 = false }
                                ) {
                                    SteeringWheelCustomActionType.entries.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type.description) },
                                            onClick = {
                                                action2 = type.key
                                                prefs.edit { putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION.key, type.key) }
                                                expanded2 = false
                                                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                                            }
                                        )
                                    }
                                }
                            }
                            if (action2 == SteeringWheelCustomActionType.OPEN_APP.key) {
                                TextField(
                                    value = package2,
                                    onValueChange = { newPkg ->
                                        package2 = newPkg
                                        prefs.edit { putString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2.key, newPkg) }
                                    },
                                    label = { Text("Pacote do App") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF2A2F37),
                                        unfocusedContainerColor = Color(0xFF2A2F37),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color(0xFFB0B8C4),
                                        focusedIndicatorColor = Color(0xFF4A9EFF),
                                        unfocusedIndicatorColor = Color(0xFF3A3F47)
                                    )
                                )
                            }
                        }
                    }
                } else null
            )
        )
    )

    nightList.addAll(
        listOfNotNull(
            SettingItem(
                title = "Ajustar brilho automaticamente",
                description = "Ajusta o brilho da tela automaticamente",
                checked = enableAutoBrightness,
                onCheckedChange = {
                    enableAutoBrightness = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_AUTO_BRIGHTNESS.key, it) }
                    AutoBrightnessManager.getInstance().setEnabled(it)
                },
                customContent = if (enableAutoBrightness) {
                    {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFF3A3F47), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Início da noite
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showStartPicker = true }
                                        .background(
                                            Color(0xFF2A2F37),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Início da noite",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "${String.format("%02d", nightStartHour)}:${String.format("%02d", nightStartMinute)}",
                                            color = Color(0xFF4A9EFF),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Fim da noite
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showEndPicker = true }
                                        .background(
                                            Color(0xFF2A2F37),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Fim da noite",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "${String.format("%02d", nightEndHour)}:${String.format("%02d", nightEndMinute)}",
                                            color = Color(0xFF4A9EFF),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Slider para nível de brilho diurno
                            Column {
                                Text("Nível de brilho diurno: $dayBrightnessLevel", color = Color.White, fontSize = 14.sp)
                                Slider(
                                    value = dayBrightnessLevel.toFloat(),
                                    onValueChange = { newValue ->
                                        dayBrightnessLevel = newValue.toInt()
                                        prefs.edit { putInt(SharedPreferencesKeys.AUTO_BRIGHTNESS_LEVEL_DAY.key, dayBrightnessLevel) }
                                    },
                                    valueRange = 1f..10f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AppColors.Primary,
                                        activeTrackColor = AppColors.Primary,
                                        inactiveTrackColor = Color(0xFF2C3139),
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent,
                                        disabledThumbColor = AppColors.Primary,
                                        disabledActiveTrackColor = AppColors.Primary,
                                        disabledInactiveTrackColor = Color(0xFF2C3139)
                                    )
                                )
                            }

                            // Slider para nível de brilho noturno
                            Column {
                                Text("Nível de brilho noturno: $nightBrightnessLevel", color = Color.White, fontSize = 14.sp)
                                Slider(
                                    value = nightBrightnessLevel.toFloat(),
                                    onValueChange = { newValue ->
                                        nightBrightnessLevel = newValue.toInt()
                                        prefs.edit { putInt(SharedPreferencesKeys.AUTO_BRIGHTNESS_LEVEL_NIGHT.key, nightBrightnessLevel) }
                                    },
                                    valueRange = 1f..10f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AppColors.Primary,
                                        activeTrackColor = AppColors.Primary,
                                        inactiveTrackColor = Color(0xFF2C3139),
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent,
                                        disabledThumbColor = AppColors.Primary,
                                        disabledActiveTrackColor = AppColors.Primary,
                                        disabledInactiveTrackColor = Color(0xFF2C3139)
                                    )
                                )
                            }
                        }
                    }
                } else null
            )
        )
    )

    soundList.addAll(
        listOfNotNull(
            SettingItem(
                title = "Definir volume inicial",
                description = SharedPreferencesKeys.SET_STARTUP_VOLUME.description,
                checked = setStartupVolume,
                onCheckedChange = {
                    setStartupVolume = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.SET_STARTUP_VOLUME.key, it) }
                },
                sliderValue = volume,
                sliderRange = 0..40,
                onSliderChange = { newVolume ->
                    volume = newVolume
                    prefs.edit { putInt(SharedPreferencesKeys.STARTUP_VOLUME.key, newVolume) }
                },
                sliderLabel = "Volume: $volume"
            )
        )
    )

    climateList.addAll(
        listOfNotNull(
            SettingItem(
                title = "Definir ar-condicionado ao ligar",
                description = SharedPreferencesKeys.SET_STARTUP_AC.description,
                checked = setStartupAc,
                onCheckedChange = {
                    setStartupAc = it
                    prefs.edit { putBoolean(SharedPreferencesKeys.SET_STARTUP_AC.key, it) }
                },
                sliderValue = startupAcTemp,
                sliderRange = 32..64,
                sliderStep = 1,
                onSliderChange = { newTemp ->
                    startupAcTemp = newTemp
                    prefs.edit { putString(SharedPreferencesKeys.STARTUP_AC_TEMPERATURE.key, "%.1f".format(newTemp / 2.0)) }
                },
                sliderLabel = "Temperatura: %.1f°C".format(startupAcTemp / 2.0),
                customContent = {
                    Text(
                        // H6: cycle_mode 1 = ar externo (fresh air), 0 = interna (invertido vs AOSP)
                        "Circulação: ${if (startupAcCycle == "1") "Externa (ar de fora)" else "Interna"}",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartupAcOptionButton(
                            label = "Externa",
                            selected = startupAcCycle == "1",
                            onClick = {
                                startupAcCycle = "1"
                                prefs.edit { putString(SharedPreferencesKeys.STARTUP_AC_CYCLE_MODE.key, "1") }
                            }
                        )
                        StartupAcOptionButton(
                            label = "Interna",
                            selected = startupAcCycle != "1",
                            onClick = {
                                startupAcCycle = "0"
                                prefs.edit { putString(SharedPreferencesKeys.STARTUP_AC_CYCLE_MODE.key, "0") }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Direção do ar", color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    startupAcBlowerOptions.chunked(4).forEach { rowOptions ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowOptions.forEach { (value, label) ->
                                StartupAcOptionButton(
                                    label = label,
                                    selected = startupAcBlower == value,
                                    onClick = {
                                        startupAcBlower = value
                                        prefs.edit { putString(SharedPreferencesKeys.STARTUP_AC_BLOWER_MODE.key, value) }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Velocidade da ventilação", color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    (0..7).chunked(4).forEach { rowFan ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowFan.forEach { level ->
                                StartupAcOptionButton(
                                    label = if (level == 0) "Não mudar" else "$level",
                                    selected = startupAcFan == level,
                                    onClick = {
                                        startupAcFan = level
                                        prefs.edit { putInt(SharedPreferencesKeys.STARTUP_AC_FAN_SPEED.key, level) }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ao dar partida", color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartupAcOptionButton(
                            label = "Ligar o A/C",
                            selected = startupAcPower,
                            onClick = {
                                startupAcPower = true
                                prefs.edit { putBoolean(SharedPreferencesKeys.STARTUP_AC_POWER.key, true) }
                            }
                        )
                        StartupAcOptionButton(
                            label = "Não mexer",
                            selected = !startupAcPower,
                            onClick = {
                                startupAcPower = false
                                prefs.edit { putBoolean(SharedPreferencesKeys.STARTUP_AC_POWER.key, false) }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Compressor (A/C)", color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartupAcOptionButton(
                            label = "Ligado",
                            selected = startupAcCompressor,
                            onClick = {
                                startupAcCompressor = true
                                prefs.edit { putBoolean(SharedPreferencesKeys.STARTUP_AC_COMPRESSOR.key, true) }
                            }
                        )
                        StartupAcOptionButton(
                            label = "Desligado",
                            selected = !startupAcCompressor,
                            onClick = {
                                startupAcCompressor = false
                                prefs.edit { putBoolean(SharedPreferencesKeys.STARTUP_AC_COMPRESSOR.key, false) }
                            }
                        )
                    }
                    Text(
                        "O compressor só é alterado quando a opção acima está em \"Ligar o A/C\".",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Direção: valores do protocolo GWM — confira no painel qual combinação aparece e ajuste se necessário.",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            ),
            SettingItem(
                title = "Modo padrão do A/C",
                description = "Perfil com temperatura do motorista e do passageiro independentes, velocidade da ventilação, circulação, direção do ar e compressor. Ativa o A/C com essas configurações de qualquer estado atual — pelo botão abaixo, pela tela de A/C do cluster (zona ao lado da circulação interna: focar e pressionar cima/baixo) ou por um botão do volante com a ação \"Aplicar o modo padrão do A/C\".",
                checked = true,
                onCheckedChange = {},
                hideSwitch = true,
                customContent = {
                    AcTempSlider(
                        label = "Temperatura — motorista: %.1f°C".format(defaultAcDriverTemp / 2.0),
                        sliderValue = defaultAcDriverTemp,
                        onValueChange = { newTemp ->
                            defaultAcDriverTemp = newTemp
                            prefs.edit { putString(SharedPreferencesKeys.DEFAULT_AC_TEMPERATURE_DRIVER.key, "%.1f".format(newTemp / 2.0)) }
                        }
                    )
                    AcTempSlider(
                        label = "Temperatura — passageiro: %.1f°C".format(defaultAcPassTemp / 2.0),
                        sliderValue = defaultAcPassTemp,
                        onValueChange = { newTemp ->
                            defaultAcPassTemp = newTemp
                            prefs.edit { putString(SharedPreferencesKeys.DEFAULT_AC_TEMPERATURE_PASSENGER.key, "%.1f".format(newTemp / 2.0)) }
                        }
                    )
                    Text(
                        "Circulação: ${if (defaultAcCycle == "1") "Externa (ar de fora)" else "Interna"}",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartupAcOptionButton(
                            label = "Externa",
                            selected = defaultAcCycle == "1",
                            onClick = {
                                defaultAcCycle = "1"
                                prefs.edit { putString(SharedPreferencesKeys.DEFAULT_AC_CYCLE_MODE.key, "1") }
                            }
                        )
                        StartupAcOptionButton(
                            label = "Interna",
                            selected = defaultAcCycle != "1",
                            onClick = {
                                defaultAcCycle = "0"
                                prefs.edit { putString(SharedPreferencesKeys.DEFAULT_AC_CYCLE_MODE.key, "0") }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Direção do ar", color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    startupAcBlowerOptions.chunked(4).forEach { rowOptions ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowOptions.forEach { (value, label) ->
                                StartupAcOptionButton(
                                    label = label,
                                    selected = defaultAcBlower == value,
                                    onClick = {
                                        defaultAcBlower = value
                                        prefs.edit { putString(SharedPreferencesKeys.DEFAULT_AC_BLOWER_MODE.key, value) }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Velocidade da ventilação", color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    (1..7).chunked(4).forEach { rowFan ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowFan.forEach { level ->
                                StartupAcOptionButton(
                                    label = "$level",
                                    selected = defaultAcFan == level,
                                    onClick = {
                                        defaultAcFan = level
                                        prefs.edit { putInt(SharedPreferencesKeys.DEFAULT_AC_FAN_SPEED.key, level) }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Compressor (A/C)", color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartupAcOptionButton(
                            label = "Ligado",
                            selected = defaultAcCompressor,
                            onClick = {
                                defaultAcCompressor = true
                                prefs.edit { putBoolean(SharedPreferencesKeys.DEFAULT_AC_COMPRESSOR.key, true) }
                            }
                        )
                        StartupAcOptionButton(
                            label = "Desligado",
                            selected = !defaultAcCompressor,
                            onClick = {
                                defaultAcCompressor = false
                                prefs.edit { putBoolean(SharedPreferencesKeys.DEFAULT_AC_COMPRESSOR.key, false) }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            ServiceManager.getInstance().applyDefaultAcMode()
                            Toast.makeText(context, "Modo padrão do A/C ativado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ativar modo padrão agora", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Aplicado por cima do estado atual: liga o A/C, ajusta o compressor, encerra secagem e MAX AUTO e restaura estas configurações.",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            )
        )
    )

    // Seção selecionada no menu lateral
    val sectionList: List<SettingItem> = when (section) {
        "Janelas e Teto" -> windowsList
        "Som" -> soundList
        "Noite e Brilho" -> nightList
        "Conforto" -> comfortList
        "Ao Desligar" -> powerOffList
        "Avançado" -> advancedList
        else -> climateList
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = section,
            color = AppColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (sectionList.isEmpty()) {
            Text(
                "Nenhuma opção nesta seção.",
                color = AppColors.TextSecondary,
                fontSize = 15.sp
            )
        } else {
            TwoColumnSettingsLayout(
                settingsList = sectionList,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showStartPicker) {
        LaunchedEffect(Unit) {
            val dialog = TimePickerDialog(
                context,
                { _, h, m ->
                    nightStartHour = h
                    nightStartMinute = m
                    prefs.edit {
                        putInt(SharedPreferencesKeys.NIGHT_START_HOUR.key, h)
                        putInt(SharedPreferencesKeys.NIGHT_START_MINUTE.key, m)
                    }
                    AutoBrightnessManager.getInstance().updateSchedule()
                },
                nightStartHour,
                nightStartMinute,
                true
            )
            dialog.setOnDismissListener { showStartPicker = false }
            dialog.show()
        }
    }
    if (showEndPicker) {
        LaunchedEffect(Unit) {
            val dialog = TimePickerDialog(
                context,
                { _, h, m ->
                    nightEndHour = h
                    nightEndMinute = m
                    prefs.edit {
                        putInt(SharedPreferencesKeys.NIGHT_END_HOUR.key, h)
                        putInt(SharedPreferencesKeys.NIGHT_END_MINUTE.key, m)
                    }
                    AutoBrightnessManager.getInstance().updateSchedule()
                },
                nightEndHour,
                nightEndMinute,
                true
            )
            dialog.setOnDismissListener { showEndPicker = false }
            dialog.show()
        }
    }
}

@Composable
fun FridaHooksTab() {
    val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    var enableFridaHooks by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOKS.key, false)) }
    var enableFridaHookSystemServer by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOK_SYSTEM_SERVER.key, false)) }
    var showFridaDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingCard(
                title = "Habilitar Frida Hooks",
                description = SharedPreferencesKeys.ENABLE_FRIDA_HOOKS.description,
                checked = enableFridaHooks,
                onCheckedChange = { newValue ->
                    if (!newValue) {
                        enableFridaHooks = false
                        prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOKS.key, false) }
                    } else {
                        showFridaDialog = true
                    }
                }
            )
        }
        item {
            SettingCard(
                title = "Hook System Server",
                description = SharedPreferencesKeys.ENABLE_FRIDA_HOOK_SYSTEM_SERVER.description,
                checked = enableFridaHookSystemServer,
                onCheckedChange = { newValue ->
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOK_SYSTEM_SERVER.key, newValue) }
                    enableFridaHookSystemServer = newValue
                    if (newValue)
                        FridaUtils.injectSystemServer()
                }
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF13151A)
                )
            ) {
                Button(
                    onClick = { showManualDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A9EFF)
                    )
                ) {
                    Text("Injetar Código Manual", color = Color.White)
                }
            }
        }
    }
    if (showFridaDialog) {
        AlertDialog(
            onDismissRequest = { showFridaDialog = false },
            title = { Text("Confirmação") },
            text = { Text("Ativar scripts fridas é uma função experimental que pode causar instabilidades, utilize por conta e risco. Caso não saiba o que é essa função é melhor manter desativada") },
            confirmButton = {
                TextButton(onClick = {
                    showFridaDialog = false
                    enableFridaHooks = true
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOKS.key, true) }
                    ServiceManager.getInstance().initializeFrida()
                }) {
                    Text("Ativar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFridaDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Hooks Manuais") },
            text = {
                val manuals = FridaUtils.ScriptProcess.entries.filter { it.injectMode == FridaUtils.InjectMode.MANUAL }
                LazyColumn {
                    items(manuals) { script ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(script.process)
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { FridaUtils.injectScript(script, false) }) {
                                Text("Injetar")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
fun TelasTab() {
    val context = LocalContext.current
    val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    var enableProjector by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_PROJECTOR.key, false)) }
    var enableWarning by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_REVISION_WARNING.key, false)) }
    var enableCustomIntegration by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.key, false)) }
    var nextKmText by remember { mutableStateOf(prefs.getInt(SharedPreferencesKeys.INSTRUMENT_REVISION_KM.key, 12000).toString()) }
    var nextDateMillis by remember { mutableLongStateOf(prefs.getLong(SharedPreferencesKeys.INSTRUMENT_REVISION_NEXT_DATE.key, 0L)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val formattedNextDate = if (nextDateMillis > 0) dateFormatter.format(nextDateMillis) else "Não definido"

    val settingsList = listOf(
        SettingItem(
            title = "Projetor do painel",
            description = SharedPreferencesKeys.ENABLE_INSTRUMENT_PROJECTOR.description,
            checked = enableProjector,
            onCheckedChange = {
                enableProjector = it
                prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_PROJECTOR.key, it) }
                if (!it) {
                    enableWarning = false
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_REVISION_WARNING.key, false) }
                    enableCustomIntegration = false
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.key, false) }

                    try {
                        ServiceManager.getInstance().ensureSystemApps()
                    } catch (e: Exception) {
                        Log.e("TelasTab", "Erro ao desabilitar projetor: ${e.message}", e)
                    }
                }
            }
        ),
        SettingItem(
            title = "Aviso de revisão",
            description = SharedPreferencesKeys.ENABLE_INSTRUMENT_REVISION_WARNING.description,
            checked = enableWarning,
            onCheckedChange = {
                enableWarning = it
                prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_REVISION_WARNING.key, it) }
            },
            enabled = enableProjector
        ),
        SettingItem(
            title = "Integração de mídia customizada",
            description = SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.description,
            checked = enableCustomIntegration,
            onCheckedChange = {
                enableCustomIntegration = it
                prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.key, it) }

                try {
                    ServiceManager.getInstance().ensureSystemApps()
                    if (enableCustomIntegration) {
                        ServiceManager.getInstance().startClusterHeartbeat()
                    }
                } catch (e: Exception) {
                    // Log do erro e desabilitar a opção se falhar
                    Log.e("TelasTab", "Erro ao configurar integração de mídia: ${e.message}", e)
                    enableCustomIntegration = false
                    prefs.edit { putBoolean(SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION.key, false) }
                }
            },
            enabled = enableProjector
        )
    )

    TwoColumnSettingsLayout(
        settingsList = settingsList,
        bottomContent = {
            if (enableWarning) {
                StyledCard(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                "Próxima KM:",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                TextField(
                                    value = nextKmText,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                                            nextKmText = newValue
                                            newValue.toIntOrNull()?.let {
                                                prefs.edit { putInt(SharedPreferencesKeys.INSTRUMENT_REVISION_KM.key, it) }
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF2A2F37),
                                        unfocusedContainerColor = Color(0xFF2A2F37),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color(0xFFB0B8C4),
                                        focusedIndicatorColor = Color(0xFF4A9EFF),
                                        unfocusedIndicatorColor = Color(0xFF3A3F47)
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val currentKm = ServiceManager.getInstance().totalOdometer
                                        val newNextKm = currentKm + 12000
                                        nextKmText = newNextKm.toString()
                                        prefs.edit { putInt(SharedPreferencesKeys.INSTRUMENT_REVISION_KM.key, newNextKm) }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4A9EFF)
                                    )
                                ) {
                                    Text("Resetar", color = Color.White)
                                }
                            }
                        }

                        Column {
                            Text(
                                "Próxima data: $formattedNextDate",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Button(
                                    onClick = { showDatePicker = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4A9EFF)
                                    )
                                ) {
                                    Text("Informar manual", color = Color.White)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.YEAR, 1)
                                        nextDateMillis = cal.timeInMillis
                                        prefs.edit { putLong(SharedPreferencesKeys.INSTRUMENT_REVISION_NEXT_DATE.key, nextDateMillis) }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4A9EFF)
                                    )
                                ) {
                                    Text("Resetar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    )


    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        if (nextDateMillis > 0) calendar.timeInMillis = nextDateMillis

        LaunchedEffect(showDatePicker) {
            if (showDatePicker) {
                val dialog = DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val cal = Calendar.getInstance()
                        cal.set(year, month, day)
                        nextDateMillis = cal.timeInMillis
                        prefs.edit { putLong(SharedPreferencesKeys.INSTRUMENT_REVISION_NEXT_DATE.key, nextDateMillis) }
                        showDatePicker = false
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                dialog.setOnDismissListener { showDatePicker = false }
                dialog.show()
            }
        }
    }
}

@Composable
fun CurrentValuesTab() {
    val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    val advancedUse = prefs.getBoolean(SharedPreferencesKeys.ADVANCE_USE.key, false)
    val dataMap = remember {
        mutableStateMapOf<String, String>().apply {
            putAll(ServiceManager.getInstance().allCurrentCachedData)
        }
    }
    var showConfigDialog by remember { mutableStateOf(false) }
    val allConstants = remember { CarConstants.entries.map { it.value } }
    val defaultKeys = remember { ServiceManager.DEFAULT_KEYS.map { it.value } } // Assuming DEFAULT_KEYS is Array<CarConstants>
    val filteredConstants = remember { allConstants.filter { it !in defaultKeys } }
    val monitoredSet = remember {
        mutableStateOf(prefs.getStringSet(SharedPreferencesKeys.CAR_MONITOR_PROPERTIES.key, emptySet()) ?: emptySet())
    }
    val tempChecked = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allConstants.forEach { this[it] = monitoredSet.value.contains(it) }
        }
    }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var searchQueryValues by remember { mutableStateOf("") }
    var searchQueryConfig by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        val listener = IDataChanged { key, value -> dataMap[key] = value }
        ServiceManager.getInstance().addDataChangedListener(listener)
        onDispose {
            ServiceManager.getInstance().removeDataChangedListener(listener)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (advancedUse) {
            Button(
                onClick = { showConfigDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A9EFF)
                )
            ) {
                Text("Configurar", color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
        }
        TextField(
            value = searchQueryValues,
            onValueChange = { searchQueryValues = it },
            label = { Text("Pesquisar valores") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2A2F37),
                unfocusedContainerColor = Color(0xFF2A2F37),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFB0B8C4),
                focusedIndicatorColor = Color(0xFF4A9EFF),
                unfocusedIndicatorColor = Color(0xFF3A3F47),
                focusedLabelColor = Color(0xFF4A9EFF),
                unfocusedLabelColor = Color(0xFFB0B8C4)
            )
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filteredData = dataMap.toList()
                .filter { it.first.lowercase().contains(searchQueryValues.lowercase()) }
                .sortedBy { it.first }
            items(filteredData) { (key, value) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .then(
                            if (advancedUse) Modifier.clickable {
                                selectedKey = key
                                newValue = value
                                showUpdateDialog = true
                            } else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF13151A)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        "$key: $value",
                        modifier = Modifier.padding(8.dp),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
    if (showConfigDialog && advancedUse) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Configurar Monitoramento") },
            text = {
                Column {
                    TextField(
                        value = searchQueryConfig,
                        onValueChange = { searchQueryConfig = it },
                        label = { Text("Pesquisar constantes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    val checked = filteredConstants.filter { tempChecked[it] ?: false }.sorted()
                    val unchecked = filteredConstants.filter { !(tempChecked[it] ?: false) }.sorted()
                    val sortedConstants = (checked + unchecked)
                        .filter { it.lowercase().contains(searchQueryConfig.lowercase()) }
                    LazyColumn {
                        items(sortedConstants) { constant ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = tempChecked[constant] ?: false,
                                    onCheckedChange = { tempChecked[constant] = it }
                                )
                                Text(constant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newSet = tempChecked.filterValues { it }.keys.toSet()
                    prefs.edit { putStringSet(SharedPreferencesKeys.CAR_MONITOR_PROPERTIES.key, newSet) }
                    monitoredSet.value = newSet
                    showConfigDialog = false
                    ServiceManager.getInstance().updateMonitoringProperties()
                    dataMap.clear()
                    dataMap.putAll(ServiceManager.getInstance().allCurrentCachedData)
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    allConstants.forEach { tempChecked[it] = monitoredSet.value.contains(it) }
                    showConfigDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
    if (showUpdateDialog && advancedUse) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Atualizar $selectedKey") },
            text = {
                TextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    label = { Text("Novo valor") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ServiceManager.getInstance().updateData(selectedKey, newValue)
                    showUpdateDialog = false
                }) {
                    Text("Atualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ContentArea(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(AppDimensions.ContentPadding)
    ) {
        content()
    }
}

@Composable
fun AppActionButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) AppColors.Primary else AppColors.ButtonSecondary
        ),
        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            color = AppColors.TextPrimary,
            fontSize = if (isPrimary) 14.sp else 13.sp,
            fontWeight = if (isPrimary) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun InstallAppsTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf(listOf<AppInfo>()) }
    var downloadingApp by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    val pm = context.packageManager
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Permission requested */ }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var installResult by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var downloadingUrl by remember { mutableStateOf(false) }
    var urlProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://raw.githubusercontent.com/bobaoapae/haval-impulse-static-files/refs/heads/main/apps.json?rnd=${System.currentTimeMillis()}")
                val conn = url.openConnection() as HttpURLConnection
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val jsonString = reader.use { it.readText() }
                    val jsonArray = JSONArray(jsonString)
                    val appList = mutableListOf<AppInfo>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val iconUrl = obj.optString("appIcon", null)
                        appList.add(
                            AppInfo(
                                obj.getString("appName"),
                                obj.getString("appVersion"),
                                obj.getString("appPackageName"),
                                obj.getString("appLink"),
                                if (!iconUrl.isNullOrEmpty() && iconUrl != "null") iconUrl else null
                            )
                        )
                        // Debug log
                        Log.d(TAG, "App: ${obj.getString("appName")}, Icon URL: $iconUrl")
                    }
                    apps = appList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun getInstalledVersion(packageName: String): String? {
        return try {
            val info = pm.getPackageInfo(packageName, 0)
            info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun compareVersions(v1: String?, v2: String): Int {
        if (v1 == null) return -1
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until min(parts1.size, parts2.size)) {
            if (parts1[i] > parts2[i]) return 1
            if (parts1[i] < parts2[i]) return -1
        }
        return parts1.size.compareTo(parts2.size)
    }

    fun startDownload(app: AppInfo) {
        downloadingApp = app.packageName
        downloadProgress = downloadProgress.toMutableMap().apply { put(app.packageName, 0f) }
        scope.launch(Dispatchers.IO) {
            try {
                val file = File(context.getExternalFilesDir(null), "${app.packageName}.apk")
                val url = URL(app.link)
                val conn = url.openConnection() as HttpURLConnection
                val length = conn.contentLength
                val input = BufferedInputStream(conn.inputStream)
                val output = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var total = 0
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    total += bytesRead
                    if (length > 0) {
                        downloadProgress = downloadProgress.toMutableMap().apply { put(app.packageName, total.toFloat() / length) }
                    }
                }
                output.close()
                input.close()
                withContext(Dispatchers.Main) {
                    if (!pm.canRequestPackageInstalls()) {
                        showPermissionDialog = true
                        return@withContext
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
            } finally {
                downloadingApp = null
            }
        }
    }

    fun startDownloadFromUrl(urlString: String) {
        downloadingUrl = true
        urlProgress = 0f
        scope.launch(Dispatchers.IO) {
            try {
                val file = File(context.getExternalFilesDir(null), "custom.apk")
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                val length = conn.contentLength
                val input = BufferedInputStream(conn.inputStream)
                val output = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var total = 0
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    total += bytesRead
                    if (length > 0) {
                        urlProgress = total.toFloat() / length
                    }
                }
                output.close()
                input.close()
                withContext(Dispatchers.Main) {
                    if (!pm.canRequestPackageInstalls()) {
                        showPermissionDialog = true
                        return@withContext
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
            } finally {
                downloadingUrl = false
            }
        }
    }

    fun uninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }
        context.startActivity(intent)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // URL Input Section
        item(span = { GridItemSpan(4) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL do APK") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2F37),
                            unfocusedContainerColor = Color(0xFF2A2F37),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFB0B8C4),
                            focusedIndicatorColor = Color(0xFF4A9EFF),
                            unfocusedIndicatorColor = Color(0xFF3A3F47),
                            focusedLabelColor = Color(0xFF4A9EFF),
                            unfocusedLabelColor = Color(0xFFB0B8C4)
                        )
                    )
                    if (!downloadingUrl) {
                        Button(
                            onClick = { if (urlInput.isNotEmpty()) startDownloadFromUrl(urlInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A9EFF)
                            ),
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("Instalar via URL", color = Color.White)
                        }
                    }
                }

                if (downloadingUrl) {
                    LinearProgressIndicator(
                        progress = { urlProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF4A9EFF)
                    )
                }

                if (installResult.isNotEmpty()) {
                    Text(installResult, color = Color.White, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Aplicativos disponíveis:",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            item(span = { GridItemSpan(4) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4A9EFF)
                    )
                }
            }
        } else {
            // Apps Grid - Ordenados por prioridade: Atualizar > Instalar > Instalados
            // Dentro de cada grupo, ordena alfabeticamente
            val sortedApps = apps.sortedWith(
                compareBy(
                    { app ->
                        val installedVersion = getInstalledVersion(app.packageName)
                        val isInstalled = installedVersion != null
                        val needsUpdate = isInstalled && compareVersions(installedVersion, app.version) < 0

                        when {
                            needsUpdate -> 0  // Prioridade máxima: precisa atualizar
                            !isInstalled -> 1 // Segunda prioridade: disponível para instalar
                            else -> 2         // Última prioridade: já instalado e atualizado
                        }
                    },
                    { app -> app.name.lowercase() } // Ordenação alfabética dentro de cada grupo
                ))

            gridItems(sortedApps) { app ->
                val installedVersion = getInstalledVersion(app.packageName)
                val isInstalled = installedVersion != null
                val needsUpdate = isInstalled && compareVersions(installedVersion, app.version) < 0
                val progress = downloadProgress[app.packageName] ?: 0f

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF1D2430),
                            shape = RoundedCornerShape(0.dp),
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF13151A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // App Icon Container with padding
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF2A2F37)
                                ) {
                                    if (!app.iconUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(app.iconUrl)
                                                .crossfade(true)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .allowHardware(false)
                                                .build(),
                                            contentDescription = app.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            onError = {
                                                Log.e(TAG, "Error loading icon for ${app.name}: ${it.result.throwable}")
                                            }
                                        )
                                    } else {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                Icons.Default.Build,
                                                contentDescription = app.name,
                                                tint = Color(0xFF4A9EFF),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // App Name
                            Text(
                                app.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            // Version info
                            Text(
                                "v${app.version}",
                                fontSize = 12.sp,
                                color = Color(0xFFB0B8C4),
                                lineHeight = 14.sp
                            )

                            if (isInstalled) {
                                Text(
                                    "Inst: v${installedVersion}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF808080),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Action Button Section
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (downloadingApp == app.packageName) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp),
                                        color = Color(0xFF4A9EFF),
                                        trackColor = Color(0xFF3A3F47)
                                    )
                                    Text(
                                        "${(progress * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Botão principal (Instalar ou Atualizar)
                                    if (!isInstalled || needsUpdate) {
                                        AppActionButton(
                                            text = if (!isInstalled) "Instalar" else "Atualizar",
                                            onClick = { startDownload(app) },
                                            isPrimary = true
                                        )
                                    }

                                    // Botão de desinstalar
                                    if (isInstalled) {
                                        AppActionButton(
                                            text = "Desinstalar",
                                            onClick = { uninstall(app.packageName) },
                                            isPrimary = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissão necessária") },
            text = { Text("Permita a instalação de apps de fontes desconhecidas.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    requestPermissionLauncher.launch(intent)
                }) {
                    Text("Configurações")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Uma release do GitHub já decodificada. `publishedAt` é o ISO-8601 cru da API.
private data class ReleaseInfo(
    val tag: String?,
    val url: String?,
    val sha256: String?,
    val publishedAt: String?
)

@Composable
fun InformacoesTab() {
    val context = LocalContext.current
    val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    var isActive by remember { mutableStateOf(ServiceManager.getInstance().isServicesInitialized) }
    var bypassSelfInstallationCheck by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK.key, false)) }
    var selfInstallationCheck by remember { mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.SELF_INSTALLATION_INTEGRITY_CHECK.key, false)) }
    var formattedTime by remember { mutableStateOf("Não inicializado") }
    var formattedTime2 by remember { mutableStateOf("Não inicializado") }
    var formattedTime3 by remember { mutableStateOf("Não inicializado") }
    var version by remember { mutableStateOf("Desconhecida") }
    var buildDate by remember { mutableStateOf("") }
    var buildCommit by remember { mutableStateOf("") }
    var isPreviewVersion by remember { mutableStateOf(false) }
    var clickCount by remember { mutableIntStateOf(0) }
    var showAdvancedDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var updateAvailable by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }
    var latestPublished by remember { mutableStateOf<String?>(null) }
    var downloadUrl by remember { mutableStateOf("") }
    var updateSha256 by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Permission requested */ }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // --- Rollback ---
    var rollbackTarget by remember { mutableStateOf<ReleaseInfo?>(null) }
    var rollbackBusy by remember { mutableStateOf(false) }
    var downloadLabel by remember { mutableStateOf("atualização") }
    var rollbackLogStatus by remember { mutableStateOf<String?>(null) }
    var rollbackMessage by remember { mutableStateOf("") }
    var showRollbackMessage by remember { mutableStateOf(false) }
    var showRollbackConfirm by remember { mutableStateOf(false) }
    var rollbackLogProblem by remember { mutableStateOf("") }
    var showRollbackNoLogs by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            version = packageInfo.versionName ?: "Desconhecida"
            isPreviewVersion = version.contains("preview")
        } catch (e: PackageManager.NameNotFoundException) {
            version = "Erro"
        }
        // Vêm do build.gradle.kts, não do PackageManager: a data é a da compilação
        // deste APK e o commit diz exatamente de onde ele saiu. É o que permite ver
        // na tela se a versão instalada é a última — ou um build local no meio do caminho.
        buildDate = BuildConfig.BUILD_DATE
        buildCommit = BuildConfig.BUILD_COMMIT
    }

    LaunchedEffect(Unit) {
        while (true) {
            isActive = ServiceManager.getInstance().isServicesInitialized
            val timeBoot = ServiceManager.getInstance().timeBootReceived
            formattedTime = if (isActive && timeBoot > 0) {
                val minutes = timeBoot / 60000
                val seconds = (timeBoot / 1000) % 60
                val millis = timeBoot % 1000
                String.format("%02d:%02d.%03d", minutes, seconds, millis)
            } else {
                "Não inicializado"
            }
            val timeStart = ServiceManager.getInstance().timeStartInitialization
            formattedTime2 = if (isActive && timeStart > 0) {
                val minutes = timeStart / 60000
                val seconds = (timeStart / 1000) % 60
                val millis = timeStart % 1000
                String.format("%02d:%02d.%03d", minutes, seconds, millis)
            } else {
                "Não inicializado"
            }
            val timeInit = ServiceManager.getInstance().timeInitialized
            formattedTime3 = if (isActive && timeInit > 0) {
                val minutes = timeInit / 60000
                val seconds = (timeInit / 1000) % 60
                val millis = timeInit % 1000
                String.format("%02d:%02d.%03d", minutes, seconds, millis)
            } else {
                "Não inicializado"
            }
            delay(100)
        }
    }

    // Fork do usuário: releases são publicadas aqui (github.com/fefezo/haval-app-tool-multimidia)
    val UPDATE_REPO = "https://api.github.com/repos/fefezo/haval-app-tool-multimidia"

    // Extrai o SHA-256 anunciado pela release: prioridade para "sha256: <hex>" no corpo,
    // fallback para o campo digest do asset (API do GitHub retorna "sha256:<hex>").
    fun extractSha256(body: String, digest: String): String? {
        val bodyHash = Regex("(?i)sha256[\\s:=]+([0-9a-f]{64})").find(body)
        if (bodyHash != null) return bodyHash.groupValues[1].lowercase()
        if (digest.startsWith("sha256:")) return digest.substringAfter("sha256:").lowercase()
        return null
    }

    // "2026-09-12T11:47:03Z" -> "12/09/2026 11:47" no fuso do aparelho.
    fun formatPublishedAt(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val instant = parser.parse(iso) ?: return null
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(instant)
        } catch (e: Exception) {
            null
        }
    }

    // Decodifica um objeto de release da API do GitHub. Compartilhado pelo
    // update e pelo rollback para que os dois leiam tag, APK, hash e data
    // exatamente do mesmo jeito.
    fun parseRelease(json: JSONObject): ReleaseInfo {
        var dlUrl: String? = null
        var sha256: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) {
                    dlUrl = a.getString("browser_download_url")
                    sha256 = extractSha256(json.optString("body", ""), a.optString("digest", ""))
                    break
                }
            }
        }
        return ReleaseInfo(
            tag = json.optString("tag_name").ifBlank { null },
            url = dlUrl,
            sha256 = sha256,
            publishedAt = json.optString("published_at").ifBlank { null }
        )
    }

    suspend fun getLatestReleaseInfo(isPreview: Boolean): ReleaseInfo {
        return withContext(Dispatchers.IO) {
            try {
                val endpoint = if (isPreview)
                    "$UPDATE_REPO/releases"
                else
                    "$UPDATE_REPO/releases/latest"

                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (conn.responseCode != 200) return@withContext ReleaseInfo(null, null, null, null)

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.use { it.readText() }

                if (!isPreview) {
                    return@withContext parseRelease(JSONObject(response))
                }

                val releases = JSONArray(response)
                for (i in 0 until releases.length()) {
                    val rel = releases.getJSONObject(i)
                    if (rel.getBoolean("prerelease")) {
                        return@withContext parseRelease(rel)
                    }
                }

                ReleaseInfo(null, null, null, null)
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching latest release info", e)
                ReleaseInfo(null, null, null, null)
            }
        }
    }


    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until min(parts1.size, parts2.size)) {
            if (parts1[i] > parts2[i]) return 1
            if (parts1[i] < parts2[i]) return -1
        }
        return parts1.size.compareTo(parts2.size)
    }

    fun sha256Of(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(65536)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "SHA-256 computation failed", e)
            null
        }
    }

    // Alvo do rollback: a release normal (fora draft e prerelease) de maior versão
    // que seja estritamente MENOR que a instalada. Compara versão a versão em vez de
    // confiar na ordem do array da API — a ordem não é garantida.
    suspend fun getPreviousRelease(currentVersion: String): ReleaseInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val currentClean = currentVersion.removePrefix("v")
                val conn = URL("$UPDATE_REPO/releases?per_page=30").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (conn.responseCode != 200) return@withContext null

                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val releases = JSONArray(response)

                var best: ReleaseInfo? = null
                var bestTag: String? = null
                for (i in 0 until releases.length()) {
                    val rel = releases.getJSONObject(i)
                    if (rel.optBoolean("draft") || rel.optBoolean("prerelease")) continue
                    val info = parseRelease(rel)
                    val tag = info.tag?.removePrefix("v") ?: continue
                    if (compareVersions(tag, currentClean) >= 0) continue
                    if (bestTag == null || compareVersions(tag, bestTag) > 0) {
                        best = info
                        bestTag = tag
                    }
                }
                best
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching previous release", e)
                null
            }
        }
    }

    fun startDownload() {
        isDownloading = true
        downloadProgress = 0f
        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                val file = File(context.getExternalFilesDir(null), "update.apk")
                withContext(Dispatchers.IO) {
                    val url = URL(downloadUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    val length = conn.contentLength
                    val input = BufferedInputStream(conn.inputStream)
                    val output = FileOutputStream(file)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var total = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        total += bytesRead
                        if (length > 0) downloadProgress = total.toFloat() / length
                    }
                    output.close()
                    input.close()
                }
                // Verificação de integridade: só instala se o SHA-256 bater com o anunciado na release
                val actualSha256 = sha256Of(file)
                val expectedSha256 = updateSha256
                if (expectedSha256 == null || actualSha256 == null || !actualSha256.equals(expectedSha256, ignoreCase = true)) {
                    file.delete()
                    isDownloading = false
                    downloadError = "Verificação SHA-256 falhou (obtido: ${actualSha256 ?: "erro"}, esperado: ${expectedSha256 ?: "—"}). Download descartado."
                    return@launch
                }
                isDownloading = false
                withContext(Dispatchers.Main) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        showPermissionDialog = true
                        return@withContext
                    }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                isDownloading = false
                downloadError = e.message ?: "Erro desconhecido"
            }
        }
    }

    // Envia os logs da versão INSTALADA — quem tem o bug é ela, e a instalação
    // substitui o processo. Devolve null em caso de sucesso; caso contrário a razão
    // da falha, para o chamador decidir se continua mesmo assim.
    suspend fun sendRollbackLogs(): String? {
        val token = prefs.getString(SharedPreferencesKeys.GITHUB_GIST_TOKEN.key, "") ?: ""
        if (token.isBlank()) {
            return "nenhum token do GitHub configurado (aba Diagnóstico)."
        }
        return try {
            val file = withContext(Dispatchers.IO) { DiagnosticsCollector.capture(context, prefs) }
            val url = withContext(Dispatchers.IO) { GistUploader.upload(file, token) }
            rollbackLogStatus = "Logs enviados: $url"
            null
        } catch (e: GistUploader.UploadException) {
            Log.e(TAG, "Falha no upload dos logs do rollback (HTTP ${e.code})", e)
            "falha no envio (HTTP ${e.code})."
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao coletar/enviar logs do rollback", e)
            e.message ?: "erro desconhecido ao coletar os logs."
        }
    }

    // Aponta o alvo do update para a release anterior e entrega ao startDownload()
    // existente — o caminho de download, o SHA-256 e o instalador são os mesmos.
    fun proceedWithRollback(target: ReleaseInfo) {
        val url = target.url
        if (url == null) {
            rollbackMessage = "A release ${target.tag} não tem APK anexado."
            showRollbackMessage = true
            return
        }
        latestVersion = target.tag ?: ""
        downloadUrl = url
        updateSha256 = target.sha256
        startDownload()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Seção de Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF13151A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Status do Sistema",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                HorizontalDivider(color = Color(0xFF1D2430))

                if (!bypassSelfInstallationCheck) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Instalado corretamente:", color = Color(0xFFB0B8C4))
                        Text(
                            if (selfInstallationCheck) "Sim" else "Não",
                            color = if (selfInstallationCheck) Color(0xFF4ADE80) else Color(0xFFEF4444)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estado:", color = Color(0xFFB0B8C4))
                    Text(
                        if (isActive) "Ativo" else "Inativo",
                        color = if (isActive) Color(0xFF4ADE80) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Boot Completed:", color = Color(0xFFB0B8C4), fontSize = 14.sp)
                        Text(formattedTime, color = Color.White, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Início:", color = Color(0xFFB0B8C4), fontSize = 14.sp)
                        Text(formattedTime2, color = Color.White, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Inicialização:", color = Color(0xFFB0B8C4), fontSize = 14.sp)
                        Text(formattedTime3, color = Color.White, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF1D2430))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Versão", color = Color(0xFFB0B8C4), fontSize = 14.sp)
                        Text(
                            // A data vem do build.gradle.kts (BuildConfig), então diz
                            // quando ESTE apk foi compilado — não é a data da release.
                            if (buildDate.isBlank()) "v${version.removePrefix("v")}"
                            else "v${version.removePrefix("v")} · $buildDate",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                clickCount++
                                if (clickCount >= 5) {
                                    showAdvancedDialog = true
                                    clickCount = 0
                                }
                            }
                        )
                        if (buildCommit.isNotBlank()) {
                            Text(
                                "commit $buildCommit",
                                color = Color(0xFF8A93A0),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val release = getLatestReleaseInfo(isPreviewVersion)
                                    val latest = release.tag
                                    val dlUrl = release.url
                                    val sha256 = release.sha256
                                    if (latest != null && dlUrl != null) {
                                        if (sha256 == null) {
                                            updateMessage = "Release sem hash SHA-256 — instalação bloqueada por segurança. Adicione \"sha256: <hash>\" no corpo da release."
                                            showUpdateDialog = true
                                        } else {
                                            val currentClean = version.removePrefix("v")
                                            val latestClean = latest.removePrefix("v")
                                            // O "99.99" é proposital: releases antigas gravaram esse
                                            // versionName fixo, e sem esta linha uma instalação antiga
                                            // nunca conseguiria migrar para uma versão com número real.
                                            if (currentClean == "99.99" || compareVersions(latestClean, currentClean) > 0) {
                                                latestVersion = latest
                                                latestPublished = formatPublishedAt(release.publishedAt)
                                                downloadUrl = dlUrl
                                                updateSha256 = sha256
                                                downloadLabel = "atualização"
                                                rollbackTarget = null
                                                rollbackLogStatus = null
                                                updateAvailable = true
                                            } else {
                                                val published = formatPublishedAt(release.publishedAt)
                                                updateMessage = if (published != null)
                                                    "Você está na versão mais recente ($latest, publicada em $published)."
                                                else
                                                    "Você está na versão mais recente ($latest)."
                                                showUpdateDialog = true
                                            }
                                        }
                                    } else {
                                        updateMessage = "Erro ao verificar atualizações"
                                        showUpdateDialog = true
                                    }
                                }
                            },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Primary
                            ),
                            shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Buscar Atualizações",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buscar Atualizações", fontSize = 14.sp)
                        }

                        TextButton(
                            onClick = {
                                scope.launch {
                                    rollbackBusy = true
                                    try {
                                        val target = getPreviousRelease(version)
                                        if (target == null) {
                                            rollbackMessage = "Não encontrei nenhuma release anterior à v${version.removePrefix("v")}."
                                            showRollbackMessage = true
                                        } else if (target.url == null) {
                                            rollbackMessage = "A release ${target.tag} não tem APK anexado."
                                            showRollbackMessage = true
                                        } else if (target.sha256 == null) {
                                            // Mesma regra fail-closed do update: sem hash, não instala.
                                            rollbackMessage = "A release ${target.tag} não anuncia SHA-256 — instalação bloqueada por segurança."
                                            showRollbackMessage = true
                                        } else {
                                            rollbackTarget = target
                                            showRollbackConfirm = true
                                        }
                                    } finally {
                                        rollbackBusy = false
                                    }
                                }
                            },
                            enabled = !rollbackBusy,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8A93A0))
                        ) {
                            if (rollbackBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF8A93A0)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Voltar para a versão anterior", fontSize = 13.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1D2430))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                component = ComponentName("com.android.settings", "com.android.settings.Settings")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configurações",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir Configurações do Android", color = Color.White)
                    }
                }
            }
        }

        // Seção de Créditos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF13151A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Créditos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color(0xFF1D2430))

                Text(
                    "HavalShisuku foi originalmente desenvolvido por netseek, bobaoapae e tontonhaval. Todo o crédito e agradecimento pelo trabalho vão para eles.",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B8C4),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Text(
                    "Este é um fork mantido por Felipe (github.com/fefezo), com restyle do cluster, correções de ar-condicionado e secagem, instalador macOS e atualização remota com verificação de integridade.",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B8C4),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Text(
                    "Obrigado pelo seu trabalho! 🙏",
                    fontSize = 14.sp,
                    color = Color(0xFF4ADE80),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showAdvancedDialog) {
        AlertDialog(
            onDismissRequest = { showAdvancedDialog = false },
            title = { Text("Confirmação") },
            text = { Text("Quer ativar o uso avançado? Pode causar instabilidades, utilize por conta e risco.") },
            confirmButton = {
                TextButton(onClick = {
                    showAdvancedDialog = false
                    prefs.edit { putBoolean(SharedPreferencesKeys.ADVANCE_USE.key, true) }
                }) {
                    Text("Ativar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvancedDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Verificação de Atualização") },
            text = { Text(updateMessage) },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (updateAvailable) {
        AlertDialog(
            onDismissRequest = { updateAvailable = false },
            title = { Text("Atualização disponível: $latestVersion") },
            text = {
                Text(
                    if (latestPublished != null) "Publicada em $latestPublished. Deseja baixar?"
                    else "Deseja baixar?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    updateAvailable = false
                    startDownload()
                }) {
                    Text("Sim")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateAvailable = false }) {
                    Text("Não")
                }
            }
        )
    }

    if (isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    if (latestVersion.isNotBlank()) "Baixando $downloadLabel — $latestVersion"
                    else "Baixando $downloadLabel"
                )
            },
            text = {
                Column {
                    LinearProgressIndicator(progress = { downloadProgress })
                    Text("${(downloadProgress * 100).toInt()}%")
                    // Resultado do envio dos logs, no caso do rollback. Fica aqui, e não
                    // num diálogo à parte, para não custar um toque extra num rollback.
                    val logStatus = rollbackLogStatus
                    if (logStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(logStatus, fontSize = 12.sp, color = Color(0xFFB0B8C4))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    downloadJob?.cancel()
                    isDownloading = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (downloadError != null) {
        AlertDialog(
            onDismissRequest = { downloadError = null },
            title = { Text("Erro no download") },
            text = { Text(downloadError!!) },
            confirmButton = {
                TextButton(onClick = {
                    downloadError = null
                    startDownload()
                }) {
                    Text("Tentar novamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { downloadError = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showRollbackMessage) {
        AlertDialog(
            onDismissRequest = { showRollbackMessage = false },
            title = { Text("Rollback indisponível") },
            text = { Text(rollbackMessage) },
            confirmButton = {
                TextButton(onClick = { showRollbackMessage = false }) {
                    Text("OK")
                }
            }
        )
    }

    val rollbackTargetNow = rollbackTarget

    if (showRollbackConfirm && rollbackTargetNow != null) {
        AlertDialog(
            onDismissRequest = { showRollbackConfirm = false },
            title = { Text("Voltar para a ${rollbackTargetNow.tag}?") },
            text = {
                Text("Os logs da versão instalada (v${version.removePrefix("v")}) serão enviados antes de instalar.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRollbackConfirm = false
                    downloadLabel = "versão anterior"
                    rollbackLogStatus = null
                    scope.launch {
                        val problem = sendRollbackLogs()
                        if (problem == null) {
                            proceedWithRollback(rollbackTargetNow)
                        } else {
                            rollbackLogProblem = problem
                            showRollbackNoLogs = true
                        }
                    }
                }) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRollbackConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Sem token ou com o envio falhando, o rollback é a saída de emergência:
    // avisa e deixa seguir, nunca bloqueia.
    if (showRollbackNoLogs && rollbackTargetNow != null) {
        AlertDialog(
            onDismissRequest = { showRollbackNoLogs = false },
            title = { Text("Não consegui enviar os logs") },
            text = {
                Text("$rollbackLogProblem\n\nContinuar o rollback para a ${rollbackTargetNow.tag} mesmo assim?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRollbackNoLogs = false
                    rollbackLogStatus = "Logs não enviados: $rollbackLogProblem"
                    proceedWithRollback(rollbackTargetNow)
                }) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRollbackNoLogs = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissão necessária") },
            text = { Text("Permita a instalação de apps de fontes desconhecidas.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    requestPermissionLauncher.launch(intent)
                }) {
                    Text("Configurações")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Compartilha o arquivo de diagnóstico via FileProvider (authority <app>.provider,
// declarada no manifest com external-files-path — cobre getExternalFilesDir).
private fun shareDiagnosticsFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Logs Haval Tool — diagnóstico")
        putExtra(Intent.EXTRA_TEXT, "Logs de diagnóstico do Haval Tool (widget do cluster).")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Enviar logs"))
}

/**
 * Reinicia a central (mesmo efeito do reset pelo botão físico). Roda o comando
 * 2s depois em background para o shell responder antes de a conexão cair —
 * se o reboot fosse síncrono, o telnet reportaria erro quando a central caísse.
 *
 * Caminho 1: telnet local (porta 23) — o shell da central é root (prompt ":/ #")
 * e o app tem acesso por ter UID ≤ 10999 (mesmo mecanismo que inicia o Shizuku).
 * Caminho 2 (fallback): Shizuku — o servidor também foi iniciado via telnet root.
 *
 * @return null em caso de sucesso; senão, mensagem de erro amigável.
 */
private fun restartHeadUnit(): String? {
    // Caminho 1: telnet local (root).
    try {
        val telnet = TelnetClientWrapper()
        telnet.connect("127.0.0.1", 23)
        try {
            telnet.executeCommand("(sleep 2; reboot) >/dev/null 2>&1 &")
            Log.w(TAG, "Reboot da central enfileirado via telnet (127.0.0.1:23)")
            return null
        } finally {
            telnet.disconnect()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Telnet indisponível para o reboot, tentando Shizuku", e)
    }
    // Caminho 2: Shizuku (servidor iniciado via telnet — também roda como root).
    if (Shizuku.pingBinder()) {
        ShizukuUtils.runCommandOnBackground(
            arrayOf("sh", "-c", "sleep 2; reboot"),
            null
        )
        Log.w(TAG, "Reboot da central enfileirado via Shizuku")
        return null
    }
    return "Não consegui reiniciar: o telnet local não respondeu e o serviço Shizuku " +
        "não está ativo. Use o botão físico de reset."
}

@Composable
fun DiagnosticsTab() {
    val context = LocalContext.current
    val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()

    var servicesActive by remember { mutableStateOf(false) }
    var clusterCard by remember { mutableIntStateOf(-1) }
    var mainScreenOn by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var lastFile by remember { mutableStateOf<String?>(null) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var lastSummary by remember { mutableStateOf<String?>(null) }
    var lastGistUrl by remember { mutableStateOf<String?>(null) }
    var token by remember {
        mutableStateOf(prefs.getString(SharedPreferencesKeys.GITHUB_GIST_TOKEN.key, "") ?: "")
    }
    var tokenVisible by remember { mutableStateOf(false) }

    var showRestartDialog by remember { mutableStateOf(false) }
    var restarting by remember { mutableStateOf(false) }
    var restartDone by remember { mutableStateOf<String?>(null) }
    var restartError by remember { mutableStateOf<String?>(null) }

    // v2.5: card de sondagem do DVR
    var dvrStatus by remember { mutableStateOf<String?>(null) }
    var dvrLastResult by remember { mutableStateOf<String?>(null) }
    var dvrBusy by remember { mutableStateOf(false) }
    var dvrParamA by remember { mutableStateOf("0") }
    var dvrParamB by remember { mutableStateOf("0") }

    // Dispara um comando DVR na central em background e mostra o resultado
    val runDvrCommand: (String) -> Unit = { command ->
        dvrBusy = true
        dvrLastResult = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val sm = ServiceManager.getInstance()
                when (command) {
                    "open" -> sm.dvrOpenApp()
                    "close" -> sm.dvrCloseApp()
                    "photo" -> sm.dvrCapturePhoto(dvrParamA.toIntOrNull() ?: 0, dvrParamB.toIntOrNull() ?: 0)
                    else -> sm.dvrCaptureVideo(dvrParamA.toIntOrNull() ?: 0, dvrParamB.toIntOrNull() ?: 0)
                }
            }
            dvrLastResult = result
            dvrBusy = false
        }
    }

    // Espelho ao vivo do estado que o widget do cluster enxerga
    LaunchedEffect(Unit) {
        while (true) {
            val sm = ServiceManager.getInstance()
            servicesActive = sm.isServicesInitialized
            mainScreenOn = sm.isMainScreenOn
            clusterCard = sm.clusterCardView
            delay(250)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Seção: estado do widget
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF13151A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Estado do Widget do Cluster",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Como o serviço está vendo as coisas agora. Se o widget sumiu, " +
                        "é este estado que interessa comparar com o que aparece no cluster.",
                    color = Color(0xFFB0B8C4),
                    fontSize = 14.sp
                )

                HorizontalDivider(color = Color(0xFF1D2430))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Serviços:", color = Color(0xFFB0B8C4))
                    Text(
                        if (servicesActive) "Ativo" else "Inativo",
                        color = if (servicesActive) Color(0xFF4ADE80) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tela principal (motor ligado):", color = Color(0xFFB0B8C4))
                    Text(
                        if (mainScreenOn) "Ligada" else "Desligada",
                        color = if (mainScreenOn) Color(0xFF4ADE80) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Card do cluster (msg 133):", color = Color(0xFFB0B8C4))
                    Text(
                        when (clusterCard) {
                            1 -> "1 — card principal (widget A/C)"
                            -1 -> "nunca recebido"
                            else -> "$clusterCard — widget oculto"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (servicesActive && clusterCard != 1) {
                    Text(
                        "Atenção: o serviço nunca recebeu (ou não está mais em) card 1. " +
                            "Isso explica o widget sumido — o cluster não está reportando a tela " +
                            "principal para o app.",
                        color = Color(0xFFEAA33E),
                        fontSize = 13.sp
                    )
                } else if (servicesActive && clusterCard == 1) {
                    Text(
                        "O card 1 está ativo — o app ACHA que o widget deveria estar visível. " +
                            "Se ele não aparece no cluster, o problema está na projeção/WebView, " +
                            "e os logs vão mostrar onde.",
                        color = Color(0xFF4ADE80),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Seção: reiniciar a central
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF13151A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Reiniciar a central",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Mesmo efeito do reset pelo botão físico: a tela desliga e o sistema " +
                        "volta em ~1 minuto (o app volta sozinho). Quando o widget some de vez " +
                        "ou o app fica travado, é o que costuma resolver — os serviços da " +
                        "central sobem do zero.",
                    color = Color(0xFFB0B8C4),
                    fontSize = 14.sp
                )

                HorizontalDivider(color = Color(0xFF1D2430))

                if (restarting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.Primary
                        )
                        Text(
                            "Enviando comando… a tela vai desligar em instantes.",
                            color = Color(0xFFB0B8C4),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Button(
                        onClick = { showRestartDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626)
                        ),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reiniciar a central agora", fontSize = 16.sp)
                    }
                }

                restartDone?.let {
                    Text(
                        it,
                        color = Color(0xFF4ADE80),
                        fontSize = 14.sp
                    )
                }

                restartError?.let {
                    Text(
                        it,
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Seção: teste DVR (v2.5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF13151A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Teste DVR (gravação)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Sondagem da interface DVR da central: descobre se o carro tem DVR " +
                        "e como os comandos de foto/vídeo se comportam. Os parâmetros ainda " +
                        "são desconhecidos — teste valores diferentes e observe o carro. " +
                        "Cada clique também vai para o logcat (WARN) e sai no envio de logs.",
                    color = Color(0xFFB0B8C4),
                    fontSize = 14.sp
                )

                HorizontalDivider(color = Color(0xFF1D2430))

                if (dvrBusy) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.Primary
                        )
                        Text(
                            "Consultando a central…",
                            color = Color(0xFFB0B8C4),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            dvrBusy = true
                            dvrLastResult = null
                            scope.launch {
                                val snapshot = withContext(Dispatchers.IO) {
                                    ServiceManager.getInstance().dvrStatusSnapshot()
                                }
                                dvrStatus = snapshot
                                dvrBusy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descobrir suporte e estado do DVR", fontSize = 16.sp)
                    }
                }

                dvrStatus?.let {
                    Text(
                        it,
                        color = Color(0xFFEAA33E),
                        fontSize = 13.sp
                    )
                }

                Text(
                    "Parâmetros de foto/vídeo (testar valores):",
                    color = Color(0xFFB0B8C4),
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = dvrParamA,
                        onValueChange = { input ->
                            dvrParamA = input.filter { ch -> ch.isDigit() || ch == '-' }
                        },
                        label = { Text("A") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2F37),
                            unfocusedContainerColor = Color(0xFF2A2F37),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFB0B8C4),
                            focusedIndicatorColor = Color(0xFF4A9EFF),
                            unfocusedIndicatorColor = Color(0xFF3A3F47)
                        )
                    )
                    TextField(
                        value = dvrParamB,
                        onValueChange = { input ->
                            dvrParamB = input.filter { ch -> ch.isDigit() || ch == '-' }
                        },
                        label = { Text("B") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2F37),
                            unfocusedContainerColor = Color(0xFF2A2F37),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFB0B8C4),
                            focusedIndicatorColor = Color(0xFF4A9EFF),
                            unfocusedIndicatorColor = Color(0xFF3A3F47)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { runDvrCommand("photo") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D4ED8)
                        ),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Foto", fontSize = 15.sp)
                    }
                    Button(
                        onClick = { runDvrCommand("video") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626)
                        ),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vídeo", fontSize = 15.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { runDvrCommand("open") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                    ) {
                        Text("Abrir app DVR", fontSize = 14.sp)
                    }
                    Button(
                        onClick = { runDvrCommand("close") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                    ) {
                        Text("Fechar app", fontSize = 14.sp)
                    }
                }

                dvrLastResult?.let {
                    Text(
                        it,
                        color = if (it.startsWith("erro") || it.contains("falha") || it.contains("não disponível")) {
                            Color(0xFFEF4444)
                        } else {
                            Color(0xFF4ADE80)
                        },
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { showRestartDialog = false },
                containerColor = Color(0xFF1A1E24),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFB0B8C4),
                title = { Text("Reiniciar a central?") },
                text = {
                    Text(
                        "A tela da central vai desligar e o sistema reinicia em ~1 minuto " +
                            "(o app volta sozinho ao terminar). Espere o carro estar parado — " +
                            "navegação e câmeras ficam fora durante o reinício."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRestartDialog = false
                            restarting = true
                            restartError = null
                            restartDone = null
                            scope.launch {
                                val error = withContext(Dispatchers.IO) {
                                    restartHeadUnit()
                                }
                                restarting = false
                                if (error == null) {
                                    restartDone =
                                        "Comando enviado — a central vai desligar em instantes."
                                } else {
                                    restartError = error
                                }
                            }
                        }
                    ) {
                        Text(
                            "Reiniciar",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestartDialog = false }) {
                        Text("Cancelar", color = Color(0xFFB0B8C4))
                    }
                }
            )
        }

        // Seção: enviar logs ao GitHub
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF13151A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Enviar ao GitHub",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Coleta um arquivo .txt com o estado do app e o logcat (processo do Haval + " +
                        "buffer de crash + linha de saúde do widget a cada 30s) e sobe como gist " +
                        "público na sua conta. A URL aparece aqui — é só colar no chat para a " +
                        "análise. O token fica salvo apenas neste aparelho e sai oculto do log.",
                    color = Color(0xFFB0B8C4),
                    fontSize = 14.sp
                )

                HorizontalDivider(color = Color(0xFF1D2430))

                TextField(
                    value = token,
                    onValueChange = { newToken ->
                        token = newToken
                        prefs.edit()
                            .putString(SharedPreferencesKeys.GITHUB_GIST_TOKEN.key, newToken)
                            .apply()
                    },
                    label = { Text("Token GitHub (escopo \"gists\")") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (tokenVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (tokenVisible) "Ocultar token" else "Mostrar token",
                                tint = Color(0xFFB0B8C4)
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2A2F37),
                        unfocusedContainerColor = Color(0xFF2A2F37),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFB0B8C4),
                        focusedIndicatorColor = Color(0xFF4A9EFF),
                        unfocusedIndicatorColor = Color(0xFF3A3F47),
                        focusedLabelColor = Color(0xFF4A9EFF),
                        unfocusedLabelColor = Color(0xFFB0B8C4)
                    )
                )

                if (token.isBlank()) {
                    Text(
                        "Cole acima o token de github.com/settings/tokens com o escopo \"gists\" " +
                            "marcado (nunca entra no APK — fica só neste aparelho).",
                        color = Color(0xFFEAA33E),
                        fontSize = 13.sp
                    )
                }

                if (sending) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.Primary
                        )
                        Text("Coletando e enviando…", color = Color(0xFFB0B8C4), fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            sending = true
                            lastError = null
                            lastSummary = null
                            lastGistUrl = null
                            lastFile = null
                            scope.launch {
                                try {
                                    val file = withContext(Dispatchers.IO) {
                                        DiagnosticsCollector.capture(context, prefs)
                                    }
                                    lastFile = file.absolutePath
                                    lastSummary = String.format(
                                        Locale.US,
                                        "%.1f KB coletados.",
                                        file.length() / 1024f
                                    )
                                    val url = withContext(Dispatchers.IO) {
                                        GistUploader.upload(file, token)
                                    }
                                    lastGistUrl = url
                                } catch (e: GistUploader.UploadException) {
                                    Log.e(TAG, "Falha no upload do gist (HTTP ${e.code})", e)
                                    lastError = e.message ?: "Falha no upload (HTTP ${e.code})"
                                } catch (e: Exception) {
                                    Log.e(TAG, "Falha ao coletar/enviar diagnóstico", e)
                                    lastError = e.message ?: "Erro desconhecido ao coletar logs"
                                } finally {
                                    sending = false
                                }
                            }
                        },
                        enabled = token.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            disabledContainerColor = Color(0xFF2A2F37),
                            disabledContentColor = Color(0xFF6A7280)
                        ),
                        shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius)
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Coletar e enviar ao GitHub", fontSize = 16.sp)
                    }

                    TextButton(
                        onClick = {
                            sending = true
                            lastError = null
                            lastSummary = null
                            lastGistUrl = null
                            scope.launch {
                                try {
                                    val file = withContext(Dispatchers.IO) {
                                        DiagnosticsCollector.capture(context, prefs)
                                    }
                                    lastFile = file.absolutePath
                                    lastSummary = String.format(
                                        Locale.US,
                                        "%.1f KB de logs coletados.",
                                        file.length() / 1024f
                                    )
                                    shareDiagnosticsFile(context, file)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Falha ao coletar diagnóstico", e)
                                    lastError = e.message ?: "Erro desconhecido ao coletar logs"
                                } finally {
                                    sending = false
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "Prefiro enviar por outro aplicativo (sem GitHub)",
                            color = Color(0xFF4A9EFF),
                            fontSize = 13.sp
                        )
                    }
                }

                lastError?.let {
                    Text(
                        "Erro: $it",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                }

                lastSummary?.let {
                    Text(
                        it,
                        color = Color(0xFF4ADE80),
                        fontSize = 14.sp
                    )
                }

                lastGistUrl?.let { url ->
                    HorizontalDivider(color = Color(0xFF1D2430))
                    Text(
                        "Gist criado! Cole esta URL no chat para a análise:",
                        color = Color(0xFFB0B8C4),
                        fontSize = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            url,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF4ADE80),
                            fontSize = 13.sp
                        )
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("URL do gist", url))
                                Toast.makeText(context, "URL copiada — cole no chat", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copiar URL",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFB0B8C4)
                            )
                        }
                    }
                    Text(
                        "O gist é público (qualquer um com a URL vê). Para análise é ideal; " +
                            "depois é só apagar em gist.github.com.",
                        color = Color(0xFFEAA33E),
                        fontSize = 12.sp
                    )
                }

                lastFile?.let {
                    Text(
                        "Arquivo: $it",
                        color = Color(0xFFB0B8C4),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HavalShisukuTheme {
        MainScreen()
    }
}