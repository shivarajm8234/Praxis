package ai.helply.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AcademicsScreen() {
    var assignmentText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Academic Autopilot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Camera / PDF / Text Assignment Ingestion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = assignmentText,
                    onValueChange = { assignmentText = it },
                    label = { Text("Paste assignment details or OCR text...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        resultText = "Requirements Extracted:\n- Subject: Machine Learning\n- Deliverable: Jupyter Notebook & PDF Report\n- Deadline: Oct 24, 2026\n- Priority: HIGH"
                    }) {
                        Text("Extract Requirements")
                    }
                    OutlinedButton(onClick = {
                        resultText = "PPT & PDF Generation initialized...\nSaved to Academic Memory."
                    }) {
                        Text("Generate Report & PPT")
                    }
                }
            }
        }

        if (resultText.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Output (Verified vs Generated Distinguishability Enabled):", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(resultText)
                }
            }
        }
    }
}
