package ai.helply.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DemoModeScreen() {
    var stepIndex by remember { mutableStateOf(1) }

    val demoSteps = listOf(
        "1. Open Command Center Dashboard (Synthetic Data Loaded)",
        "2. Academic Memory verified with existing Android & AI Projects",
        "3. New College Email Arrived: 'END SEMESTER EXAMINATION CIRCULAR'",
        "4. AI Classifier: Category = EXAMINATION, Priority = CRITICAL",
        "5. Exam Date Extracted: DBMS Final Exam on Oct 28, 2026",
        "6. 5-Day Day-by-Day Study Plan Generated",
        "7. Focus / Productivity Mode Scheduled in Android System",
        "8. Placement Inquiry: Company X - Android Lead Role",
        "9. Company Requirements Analyzed vs Student Academic Memory",
        "10. Resume Estimated ATS Score Calculated: 84% Match",
        "11. GitHub Connected via OAuth (3 Repos Verified)",
        "12. New Portfolio-Worthy Achievement Detected: 'Smart City Hackathon Winner'",
        "13. Recommendation Prompted: 'Add to Portfolio?'",
        "14. Student Approves Update",
        "15. Portfolio HTML Compiled with 'Modern Developer' Theme",
        "16. GitHub Repository 'student-portfolio' Created",
        "17. GitHub Actions Pipeline Triggered",
        "18. Live Deployment Ready: https://student.github.io/portfolio/"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hackathon Demo Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Badge { Text("DEMO DATA") }
        }

        LinearProgressIndicator(
            progress = { stepIndex.toFloat() / demoSteps.size },
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CURRENT DEMO STEP (${stepIndex}/${demoSteps.size}):", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(demoSteps[stepIndex - 1], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { if (stepIndex > 1) stepIndex-- },
                enabled = stepIndex > 1,
                modifier = Modifier.weight(1f)
            ) {
                Text("Previous Step")
            }
            Button(
                onClick = { if (stepIndex < demoSteps.size) stepIndex++ },
                enabled = stepIndex < demoSteps.size,
                modifier = Modifier.weight(1f)
            ) {
                Text("Next Step")
            }
        }

        Text("Full Demo Scenario Execution History:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(stepIndex) { idx ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (idx + 1 == stepIndex) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text(
                        text = demoSteps[idx],
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
