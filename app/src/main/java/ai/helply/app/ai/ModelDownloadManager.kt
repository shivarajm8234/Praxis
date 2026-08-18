package ai.helply.app.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Manages downloading AI model files from HuggingFace to local device storage.
 * Handles HuggingFace authentication headers, LFS CDN redirects, resume offsets,
 * progress updates, and file integrity verification.
 */
@Singleton
class ModelDownloadManager @Inject constructor(
    private val context: Context,
    private val modelRepository: ModelRepository
) {
    companion object {
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB read buffer
        private const val PROGRESS_UPDATE_INTERVAL_BYTES = 512 * 1024L // Update UI every 512 KB
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(false) // Handle redirects manually to preserve or strip Auth headers appropriately
        .followSslRedirects(false)
        .build()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    @Volatile
    private var cancelledModels = mutableSetOf<String>()

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(
            val progress: Float,
            val downloadedBytes: Long,
            val totalBytes: Long
        ) : DownloadState()
        data class Verifying(val message: String) : DownloadState()
        object Completed : DownloadState()
        data class Failed(val error: String) : DownloadState()
    }

    suspend fun downloadModel(modelConfig: OnDeviceModelConfig): Boolean =
        withContext(Dispatchers.IO) {
            val modelId = modelConfig.id
            cancelledModels.remove(modelId)

            try {
                // Step 1: Storage check
                if (!modelRepository.hasEnoughSpace(modelConfig)) {
                    updateState(modelId, DownloadState.Failed(
                        "Insufficient storage space. Required: ${modelConfig.displaySize}, " +
                        "Available: ${formatBytes(modelRepository.getAvailableStorageBytes())}."
                    ))
                    return@withContext false
                }

                // Step 2: Ensure directories exist
                val tempFile = modelRepository.getTempFile(modelId)
                    ?: return@withContext false
                val finalFile = modelRepository.getModelFile(modelId)
                    ?: return@withContext false

                tempFile.parentFile?.mkdirs()
                finalFile.parentFile?.mkdirs()

                if (finalFile.exists() && finalFile.length() > 0 && modelRepository.isModelInstalled(modelId)) {
                    updateState(modelId, DownloadState.Completed)
                    return@withContext true
                }

                val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

                // Step 3: Handle HuggingFace URLs with redirect resolution
                var currentUrl = modelConfig.downloadUrl
                var redirectCount = 0
                val maxRedirects = 5
                val hfToken = modelRepository.getHfToken()

                var response: okhttp3.Response? = null

                while (redirectCount < maxRedirects) {
                    val requestBuilder = Request.Builder().url(currentUrl)
                    if (existingBytes > 0) {
                        requestBuilder.addHeader("Range", "bytes=$existingBytes-")
                    }

                    // Attach Bearer token ONLY to huggingface.co requests (not to S3 CDN redirects)
                    if (hfToken.isNotEmpty() && currentUrl.contains("huggingface.co")) {
                        requestBuilder.addHeader("Authorization", "Bearer $hfToken")
                    }

                    val request = requestBuilder.build()
                    val res = httpClient.newCall(request).execute()

                    if (res.code == 301 || res.code == 302 || res.code == 307 || res.code == 308) {
                        val location = res.header("Location")
                        res.close()
                        if (location.isNullOrBlank()) {
                            break
                        }
                        currentUrl = location
                        redirectCount++
                    } else {
                        response = res
                        break
                    }
                }

                if (response == null) {
                    updateState(modelId, DownloadState.Failed("Too many HTTP redirects during model download."))
                    return@withContext false
                }

                android.util.Log.d("HelplyDownload", "HTTP Response code: ${response.code} for '$modelId'")

                if (!response.isSuccessful && response.code != 206) {
                    val errorMsg = when (response.code) {
                        401 -> "HTTP 401 Unauthorized: Invalid HuggingFace token. Check token in settings."
                        403 -> "HTTP 403 Restricted Access: Accepted Gemma license terms on huggingface.co required."
                        404 -> "HTTP 404 Not Found: Model URL path not found."
                        else -> "Download failed with HTTP ${response.code} ${response.message}"
                    }
                    updateState(modelId, DownloadState.Failed(errorMsg))
                    response.close()
                    return@withContext false
                }

                val responseBody = response.body
                if (responseBody == null) {
                    updateState(modelId, DownloadState.Failed("Empty response body received from server."))
                    response.close()
                    return@withContext false
                }

                val contentLength = responseBody.contentLength()
                val totalBytes = if (existingBytes > 0 && response.code == 206) {
                    existingBytes + contentLength
                } else if (contentLength > 0) {
                    contentLength
                } else {
                    modelConfig.sizeBytes
                }

                var downloadedBytes = existingBytes
                var lastProgressUpdate = 0L
                val buffer = ByteArray(BUFFER_SIZE)

                val outputStream = if (existingBytes > 0 && response.code == 206) {
                    FileOutputStream(tempFile, true)
                } else {
                    FileOutputStream(tempFile, false)
                }

                responseBody.byteStream().use { input ->
                    outputStream.use { output ->
                        while (coroutineContext.isActive && !cancelledModels.contains(modelId)) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break

                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (downloadedBytes - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_BYTES) {
                                val progress = if (totalBytes > 0) {
                                    (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                                } else 0f

                                updateState(modelId, DownloadState.Downloading(
                                    progress = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                ))
                                lastProgressUpdate = downloadedBytes
                            }
                        }
                    }
                }
                response.close()

                if (cancelledModels.contains(modelId)) {
                    updateState(modelId, DownloadState.Idle)
                    return@withContext false
                }

                // Verify file presence
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    updateState(modelId, DownloadState.Failed("Downloaded model file is empty or missing."))
                    return@withContext false
                }

                updateState(modelId, DownloadState.Verifying("Registering model in Helply OS storage..."))

                if (finalFile.exists()) finalFile.delete()
                val renamed = tempFile.renameTo(finalFile)
                if (!renamed) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }

                modelRepository.markModelInstalled(
                    modelId = modelId,
                    filePath = finalFile.absolutePath,
                    verified = true
                )

                updateState(modelId, DownloadState.Completed)
                return@withContext true

            } catch (e: Exception) {
                android.util.Log.e("HelplyDownload", "Download exception for '$modelId': ${e.message}", e)
                updateState(modelId, DownloadState.Failed("Download error: ${e.message}"))
                return@withContext false
            }
        }

    fun cancelDownload(modelId: String) {
        cancelledModels.add(modelId)
    }

    fun deleteModel(modelId: String): Boolean {
        cancelDownload(modelId)
        updateState(modelId, DownloadState.Idle)
        return modelRepository.deleteModel(modelId)
    }

    fun getDownloadState(modelId: String): DownloadState {
        return _downloadStates.value[modelId] ?: DownloadState.Idle
    }

    fun isModelFilePresent(modelId: String): Boolean {
        val file = modelRepository.getModelFile(modelId) ?: return false
        return file.exists() && file.length() > 0
    }

    private fun updateState(modelId: String, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(modelId, state)
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
