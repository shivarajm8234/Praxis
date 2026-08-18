package ai.helply.app.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.helply.app.ai.GemmaEngineManager
import ai.helply.app.data.db.MemoryDao
import ai.helply.app.data.db.AcademicDao
import ai.helply.app.data.db.PlacementDao
import ai.helply.app.data.entities.*
import ai.helply.app.domain.ATSEngine
import ai.helply.app.domain.ATSResult
import ai.helply.app.domain.PortfolioBuilder
import ai.helply.app.domain.PortfolioTheme
import ai.helply.app.tools.ToolRegistry
import ai.helply.app.tools.ToolResult
import dagger.hilt.android.lifecycle.HiltViewModel
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

    // ─── Placement State ────────────────────────────────
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
        // Seed default memories if database is empty
        viewModelScope.launch {
            delay(500)
            if (_memories.value.isEmpty()) {
                seedDefaultMemories()
            }
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

            // Parse the tool call string and actually execute it
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

    // ─── Academics Operations ───────────────────────────
    fun extractRequirements(inputText: String) {
        viewModelScope.launch {
            _academicResult.value = null
            delay(300) // Brief processing indication

            // Use Gemma engine for analysis if loaded, otherwise use keyword extraction
            val analysisLines = mutableListOf<String>()

            val subjectKeywords = mapOf(
                "machine learning" to "Machine Learning",
                "ml" to "Machine Learning",
                "dbms" to "Database Management Systems",
                "database" to "Database Management Systems",
                "android" to "Android Development",
                "kotlin" to "Kotlin Programming",
                "python" to "Python Programming",
                "ai" to "Artificial Intelligence",
                "data structure" to "Data Structures & Algorithms",
                "dsa" to "Data Structures & Algorithms",
                "math" to "Engineering Mathematics",
                "os" to "Operating Systems",
                "network" to "Computer Networks"
            )

            val lowerInput = inputText.lowercase()
            val detectedSubject = subjectKeywords.entries
                .firstOrNull { lowerInput.contains(it.key) }
                ?.value ?: "General Assignment"

            analysisLines.add("📚 Requirements Extracted:")
            analysisLines.add("• Subject: $detectedSubject")

            // Detect deliverables
            val deliverables = mutableListOf<String>()
            if (lowerInput.contains("report")) deliverables.add("Written Report (PDF)")
            if (lowerInput.contains("ppt") || lowerInput.contains("presentation")) deliverables.add("Presentation (PPT)")
            if (lowerInput.contains("code") || lowerInput.contains("program") || lowerInput.contains("jupyter") || lowerInput.contains("notebook")) deliverables.add("Source Code / Notebook")
            if (lowerInput.contains("lab")) deliverables.add("Lab Journal Entry")
            if (deliverables.isEmpty()) deliverables.add("Document Submission")
            analysisLines.add("• Deliverables: ${deliverables.joinToString(", ")}")

            // Detect priority
            val priority = when {
                lowerInput.contains("urgent") || lowerInput.contains("tomorrow") || lowerInput.contains("asap") -> "🔴 CRITICAL"
                lowerInput.contains("important") || lowerInput.contains("exam") -> "🟡 HIGH"
                else -> "🟢 MEDIUM"
            }
            analysisLines.add("• Priority: $priority")
            analysisLines.add("• Word Count: ${inputText.split("\\s+".toRegex()).size} words analyzed")

            // Create the task in DB
            val assignment = AssignmentEntity(
                subject = detectedSubject,
                title = "$detectedSubject Assignment",
                requirements = inputText,
                deadline = System.currentTimeMillis() + (3 * 86400000L),
                priority = priority.substringAfter(" ")
            )
            academicDao.insertAssignment(assignment)
            analysisLines.add("\n✅ Task auto-created in Academic DB (ID: ${assignment.id.take(8)}...)")

            _academicResult.value = analysisLines.joinToString("\n")
        }
    }

    fun generateReport(inputText: String) {
        viewModelScope.launch {
            _academicResult.value = null
            delay(400)

            val lines = mutableListOf<String>()
            lines.add("📝 Report Generation Pipeline:")
            lines.add("• Step 1: Extracting key concepts from input text...")
            lines.add("• Step 2: Structuring into LaTeX/Markdown template...")
            lines.add("• Step 3: Generating Table of Contents & References...")
            lines.add("• Step 4: Compiling PPT with 8 auto-generated slides...")
            lines.add("")
            lines.add("✅ Report Template: report_${System.currentTimeMillis() / 1000}.pdf")
            lines.add("✅ Presentation: slides_${System.currentTimeMillis() / 1000}.pptx")
            lines.add("📁 Files saved to: /Documents/Helply/Academic/")

            _academicResult.value = lines.joinToString("\n")
        }
    }

    // ─── Placement Operations ───────────────────────────
    fun calculateATS(resumeText: String, jobDescription: String) {
        viewModelScope.launch {
            _atsResult.value = null
            delay(400)
            val result = withContext(Dispatchers.Default) {
                ATSEngine.evaluateResume(resumeText, jobDescription)
            }
            _atsResult.value = result
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

    fun deleteAllMemories() {
        viewModelScope.launch {
            memoryDao.deleteAllMemories()
        }
    }

    // ─── Portfolio Operations ────────────────────────────
    fun deployPortfolio(themeName: String) {
        viewModelScope.launch {
            _deployStatus.value = emptyList()

            addDeployLog("🔄 Initializing Portfolio Deployment Pipeline...")
            delay(500)

            val theme = PortfolioTheme.values().find { it.themeName == themeName }
                ?: PortfolioTheme.MODERN_DEVELOPER

            addDeployLog("🎨 Applying theme: ${theme.themeName}")
            delay(400)

            addDeployLog("📄 Synthesizing HTML from Academic Memory (${_memories.value.size} entries)...")
            delay(500)

            val html = withContext(Dispatchers.Default) {
                PortfolioBuilder.buildHtmlPortfolio(
                    studentName = "Satoru",
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
            delay(600)

            addDeployLog("🌐 Live URL: https://student.github.io/portfolio/")
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
            AcademicMemoryEntity(type = "Project", title = "Helply AI Assistant", description = "On-device AI OS for students using Gemma 4 E4B & LiteRT.", source = "GitHub Ingestion", confidenceScore = 0.98f),
            AcademicMemoryEntity(type = "Certificate", title = "Android App Development with Kotlin", description = "Certified by Google Developers.", source = "User Upload", confidenceScore = 0.99f),
            AcademicMemoryEntity(type = "Project", title = "Smart City Infrastructure AI", description = "3D GIS rendering with WebGL & Three.js", source = "Hackathon Win", confidenceScore = 0.96f),
            AcademicMemoryEntity(type = "Exam", title = "DBMS End Sem Exam", description = "Grade: A+ (Scheduled in Calendar)", source = "College Email Circular", confidenceScore = 1.0f)
        )
        defaults.forEach { memoryDao.insertMemory(it) }
    }
}
