package ai.helply.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.helply.app.ui.HelplyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AIModelInfo(
    val id: String,
    val name: String,
    val architecture: String,
    val size: String,
    val description: String,
    val isInstalled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: HelplyViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var npuEnabled by remember { mutableStateOf(true) }
    var gpuDelegateEnabled by remember { mutableStateOf(true) }
    var gmailConnected by remember { mutableStateOf(true) }
    var githubConnected by remember { mutableStateOf(true) }

    val isGemmaLoaded by viewModel.isModelLoaded.collectAsState()
    val modelLoadProgress by viewModel.modelLoadProgress.collectAsState()

    var modelsList by remember {
        mutableStateOf(
            listOf(
                AIModelInfo(
                    id = "1",
                    name = "LiteRT Gemma 4 E4B",
                    architecture = "Quantized INT4 (NPU Optimized)",
                    size = "2.4 GB",
                    description = "Primary autonomous agentic router for tool calls & OCR synthesis.",
                    isInstalled = true
                ),
                AIModelInfo(
                    id = "2",
                    name = "Gemma 2B IT",
                    architecture = "Instruction-Tuned FP16",
                    size = "1.3 GB",
                    description = "Lightweight offline conversational LLM for quick note summaries.",
                    isInstalled = false
                ),
                AIModelInfo(
                    id = "3",
                    name = "Whisper Tiny Quantized",
                    architecture = "Speech-to-Text Encoder",
                    size = "75 MB",
                    description = "On-device voice command recognition and lecture note audio parsing.",
                    isInstalled = true
                )
            )
        )
    }

    var downloadingModelId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    fun startModelDownload(modelId: String) {
        if (downloadingModelId != null) return
        downloadingModelId = modelId
        downloadProgress = 0.05f

        coroutineScope.launch {
            for (p in 1..20) {
                delay(150)
                downloadProgress = p * 0.05f
            }
            modelsList = modelsList.map { model ->
                if (model.id == modelId) {
                    model.copy(isInstalled = true)
                } else model
            }
            downloadingModelId = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "AI SYSTEM CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI Model Installation & Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section 1: AI Model Installation & Management
        item {
            Text(
                text = "ON-DEVICE AI MODEL MANAGEMENT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        }

        items(modelsList) { model ->
            val isThisModelLoaded = (model.id == "1" && isGemmaLoaded)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (model.isInstalled) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (model.isInstalled) Color(0xFF3B82F6) else Color(0xFF64748B),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = model.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${model.architecture} • ${model.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (model.isInstalled) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isThisModelLoaded) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (isThisModelLoaded) "Active in RAM" else "Installed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isThisModelLoaded) Color(0xFF15803D) else Color(0xFF475569),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (model.id == "1" && modelLoadProgress > 0f && modelLoadProgress < 1f) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Initializing LiteRT Engine & Warm-up...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${(modelLoadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { modelLoadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    } else if (downloadingModelId == model.id) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Downloading Model Weights...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    } else if (!model.isInstalled) {
                        Button(
                            onClick = { startModelDownload(model.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Model (${model.size})")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (isThisModelLoaded) {
                                    viewModel.unloadModel()
                                } else {
                                    viewModel.initializeModel()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isThisModelLoaded) "Unload from RAM" else "Load Model into RAM",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Hardware Acceleration Settings
        item {
            Text(
                text = "HARDWARE ACCELERATION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        }

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Qualcomm Hexagon NPU Acceleration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Offloads INT4 matrix multiplications to device neural engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = npuEnabled,
                            onCheckedChange = { npuEnabled = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OpenCL GPU Delegate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Accelerates FP16 fallback operations on Adreno GPU",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = gpuDelegateEnabled,
                            onCheckedChange = { gpuDelegateEnabled = it }
                        )
                    }
                }
            }
        }

        // Section 3: Security & Integrations
        item {
            Text(
                text = "OAUTH INTEGRATIONS & STORAGE SECURITY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        }

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
                        Text("Gmail / Outlook Circular Sync", fontWeight = FontWeight.SemiBold)
                        Switch(checked = gmailConnected, onCheckedChange = { gmailConnected = it })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GitHub OAuth Sync (Portfolio Deploy)", fontWeight = FontWeight.SemiBold)
                        Switch(checked = githubConnected, onCheckedChange = { githubConnected = it })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SQLCipher AES-256 Key Encryption", fontWeight = FontWeight.SemiBold)
                        Text("Hardware KeyStore", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
