package ai.helply.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import ai.helply.app.ui.HelplyViewModel

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
fun HomeScreen(navController: NavController, viewModel: HelplyViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedNoteForDialog by remember { mutableStateOf<NotepadNote?>(null) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }

    val llmExecutionTrace by viewModel.agentTrace.collectAsState()
    val isExecutingLLM by viewModel.isAgentRunning.collectAsState()

    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }

    var notesList by remember {
        mutableStateOf(emptyList<NotepadNote>())
    }

    val filteredNotes = notesList.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.contentSnippet.contains(searchQuery, ignoreCase = true)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Scaffold(
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
                    .padding(horizontal = if (isWideScreen) 24.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Box
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
                        }
                    }
                }

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

                if (filteredNotes.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "📝 No Academic Notes Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap the + button to add real study notes or project tasks.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredNotes) { note ->
                        NoteCardResponsive(
                            note = note,
                            onClick = {
                                viewModel.clearTrace()
                                selectedNoteForDialog = note
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    // Detail & AI Agent Dispatch Dialog
    selectedNoteForDialog?.let { note ->
        AlertDialog(
            onDismissRequest = { selectedNoteForDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                llmExecutionTrace.forEach { trace ->
                                    Text(
                                        text = trace,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executeAgentDispatch(
                            noteTitle = note.title,
                            agentName = note.assignedAgent,
                            toolCallStr = note.defaultToolCall
                        )
                    },
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
                                defaultToolCall = "createTask(title='$newNoteTitle', subject='General', deadlineDays=3)"
                            )
                            notesList = listOf(newNote) + notesList
                            viewModel.addMemory(
                                title = newNoteTitle,
                                type = "Note",
                                description = newNoteContent,
                                source = "Notepad Interface"
                            )
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

@Composable
fun NoteCardResponsive(note: NotepadNote, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (note.isPinned) {
                            Text(
                                text = "📌",
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
