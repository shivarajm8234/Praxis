package ai.helply.app.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class AdbInferenceReceiver : BroadcastReceiver() {

    @Inject
    lateinit var gemmaEngine: GemmaEngineManager

    override fun onReceive(context: Context?, intent: Intent?) {
        val prompt = intent?.getStringExtra("prompt") ?: "Explain Quantum Computing and On-Device LLM routing"
        val modelId = intent?.getStringExtra("model") ?: ModelRegistry.QWEN_05B.id

        Log.d("AI_RESPONSE", "==================================================")
        Log.d("AI_RESPONSE", "ADB TERMINAL INFERENCE TRIGGERED")
        Log.d("AI_RESPONSE", "Model ID: $modelId")
        Log.d("AI_RESPONSE", "Input Prompt: \"$prompt\"")
        Log.d("AI_RESPONSE", "==================================================")

        val outputFile = File(context?.getExternalFilesDir(null), "latest_ai_response.txt")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                gemmaEngine.initializeModel(modelId) { }
                val responseBuilder = StringBuilder()

                gemmaEngine.generateStreamingResponse(prompt = prompt, modelId = modelId).collect { chunk ->
                    Log.d("AI_RESPONSE", chunk)
                    responseBuilder.append(chunk)
                }

                outputFile.writeText(responseBuilder.toString())

                Log.d("AI_RESPONSE", "==================================================")
                Log.d("AI_RESPONSE", "AI INFERENCE COMPLETE | Output saved to: ${outputFile.absolutePath}")
                Log.d("AI_RESPONSE", "==================================================")
            } catch (e: Exception) {
                Log.e("AI_RESPONSE", "Error executing ADB inference: ${e.message}", e)
            }
        }
    }
}
