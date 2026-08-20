package ai.helply.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.helply.app.data.entities.EmailEntity
import ai.helply.app.data.entities.ExamEntity
import ai.helply.app.domain.ExamLockState
import ai.helply.app.domain.PriorityLevel
import ai.helply.app.ui.HelplyViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─── Priority Color Palette ───────────────────────────────────────────────────

private val CRITICAL_COLOR = Color(0xFFEF4444)
private val HIGH_COLOR      = Color(0xFFF97316)
private val MEDIUM_COLOR    = Color(0xFFEAB308)
private val LOW_COLOR       = Color(0xFF22C55E)
private val SURFACE_DARK    = Color(0xFF1A1A2E)
private val CARD_BG         = Color(0xFF16213E)
private val ACCENT_BLUE     = Color(0xFF0F3460)

// ─── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailIntelligenceScreen(viewModel: HelplyViewModel) {
    val emails         by viewModel.emails.collectAsState()
    val exams          by viewModel.allExams.collectAsState()
    val examLockState  by viewModel.examLockState.collectAsState()
    val gmailState     by viewModel.gmailConnectionState.collectAsState()
    val isAgentRunning by viewModel.isAgentRunning.collectAsState()

    val activity = androidx.compose.ui.platform.LocalContext.current as? ai.helply.app.ui.MainActivity

    var showExamScheduleSheet by remember { mutableStateOf(false) }
    var selectedPriority      by remember { mutableStateOf<String?>(null) }

    val filteredEmails = remember(emails, selectedPriority) {
        if (selectedPriority == null) emails
        else emails.filter { it.priority == selectedPriority }
    }

    val criticalCount = emails.count { it.priority == "CRITICAL_RED" }
    val highCount     = emails.count { it.priority == "HIGH_ORANGE" }
    val mediumCount   = emails.count { it.priority == "MEDIUM_YELLOW" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── Header ──────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EMAIL INTELLIGENCE",
                            color = Color(0xFF6366F1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "College Inbox Monitor",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Live indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color(0xFF1A1A2E), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val pulse = rememberInfiniteTransition(label = "pulse")
                        val alpha by pulse.animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (gmailState is HelplyViewModel.GmailConnectionState.Connected) LOW_COLOR.copy(alpha = alpha) else Color.Gray)
                        )
                        Text(
                            text = if (gmailState is HelplyViewModel.GmailConnectionState.Connected) "LIVE" else "OFF",
                            color = if (gmailState is HelplyViewModel.GmailConnectionState.Connected) LOW_COLOR else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── Gmail Connection Card ────────────────────────────────────────
            item {
                GmailAccountCard(
                    gmailState   = gmailState,
                    isLoading    = isAgentRunning,
                    onConnect    = { activity?.launchGmailOAuth() },
                    onDisconnect = { viewModel.disconnectGmailAccount() },
                    onSyncNow    = { viewModel.syncEmailsNow() }
                )
            }

            // ─── Exam Lock Banner ─────────────────────────────────────────────
            if (examLockState.isLockActive) {
                item {
                    ExamLockBanner(
                        examLockState = examLockState,
                        onViewSchedule = { showExamScheduleSheet = true }
                    )
                }
            }

            // ─── Priority Filter Row ──────────────────────────────────────────
            if (emails.isNotEmpty()) {
                item {
                    PriorityFilterRow(
                        criticalCount   = criticalCount,
                        highCount       = highCount,
                        mediumCount     = mediumCount,
                        selectedPriority = selectedPriority,
                        onSelect        = { p -> selectedPriority = if (selectedPriority == p) null else p }
                    )
                }
            }

            // ─── Email List ───────────────────────────────────────────────────
            if (filteredEmails.isEmpty()) {
                item {
                    EmptyEmailState(
                        isConnected = gmailState is HelplyViewModel.GmailConnectionState.Connected,
                        onConnect   = { activity?.launchGmailOAuth() }
                    )
                }
            } else {
                item {
                    Text(
                        text = "PRIORITY INBOX (${filteredEmails.size})",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                items(filteredEmails, key = { it.id }) { email ->
                    PriorityEmailCard(email = email)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // Loading overlay
        if (isAgentRunning) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Color(0xFF6366F1), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Analysing emails...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // ─── Sheets ───────────────────────────────────────────────────────────────
    if (showExamScheduleSheet) {
        ExamScheduleBottomSheet(
            exams = exams,
            onDismiss = { showExamScheduleSheet = false }
        )
    }
}

// ─── Gmail Account Card ───────────────────────────────────────────────────────

@Composable
private fun GmailAccountCard(
    gmailState:  HelplyViewModel.GmailConnectionState,
    isLoading:   Boolean,
    onConnect:   () -> Unit,
    onDisconnect: () -> Unit,
    onSyncNow:   () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (gmailState) {
                is HelplyViewModel.GmailConnectionState.Connected -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Google 'G' logo colour pill
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF4285F4), Color(0xFF34A853))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text(gmailState.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(gmailState.email, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("Gmail · Auto-polling every 15 min", color = LOW_COLOR, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = onDisconnect) {
                            Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = Color(0xFF94A3B8))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSyncNow,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF6366F1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sync Now", color = Color(0xFF6366F1), fontSize = 13.sp)
                    }
                }

                is HelplyViewModel.GmailConnectionState.Disconnected -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2D2D44)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No Gmail Connected", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Sign in with Google to monitor college emails", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Google-branded sign-in button
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                    ) {
                        Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Sign in with Google", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }

                is HelplyViewModel.GmailConnectionState.Authorizing -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF4285F4), strokeWidth = 2.dp)
                        Text("Signing in with Google...", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                }

                is HelplyViewModel.GmailConnectionState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CRITICAL_COLOR, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Sign-in Failed", color = CRITICAL_COLOR, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(gmailState.message, color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onConnect,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again", color = Color.White)
                    }
                }
            }
        }
    }
}

// ─── Exam Lock Banner ─────────────────────────────────────────────────────────

@Composable
private fun ExamLockBanner(
    examLockState: ExamLockState,
    onViewSchedule: () -> Unit
) {
    val now = System.currentTimeMillis()
    val totalLockDuration = (examLockState.examEndMillis + 86_400_000L) -
        (examLockState.examDateMillis - (5 * 86_400_000L))
    val elapsed = now - (examLockState.examDateMillis - (5 * 86_400_000L))
    val progress = if (totalLockDuration > 0) (elapsed.toFloat() / totalLockDuration).coerceIn(0f, 1f) else 0f

    val daysLeft = if (examLockState.examDateMillis > now)
        ((examLockState.examDateMillis - now) / 86_400_000L).toInt()
    else 0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A0A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CRITICAL_COLOR.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔒", fontSize = 20.sp)
                    Column {
                        Text("EXAM LOCK ACTIVE", color = CRITICAL_COLOR, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(examLockState.examTitle.take(40), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    color = CRITICAL_COLOR.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (daysLeft > 0) "$daysLeft days\nto exam" else "Exams\nongoing",
                        color = CRITICAL_COLOR,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Lockdown progress bar
            val startText = formatMs(examLockState.examDateMillis - (5 * 86_400_000L))
            val endText = formatMs(examLockState.examEndMillis)
            Text("$startText → $endText", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = CRITICAL_COLOR,
                trackColor = Color(0xFF2D1A1A)
            )

            Spacer(Modifier.height(10.dp))

            // Locked apps preview
            Text("Blocked: Instagram • YouTube • Twitter • Reddit +more", color = Color(0xFF94A3B8), fontSize = 11.sp)

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onViewSchedule,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CRITICAL_COLOR.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("📅 View Exam Schedule", color = CRITICAL_COLOR, fontSize = 13.sp)
            }
        }
    }
}

// ─── Priority Filter Row ──────────────────────────────────────────────────────

@Composable
private fun PriorityFilterRow(
    criticalCount: Int,
    highCount: Int,
    mediumCount: Int,
    selectedPriority: String?,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PriorityChip("🔴 $criticalCount Critical", "CRITICAL_RED", CRITICAL_COLOR, selectedPriority, onSelect)
        PriorityChip("🟠 $highCount High", "HIGH_ORANGE", HIGH_COLOR, selectedPriority, onSelect)
        PriorityChip("🟡 $mediumCount Medium", "MEDIUM_YELLOW", MEDIUM_COLOR, selectedPriority, onSelect)
    }
}

@Composable
private fun PriorityChip(label: String, priority: String, color: Color, selected: String?, onSelect: (String) -> Unit) {
    val isSelected = selected == priority
    Surface(
        onClick = { onSelect(priority) },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF1A1A2E),
        border = BorderStroke(1.dp, if (isSelected) color else Color(0xFF2D2D44))
    ) {
        Text(label, color = if (isSelected) color else Color(0xFF94A3B8), fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

// ─── Priority Email Card ──────────────────────────────────────────────────────

@Composable
fun PriorityEmailCard(email: EmailEntity) {
    var expanded by remember { mutableStateOf(false) }

    val priorityColor = when (email.priority) {
        "CRITICAL_RED"   -> CRITICAL_COLOR
        "HIGH_ORANGE"    -> HIGH_COLOR
        "MEDIUM_YELLOW"  -> MEDIUM_COLOR
        else             -> LOW_COLOR
    }
    val priorityEmoji = when (email.priority) {
        "CRITICAL_RED"   -> "🔴"
        "HIGH_ORANGE"    -> "🟠"
        "MEDIUM_YELLOW"  -> "🟡"
        else             -> "🟢"
    }
    val categoryLabel = email.category.replace('_', ' ').split(" ")
        .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

    Card(
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, priorityColor.copy(alpha = if (expanded) 0.6f else 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: priority badge + category + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(priorityEmoji, fontSize = 14.sp)
                    Surface(color = priorityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            categoryLabel,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(formatRelativeTime(email.receivedAt), color = Color(0xFF64748B), fontSize = 11.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Subject
            Text(
                text = email.subject,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Sender
            Text(
                text = "From: ${email.sender.substringBefore('<').trim().ifBlank { email.sender }}",
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )

            // AI Summary
            if (email.aiSummary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF0D0D1A),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = email.aiSummary,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(10.dp),
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expanded full body
            AnimatedVisibility(visible = expanded && email.fullBody.isNotBlank()) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF2D2D44))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = email.fullBody.take(1500),
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Expand toggle
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (expanded) "▲ Show less" else "▼ Read more",
                color = Color(0xFF6366F1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ─── Exam Schedule Bottom Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScheduleBottomSheet(
    exams: List<ExamEntity>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = {
            Box(modifier = Modifier.padding(8.dp)) {
                Surface(color = Color(0xFF2D2D44), shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.width(40.dp).height(4.dp)) {}
            }
        }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("📅 Detected Exam Schedule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            if (exams.isEmpty()) {
                Text("No exam circulars detected yet.", color = Color(0xFF94A3B8), fontSize = 14.sp)
            } else {
                exams.forEach { exam ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D1A)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CRITICAL_COLOR.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("📋 ${exam.subject}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("📅 Start: ${formatMs(exam.examStartDate)}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("📅 End:   ${formatMs(exam.examEndDate)}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("🔒 Lock from: ${formatMs(exam.lockdownStartDate)}", color = CRITICAL_COLOR, fontSize = 12.sp)
                            if (exam.venue.isNotBlank()) {
                                Text("📍 Venue: ${exam.venue}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyEmailState(isConnected: Boolean, onConnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📬", fontSize = 48.sp)
        Text(
            if (isConnected) "No emails yet" else "Connect your college email",
            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
        )
        Text(
            if (isConnected) "Tap 'Sync Now' to check for new emails, or wait for the automatic 15-minute poll."
            else "Helply will monitor your inbox, classify emails by priority, and automatically lock social apps when exam circulars are detected.",
            color = Color(0xFF64748B), fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (!isConnected) {
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Connect Email")
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatMs(ms: Long): String {
    if (ms == 0L) return "N/A"
    return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(ms))
}

private fun formatRelativeTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000L               -> "Just now"
        diff < 3_600_000L            -> "${diff / 60_000}m ago"
        diff < 86_400_000L           -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000L       -> "${diff / 86_400_000}d ago"
        else                         -> SimpleDateFormat("MMM dd", Locale.US).format(Date(ms))
    }
}
