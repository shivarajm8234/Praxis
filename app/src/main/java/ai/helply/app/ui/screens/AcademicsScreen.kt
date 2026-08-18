package ai.helply.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.helply.app.core.AppLockPermissionManager
import ai.helply.app.domain.AIAppClassifierEngine
import ai.helply.app.domain.ClassifiedApp
import ai.helply.app.ui.HelplyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicsScreen(viewModel: HelplyViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var assignmentText by remember { mutableStateOf("") }
    val isAgentRunning by viewModel.isAgentRunning.collectAsState()
    val autonomousResult by viewModel.autonomousPipelineResult.collectAsState()

    val emailScanSummary by viewModel.emailScanSummary.collectAsState()
    val examLockState by viewModel.examLockState.collectAsState()
    val manuallyLockedPackages by viewModel.manuallyLockedPackages.collectAsState()

    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var dialogSearchQuery by remember { mutableStateOf("") }

    var hasPermission by remember {
        mutableStateOf(AppLockPermissionManager.hasUsageStatsPermission(context))
    }

    var blockedAppsWithTime by remember {
        mutableStateOf(AIAppClassifierEngine.scanOnlyBlockedApps(context))
    }

    // Auto-refresh permissions & app scan when user returns to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = AppLockPermissionManager.hasUsageStatsPermission(context)
                blockedAppsWithTime = AIAppClassifierEngine.scanOnlyBlockedApps(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allInstalledApps = remember(context) {
        AIAppClassifierEngine.scanInstalledApps(context)
    }

    val filteredDialogApps = remember(dialogSearchQuery, allInstalledApps) {
        if (dialogSearchQuery.isBlank()) {
            allInstalledApps
        } else {
            allInstalledApps.filter { item ->
                item.appName.contains(dialogSearchQuery, ignoreCase = true) ||
                item.packageName.contains(dialogSearchQuery, ignoreCase = true)
            }
        }
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

        // Permission Warning Banner if Usage Stats Permission is missing
        if (!hasPermission) {
            item {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF59E0B))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Required for App Lock",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Helply needs Usage Stats permission to monitor running apps and enforce lock.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                        Button(
                            onClick = {
                                AppLockPermissionManager.openUsageStatsSettings(context)
                                hasPermission = AppLockPermissionManager.hasUsageStatsPermission(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp)
                        }
                    }
                }
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
                            if (assignmentText.isNotBlank()) {
                                viewModel.runAutonomousAcademicAgent(assignmentText.trim())
                            }
                        },
                        enabled = !isAgentRunning && assignmentText.isNotBlank(),
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

        // Feature 2: College Email Scanner & Live Exam Lockdown
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
                                text = "College Email Scanner & Exam Lock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Switch(
                            checked = examLockState.isLockActive,
                            onCheckedChange = { active ->
                                if (active && !AppLockPermissionManager.hasUsageStatsPermission(context)) {
                                    AppLockPermissionManager.openUsageStatsSettings(context)
                                }
                                viewModel.toggleExamLockdown(active)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (examLockState.isLockActive)
                            "🔒 EXAM LOCK ACTIVE (STRICT NO OVERRIDE): Real-time background enforcer instantly kicks you to Home screen if you attempt to launch YouTube, Instagram, Snapchat, X, or manually locked apps!"
                        else
                            "Scan incoming college circulars or toggle Lock switch above to enable real 5-day exam enforcement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (examLockState.isLockActive) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (examLockState.isLockActive) FontWeight.SemiBold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.scanCollegeEmails() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Scan College Circulars & Trigger Lockdown")
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

        // Feature 3: RESTRICTED/BLOCKED APPS WITH USAGE TIME & POPUP MANUAL SELECTION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444))
                            Text(
                                text = "Restricted Distraction Apps (${blockedAppsWithTime.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }

                        Button(
                            onClick = {
                                if (!AppLockPermissionManager.hasUsageStatsPermission(context)) {
                                    AppLockPermissionManager.openUsageStatsSettings(context)
                                }
                                showAppSelectionDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔒 Select Apps to Lock", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Exclusively lists social media & video entertainment apps with daily usage time. Click 'Select Apps to Lock' to manually pick apps to lock without override capability.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (blockedAppsWithTime.isEmpty()) {
                        Text(
                            text = "No distraction apps detected on device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        blockedAppsWithTime.forEach { app ->
                            val isManuallyLocked = manuallyLockedPackages.contains(app.packageName)
                            val isEffectiveLockActive = isManuallyLocked || examLockState.isLockActive

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(app.iconEmoji, fontSize = 22.sp)
                                    Column {
                                        Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Usage: ${app.usageTimeFormatted}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFC2410C),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isEffectiveLockActive) Color(0xFFFEF2F2) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = if (isEffectiveLockActive) "LOCKED 🔒" else "RESTRICTED 🔴",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEffectiveLockActive) Color(0xFF991B1B) else Color(0xFFEA580C),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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

    // ─── POPUP DIALOG FOR MANUAL APP LOCK SELECTION ─────────
    if (showAppSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showAppSelectionDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒 Manual App Lockdown Picker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showAppSelectionDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select any installed app to lock manually. Once locked, background enforcer strictly kicks out access with NO manual override allowed!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    // Search field
                    OutlinedTextField(
                        value = dialogSearchQuery,
                        onValueChange = { dialogSearchQuery = it },
                        placeholder = { Text("Search installed apps...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredDialogApps) { app ->
                            val isChecked = manuallyLockedPackages.contains(app.packageName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.toggleManualAppLock(app.packageName)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(app.iconEmoji, fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        viewModel.toggleManualAppLock(app.packageName)
                                    }
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppSelectionDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Done (${manuallyLockedPackages.size} Locked)")
                }
            }
        )
    }
}
