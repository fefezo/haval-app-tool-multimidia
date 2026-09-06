package br.com.redesurftank.havalshisuku.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sobe o arquivo de diagnóstico como um gist público na conta do usuário.
 * O token (escopo "gists", colado no app) nunca vai para o APK nem para o log —
 * fica apenas nas preferências do aparelho. O retorno é a URL do gist, que o
 * usuário cola no chat para análise.
 */
object GistUploader {
    private const val TAG = "GistUploader"
    private const val API_URL = "https://api.github.com/gists"
    // Limite de arquivo do GitHub Gists (1 MB). Com folga para o JSON/encoding.
    private const val MAX_CONTENT_BYTES = 950_000
    // Fatias mantidas ao truncar: cabeçalho do log + cauda (eventos recentes).
    private const val HEAD_CHARS = 120_000
    private const val TAIL_CHARS = 700_000

    /**
     * Faz upload do [file] como gist público e retorna o html_url.
     * Lança IOException com mensagem amigável em caso de falha.
     */
    suspend fun upload(file: File, token: String): String = withContext(Dispatchers.IO) {
        val content = truncateToFit(file)
        val body = JSONObject()
            .put("description", "Logs Haval Tool — diagnóstico (widget do cluster / secagem)")
            .put("public", true)
            .put(
                "files", JSONObject().put(
                    file.name,
                    JSONObject().put("content", content)
                )
            )

        val conn = URL(API_URL).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "HavalTool-Diagnostics")
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val responseBody = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                throw UploadException(code, err)
            }

            val url = JSONObject(responseBody).optString("html_url")
            if (url.isBlank()) {
                throw IllegalStateException("Resposta do GitHub sem html_url")
            }
            Log.i(TAG, "Gist criado: $url")
            url
        } finally {
            conn.disconnect()
        }
    }

    /** Erro de upload com o código HTTP e a mensagem do GitHub para exibir na UI. */
    class UploadException(val code: Int, responseBody: String) :
        Exception(describeUploadError(code, responseBody))

    private fun describeUploadError(code: Int, body: String): String {
        val msg = try {
            JSONObject(body).optString("message", "")
        } catch (e: Exception) {
            body.take(300)
        }
        return when (code) {
            401 -> "Token inválido ou revogado (HTTP 401). Gere um novo em github.com/settings/tokens e cole de novo."
            403 -> "Sem permissão para criar gists (HTTP 403). O token precisa do escopo \"Gists\"."
            404 -> "Token sem acesso à API (HTTP 404)."
            422 -> "GitHub recusou o conteúdo (HTTP 422): $msg"
            else -> "Falha no upload (HTTP $code): $msg"
        }
    }

    /**
     * Gists limitam arquivos a ~1 MB. Logs de logcat podem passar disso: mantém o
     * começo (cabeçalho/estado) e o fim (eventos recentes — o que interessa para
     * debugar) e corta o meio. O arquivo local completo fica no aparelho.
     */
    private fun truncateToFit(file: File): String {
        val text = file.readText()
        if (text.toByteArray(Charsets.UTF_8).size <= MAX_CONTENT_BYTES) return text

        val head = text.take(HEAD_CHARS)
        // Caso extremo (linhas gigantes): encurta a cauda até caber.
        var tailChars = TAIL_CHARS
        var result = ""
        while (true) {
            val tail = text.takeLast(tailChars)
            result = head +
                "\n\n[... LOG TRUNCADO no upload (limite do gist). " +
                "O arquivo completo permanece no aparelho. ...]\n\n" +
                tail
            if (result.toByteArray(Charsets.UTF_8).size <= MAX_CONTENT_BYTES || tailChars <= 10_000) break
            tailChars /= 2
        }
        Log.w(TAG, "Log truncado para upload: ${text.length} -> ${result.length} chars")
        return result
    }
}
