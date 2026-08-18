package ai.helply.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PortfolioScreen() {
    var selectedTheme by remember { mutableStateOf("Modern Developer") }
    var deployStatus by remember { mutableStateOf("") }

    val themes = listOf(
        "Minimal Developer",
        "Modern Developer",
        "AI/ML Portfolio",
        "Corporate Professional",
        "Research Portfolio",
        "Fresher Portfolio",
        "Creative Developer",
        "Dark Developer"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Portfolio Generator & Deployment", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Portfolio Theme:", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                themes.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { theme ->
                            FilterChip(
                                selected = selectedTheme == theme,
                                onClick = { selectedTheme = theme },
                                label = { Text(theme) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        deployStatus = "Synthesizing HTML static bundle...\nCreating GitHub Repository...\nConfiguring GitHub Actions...\nLive URL: https://student.github.io/portfolio/"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Deploy Portfolio to GitHub Pages")
                }
            }
        }

        if (deployStatus.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Deployment Progress:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(deployStatus)
                }
            }
        }
    }
}
