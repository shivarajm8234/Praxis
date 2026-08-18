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
        "Clean Light Theme"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AUTOMATED PORTFOLIO PUBLISHER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI Portfolio Generator",
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
                                    label = { Text(theme, fontSize = 11.sp) },
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Deploy Portfolio to GitHub Pages")
                    }
                }
            }
        }

        if (deployStatus.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Deployment Progress:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = deployStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
