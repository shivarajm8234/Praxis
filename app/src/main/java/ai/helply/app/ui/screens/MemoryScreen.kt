package ai.helply.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MemoryScreen() {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Personal Academic Memory", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search academic memory (projects, skills, certs)...") },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                MemoryCard("Helply AI Assistant", "Project", "On-device AI OS for students using Gemma 4 E4B & LiteRT.", "GitHub Ingestion", 0.98f)
            }
            item {
                MemoryCard("Android App Development with Kotlin", "Certificate", "Certified by Google Developers.", "User Upload", 0.99f)
            }
            item {
                MemoryCard("Smart City Infrastructure AI", "Project", "3D GIS rendering with WebGL & Three.js", "Hackathon Win", 0.96f)
            }
            item {
                MemoryCard("DBMS End Sem Exam", "Exam", "Grade: A+ (Scheduled in Calendar)", "College Email Circular", 1.0f)
            }
        }
    }
}

@Composable
fun MemoryCard(title: String, type: String, description: String, source: String, confidence: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SuggestionChip(onClick = {}, label = { Text(type) })
            }
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Source: $source", style = MaterialTheme.typography.labelSmall)
                Text("Confidence: ${(confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {}) { Text("Verify") }
                TextButton(onClick = {}) { Text("Edit") }
                TextButton(onClick = {}, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Forget") }
            }
        }
    }
}
