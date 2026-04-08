package activity.amigosecreto.util

import activity.amigosecreto.BuildConfig
import activity.amigosecreto.db.Desejo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente mínimo para a Gemini API (generateContent).
 *
 * A chave de API é lida de [BuildConfig.GEMINI_API_KEY] — definida em `local.properties`
 * (desenvolvimento local) ou pela variável de ambiente `GEMINI_API_KEY` (CI).
 * Quando ausente, [isDisponivel] retorna false e o call site exibe uma mensagem adequada.
 *
 * Chamada em background obrigatória — este método faz I/O de rede (bloqueante via OkHttp).
 */
object GeminiClient {

    private const val BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Retorna true quando a chave de API está configurada. */
    val isDisponivel: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /**
     * Sugere presentes complementares com base na lista de desejos do participante.
     *
     * @param nomeParticipante nome do participante (contexto para o modelo)
     * @param desejos lista de desejos já cadastrados
     * @return texto de resposta do modelo, ou null em caso de erro
     */
    fun sugerirPresentes(nomeParticipante: String, desejos: List<Desejo>): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return null

        val listaFormatada = if (desejos.isEmpty()) {
            "Nenhum desejo cadastrado ainda."
        } else {
            desejos.joinToString("\n") { d ->
                buildString {
                    append("- ${d.produto}")
                    if (!d.categoria.isNullOrBlank()) append(" (${d.categoria})")
                    if (d.precoMinimo > 0 || d.precoMaximo > 0) {
                        val nf = WindowInsetsUtils.numberFormatPtBr()
                        append(", faixa R$ ${nf.format(d.precoMinimo)}–${nf.format(d.precoMaximo)}")
                    }
                }
            }
        }

        val prompt = """
Você é um assistente de amigo secreto. Com base nos desejos de $nomeParticipante:

$listaFormatada

Sugira de 3 a 5 presentes complementares criativos e acessíveis que combinem com o perfil desta pessoa.
Seja específico, mencione marcas ou tipos de produtos quando relevante, e indique uma faixa de preço aproximada em reais.
Responda em português, de forma concisa e amigável.
        """.trimIndent()

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("GeminiClient: HTTP ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: IOException) {
            Timber.e(e, "GeminiClient: network error")
            null
        } catch (e: Exception) {
            Timber.e(e, "GeminiClient: parse error")
            null
        }
    }
}
