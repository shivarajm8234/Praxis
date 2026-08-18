package ai.helply.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PlacementScreen() {
    var companyName by remember { mutableStateOf("TechCorp") }
    var jobDescription by remember { mutableStateOf("Looking for Android Engineer with Kotlin, Jetpack Compose, Room, and AI Edge experience.") }
    var atsScore by remember { mutableStateOf<Int?>(84) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Placement Copilot & ATS Intelligence", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Target Company") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jobDescription,
                    onValueChange = { jobDescription = it },
                    label = { Text("Paste Job Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { atsScore = 88 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate Estimated ATS Score")
                }
            }
        }

        atsScore?.let { score ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estimated ATS Compatibility Score: $score%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Matched Skills: Kotlin, Jetpack Compose, Room DB, Git")
                    Text("Missing Keywords: Docker, CI/CD Actions")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {}) {
                        Text("Generate Customized Resume V2")
                    }
                }
            }
        }
    }
}
