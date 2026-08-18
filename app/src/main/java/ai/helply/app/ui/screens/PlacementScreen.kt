package ai.helply.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.helply.app.ui.HelplyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementScreen(viewModel: HelplyViewModel) {
    var companyName by remember { mutableStateOf("") }
    var candidateResumeText by remember { mutableStateOf("") }

    val companyAnalysis by viewModel.companyAnalysis.collectAsState()

    val quickCompanies = listOf("TechCorp", "Google", "Amazon", "Goldman Sachs")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "COMPANY 360° & ATS SHORTLIST ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Placements Intelligence",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enter Visiting Company Name:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AI analyzes company culture, past intake, salary CTC, and tells you EXACT changes needed in your resume to get shortlisted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickCompanies.forEach { name ->
                            FilterChip(
                                selected = companyName.equals(name, ignoreCase = true),
                                onClick = { companyName = name },
                                label = { Text(name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = candidateResumeText,
                        onValueChange = { candidateResumeText = it },
                        label = { Text("Pre-Uploaded Resume Summary") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.analyzeCompanyShortlist(companyName, candidateResumeText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("Analyze Company 360° & Get Shortlist Advice", fontSize = 13.sp)
                    }
                }
            }
        }

        // Company 360° & Shortlist Output
        companyAnalysis?.let { analysis ->
            val comp = analysis.company
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(comp.logoEmoji, fontSize = 28.sp)
                                Column {
                                    Text(comp.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(comp.roleTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "Match: ${analysis.candidateMatchScore}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("CTC PACKAGE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E40AF))
                                    Text(comp.ctcRange, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                                }
                            }

                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("AVG INTAKE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF166534))
                                    Text(comp.averageIntake, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Company Work Culture:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(comp.workCulture, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Interview Rounds & Selection Flow:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        comp.interviewRounds.forEach { round ->
                            Text(" • $round", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Shortlist Resume Recommendations
                        Text("🎯 EXACT RESUME CHANGES REQUIRED FOR SHORTLISTING:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                        Spacer(modifier = Modifier.height(6.dp))

                        analysis.requiredResumeChanges.forEach { req ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text("⚡", fontSize = 12.sp)
                                Text(req, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9A3412), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                candidateResumeText = "$candidateResumeText Skills added for ${comp.name}: ${analysis.missingTechnicalSkills.joinToString(", ")}."
                                viewModel.analyzeCompanyShortlist(companyName, candidateResumeText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Auto-Apply Shortlist Resume Fixes", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
