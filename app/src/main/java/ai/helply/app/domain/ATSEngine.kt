package ai.helply.app.domain

data class ATSResult(
    val estimatedScore: Int,
    val keywordMatchPercentage: Int,
    val semanticSimilarityPercentage: Int,
    val structuralScorePercentage: Int,
    val missingKeywords: List<String>,
    val matchedSkills: List<String>,
    val recommendations: List<String>
)

object ATSEngine {
    fun evaluateResume(resumeText: String, jobDescription: String): ATSResult {
        val requiredKeywords = setOf("Kotlin", "Android", "Jetpack Compose", "Room", "Coroutines", "Clean Architecture", "REST API", "Git", "Docker", "CI/CD")
        val resumeWords = resumeText.lowercase().split(Regex("\\W+")).toSet()
        
        val matched = requiredKeywords.filter { keyword -> resumeWords.contains(keyword.lowercase()) }
        val missing = requiredKeywords.filter { keyword -> !resumeWords.contains(keyword.lowercase()) }

        val keywordScore = ((matched.size.toFloat() / requiredKeywords.size.toFloat()) * 100).toInt()
        val semanticScore = (keywordScore + 15).coerceAtMost(98)
        val structuralScore = 90

        val finalScore = ((0.45 * keywordScore) + (0.35 * semanticScore) + (0.20 * structuralScore)).toInt()

        val recs = mutableListOf<String>()
        if (missing.isNotEmpty()) {
            recs.add("Add missing key technologies: ${missing.take(3).joinToString(", ")}")
        }
        recs.add("Quantify project impact with bullet points (e.g. 'Improved query latency by 40%')")
        recs.add("Ensure section headings match standard labels (Education, Experience, Projects, Skills)")

        return ATSResult(
            estimatedScore = finalScore,
            keywordMatchPercentage = keywordScore,
            semanticSimilarityPercentage = semanticScore,
            structuralScorePercentage = structuralScore,
            missingKeywords = missing,
            matchedSkills = matched,
            recommendations = recs
        )
    }
}
