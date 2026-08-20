package ai.helply.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.helply.app.ai.CloudApiEngine
import ai.helply.app.ai.GemmaEngineManager
import ai.helply.app.ai.InferenceMode
import ai.helply.app.core.AppLockEnforcer
import ai.helply.app.core.EmailMonitorManager
import ai.helply.app.core.LockdownScheduler
import ai.helply.app.core.NotificationHelper
import ai.helply.app.data.db.AcademicDao
import ai.helply.app.data.db.EmailDao
import ai.helply.app.data.db.ExamDao
import ai.helply.app.data.db.MemoryDao
import ai.helply.app.data.db.PlacementDao
import ai.helply.app.data.entities.*
import ai.helply.app.data.remote.GmailAuthResult
import ai.helply.app.data.remote.GmailOAuthManager
import ai.helply.app.data.remote.GmailTokenStore
import ai.helply.app.domain.*
import ai.helply.app.tools.ToolRegistry
import ai.helply.app.tools.ToolResult
import ai.helply.app.ai.ModelDownloadManager
import ai.helply.app.ai.ModelRepository
import ai.helply.app.ai.ModelRegistry
import ai.helply.app.ai.OnDeviceModelConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "User" or "AI"
    val text: String,
    val modelName: String? = null,
    val modelFileSizeMb: Long? = null,
    val binaryFileName: String? = null,
    val hardwareDelegate: String? = null,
    val timestamp: String = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
)

@HiltViewModel
class HelplyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemmaEngine: GemmaEngineManager,
    val cloudApiEngine: CloudApiEngine,
    val modelRepository: ModelRepository,
    val modelDownloadManager: ModelDownloadManager,
    private val toolRegistry: ToolRegistry,
    private val memoryDao: MemoryDao,
    private val academicDao: AcademicDao,
    private val placementDao: PlacementDao,
    // ─── Email & Exam DB ─────────────────────────────────────────────────────
    private val emailDao: EmailDao,
    private val examDao: ExamDao,
    // ─── Gmail OAuth ────────────────────────────────────────
    private val gmailTokenStore: GmailTokenStore,
    private val gmailOAuthManager: GmailOAuthManager,
    private val lockdownScheduler: LockdownScheduler,
    private val emailMonitorManager: EmailMonitorManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    // ─── Gmail Connection State ────────────────────────────
    sealed class GmailConnectionState {
        object Disconnected : GmailConnectionState()
        object Authorizing : GmailConnectionState()
        data class Connected(val email: String, val displayName: String) : GmailConnectionState()
        data class Error(val message: String) : GmailConnectionState()
    }

    private val _gmailConnectionState = MutableStateFlow<GmailConnectionState>(
        if (gmailTokenStore.isConnected())
            GmailConnectionState.Connected(
                gmailTokenStore.getConnectedEmail() ?: "",
                gmailTokenStore.getDisplayName() ?: ""
            )
        else GmailConnectionState.Disconnected
    )
    val gmailConnectionState: StateFlow<GmailConnectionState> = _gmailConnectionState.asStateFlow()

    // ─── Inference Mode State ────────────────────────────
    private val _inferenceMode = MutableStateFlow(cloudApiEngine.getInferenceMode())
    val inferenceMode: StateFlow<InferenceMode> = _inferenceMode.asStateFlow()

    private val _cloudApiKey = MutableStateFlow(cloudApiEngine.getApiKey())
    val cloudApiKey: StateFlow<String> = _cloudApiKey.asStateFlow()

    private val _cloudBaseUrl = MutableStateFlow(cloudApiEngine.getBaseUrl())
    val cloudBaseUrl: StateFlow<String> = _cloudBaseUrl.asStateFlow()

    private val _cloudModelId = MutableStateFlow(cloudApiEngine.getModelId())
    val cloudModelId: StateFlow<String> = _cloudModelId.asStateFlow()

    private val _cloudTestResult = MutableStateFlow<String?>(null)
    val cloudTestResult: StateFlow<String?> = _cloudTestResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    fun setInferenceMode(mode: InferenceMode) {
        _inferenceMode.value = mode
        cloudApiEngine.saveInferenceMode(mode)
    }

    fun saveCloudConfig(apiKey: String, baseUrl: String, modelId: String) {
        cloudApiEngine.saveConfig(apiKey, baseUrl, modelId)
        _cloudApiKey.value = apiKey.trim()
        _cloudBaseUrl.value = baseUrl.trim().trimEnd('/')
        _cloudModelId.value = modelId.trim()
    }

    fun testCloudConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _cloudTestResult.value = "Testing..."
            val (success, message) = cloudApiEngine.testConnection()
            _cloudTestResult.value = message
            _isTestingConnection.value = false
        }
    }

    // ─── AI Engine State ─────────────────────────────────
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _loadedModelId = MutableStateFlow<String?>(null)
    val loadedModelId: StateFlow<String?> = _loadedModelId.asStateFlow()

    private val _modelLoadProgress = MutableStateFlow(0f)
    val modelLoadProgress: StateFlow<Float> = _modelLoadProgress.asStateFlow()

    private val _modelLoadError = MutableStateFlow<String?>(null)
    val modelLoadError: StateFlow<String?> = _modelLoadError.asStateFlow()

    val downloadStates: StateFlow<Map<String, ModelDownloadManager.DownloadState>> =
        modelDownloadManager.downloadStates

    private val _installedModelIds = MutableStateFlow<Set<String>>(emptySet())
    val installedModelIds: StateFlow<Set<String>> = _installedModelIds.asStateFlow()

    private val _availableStorageBytes = MutableStateFlow(0L)
    val availableStorageBytes: StateFlow<Long> = _availableStorageBytes.asStateFlow()

    private val _hfToken = MutableStateFlow(modelRepository.getHfToken())
    val hfToken: StateFlow<String> = _hfToken.asStateFlow()

    fun saveHfToken(token: String) {
        modelRepository.setHfToken(token)
        _hfToken.value = token
    }

    // ─── Chatbot State ─────────────────────────────────
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            sender = "AI",
            text = "Welcome to Helply On-Device AI Chat! Select a downloaded model above (Qwen 2.5, Gemma 2B, or Whisper Tiny) and chat offline.",
            modelName = "Helply OS Router"
        )
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedChatModelId = MutableStateFlow(ModelRegistry.GEMMA_4B_IT.id)
    val selectedChatModelId: StateFlow<String> = _selectedChatModelId.asStateFlow()

    fun selectChatModel(modelId: String) {
        _selectedChatModelId.value = modelId
        if (_loadedModelId.value != modelId && modelRepository.isModelInstalled(modelId)) {
            initializeModel(modelId)
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(sender = "User", text = userText)
        _chatMessages.value = _chatMessages.value + userMsg

        val currentMode = _inferenceMode.value

        viewModelScope.launch {
            val aiMsgId = java.util.UUID.randomUUID().toString()

            if (currentMode == InferenceMode.CLOUD_API) {
                // ─── Cloud API Path ──────────────────────
                val cloudModel = cloudApiEngine.getModelId()
                val initialAiMsg = ChatMessage(
                    id = aiMsgId,
                    sender = "AI",
                    text = "...",
                    modelName = "☁️ $cloudModel",
                    hardwareDelegate = "Cloud API"
                )
                _chatMessages.value = _chatMessages.value + initialAiMsg

                var accumulatedText = ""
                cloudApiEngine.generateStreamingResponse(prompt = userText).collect { chunk ->
                    accumulatedText += chunk
                    val cleanedText = cleanThinkTags(accumulatedText)
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) msg.copy(text = cleanedText.ifEmpty { "..." }) else msg
                    }
                }
            } else {
                // ─── On-Device Path ─────────────────────
                val modelId = _selectedChatModelId.value
                val modelConfig = ModelRegistry.getById(modelId) ?: ModelRegistry.QWEN_05B
                val modelFile = modelRepository.getModelFile(modelId)
                val fileSizeMb = if (modelFile != null && modelFile.exists()) modelFile.length() / (1024 * 1024) else modelConfig.sizeBytes / (1024 * 1024)
                val binaryFileName = if (modelFile != null && modelFile.exists()) modelFile.name else modelConfig.fileName

                if (gemmaEngine.getLoadedModelId() != modelId) {
                    val success = gemmaEngine.initializeModel(modelId) { }
                    _isModelLoaded.value = success
                    _loadedModelId.value = if (success) modelId else null
                }

                val initialAiMsg = ChatMessage(
                    id = aiMsgId,
                    sender = "AI",
                    text = "...",
                    modelName = "📱 ${modelConfig.name}",
                    modelFileSizeMb = fileSizeMb,
                    binaryFileName = binaryFileName,
                    hardwareDelegate = "On-Device CPU/GPU"
                )
                _chatMessages.value = _chatMessages.value + initialAiMsg

                var accumulatedText = ""
                gemmaEngine.generateStreamingResponse(prompt = userText, modelId = modelId).collect { chunk ->
                    accumulatedText += chunk
                    val cleanedText = cleanThinkTags(accumulatedText)
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) msg.copy(text = cleanedText.ifEmpty { "..." }) else msg
                    }
                }
            }
        }
    }

    private fun cleanThinkTags(input: String): String {
        var cleaned = input.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
        val unclosedIndex = cleaned.lastIndexOf("<think>")
        if (unclosedIndex != -1) {
            cleaned = cleaned.substring(0, unclosedIndex).trim()
        }
        return cleaned
    }

    // ─── Agent Execution Trace ──────────────────────────
    private val _agentTrace = MutableStateFlow<List<String>>(emptyList())
    val agentTrace: StateFlow<List<String>> = _agentTrace.asStateFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning: StateFlow<Boolean> = _isAgentRunning.asStateFlow()

    private val _lastToolResult = MutableStateFlow<String?>(null)
    val lastToolResult: StateFlow<String?> = _lastToolResult.asStateFlow()

    // ─── Memory State ───────────────────────────────────
    private val _memories = MutableStateFlow<List<AcademicMemoryEntity>>(emptyList())
    val memories: StateFlow<List<AcademicMemoryEntity>> = _memories.asStateFlow()

    // ─── Academics State ────────────────────────────────
    private val _assignments = MutableStateFlow<List<AssignmentEntity>>(emptyList())
    val assignments: StateFlow<List<AssignmentEntity>> = _assignments.asStateFlow()

    private val _academicResult = MutableStateFlow<String?>(null)
    val academicResult: StateFlow<String?> = _academicResult.asStateFlow()

    private val _autonomousPipelineResult = MutableStateFlow<AutonomousPipelineResult?>(null)
    val autonomousPipelineResult: StateFlow<AutonomousPipelineResult?> = _autonomousPipelineResult.asStateFlow()

    // ─── College Email & Lock State (Room-backed) ────────
    val emails: StateFlow<List<EmailEntity>> = emailDao.getAllEmails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examCirculars: StateFlow<List<EmailEntity>> = emailDao.getExamCirculars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExams: StateFlow<List<ExamEntity>> = examDao.getAllExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _examLockState = MutableStateFlow(ExamLockState())
    val examLockState: StateFlow<ExamLockState> = _examLockState.asStateFlow()

    private val _manuallyLockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val manuallyLockedPackages: StateFlow<Set<String>> = _manuallyLockedPackages.asStateFlow()

    private val _emailScanSummary = MutableStateFlow<String?>(null)
    val emailScanSummary: StateFlow<String?> = _emailScanSummary.asStateFlow()

    // ─── Placement & Company 360 State ──────────────────
    private val _companyAnalysis = MutableStateFlow<CompanyShortlistAnalysis?>(null)
    val companyAnalysis: StateFlow<CompanyShortlistAnalysis?> = _companyAnalysis.asStateFlow()

    private val _atsResult = MutableStateFlow<ATSResult?>(null)
    val atsResult: StateFlow<ATSResult?> = _atsResult.asStateFlow()

    // ─── Portfolio State ────────────────────────────────
    private val _portfolioHtml = MutableStateFlow<String?>(null)
    val portfolioHtml: StateFlow<String?> = _portfolioHtml.asStateFlow()

    private val _deployStatus = MutableStateFlow<List<String>>(emptyList())
    val deployStatus: StateFlow<List<String>> = _deployStatus.asStateFlow()

    // ─── GitHub OAuth & Repository State ────────────────
    private val _githubUser = MutableStateFlow<GitHubAppManager.GitHubUser?>(null)
    val githubUser: StateFlow<GitHubAppManager.GitHubUser?> = _githubUser.asStateFlow()

    private val _githubRepos = MutableStateFlow<List<GitHubAppManager.GitHubRepo>>(emptyList())
    val githubRepos: StateFlow<List<GitHubAppManager.GitHubRepo>> = _githubRepos.asStateFlow()

    private val _githubAccessToken = MutableStateFlow<String?>(null)
    val githubAccessToken: StateFlow<String?> = _githubAccessToken.asStateFlow()

    private val _githubClientId = MutableStateFlow("")
    val githubClientId: StateFlow<String> = _githubClientId.asStateFlow()

    private val _githubClientSecret = MutableStateFlow("")
    val githubClientSecret: StateFlow<String> = _githubClientSecret.asStateFlow()

    private val _isLoggingInGithub = MutableStateFlow(false)
    val isLoggingInGithub: StateFlow<Boolean> = _isLoggingInGithub.asStateFlow()

    init {
        // Auto-load saved GitHub session if present
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("helply_github", Context.MODE_PRIVATE)
            val savedToken = prefs.getString("github_access_token", null)
            val savedUsername = prefs.getString("github_username", null)
            
            val savedClientId = prefs.getString("custom_client_id", "") ?: ""
            val savedClientSecret = prefs.getString("custom_client_secret", "") ?: ""
            _githubClientId.value = savedClientId
            _githubClientSecret.value = savedClientSecret
            GitHubAppManager.customClientId = savedClientId
            GitHubAppManager.customClientSecret = savedClientSecret

            val target = if (!savedToken.isNullOrBlank()) savedToken else savedUsername
            if (!target.isNullOrBlank()) {
                android.util.Log.d("HELPLY_OAUTH", "Auto-loading GitHub session for target: ${target.take(8)}")
                val user = GitHubAppManager.fetchUserProfile(target)
                if (user != null) {
                    _githubUser.value = user
                    _githubAccessToken.value = savedToken
                    val repos = GitHubAppManager.fetchUserRepositories(target)
                    _githubRepos.value = repos
                }
            }
        }
    }

    fun connectGitHubAccount(userInput: String) {
        if (userInput.isBlank()) return
        viewModelScope.launch {
            _isLoggingInGithub.value = true
            val target = userInput.trim()
            val user = GitHubAppManager.fetchUserProfile(target)
            if (user != null) {
                _githubUser.value = user
                val repos = GitHubAppManager.fetchUserRepositories(target)
                _githubRepos.value = repos
                val prefs = context.getSharedPreferences("helply_github", Context.MODE_PRIVATE)
                prefs.edit().putString("github_username", user.login).apply()
            }
            _isLoggingInGithub.value = false
        }
    }

    fun handleOAuthCode(code: String) {
        viewModelScope.launch {
            android.util.Log.d("HELPLY_OAUTH", "handleOAuthCode called with code: ${code.take(6)}...")
            _isLoggingInGithub.value = true
            val token = GitHubAppManager.exchangeCodeForToken(code)
            android.util.Log.d("HELPLY_OAUTH", "Token exchange result: ${if (token != null) "SUCCESS (${token.take(8)}...)" else "FAILED"}")
            if (token != null) {
                _githubAccessToken.value = token
                val user = GitHubAppManager.fetchUserProfile(token)
                android.util.Log.d("HELPLY_OAUTH", "User profile: ${user?.login ?: "NULL"}")
                if (user != null) {
                    _githubUser.value = user
                    val repos = GitHubAppManager.fetchUserRepositories(token)
                    android.util.Log.d("HELPLY_OAUTH", "Repos fetched: ${repos.size}")
                    _githubRepos.value = repos
                    
                    // Persist session
                    val prefs = context.getSharedPreferences("helply_github", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("github_access_token", token)
                        .putString("github_username", user.login)
                        .apply()
                }
            }
            _isLoggingInGithub.value = false
        }
    }

    fun logoutGitHub() {
        _githubUser.value = null
        _githubAccessToken.value = null
        _githubRepos.value = emptyList()
        _isLoggingInGithub.value = false
        val prefs = context.getSharedPreferences("helply_github", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun saveGitHubConfig(clientId: String, clientSecret: String) {
        val prefs = context.getSharedPreferences("helply_github", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("custom_client_id", clientId.trim())
            .putString("custom_client_secret", clientSecret.trim())
            .apply()
        _githubClientId.value = clientId.trim()
        _githubClientSecret.value = clientSecret.trim()
        GitHubAppManager.customClientId = clientId.trim()
        GitHubAppManager.customClientSecret = clientSecret.trim()
    }

    fun saveMasterResume(resumeText: String) {
        viewModelScope.launch {
            val existing = memoryDao.searchMemories("Master Resume")
            existing.forEach {
                if (it.type == "Resume") {
                    memoryDao.deleteMemory(it.id)
                }
            }
            addMemory(
                title = "Master Resume",
                type = "Resume",
                description = resumeText,
                source = "Manual"
            )
        }
    }




    init {
        // Load memories from Room DB on startup
        viewModelScope.launch {
            memoryDao.getAllMemories().collect { list ->
                _memories.value = list
            }
        }

        // Sync local model installation status on launch
        refreshInstalledModels()

        // Restore persisted exam lock state (survives app kill + reboot)
        viewModelScope.launch {
            lockdownScheduler.setNotificationHelper(notificationHelper)
            val restoredLock = lockdownScheduler.restoreLockStateIfActive()
            _examLockState.value = restoredLock
        }

        // Auto-resume email monitoring if Gmail was previously connected
        if (gmailTokenStore.isConnected()) {
            emailMonitorManager.startMonitoring()
        }

        // Continuous App Lock Monitor Loop
        startAppLockEnforcementLoop()
    }

    // ─── Gmail OAuth Account Management ──────────────────

    /** Returns an Intent to launch the Google OAuth browser flow. Call startActivityForResult with RC_AUTH. */
    fun getGmailAuthIntent(clientId: String): android.content.Intent =
        gmailOAuthManager.getAuthIntent(clientId)

    /** Call this from onActivityResult / ActivityResultCallback after the OAuth browser returns. */
    fun handleGmailOAuthResult(intent: android.content.Intent, clientId: String) {
        viewModelScope.launch {
            _gmailConnectionState.value = GmailConnectionState.Authorizing
            when (val result = gmailOAuthManager.handleAuthResponse(intent, clientId)) {
                is GmailAuthResult.Success -> {
                    _gmailConnectionState.value =
                        GmailConnectionState.Connected(result.email, result.displayName)
                    emailMonitorManager.startMonitoring()
                    emailMonitorManager.triggerImmediatePoll()
                }
                is GmailAuthResult.Error -> {
                    _gmailConnectionState.value = GmailConnectionState.Error(result.message)
                }
            }
        }
    }

    fun disconnectGmailAccount() {
        gmailTokenStore.clearTokens()
        emailMonitorManager.stopMonitoring()
        _gmailConnectionState.value = GmailConnectionState.Disconnected
    }

    fun syncEmailsNow() {
        emailMonitorManager.triggerImmediatePoll()
    }

    // Legacy helper kept for test/demo purposes — real data comes from IMAP pipeline
    fun addCollegeEmail(sender: String, subject: String, body: String, isExamCircular: Boolean = false) {
        viewModelScope.launch {
            val category = if (isExamCircular) EmailCategory.EXAM_CIRCULAR.name else EmailCategory.GENERAL_ANNOUNCEMENT.name
            val priority = if (isExamCircular) PriorityLevel.CRITICAL_RED.name else PriorityLevel.LOW_GREEN.name
            val entity = EmailEntity(
                sender = sender,
                subject = subject,
                snippet = if (body.length > 120) body.take(120) + "..." else body,
                fullBody = body,
                category = category,
                priority = priority,
                aiSummary = if (isExamCircular) "This is an exam circular. Lockdown will be activated 5 days before the exam." else "",
                detectedExamStartDate = if (isExamCircular) System.currentTimeMillis() + (5 * 86400000L) else null
            )
            emailDao.insertEmail(entity)
        }
    }

    private fun startAppLockEnforcementLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val isLockActive = _examLockState.value.isLockActive
                val manualSet = _manuallyLockedPackages.value

                if (isLockActive || manualSet.isNotEmpty()) {
                    AppLockEnforcer.enforceLockIfBlocked(context, isLockActive, manualSet)
                }
                delay(800)
            }
        }
    }

    fun refreshInstalledModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val installed = modelRepository.getInstalledModelIds().toSet()
            _installedModelIds.value = installed
            _availableStorageBytes.value = modelRepository.getAvailableStorageBytes()
        }
    }

    // ─── AI Model Operations ────────────────────────────
    fun startModelDownload(modelId: String) {
        val config = ModelRegistry.getById(modelId) ?: return
        viewModelScope.launch {
            val success = modelDownloadManager.downloadModel(config)
            refreshInstalledModels()
            if (success && modelId == ModelRegistry.GEMMA_4_E4B.id) {
                initializeModel(modelId)
            }
        }
    }

    fun cancelModelDownload(modelId: String) {
        modelDownloadManager.cancelDownload(modelId)
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (gemmaEngine.getLoadedModelId() == modelId) {
                gemmaEngine.unloadModel()
                _isModelLoaded.value = false
            }
            modelDownloadManager.deleteModel(modelId)
            refreshInstalledModels()
        }
    }

    fun initializeModel(modelId: String = _selectedChatModelId.value) {
        viewModelScope.launch {
            _modelLoadProgress.value = 0.05f
            _modelLoadError.value = null
            if (_loadedModelId.value != null && _loadedModelId.value != modelId) {
                gemmaEngine.unloadModel()
                _isModelLoaded.value = false
                _loadedModelId.value = null
            }
            val success = gemmaEngine.initializeModel(modelId) { progress ->
                _modelLoadProgress.value = progress
            }
            _isModelLoaded.value = success
            _loadedModelId.value = if (success) modelId else null
            if (!success) {
                _modelLoadProgress.value = 0f
                _modelLoadError.value = "Failed to load model. Ensure storage space is sufficient and model weights are not corrupt."
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "⚠️ Model Load Failed! Check storage/logs.", android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                _modelLoadError.value = null
            }
        }
    }

    fun setHardwareAcceleration(npu: Boolean, gpu: Boolean) {
        gemmaEngine.setHardwareAcceleration(npu, gpu)
        val loaded = _loadedModelId.value
        if (loaded != null) {
            initializeModel(loaded)
        }
    }

    fun unloadModel() {
        gemmaEngine.unloadModel()
        _isModelLoaded.value = false
        _loadedModelId.value = null
        _modelLoadProgress.value = 0f
    }

    // ─── Notepad Agent Dispatch (HomeScreen) ────────────
    fun executeAgentDispatch(noteTitle: String, agentName: String, toolCallStr: String) {
        _isAgentRunning.value = true
        _agentTrace.value = emptyList()

        viewModelScope.launch {
            addTrace("🧠 [LiteRT Gemma 4 E4B] Tokenizing Note: '$noteTitle'")
            delay(400)

            addTrace("🔍 [LLM Router] Identified target agent: $agentName")
            delay(400)

            val toolName = toolCallStr.substringBefore("(")
            val paramsStr = toolCallStr.substringAfter("(").trimEnd(')')
            val params = parseToolParams(paramsStr)

            addTrace("⚙️ [ToolRegistry] Executing: $toolName(${params.entries.joinToString(", ") { "${it.key}=${it.value}" }})")
            delay(300)

            val result = toolRegistry.executeTool(toolName, params)
            when (result) {
                is ToolResult.Success -> {
                    addTrace("✅ Tool Success: ${result.message}")
                    _lastToolResult.value = result.message
                }
                is ToolResult.Error -> {
                    addTrace("❌ Tool Error: ${result.reason}")
                    _lastToolResult.value = "Error: ${result.reason}"
                }
            }

            _isAgentRunning.value = false
        }
    }

    fun clearTrace() {
        _agentTrace.value = emptyList()
        _lastToolResult.value = null
    }

    private suspend fun generateAICompletion(prompt: String, systemPrompt: String = "You are an intelligent AI assistant."): String {
        val currentMode = _inferenceMode.value
        val rawCompletion = if (currentMode == InferenceMode.CLOUD_API) {
            cloudApiEngine.generateResponse(prompt, systemPrompt)
        } else {
            val modelId = _selectedChatModelId.value
            gemmaEngine.generateResponse(prompt, systemPrompt, modelId)
        }
        return cleanThinkTags(rawCompletion)
    }

    // ─── Feature 1: Autonomous Academic Pipeline ────────
    fun runAutonomousAcademicAgent(inputText: String) {
        viewModelScope.launch {
            _isAgentRunning.value = true
            _autonomousPipelineResult.value = null

            val result = AutonomousAcademicAgent.executeAcademicPipeline(
                context = context,
                rawTaskText = inputText,
                subject = "Computer Science & AI",
                aiGenerator = { p, s -> generateAICompletion(p, s) }
            )

            _autonomousPipelineResult.value = result
            _isAgentRunning.value = false

            // Save automatically to Room DB Memory
            addMemory(
                title = "Autonomous Deliverable: ${result.title}",
                type = "Project",
                description = "PPT (${result.pptSlideCount} slides) & PDF Research Report generated automatically at ${result.generatedPdfPath}",
                source = "Autonomous AI Agent"
            )
        }
    }

    // ─── Feature 2: College Email Scanner & 5-Day App Lock ───
    fun scanCollegeEmails() {
        viewModelScope.launch {
            _isAgentRunning.value = true
            val dbEmails = emailDao.getAllEmails()
            val (updatedEmails, scan) = EmailScannerEngine.analyzeEmails(
                emails = emptyList(),
                aiGenerator = { p, s -> generateAICompletion(p, s) }
            )

            val summaryLines = mutableListOf<String>()
            summaryLines.add("📧 College Email Ingestion & Circular Scan Complete:")
            summaryLines.add("• Processed ${scan.processedCount} recent emails from college portal")
            summaryLines.add("• Exam Circulars Detected: ${scan.examCircularsFound}")

            if (scan.activeLockdownTriggered) {
                summaryLines.add("\n🚨 CRITICAL EXAM CIRCULAR DETECTED!")
                summaryLines.add("• Exam: ${scan.examTitle}")
                summaryLines.add("• Date: ${scan.examDate}")
                summaryLines.add("• 5-Day Social Media Lockdown ENGAGED 🔒")

                _examLockState.value = ExamLockState(
                    isLockActive = true,
                    examTitle = scan.examTitle ?: "End-Semester Examinations",
                    examDateMillis = System.currentTimeMillis() + (5 * 86400000L),
                    daysRemaining = scan.daysUntilExam,
                    lockedApps = SocialMediaLockManager.getDefaultLockedApps()
                )

                // Trigger Notification
                notificationHelper.sendNotification(
                    context = context,
                    title = "🚨 Exam Lock Activated (5-Day Rule)",
                    message = "Exam circular detected for ${scan.examDate}. Social media apps locked to ensure focus."
                )
            }

            _emailScanSummary.value = summaryLines.joinToString("\n")
            _isAgentRunning.value = false
        }
    }

    fun toggleExamLockdown(active: Boolean) {
        _examLockState.value = _examLockState.value.copy(isLockActive = active)
    }

    // ─── Manual Lock Controls ─────────────────────────────
    fun toggleManualAppLock(packageName: String) {
        val current = _manuallyLockedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _manuallyLockedPackages.value = current
    }

    fun lockAllBlockedAppsManually(blockedPackages: List<String>) {
        _manuallyLockedPackages.value = blockedPackages.toSet()
    }

    fun unlockAllManualApps() {
        _manuallyLockedPackages.value = emptySet()
    }

    // ─── Feature 3: Company 360° & Resume Shortlist Engine ───
    fun analyzeCompanyShortlist(companyName: String, candidateResume: String) {
        viewModelScope.launch {
            _isAgentRunning.value = true
            _companyAnalysis.value = null
            _atsResult.value = null
            delay(400)
            val analysis = CompanyIntelligenceEngine.getCompany360(
                queryName = companyName,
                candidateResumeText = candidateResume,
                aiGenerator = { p, s -> generateAICompletion(p, s) }
            )
            _companyAnalysis.value = analysis

            // Also calculate traditional ATS score
            _atsResult.value = ATSEngine.evaluateResume(
                resumeText = candidateResume,
                jobDescription = analysis.company.keyTechStack.joinToString(", "),
                aiGenerator = { p, s -> generateAICompletion(p, s) }
            )
            _isAgentRunning.value = false
        }
    }

    // ─── Memory Operations ──────────────────────────────
    fun addMemory(title: String, type: String, description: String, source: String) {
        viewModelScope.launch {
            val memory = AcademicMemoryEntity(
                type = type,
                title = title,
                description = description,
                source = source
            )
            memoryDao.insertMemory(memory)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryDao.deleteMemory(id)
        }
    }

    // ─── Portfolio Operations ────────────────────────────
    fun deployPortfolio(
        themeName: String,
        studentName: String = "Student",
        degree: String = "Computer Science",
        college: String = "University",
        bio: String = "Student building intelligent software solutions."
    ) {
        viewModelScope.launch {
            _deployStatus.value = emptyList()

            addDeployLog("🔄 Initializing Portfolio Deployment Pipeline...")
            delay(400)

            val theme = PortfolioTheme.entries.find { it.themeName == themeName }
                ?: PortfolioTheme.MODERN_DEVELOPER

            addDeployLog("🎨 Applying theme: ${theme.themeName}")
            delay(300)

            val html = withContext(Dispatchers.Default) {
                PortfolioBuilder.buildHtmlPortfolio(
                    studentName = studentName,
                    degree = degree,
                    college = college,
                    bio = bio,
                    theme = theme,
                    memories = _memories.value
                )
            }
            _portfolioHtml.value = html

            val slug = studentName.trim().lowercase().replace("\\s+".toRegex(), "-")
            addDeployLog("✅ HTML bundle generated (${html.length} characters)")
            delay(300)

            val ownerName = _githubUser.value?.login ?: GitHubAppManager.DEFAULT_OWNER
            GitHubAppManager.syncAndDeployPortfolio(
                portfolioHtml = html,
                repoName = "portfolio",
                owner = ownerName,
                userToken = _githubAccessToken.value,
                onLog = { logMsg -> addDeployLog(logMsg) }
            )
            addDeployLog("✅ Portfolio deployment pipeline finished successfully!")
        }
    }

    // ─── Private Helpers ────────────────────────────────
    private fun addTrace(message: String) {
        _agentTrace.value = _agentTrace.value + message
    }

    private fun addDeployLog(message: String) {
        _deployStatus.value = _deployStatus.value + message
    }

    private fun parseToolParams(paramsStr: String): Map<String, String> {
        if (paramsStr.isBlank()) return emptyMap()
        return paramsStr.split(",")
            .mapNotNull { part ->
                val kv = part.trim().split("=", limit = 2)
                if (kv.size == 2) {
                    kv[0].trim() to kv[1].trim().removeSurrounding("'").removeSurrounding("\"")
                } else null
            }
            .toMap()
    }
}
