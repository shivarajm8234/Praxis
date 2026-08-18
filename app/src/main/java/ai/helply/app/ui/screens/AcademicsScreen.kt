package ai.helply.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.helply.app.domain.AIAppClassifierEngine
import ai.helply.app.domain.ClassifiedApp
import ai.helply.app.ui.HelplyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicsScreen(viewModel: HelplyViewModel) {
    val context = LocalContext.current

    var assignmentText by remember { mutableStateOf("") }
    val isAgentRunning by viewModel.isAgentRunning.collectAsState()
    val autonomousResult by viewModel.autonomousPipelineResult.collectAsState()

    val emailScanSummary by viewModel.emailScanSummary.collectAsState()
    val examLockState by viewModel.examLockState.collectAsState()

    var classifiedApps by remember {
        mutableStateOf(AIAppClassifierEngine.scanInstalledApps(context))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AUTONOMOUS ACADEMIC AGENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI Homework & Exam Autopilot",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Feature 1: Autonomous Academic Agent (PPT, PDF, Research, Notification)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 Fully Autonomous Homework Agent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter assignment or topic. The AI agent automatically researches, generates Presentation PPT slides, PDF Research Report, notes, and notifies you when complete!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = assignmentText,
                        onValueChange = { assignmentText = it },
                        label = { Text("Topic or assignment prompt...") },
                        placeholder = { Text("e.g. ResNet-18 Image Classification Lab Report & Presentation") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val prompt = if (assignmentText.isNotBlank()) assignmentText else "Machine Learning ResNet-18 Transfer Learning Lab"
                            viewModel.runAutonomousAcademicAgent(prompt)
                        },
                        enabled = !isAgentRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        if (isAgentRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agent Researching & Generating...")
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Let AI Do Everything (PPT + PDF + Notify)")
                        }
                    }
                }
            }
        }

        // Autonomous Pipeline Output Result
        autonomousResult?.let { result ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF3B82F6))
                            Text(
                                text = "Autonomous Deliverables Generated!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = result.researchSummary,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1E3A8A)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = {}, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                Text("Open PPT (${result.pptSlideCount} Slides)", fontSize = 11.sp)
                            }
                            OutlinedButton(onClick = {}, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                Text("Open PDF Report", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Feature 2: College Email Scanner & 5-Day Social Media Lockdown
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (examLockState.isLockActive) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = if (examLockState.isLockActive) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "College Email & Exam Circular Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (examLockState.isLockActive) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFCA5A5)
                            ) {
                                Text(
                                    text = "5-Day Lock Active 🔒",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7F1D1D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Real-time AI analyzer scans incoming college circulars. If an exam is detected, social media apps are strictly locked 5 days before the exam until completion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.scanCollegeEmails() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Scan College Circulars & Check Exam Lockdown")
                    }

                    emailScanSummary?.let { summary ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Feature 3: AI App List Classification & Lock Decisions Table
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🧠 AI App List Classifier & Exam Lock Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AI automatically scans installed apps: Locks Social Media & YouTube. EXEMPTS Payment apps (GPay, PhonePe, Paytm) and AI tools (ChatGPT, Gemini, Helply OS).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    classifiedApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(app.iconEmoji, fontSize = 18.sp)
                                Column {
                                    Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(app.reasoning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (app.isBlockedDuringExams) Color(0xFFFEF2F2) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (app.isBlockedDuringExams) "BLOCKED 🔒" else "ALLOWED ✅",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (app.isBlockedDuringExams) Color(0xFF991B1B) else Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}
