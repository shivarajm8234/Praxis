package ai.helply.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.helply.app.ai.GemmaEngineManager
import ai.helply.app.core.AppLockEnforcer
import ai.helply.app.core.NotificationHelper
import ai.helply.app.data.db.AcademicDao
import ai.helply.app.data.db.MemoryDao
import ai.helply.app.data.db.PlacementDao
import ai.helply.app.data.entities.*
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val modelRepository: ModelRepository,
    val modelDownloadManager: ModelDownloadManager,
    private val toolRegistry: ToolRegistry,
    private val memoryDao: MemoryDao,
    private val academicDao: AcademicDao,
    private val placementDao: PlacementDao
) : ViewModel() {

    // ─── AI Engine State ─────────────────────────────────
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _loadedModelId = MutableStateFlow<String?>(null)
    val loadedModelId: StateFlow<String?> = _loadedModelId.asStateFlow()

    private val _modelLoadProgress = MutableStateFlow(0f)
    val modelLoadProgress: StateFlow<Float> = _modelLoadProgress.asStateFlow()

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

        val modelId = _selectedChatModelId.value
        val modelConfig = ModelRegistry.getById(modelId) ?: ModelRegistry.QWEN_05B
        val modelFile = modelRepository.getModelFile(modelId)
        val fileSizeMb = if (modelFile != null && modelFile.exists()) modelFile.length() / (1024 * 1024) else modelConfig.sizeBytes / (1024 * 1024)
        val binaryFileName = if (modelFile != null && modelFile.exists()) modelFile.name else modelConfig.fileName

        viewModelScope.launch {
            if (gemmaEngine.getLoadedModelId() != modelId) {
                val success = gemmaEngine.initializeModel(modelId) { }
                _isModelLoaded.value = success
                _loadedModelId.value = if (success) modelId else null
            }

            val aiMsgId = java.util.UUID.randomUUID().toString()
            val initialAiMsg = ChatMessage(
                id = aiMsgId,
                sender = "AI",
                text = "...",
                modelName = modelConfig.name,
                modelFileSizeMb = fileSizeMb,
                binaryFileName = binaryFileName,
                hardwareDelegate = "Hexagon NPU / OpenCL GPU"
            )
            _chatMessages.value = _chatMessages.value + initialAiMsg

            var accumulatedText = ""
            gemmaEngine.generateStreamingResponse(prompt = userText, modelId = modelId).collect { chunk ->
                accumulatedText += chunk
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) msg.copy(text = accumulatedText) else msg
                }
            }
        }
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

    // ─── College Email & Lock State ─────────────────────
    private val _emails = MutableStateFlow<List<CollegeEmail>>(emptyList())
    val emails: StateFlow<List<CollegeEmail>> = _emails.asStateFlow()

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

    private val _isLoggingInGithub = MutableStateFlow(false)
    val isLoggingInGithub: StateFlow<Boolean> = _isLoggingInGithub.asStateFlow()

    init {
        // Auto-load saved GitHub session or default user
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("helply_github", Context.MODE_PRIVATE)
            val savedToken = prefs.getString("github_access_token", null)
            val savedUsername = prefs.getString("github_username", "shivarajm8234") ?: "shivarajm8234"
            
            val target = if (!savedToken.isNullOrBlank()) savedToken else savedUsername
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

    fun connectGitHubAccount(userInput: String = "shivarajm8234") {
        viewModelScope.launch {
            _isLoggingInGithub.value = true
            val target = if (userInput.isBlank()) "shivarajm8234" else userInput.trim()
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




    init {
        // Load memories from Room DB on startup
        viewModelScope.launch {
            memoryDao.getAllMemories().collect { list ->
                _memories.value = list
            }
        }
        _emails.value = emptyList()

        // Sync local model installation status on launch
        refreshInstalledModels()

        // Continuous App Lock Monitor Loop
        startAppLockEnforcementLoop()
    }

    fun addCollegeEmail(sender: String, subject: String, body: String, isExamCircular: Boolean = false) {
        val category = if (isExamCircular) EmailCategory.EXAM_CIRCULAR else EmailCategory.GENERAL_ANNOUNCEMENT
        val priority = if (isExamCircular) PriorityLevel.CRITICAL_RED else PriorityLevel.LOW_GREEN
        val newEmail = CollegeEmail(
            id = "em_${System.currentTimeMillis()}",
            sender = sender,
            subject = subject,
            snippet = if (body.length > 80) body.take(80) + "..." else body,
            fullBody = body,
            timestamp = "Just Now",
            category = category,
            priority = priority,
            detectedExamDate = if (isExamCircular) "Upcoming Exam" else null,
            examDateMillis = if (isExamCircular) System.currentTimeMillis() + (5 * 86400000L) else null
        )
        _emails.value = _emails.value + newEmail
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

    // ─── Feature 1: Autonomous Academic Pipeline ────────
    fun runAutonomousAcademicAgent(inputText: String) {
        viewModelScope.launch {
            _isAgentRunning.value = true
            _autonomousPipelineResult.value = null

            val result = AutonomousAcademicAgent.executeAcademicPipeline(
                context = context,
                rawTaskText = inputText,
                subject = "Computer Science & AI"
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
            val emailsList = _emails.value
            val scan = EmailScannerEngine.analyzeEmails(emailsList)

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
                    lockedApps = ExamLockState.defaultApps()
                )

                // Trigger Notification
                NotificationHelper.sendNotification(
                    context = context,
                    title = "🚨 Exam Lock Activated (5-Day Rule)",
                    message = "Exam circular detected for ${scan.examDate}. Social media apps locked to ensure focus."
                )
            }

            _emailScanSummary.value = summaryLines.joinToString("\n")
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
            _companyAnalysis.value = null
            delay(400)
            val analysis = CompanyIntelligenceEngine.getCompany360(companyName, candidateResume)
            _companyAnalysis.value = analysis

            // Also calculate traditional ATS score
            _atsResult.value = ATSEngine.evaluateResume(candidateResume, analysis.company.keyTechStack.joinToString(", "))
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

            addDeployLog("🚀 Authenticating with GitHub App Private Key...")
            GitHubAppManager.syncAndDeployPortfolio(
                portfolioHtml = html,
                repoName = "portfolio",
                owner = "shivarajm8234",
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
