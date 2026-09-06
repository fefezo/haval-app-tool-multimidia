package br.com.redesurftank.havalshisuku.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import br.com.redesurftank.havalshisuku.managers.ServiceManager
import br.com.redesurftank.havalshisuku.models.CarConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Coleta um pacote de diagnóstico para debug (ex.: widget do cluster que some).
 * O arquivo gerado reúne: identificação do aparelho/app, estado dos serviços do
 * Haval, preferências relevantes e dumps do logcat (buffer principal + crash).
 * Tudo é gravado em texto simples — nada sai do aparelho até o usuário enviar.
 */
object DiagnosticsCollector {
    private const val TAG = "DiagnosticsCollector"

    /**
     * Gera o arquivo de diagnóstico em getExternalFilesDir e retorna o [File].
     * Bloqueante — chamar de um dispatcher de IO.
     */
    suspend fun capture(context: Context, prefs: SharedPreferences): File = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        appendHeader(sb, context, prefs)
        appendLogcatSection(sb, "Buffer principal do logcat (tudo, mais recente primeiro)", "-t", "20000")
        appendLogcatSection(sb, "Buffer de crash (últimos 2000 eventos)", "-b", "crash", "-t", "2000")
        appendLogcatSection(sb, "Linhas do processo do app (pid ${Process.myPid()})", "--pid", Process.myPid().toString(), "-t", "8000")

        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(context.getExternalFilesDir(null), "haval-diagnostico-$ts.txt")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        Log.i(TAG, "Diagnóstico salvo em ${file.absolutePath} (${file.length()} bytes)")
        file
    }

    // ------------------------------------------------------------------
    // Cabeçalho com o estado do aparelho + serviços + preferências
    // ------------------------------------------------------------------

    private fun appendHeader(sb: StringBuilder, context: Context, prefs: SharedPreferences) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
        val versionCode = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: Exception) {
            -1
        }
        val sm = ServiceManager.getInstance()

        sb.appendLine("===== Haval Tool — Diagnóstico =====")
        sb.appendLine("Coletado em: $now")
        sb.appendLine("")
        sb.appendLine("--- Aparelho ---")
        sb.appendLine("Fabricante/Modelo: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Firmware: ${Build.DISPLAY}")
        sb.appendLine("App: ${context.packageName} $versionName (code $versionCode)")
        sb.appendLine("Processo: pid ${Process.myPid()}, uid ${Process.myUid()}")
        sb.appendLine("Uptime do sistema: ${fmtMs(SystemClock.elapsedRealtime())}")
        sb.appendLine("")

        sb.appendLine("--- Estado dos serviços Haval ---")
        sb.appendLine("Services inicializados: ${sm.isServicesInitialized}")
        sb.appendLine("Boot recebido: ${fmtMs(sm.timeBootReceived)}")
        sb.appendLine("Início da inicialização: ${fmtMs(sm.timeStartInitialization)}")
        sb.appendLine("Inicializado: ${fmtMs(sm.timeInitialized)}")
        sb.appendLine("Tela principal ligada: ${sm.isMainScreenOn}")
        sb.appendLine("Card do cluster (último msg 133): ${sm.clusterCardView}")
        sb.appendLine("  → 1 = card principal (widget do A/C deveria aparecer)")
        val engine = sm.getData(CarConstants.CAR_BASIC_ENGINE_STATE.value)
        sb.appendLine("Estado do motor (raw): ${engine ?: "n/d"}")
        sb.appendLine("")

        sb.appendLine("--- Preferências (haval_prefs) ---")
        if (prefs.all.isEmpty()) {
            sb.appendLine("(vazias)")
        } else {
            prefs.all.entries.sortedBy { it.key }.forEach { (k, v) ->
                sb.appendLine("$k=$v")
            }
        }
        sb.appendLine("")
        sb.appendLine("--- Fim do cabeçalho — dumps do logcat abaixo ---")
        sb.appendLine("")
    }

    // ------------------------------------------------------------------
    // Dumps do logcat
    // ------------------------------------------------------------------

    private fun appendLogcatSection(sb: StringBuilder, title: String, vararg extraArgs: String) {
        sb.appendLine("==============================================")
        sb.appendLine("### $title")
        sb.appendLine("")

        val shizuku = runViaShizuku(*extraArgs)
        if (shizuku.isNotBlank()) {
            sb.appendLine("(fonte: shizuku/shell)")
            sb.appendLine(shizuku.trimEnd())
            sb.appendLine("")
            return
        }

        val local = runLocal(*extraArgs)
        if (local.isNotBlank()) {
            sb.appendLine("(fonte: logcat local — sem shizuku, só linhas do próprio app)")
            sb.appendLine(local.trimEnd())
            sb.appendLine("")
            return
        }

        sb.appendLine("(sem saída — Shizuku indisponível e logcat local sem permissão)")
        sb.appendLine("")
    }

    private fun runViaShizuku(vararg extraArgs: String): String {
        return try {
            val args = arrayOf("logcat", "-d", "-v", "threadtime") + extraArgs
            ShizukuUtils.runCommandAndGetOutput(args)
        } catch (e: Exception) {
            Log.w(TAG, "logcat via shizuku falhou", e)
            ""
        }
    }

    private fun runLocal(vararg extraArgs: String): String {
        return try {
            val cmd = ArrayList<String>()
            cmd.add("logcat")
            cmd.add("-d")
            cmd.add("-v")
            cmd.add("threadtime")
            extraArgs.forEach { cmd.add(it) }
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val out = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            out
        } catch (e: Exception) {
            Log.w(TAG, "logcat local falhou", e)
            ""
        }
    }

    /** Milissegundos (elapsedRealtime) -> mm:ss.mmm, como na tela Informações. */
    private fun fmtMs(ms: Long): String {
        if (ms <= 0) return "não inicializado"
        val minutes = ms / 60000
        val seconds = (ms / 1000) % 60
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
    }
}
