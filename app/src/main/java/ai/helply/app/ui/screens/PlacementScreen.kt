package ai.helply.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementScreen() {
    var companyName by remember { mutableStateOf("TechCorp") }
    var jobDescription by remember { mutableStateOf("Looking for Android Engineer with Kotlin, Jetpack Compose, Room, and AI Edge experience.") }
    var atsScore by remember { mutableStateOf<Int?>(84) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "CAREER & RECRUITER CO-PILOT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Placement & ATS Intelligence",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Target Company") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("Paste Job Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { atsScore = 88 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Calculate Estimated ATS Score", fontSize = 13.sp)
                    }
                }
            }
        }

        atsScore?.let { score ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Estimated ATS Compatibility Score: $score%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Matched Skills: Kotlin, Jetpack Compose, Room DB, Git\n• Missing Keywords: Docker, CI/CD Actions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Generate Customized Resume V2", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
