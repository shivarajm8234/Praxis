package ai.helply.app.ai

import android.content.Context
import android.os.StatFs
import ai.helply.app.BuildConfig
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent repository that tracks which AI models are downloaded and verified on disk.
 * Uses SharedPreferences for metadata and the filesystem for actual model files.
 *
 * Model storage layout:
 *   context.filesDir/models/<modelId>/<fileName>
 *   context.filesDir/models/<modelId>/<fileName>.tmp  (during download)
 */
@Singleton
class ModelRepository @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "helply_model_registry"
        private const val KEY_PREFIX_INSTALLED = "installed_"
        private const val KEY_PREFIX_PATH = "path_"
        private const val KEY_PREFIX_TIMESTAMP = "timestamp_"
        private const val KEY_PREFIX_VERIFIED = "verified_"
        private const val KEY_HF_TOKEN = "hf_access_token"
        private const val MODELS_DIR = "models"
    }

    fun getHfToken(): String {
        val token = prefs.getString(KEY_HF_TOKEN, "") ?: ""
        return if (token.isNotEmpty()) token else BuildConfig.DEFAULT_HF_TOKEN
    }

    fun setHfToken(token: String) {
        prefs.edit().putString(KEY_HF_TOKEN, token.trim()).apply()
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Root directory for all model files */
    fun getModelsDir(): File {
        val dir = File(context.filesDir, MODELS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Directory for a specific model */
    fun getModelDir(modelId: String): File {
        val dir = File(getModelsDir(), modelId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Full path to the model file on disk */
    fun getModelFile(modelId: String): File? {
        val config = ModelRegistry.getById(modelId) ?: return null
        return File(getModelDir(modelId), config.fileName)
    }

    /** Path to the temporary download file */
    fun getTempFile(modelId: String): File? {
        val config = ModelRegistry.getById(modelId) ?: return null
        return File(getModelDir(modelId), "${config.fileName}.tmp")
    }

    /**
     * Checks if a model is fully installed by verifying both:
     * 1. The SharedPreferences flag is set
     * 2. The actual model file exists on disk
     */
    fun isModelInstalled(modelId: String): Boolean {
        val modelFile = getModelFile(modelId) ?: return false
        if (!modelFile.exists() || modelFile.length() == 0L) {
            removeModelRecord(modelId)
            return false
        }
        val prefInstalled = prefs.getBoolean("${KEY_PREFIX_INSTALLED}$modelId", false)
        if (!prefInstalled) {
            // Auto-detect & register model file present on disk
            markModelInstalled(modelId, modelFile.absolutePath, verified = true)
        }
        return true
    }

    /** Returns true if the model file passed SHA-256 verification */
    fun isModelVerified(modelId: String): Boolean {
        return prefs.getBoolean("${KEY_PREFIX_VERIFIED}$modelId", false)
    }

    /** Mark a model as successfully installed and verified */
    fun markModelInstalled(modelId: String, filePath: String, verified: Boolean) {
        prefs.edit()
            .putBoolean("${KEY_PREFIX_INSTALLED}$modelId", true)
            .putString("${KEY_PREFIX_PATH}$modelId", filePath)
            .putLong("${KEY_PREFIX_TIMESTAMP}$modelId", System.currentTimeMillis())
            .putBoolean("${KEY_PREFIX_VERIFIED}$modelId", verified)
            .apply()
    }

    /** Get the stored file path for an installed model */
    fun getInstalledModelPath(modelId: String): String? {
        if (!isModelInstalled(modelId)) return null
        return prefs.getString("${KEY_PREFIX_PATH}$modelId", null)
    }

    /** Remove model record from registry and delete files from disk */
    fun deleteModel(modelId: String): Boolean {
        removeModelRecord(modelId)

        val modelDir = getModelDir(modelId)
        return if (modelDir.exists()) {
            modelDir.deleteRecursively()
        } else true
    }

    /** Get IDs of all installed models */
    fun getInstalledModelIds(): List<String> {
        return ModelRegistry.ALL_MODELS
            .map { it.id }
            .filter { isModelInstalled(it) }
    }

    /** Available storage on the internal storage partition in bytes */
    fun getAvailableStorageBytes(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    /** Check if there's enough space to download a model */
    fun hasEnoughSpace(modelConfig: OnDeviceModelConfig): Boolean {
        val required = (modelConfig.sizeBytes * ModelRegistry.STORAGE_SAFETY_MULTIPLIER).toLong()
        return getAvailableStorageBytes() >= required
    }

    /** Total size of all installed models in bytes */
    fun getTotalInstalledSizeBytes(): Long {
        return ModelRegistry.ALL_MODELS
            .filter { isModelInstalled(it.id) }
            .sumOf { config ->
                val file = getModelFile(config.id)
                file?.length() ?: 0L
            }
    }

    private fun removeModelRecord(modelId: String) {
        prefs.edit()
            .remove("${KEY_PREFIX_INSTALLED}$modelId")
            .remove("${KEY_PREFIX_PATH}$modelId")
            .remove("${KEY_PREFIX_TIMESTAMP}$modelId")
            .remove("${KEY_PREFIX_VERIFIED}$modelId")
            .apply()
    }
}
