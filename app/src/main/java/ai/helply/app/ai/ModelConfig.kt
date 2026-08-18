package ai.helply.app.ai

/**
 * Configuration for an on-device AI model that can be downloaded and installed locally.
 *
 * @param id Unique identifier for the model
 * @param name Human-readable display name
 * @param architecture Technical architecture description
 * @param displaySize Human-readable file size string
 * @param sizeBytes Exact file size in bytes (used for download progress and storage checks)
 * @param description What this model is used for
 * @param downloadUrl Direct download URL (HuggingFace resolve link)
 * @param sha256Hash Expected SHA-256 hash for integrity verification (empty = skip verification)
 * @param fileName Local file name on disk inside models directory
 * @param modelType Classification of the model's purpose
 */
data class OnDeviceModelConfig(
    val id: String,
    val name: String,
    val architecture: String,
    val displaySize: String,
    val sizeBytes: Long,
    val description: String,
    val downloadUrl: String,
    val sha256Hash: String,
    val fileName: String,
    val modelType: ModelType
)

enum class ModelType {
    LLM_GEMMA,
    SPEECH_TO_TEXT,
    TEXT_EMBEDDING
}

/**
 * Central registry of all available on-device AI models.
 *
 * Model files are downloaded from HuggingFace at runtime and stored in:
 *   context.filesDir/models/<modelId>/<fileName>
 */
object ModelRegistry {

    val GEMMA_4_E2B_IT = OnDeviceModelConfig(
        id = "gemma-4-e2b-it",
        name = "Gemma 4 E2B IT (v1.1 CPU)",
        architecture = "Google Gemma 1.1 2B INT4 (MediaPipe LLM)",
        displaySize = "1.3 GB",
        sizeBytes = 1_346_427_328L,
        description = "Google Gemma 1.1 2B Instruct CPU model for fast offline student query resolution.",
        downloadUrl = "https://huggingface.co/innermost47/gemma-2b-it-int4-mediapipe/resolve/main/gemma-1.1-2b-it-cpu-int4.bin",
        sha256Hash = "",
        fileName = "gemma-4-e2b-it.bin",
        modelType = ModelType.LLM_GEMMA
    )

    val GEMMA_4B_IT = OnDeviceModelConfig(
        id = "gemma-4b-it",
        name = "Gemma 2B IT (v1.0 Standard)",
        architecture = "Google Gemma 1.0 2B INT4 (MediaPipe LLM)",
        displaySize = "1.3 GB",
        sizeBytes = 1_346_427_328L,
        description = "Official Google Gemma 1.0 2B Instruct model for offline reasoning.",
        downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-cpu-int4.bin",
        sha256Hash = "",
        fileName = "gemma-2b-it-v1-cpu.bin",
        modelType = ModelType.LLM_GEMMA
    )

    val GEMMA_2B_IT = OnDeviceModelConfig(
        id = "gemma-2b-it",
        name = "Gemma 2B IT (GPU INT4)",
        architecture = "Google Gemma 2B GPU INT4 (MediaPipe LLM)",
        displaySize = "1.35 GB",
        sizeBytes = 1_354_301_440L,
        description = "Google Gemma 2B GPU-optimized INT4 model for high-throughput local inference.",
        downloadUrl = "https://huggingface.co/google/gemma-2b-it-tflite/resolve/main/gemma-2b-it-gpu-int4.bin",
        sha256Hash = "",
        fileName = "gemma-2b-it-gpu-int4.bin",
        modelType = ModelType.LLM_GEMMA
    )

    val WHISPER_TINY = OnDeviceModelConfig(
        id = "whisper-tiny",
        name = "Whisper Tiny",
        architecture = "Speech-to-Text Encoder (Open-Access)",
        displaySize = "151 MB",
        sizeBytes = 151_061_672L,
        description = "On-device voice command recognition and lecture note audio parsing.",
        downloadUrl = "https://huggingface.co/openai/whisper-tiny/resolve/main/model.safetensors",
        sha256Hash = "",
        fileName = "whisper-tiny.bin",
        modelType = ModelType.SPEECH_TO_TEXT
    )

    /** All available models, ordered by priority */
    val ALL_MODELS: List<OnDeviceModelConfig> = listOf(
        GEMMA_4_E2B_IT,
        GEMMA_4B_IT,
        GEMMA_2B_IT,
        WHISPER_TINY
    )

    val GEMMA_4_E4B = GEMMA_4B_IT
    val QWEN_05B = GEMMA_4_E2B_IT

    fun getById(id: String): OnDeviceModelConfig? = ALL_MODELS.find { it.id == id }

    /** Minimum free space multiplier (model size * 1.5) to account for temp files during download */
    const val STORAGE_SAFETY_MULTIPLIER = 1.5
}
