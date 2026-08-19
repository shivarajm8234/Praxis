package ai.helply.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.helply.app.ai.ModelRegistry
import ai.helply.app.ui.ChatMessage
import ai.helply.app.ui.HelplyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(viewModel: HelplyViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val selectedModelId by viewModel.selectedChatModelId.collectAsState()
    val installedModelIds by viewModel.installedModelIds.collectAsState()
    val loadedModelId by viewModel.loadedModelId.collectAsState()
    val inferenceMode by viewModel.inferenceMode.collectAsState()
    val cloudModelId by viewModel.cloudModelId.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new message
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Mode Badge + Model Selection Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Inference Mode indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (inferenceMode == ai.helply.app.ai.InferenceMode.CLOUD_API)
                            "CLOUD API MODE" else "ON-DEVICE MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (inferenceMode == ai.helply.app.ai.InferenceMode.CLOUD_API)
                            Color(0xFF4F46E5).copy(alpha = 0.15f)
                        else
                            Color(0xFF15803D).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (inferenceMode == ai.helply.app.ai.InferenceMode.CLOUD_API)
                                "☁️ $cloudModelId" else "📱 On-Device",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (inferenceMode == ai.helply.app.ai.InferenceMode.CLOUD_API)
                                Color(0xFF4F46E5) else Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Only show on-device model selector chips in ON_DEVICE mode
                if (inferenceMode == ai.helply.app.ai.InferenceMode.ON_DEVICE) {
                    Text(
                        text = "SELECT ON-DEVICE AI MODEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModelRegistry.ALL_MODELS.forEach { model ->
                            val isSelected = (model.id == selectedModelId)
                            val isInstalled = installedModelIds.contains(model.id)
                            val isLoaded = (loadedModelId == model.id)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectChatModel(model.id)
                                },
                                label = {
                                    Column {
                                        Text(
                                            text = model.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = when {
                                                isLoaded -> "Active RAM"
                                                isInstalled -> "Installed"
                                                else -> "Not Installed"
                                            },
                                            fontSize = 9.sp,
                                            color = when {
                                                isLoaded -> Color(0xFF15803D)
                                                isInstalled -> Color(0xFF1D4ED8)
                                                else -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Chat Message History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { message ->
                ChatMessageItem(message = message)
            }
        }

        // Bottom Input Row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask your on-device AI model...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.sendChatMessage(textToSend)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF4F46E5), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Send Message",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.sender == "User"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) Color(0xFF4F46E5) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser && message.modelName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤖 ${message.modelName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5)
                        )

                        if (message.modelFileSizeMb != null && message.modelFileSizeMb > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF15803D).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "⚡ ${message.modelFileSizeMb} MB Binary",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )

                if (!isUser && message.binaryFileName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📁 ${message.binaryFileName}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "100% Offline On-Device",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.timestamp,
                    fontSize = 9.sp,
                    color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
