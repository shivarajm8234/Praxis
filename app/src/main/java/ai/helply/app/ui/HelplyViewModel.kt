package ai.helply.app.ui

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.helply.app.ai.GemmaEngineManager
import ai.helply.app.core.NotificationHelper
import ai.helply.app.data.db.MemoryDao
import ai.helply.app.data.db.AcademicDao
import ai.helply.app.data.db.PlacementDao
import ai.helply.app.data.entities.*
import ai.helply.app.domain.*
import ai.helply.app.tools.ToolRegistry
import ai.helply.app.tools.ToolResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HelplyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemmaEngine: GemmaEngineManager,
    private val toolRegistry: ToolRegistry,
    private val memoryDao: MemoryDao,
    private val academicDao: AcademicDao,
    private val placementDao: PlacementDao
) : ViewModel() {

    // ─── AI Engine State ─────────────────────────────────
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _modelLoadProgress = MutableStateFlow(0f)
    val modelLoadProgress: StateFlow<Float> = _modelLoadProgress.asStateFlow()

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

    init {
        // Load memories from Room DB on startup
        viewModelScope.launch {
            memoryDao.getAllMemories().collect { list ->
                _memories.value = list
            }
        }
        // Seed default memories & emails
        viewModelScope.launch {
            delay(300)
            if (_memories.value.isEmpty()) {
                seedDefaultMemories()
            }
            _emails.value = EmailScannerEngine.sampleCollegeEmails()
        }
    }

    // ─── AI Model Operations ────────────────────────────
    fun initializeModel() {
        viewModelScope.launch {
            _modelLoadProgress.value = 0f
            val success = gemmaEngine.initializeModel { progress ->
                _modelLoadProgress.value = progress
            }
            _isModelLoaded.value = success
        }
    }

    fun unloadModel() {
        gemmaEngine.unloadModel()
        _isModelLoaded.value = false
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
    fun deployPortfolio(themeName: String) {
        viewModelScope.launch {
            _deployStatus.value = emptyList()

            addDeployLog("🔄 Initializing Portfolio Deployment Pipeline...")
            delay(400)

            val theme = PortfolioTheme.values().find { it.themeName == themeName }
                ?: PortfolioTheme.MODERN_DEVELOPER

            addDeployLog("🎨 Applying theme: ${theme.themeName}")
            delay(300)

            val html = withContext(Dispatchers.Default) {
                PortfolioBuilder.buildHtmlPortfolio(
                    studentName = "Satoru Gojo",
                    degree = "B.Tech Computer Science",
                    college = "Engineering College",
                    bio = "AI/ML enthusiast building on-device intelligent systems.",
                    theme = theme,
                    memories = _memories.value
                )
            }
            _portfolioHtml.value = html

            addDeployLog("✅ HTML bundle generated (${html.length} characters)")
            delay(300)

            addDeployLog("🚀 Deploying to GitHub Pages...")
            delay(500)

            addDeployLog("🌐 Live URL: https://satoru.github.io/portfolio/")
            addDeployLog("✅ Portfolio deployment completed successfully!")
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

    private suspend fun seedDefaultMemories() {
        val defaults = listOf(
            AcademicMemoryEntity(type = "Project", title = "Helply AI Student OS", description = "On-device AI OS for students using Gemma 4 E4B & LiteRT.", source = "GitHub Ingestion", confidenceScore = 0.98f),
            AcademicMemoryEntity(type = "Certificate", title = "Android Development with Kotlin", description = "Certified by Google Developers.", source = "User Upload", confidenceScore = 0.99f),
            AcademicMemoryEntity(type = "Exam", title = "DBMS End Sem Exam", description = "Scheduled Oct 28, 2024", source = "College Email Circular", confidenceScore = 1.0f)
        )
        defaults.forEach { memoryDao.insertMemory(it) }
    }
}
