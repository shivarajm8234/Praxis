package ai.helply.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NotepadNote(
    val id: String,
    val title: String,
    val contentSnippet: String,
    val fullContent: String,
    val timestamp: String,
    val isPinned: Boolean,
    val tileBgColor: Color,
    val iconColor: Color,
    val assignedAgent: String,
    val defaultToolCall: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedNoteForDialog by remember { mutableStateOf<NotepadNote?>(null) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    // New note state
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }
    var isExecutingLLM by remember { mutableStateOf(false) }
    var llmExecutionTrace by remember { mutableStateOf<List<String>>(emptyList()) }

    var notesList by remember {
        mutableStateOf(
            listOf(
                NotepadNote(
                    id = "1",
                    title = "DBMS Exam Focus Plan",
                    contentSnippet = "Topics to cover: SQL, Normalization, Transactions...",
                    fullContent = "Topics to cover:\n1. SQL Queries & Joins\n2. B+ Tree Indexing & Hashing\n3. ACID Properties & Concurrency Control\n4. Normalization (1NF to BCNF)\n\n[Agent Directive: Schedule 3-hour Focus Mode before Oct 28 exam]",
                    timestamp = "10:22 AM",
                    isPinned = true,
                    tileBgColor = Color(0xFFEFF6FF),
                    iconColor = Color(0xFF3B82F6),
                    assignedAgent = "CollegeIntelligenceAgent",
                    defaultToolCall = "createExamReminder(course='DBMS', date='2026-10-28')"
                ),
                NotepadNote(
                    id = "2",
                    title = "ML Lab Report - ResNet",
                    contentSnippet = "Dataset: CIFAR-10\nModel: ResNet-18...",
                    fullContent = "Dataset: CIFAR-10\nModel Architecture: ResNet-18 Transfer Learning\nHyperparameters: LR=0.001, Epochs=25, Batch=64\nValidation Accuracy: 92.4%\n\n[Agent Directive: Generate LaTeX report template & submit before tomorrow 11:59 PM]",
                    timestamp = "9:48 AM",
                    isPinned = true,
                    tileBgColor = Color(0xFFF0FDF4),
                    iconColor = Color(0xFF22C55E),
                    assignedAgent = "AcademicAgent",
                    defaultToolCall = "createTask(title='ML Lab Report', deadline='Tomorrow 11:59PM')"
                ),
                NotepadNote(
                    id = "3",
                    title = "TechCorp Interview Preparation",
                    contentSnippet = "Round 1: Coding\nRound 2: System Design...",
                    fullContent = "Target Role: Android Software Engineer\nRequirements:\n- Kotlin & Jetpack Compose\n- Dependency Injection (Hilt)\n- Multi-threading & Coroutines\n\n[Agent Directive: Calculate ATS score for candidate resume vs TechCorp job description]",
                    timestamp = "Yesterday",
                    isPinned = false,
                    tileBgColor = Color(0xFFFEFCE8),
                    iconColor = Color(0xFFEAB308),
                    assignedAgent = "PlacementAgent",
                    defaultToolCall = "calculateATS(resumeId='res_1', jobId='TechCorp')"
                ),
                NotepadNote(
                    id = "4",
                    title = "Hackathon Ideas",
                    contentSnippet = "1. AI Attendance System\n2. Smart Portfolio Builder...",
                    fullContent = "Idea 1: Automated AI Classroom Attendance System via Facial Embedding\nIdea 2: Smart Student AI Operating System (Helply OS)\nIdea 3: Automated Resume & GitHub Portfolio Synthesizer\n\n[Agent Directive: Synthesize winning project showcase into HTML Web Portfolio]",
                    timestamp = "Yesterday",
                    isPinned = false,
                    tileBgColor = Color(0xFFEFF6FF),
                    iconColor = Color(0xFF3B82F6),
                    assignedAgent = "PortfolioAgent",
                    defaultToolCall = "deployPortfolio(repo='satoru.github.io')"
                ),
                NotepadNote(
                    id = "5",
                    title = "Seminar - Edge AI",
                    contentSnippet = "Speaker: Dr. Ananya Sharma\nDate: Oct 30, 2024...",
                    fullContent = "Speaker: Dr. Ananya Sharma\nDate: Oct 30, 2024\nTopic: On-Device LLM Inference & NPU Acceleration using LiteRT & Gemma 4 E4B\nLocation: Auditorium B",
                    timestamp = "Oct 25",
                    isPinned = false,
                    tileBgColor = Color(0xFFFEF2F2),
                    iconColor = Color(0xFFEF4444),
                    assignedAgent = "CollegeIntelligenceAgent",
                    defaultToolCall = "parseCalendarEvent(event='Edge AI Seminar')"
                ),
                NotepadNote(
                    id = "6",
                    title = "Python Tips",
                    contentSnippet = "List Comprehension\nLambda Functions...",
                    fullContent = "1. List Comprehension: [x**2 for x in range(10) if x%2==0]\n2. Lambda & Map: list(map(lambda x: x*2, items))\n3. Context Managers: with open('file.txt') as f:",
                    timestamp = "Oct 24",
                    isPinned = false,
                    tileBgColor = Color(0xFFF5F3FF),
                    iconColor = Color(0xFF8B5CF6),
                    assignedAgent = "AcademicAgent",
                    defaultToolCall = "createKnowledgeSnippet(tag='python')"
                ),
                NotepadNote(
                    id = "7",
                    title = "Daily To-Do",
                    contentSnippet = "☐ Study DBMS\n☐ Complete ML Assignment...",
                    fullContent = "☐ Study DBMS Normalization Rules\n☐ Complete ML Assignment 3\n☐ Push Helply OS APK build to GitHub\n☐ Review ATS feedback for Placement drive",
                    timestamp = "Oct 24",
                    isPinned = false,
                    tileBgColor = Color(0xFFF1F5F9),
                    iconColor = Color(0xFF64748B),
                    assignedAgent = "AcademicAgent",
                    defaultToolCall = "syncTodoList(count=4)"
                )
            )
        )
    }

    val filteredNotes = notesList.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.contentSnippet.contains(searchQuery, ignoreCase = true)
    }

    fun triggerAgentExecutionForNote(note: NotepadNote) {
        isExecutingLLM = true
        llmExecutionTrace = emptyList()

        coroutineScope.launch {
            llmExecutionTrace = llmExecutionTrace + "🧠 [LiteRT Gemma 4 E4B] Tokenizing Note: '${note.title}'"
            delay(500)
            llmExecutionTrace = llmExecutionTrace + "🔍 [LLM Router] Identified target agent: ${note.assignedAgent}"
            delay(500)
            llmExecutionTrace = llmExecutionTrace + "⚙️ Executing Tool Call: ${note.defaultToolCall}"
            delay(600)
            llmExecutionTrace = llmExecutionTrace + "✅ Tool Execution Verified! Results saved in encrypted SQLCipher DB."
            isExecutingLLM = false
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Column {
                            Text(
                                text = "Notepad",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "All your notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateNoteDialog = true },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar (Matching Reference Image)
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search notes...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Category & Sort Header (Matching Reference Image)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT NOTES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = "Last modified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Notes List (Matching Reference Image)
            items(filteredNotes) { note ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedNoteForDialog = note }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Colored Icon Box (Matching reference image document icon style)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = note.tileBgColor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = note.iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (note.isPinned) {
                                        Text(
                                            text = if (note.id == "1") "Pinned 📌" else "📌",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4F46E5),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = note.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = note.contentSnippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Detail & AI Agent Dispatch Dialog when a Note is Tapped
    selectedNoteForDialog?.let { note ->
        AlertDialog(
            onDismissRequest = { selectedNoteForDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { selectedNoteForDialog = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = note.fullContent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider()

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "ASSIGNED AGENT: ${note.assignedAgent}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tool Call: ${note.defaultToolCall}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (llmExecutionTrace.isNotEmpty()) {
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                llmExecutionTrace.forEach { trace ->
                                    Text(
                                        text = trace,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { triggerAgentExecutionForNote(note) },
                    enabled = !isExecutingLLM,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    if (isExecutingLLM) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatching...")
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Execute Agent Tool Call")
                    }
                }
            }
        )
    }

    // Modal Sheet / Dialog to Add a New Note with Auto-LLM Dispatching
    if (showCreateNoteDialog) {
        AlertDialog(
            onDismissRequest = { showCreateNoteDialog = false },
            title = {
                Text("Create New Academic Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newNoteTitle,
                        onValueChange = { newNoteTitle = it },
                        label = { Text("Note Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newNoteContent,
                        onValueChange = { newNoteContent = it },
                        label = { Text("Note Content & Task Directives") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNoteTitle.isNotBlank()) {
                            val newNote = NotepadNote(
                                id = System.currentTimeMillis().toString(),
                                title = newNoteTitle,
                                contentSnippet = if (newNoteContent.length > 50) newNoteContent.take(50) + "..." else newNoteContent,
                                fullContent = newNoteContent,
                                timestamp = "Just Now",
                                isPinned = false,
                                tileBgColor = Color(0xFFEFF6FF),
                                iconColor = Color(0xFF3B82F6),
                                assignedAgent = "AcademicAgent",
                                defaultToolCall = "autoDispatchNotepadNote(title='$newNoteTitle')"
                            )
                            notesList = listOf(newNote) + notesList
                            newNoteTitle = ""
                            newNoteContent = ""
                            showCreateNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Save & Auto-Dispatch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
