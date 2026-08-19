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
    suspend fun evaluateResume(
        resumeText: String, 
        jobDescription: String,
        aiGenerator: suspend (String, String) -> String
    ): ATSResult {
        val prompt = """
            You are an ATS Evaluation Agent.
            Evaluate this candidate resume against the job description.
            
            Job Description:
            $jobDescription
            
            Candidate Resume:
            $resumeText
            
            Return a valid JSON object matching the following structure EXACTLY:
            {
              "score": 78,
              "keywordMatch": 80,
              "semanticSimilarity": 75,
              "structuralScore": 85,
              "missingKeywords": ["Docker", "Kubernetes"],
              "matchedSkills": ["Kotlin", "Android"],
              "recommendations": ["Recommendation 1", "Recommendation 2"]
            }
            Do not include any thinking tags or markdown wrapper. Output raw JSON only.
        """.trimIndent()

        val aiResultStr = aiGenerator(prompt, "You are a helpful ATS resume scanner agent.")

        val cleanJson = if (aiResultStr.contains("```json")) {
            aiResultStr.substringAfter("```json").substringBefore("```").trim()
        } else if (aiResultStr.contains("```")) {
            aiResultStr.substringAfter("```").substringBefore("```").trim()
        } else {
            aiResultStr.trim()
        }

        var finalScore = 70
        var keywordScore = 70
        var semanticScore = 70
        var structuralScore = 70
        val missing = mutableListOf<String>()
        val matched = mutableListOf<String>()
        val recs = mutableListOf<String>()

        try {
            val json = org.json.JSONObject(cleanJson)
            finalScore = json.optInt("score", 70)
            keywordScore = json.optInt("keywordMatch", 70)
            semanticScore = json.optInt("semanticSimilarity", 70)
            structuralScore = json.optInt("structuralScore", 70)
            
            val misArray = json.optJSONArray("missingKeywords")
            if (misArray != null) {
                for (i in 0 until misArray.length()) missing.add(misArray.getString(i))
            }
            val matArray = json.optJSONArray("matchedSkills")
            if (matArray != null) {
                for (i in 0 until matArray.length()) matched.add(matArray.getString(i))
            }
            val recArray = json.optJSONArray("recommendations")
            if (recArray != null) {
                for (i in 0 until recArray.length()) recs.add(recArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback heuristics
            val requiredKeywords = setOf("Kotlin", "Android", "Jetpack Compose", "Room", "Coroutines", "Clean Architecture", "REST API", "Git", "Docker", "CI/CD")
            val resumeWords = resumeText.lowercase().split(Regex("\\W+")).toSet()
            val localMatched = requiredKeywords.filter { keyword -> resumeWords.contains(keyword.lowercase()) }
            val localMissing = requiredKeywords.filter { keyword -> !resumeWords.contains(keyword.lowercase()) }
            
            matched.addAll(localMatched)
            missing.addAll(localMissing)
            keywordScore = ((localMatched.size.toFloat() / requiredKeywords.size.toFloat()) * 100).toInt()
            semanticScore = (keywordScore + 15).coerceAtMost(98)
            structuralScore = 90
            finalScore = ((0.45 * keywordScore) + (0.35 * semanticScore) + (0.20 * structuralScore)).toInt()
            recs.add("Add missing key technologies: ${localMissing.take(3).joinToString(", ")}")
            recs.add("Quantify project impact with bullet points.")
        }

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
