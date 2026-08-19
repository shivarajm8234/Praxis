package ai.helply.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.helply.app.ai.ModelDownloadManager
import ai.helply.app.ai.ModelRegistry
import ai.helply.app.ai.OnDeviceModelConfig
import ai.helply.app.ui.HelplyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: HelplyViewModel) {
    var npuEnabled by remember { mutableStateOf(true) }
    var gpuDelegateEnabled by remember { mutableStateOf(false) }
    var gmailConnected by remember { mutableStateOf(true) }
    var githubConnected by remember { mutableStateOf(true) }

    val isGemmaLoaded by viewModel.isModelLoaded.collectAsState()
    val modelLoadProgress by viewModel.modelLoadProgress.collectAsState()
    val installedModelIds by viewModel.installedModelIds.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val availableStorageBytes by viewModel.availableStorageBytes.collectAsState()
    val loadedModelId by viewModel.loadedModelId.collectAsState()

    // Cloud API state
    val inferenceMode by viewModel.inferenceMode.collectAsState()
    val savedApiKey by viewModel.cloudApiKey.collectAsState()
    val savedBaseUrl by viewModel.cloudBaseUrl.collectAsState()
    val savedModelId by viewModel.cloudModelId.collectAsState()
    val testResult by viewModel.cloudTestResult.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()

    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var baseUrlInput by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var modelIdInput by remember(savedModelId) { mutableStateOf(savedModelId) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isCloudConfigSaved by remember { mutableStateOf(false) }

    val availableStorageFormatted = remember(availableStorageBytes) {
        when {
            availableStorageBytes >= 1_073_741_824 -> "%.1f GB".format(availableStorageBytes / 1_073_741_824.0)
            availableStorageBytes >= 1_048_576 -> "%.0f MB".format(availableStorageBytes / 1_048_576.0)
            else -> "$availableStorageBytes B"
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

        // ─── Inference Mode Toggle ─────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (inferenceMode == ai.helply.app.ai.InferenceMode.CLOUD_API)
                        Color(0xFF1E1B4B).copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INFERENCE MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cloud API chip
                        FilterChip(
                            selected = inferenceMode == ai.helply.app.ai.InferenceMode.CLOUD_API,
                            onClick = { viewModel.setInferenceMode(ai.helply.app.ai.InferenceMode.CLOUD_API) },
                            label = {
                                Column {
                                    Text("☁️ Cloud API", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("OpenAI Compatible", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // On-Device chip
                        FilterChip(
                            selected = inferenceMode == ai.helply.app.ai.InferenceMode.ON_DEVICE,
                            onClick = { viewModel.setInferenceMode(ai.helply.app.ai.InferenceMode.ON_DEVICE) },
                            label = {
                                Column {
                                    Text("📱 On-Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("100% Offline", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ─── Cloud API Configuration (shown when Cloud mode selected) ───
        if (inferenceMode == ai.helply.app.ai.InferenceMode.CLOUD_API) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CLOUD API CONFIGURATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Works with OpenAI, Groq, Together AI, OpenRouter, or any OpenAI-compatible endpoint.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick provider presets
                        Text(
                            text = "QUICK SELECT PROVIDER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Provider preset chips — 2 rows
                        ai.helply.app.ai.CloudApiEngine.PROVIDER_PRESETS.chunked(3).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowPresets.forEach { preset ->
                                    AssistChip(
                                        onClick = {
                                            baseUrlInput = preset.baseUrl
                                            modelIdInput = preset.defaultModel
                                            isCloudConfigSaved = false
                                        },
                                        label = {
                                            Text(preset.name, fontSize = 11.sp, maxLines = 1)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill remaining space if less than 3 items
                                repeat(3 - rowPresets.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // API Key input
                        Text("API Key", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it; isCloudConfigSaved = false },
                                placeholder = { Text("sk-...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                visualTransformation = if (isApiKeyVisible)
                                    androidx.compose.ui.text.input.VisualTransformation.None
                                else
                                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedButton(
                                onClick = { isApiKeyVisible = !isApiKeyVisible },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(if (isApiKeyVisible) "Hide" else "Show", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Base URL input
                        Text("Base URL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = baseUrlInput,
                            onValueChange = { baseUrlInput = it; isCloudConfigSaved = false },
                            placeholder = { Text("https://api.openai.com/v1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model ID input
                        Text("Model ID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = modelIdInput,
                            onValueChange = { modelIdInput = it; isCloudConfigSaved = false },
                            placeholder = { Text("gpt-4o-mini") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveCloudConfig(apiKeyInput, baseUrlInput, modelIdInput)
                                    isCloudConfigSaved = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isCloudConfigSaved) "Saved ✓" else "Save Config")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.saveCloudConfig(apiKeyInput, baseUrlInput, modelIdInput)
                                    viewModel.testCloudConnection()
                                },
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isTesting && apiKeyInput.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isTesting) "Testing..." else "Test Connection")
                            }
                        }

                        // Test result display
                        if (testResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (testResult!!.startsWith("✅"))
                                    Color(0xFF15803D).copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = testResult!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp),
                                    color = if (testResult!!.startsWith("✅"))
                                        Color(0xFF15803D) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Storage status banner
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE DEVICE STORAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = availableStorageFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.refreshInstalledModels() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", fontSize = 12.sp)
                    }
                }
            }
        }

        // Hugging Face Token Section (Masked API key in frontend)
        item {
            val savedHfToken by viewModel.hfToken.collectAsState()
            var tokenInput by remember(savedHfToken) { mutableStateOf(savedHfToken) }
            var isSaved by remember { mutableStateOf(false) }
            var isTokenVisible by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HUGGING FACE ACCESS TOKEN (SECURED)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gated models like Gemma require a Hugging Face Read Token. Key is securely stored and masked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = {
                                tokenInput = it
                                isSaved = false
                            },
                            placeholder = { Text("••••••••••••••••••••••••") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            visualTransformation = if (isTokenVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedButton(
                            onClick = { isTokenVisible = !isTokenVisible },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(if (isTokenVisible) "Hide" else "Show", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.saveHfToken(tokenInput)
                                isSaved = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isSaved) "Saved ✓" else "Save")
                        }
                    }
                }
            }
        }

        // Section 1: AI Model Management
        item {
            Text(
                text = "ON-DEVICE AI MODEL MANAGEMENT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
        }

        items(ModelRegistry.ALL_MODELS) { model ->
            val isInstalled = installedModelIds.contains(model.id)
            val isThisModelLoaded = (isGemmaLoaded && loadedModelId == model.id)
            val downloadState = downloadStates[model.id] ?: ModelDownloadManager.DownloadState.Idle

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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isInstalled) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isInstalled) Color(0xFF3B82F6) else Color(0xFF64748B),
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
                                    text = "${model.architecture} • ${model.displaySize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isInstalled) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isThisModelLoaded) Color(0xFFDCFCE7) else Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = if (isThisModelLoaded) "Active in RAM" else "Installed Local",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isThisModelLoaded) Color(0xFF15803D) else Color(0xFF1D4ED8),
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

                    // RAM Load Progress
                    if (model.id == ModelRegistry.GEMMA_4_E4B.id && modelLoadProgress > 0f && modelLoadProgress < 1f) {
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
                    }

                    val modelLoadError by viewModel.modelLoadError.collectAsState()
                    val selectedChatModelId by viewModel.selectedChatModelId.collectAsState()
                    if (model.id == selectedChatModelId && modelLoadError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = modelLoadError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Download state UI
                    else when (downloadState) {
                        is ModelDownloadManager.DownloadState.Downloading -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Downloading (${(downloadState.downloadedBytes / 1_048_576)} MB / ${(downloadState.totalBytes / 1_048_576)} MB)...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${(downloadState.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { downloadState.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.cancelModelDownload(model.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel Download", fontSize = 13.sp)
                                }
                            }
                        }
                        is ModelDownloadManager.DownloadState.Verifying -> {
                            Column {
                                Text(
                                    text = downloadState.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        is ModelDownloadManager.DownloadState.Failed -> {
                            Column {
                                Text(
                                    text = "Error: ${downloadState.error}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.startModelDownload(model.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Download")
                                }
                            }
                        }
                        else -> {
                            if (!isInstalled) {
                                Button(
                                    onClick = { viewModel.startModelDownload(model.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Model (${model.displaySize})")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (isThisModelLoaded) {
                                                viewModel.unloadModel()
                                            } else {
                                                viewModel.initializeModel(model.id)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = if (isThisModelLoaded) "Unload from RAM" else "Load Model into RAM",
                                            fontSize = 13.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteModel(model.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Model",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
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
                            onCheckedChange = {
                                npuEnabled = it
                                viewModel.setHardwareAcceleration(npu = it, gpu = gpuDelegateEnabled)
                            }
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
                            onCheckedChange = {
                                gpuDelegateEnabled = it
                                viewModel.setHardwareAcceleration(npu = npuEnabled, gpu = it)
                            }
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

                    if (githubConnected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val customClientId by viewModel.githubClientId.collectAsState()
                        val customClientSecret by viewModel.githubClientSecret.collectAsState()

                        var clientIdInput by remember(customClientId) { mutableStateOf(customClientId) }
                        var clientSecretInput by remember(customClientSecret) { mutableStateOf(customClientSecret) }
                        var isSecretVisible by remember { mutableStateOf(false) }
                        var isSaved by remember { mutableStateOf(false) }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🔒 CUSTOM GITHUB OAUTH CREDENTIALS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure custom credentials to avoid pushing credentials to version control. Set Authorization Callback URL in your GitHub settings to 'helply://oauth/callback'. Defaults will be used if left blank.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            
                            OutlinedTextField(
                                value = clientIdInput,
                                onValueChange = { clientIdInput = it; isSaved = false },
                                label = { Text("Client ID", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = clientSecretInput,
                                    onValueChange = { clientSecretInput = it; isSaved = false },
                                    label = { Text("Client Secret", fontSize = 11.sp) },
                                    singleLine = true,
                                    visualTransformation = if (isSecretVisible)
                                        androidx.compose.ui.text.input.VisualTransformation.None
                                    else
                                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedButton(
                                    onClick = { isSecretVisible = !isSecretVisible },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (isSecretVisible) "Hide" else "Show", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.saveGitHubConfig(clientIdInput, clientSecretInput)
                                    isSaved = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (isSaved) "Saved ✓" else "Save Credentials", fontSize = 11.sp)
                            }
                        }
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
