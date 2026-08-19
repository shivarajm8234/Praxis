package ai.helply.app.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI-compatible Cloud API inference engine.
 *
 * Works with any provider that implements the OpenAI Chat Completions API:
 *   - OpenAI (api.openai.com)
 *   - Groq (api.groq.com)
 *   - Together AI (api.together.xyz)
 *   - Ollama (localhost:11434)
 *   - Any OpenRouter / LiteLLM / vLLM endpoint
 *
 * Config (apiKey, baseUrl, modelId) is persisted in SharedPreferences.
 */
@Singleton
class CloudApiEngine @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "helply_cloud_api"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL_ID = "model_id"
        private const val KEY_INFERENCE_MODE = "inference_mode"

        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL_ID = "gpt-4o-mini"

        /** Common provider presets */
        data class ProviderPreset(
            val name: String,
            val baseUrl: String,
            val defaultModel: String,
            val description: String
        )

        val PROVIDER_PRESETS = listOf(
            ProviderPreset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", "GPT-4o, GPT-4o-mini"),
            ProviderPreset("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", "Fast LLaMA, Mixtral"),
            ProviderPreset("Together AI", "https://api.together.xyz/v1", "meta-llama/Llama-3-8b-chat-hf", "Open-source models"),
            ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", "Multi-provider router"),
            ProviderPreset("Ollama (Local)", "http://localhost:11434/v1", "llama3", "Local self-hosted")
        )
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ─── Config Persistence ──────────────────────────────

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""
    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    fun getModelId(): String = prefs.getString(KEY_MODEL_ID, DEFAULT_MODEL_ID) ?: DEFAULT_MODEL_ID

    fun getInferenceMode(): InferenceMode {
        val stored = prefs.getString(KEY_INFERENCE_MODE, InferenceMode.ON_DEVICE.name)
        return try {
            InferenceMode.valueOf(stored ?: InferenceMode.ON_DEVICE.name)
        } catch (_: Exception) {
            InferenceMode.ON_DEVICE
        }
    }

    fun saveConfig(apiKey: String, baseUrl: String, modelId: String) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_BASE_URL, baseUrl.trim().trimEnd('/'))
            .putString(KEY_MODEL_ID, modelId.trim())
            .apply()
    }

    fun saveInferenceMode(mode: InferenceMode) {
        prefs.edit().putString(KEY_INFERENCE_MODE, mode.name).apply()
    }

    fun isConfigured(): Boolean {
        val key = getApiKey()
        val url = getBaseUrl()
        return key.isNotBlank() && url.isNotBlank()
    }

    // ─── Streaming Chat Completions ─────────────────────

    /**
     * Sends a streaming chat completion request to the configured OpenAI-compatible endpoint.
     * Returns a Flow<String> of content deltas, identical to GemmaEngineManager's interface.
     */
    fun generateStreamingResponse(
        prompt: String,
        systemPrompt: String = "You are an intelligent AI assistant inside Helply Student OS. Be concise, helpful, and accurate."
    ): Flow<String> = flow {
        val apiKey = getApiKey()
        val baseUrl = getBaseUrl()
        val modelId = getModelId()

        if (apiKey.isBlank()) {
            emit("⚠️ API key not configured. Go to Settings → Cloud API to enter your API key.")
            return@flow
        }

        android.util.Log.d("HelplyCloud", "═══════════════════════════════════════════════")
        android.util.Log.d("HelplyCloud", "Cloud API Request")
        android.util.Log.d("HelplyCloud", "  Provider:  $baseUrl")
        android.util.Log.d("HelplyCloud", "  Model:     $modelId")
        android.util.Log.d("HelplyCloud", "  Prompt:    \"${prompt.take(80)}...\"")
        android.util.Log.d("HelplyCloud", "═══════════════════════════════════════════════")

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val requestBody = JSONObject().apply {
            put("model", modelId)
            put("messages", messagesArray)
            put("stream", true)
            put("max_tokens", 2048)
            put("temperature", 0.7)
        }

        val endpoint = "$baseUrl/chat/completions"

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                val statusCode = response.code

                val userMessage = when (statusCode) {
                    401 -> "❌ Invalid API key. Check your key in Settings."
                    403 -> "❌ Access denied. Your API key may lack permissions for model '$modelId'."
                    404 -> "❌ Model '$modelId' not found at $baseUrl. Check your model ID."
                    429 -> "⚠️ Rate limit exceeded. Wait a moment and try again."
                    500, 502, 503 -> "⚠️ Server error ($statusCode). The API provider may be experiencing issues."
                    else -> "❌ API error ($statusCode): ${parseApiError(errorBody)}"
                }

                android.util.Log.e("HelplyCloud", "API Error $statusCode: $errorBody")
                emit(userMessage)
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val data = line ?: continue

                if (data.startsWith("data: ")) {
                    val jsonStr = data.removePrefix("data: ").trim()

                    if (jsonStr == "[DONE]") {
                        android.util.Log.d("HelplyCloud", "✅ Stream complete")
                        break
                    }

                    try {
                        val chunk = JSONObject(jsonStr)
                        val choices = chunk.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) {
                                emit(content)
                            }
                        }
                    } catch (parseError: Exception) {
                        // Skip malformed SSE lines (keep-alive, comments, etc.)
                        android.util.Log.v("HelplyCloud", "Skipping non-JSON SSE line: ${data.take(50)}")
                    }
                }
            }

            reader.close()
            response.close()

        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("HelplyCloud", "DNS resolution failed: ${e.message}")
            emit("❌ Cannot reach $baseUrl. Check your internet connection and base URL.")
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("HelplyCloud", "Connection timeout: ${e.message}")
            emit("⚠️ Request timed out. The server may be overloaded.")
        } catch (e: javax.net.ssl.SSLException) {
            android.util.Log.e("HelplyCloud", "SSL error: ${e.message}")
            emit("❌ SSL/TLS error connecting to $baseUrl. Check if the URL is correct.")
        } catch (e: Exception) {
            android.util.Log.e("HelplyCloud", "Unexpected error: ${e.message}", e)
            emit("❌ Connection error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming test to verify API key and connectivity.
     * Returns a Pair<Boolean, String> — (success, message).
     */
    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val baseUrl = getBaseUrl()
        val modelId = getModelId()

        if (apiKey.isBlank()) return@withContext Pair(false, "API key is empty")

        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Say 'connected' in one word.")
                })
            }

            val requestBody = JSONObject().apply {
                put("model", modelId)
                put("messages", messagesArray)
                put("max_tokens", 10)
                put("stream", false)
            }

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content", "") ?: ""
                Pair(true, "✅ Connected! Model responded: \"${content.trim().take(50)}\"")
            } else {
                Pair(false, "Error ${response.code}: ${parseApiError(body)}")
            }
        } catch (e: Exception) {
            Pair(false, "Connection failed: ${e.message?.take(80)}")
        }
    }

    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String = "You are an intelligent AI assistant inside Helply Student OS. Be concise, helpful, and accurate."
    ): String = withContext(Dispatchers.IO) {
        val result = StringBuilder()
        generateStreamingResponse(prompt, systemPrompt).collect { chunk ->
            result.append(chunk)
        }
        result.toString()
    }

    private fun parseApiError(body: String): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            error?.optString("message", body) ?: body
        } catch (_: Exception) {
            body.take(120)
        }
    }
}

/** Determines whether inference runs on-device or via cloud API */
enum class InferenceMode {
    ON_DEVICE,
    CLOUD_API
}
