package ai.helply.app.domain

data class CompanyProfile(
    val name: String,
    val logoEmoji: String,
    val roleTitle: String,
    val ctcRange: String,
    val workCulture: String,
    val averageIntake: String,
    val keyTechStack: List<String>,
    val interviewRounds: List<String>,
    val keySelectionCriteria: List<String>
)

data class CompanyShortlistAnalysis(
    val company: CompanyProfile,
    val candidateMatchScore: Int,
    val requiredResumeChanges: List<String>,
    val missingTechnicalSkills: List<String>,
    val actionableShortlistStrategy: List<String>
)

object CompanyIntelligenceEngine {

    private val companyDatabase = mapOf(
        "techcorp" to CompanyProfile(
            name = "TechCorp",
            logoEmoji = "🏢",
            roleTitle = "Software Development Engineer (SDE-1)",
            ctcRange = "16 - 22 LPA",
            workCulture = "Fast-paced product startup culture. Strong emphasis on system design, microservices, Kotlin, and clean architecture.",
            averageIntake = "15 - 20 Students / Year",
            keyTechStack = listOf("Kotlin", "Android Jetpack", "Coroutines", "System Design", "Docker", "REST APIs"),
            interviewRounds = listOf(
                "Round 1: Online Coding (DSA & Algorithms - 2 Medium, 1 Hard)",
                "Round 2: Android Core & Multi-threading Deep Dive",
                "Round 3: System Design & Architecture (Clean Architecture, Room, Hilt)",
                "Round 4: Engineering Manager & Culture Fit"
            ),
            keySelectionCriteria = listOf(
                "Must have published Android app or production-grade GitHub project.",
                "Demonstrated knowledge of Kotlin Coroutines & Flow.",
                "Strong grasp of DSA (Trees, Graphs, Dynamic Programming)."
            )
        ),
        "google" to CompanyProfile(
            name = "Google",
            logoEmoji = "🔍",
            roleTitle = "Software Engineer - University Graduate",
            ctcRange = "32 - 45 LPA",
            workCulture = "Research-driven, high scale distributed systems engineering with peer code reviews and high autonomy.",
            averageIntake = "5 - 8 Students / Year",
            keyTechStack = listOf("C++", "Java", "Python", "Algorithms", "Distributed Systems", "LiteRT"),
            interviewRounds = listOf(
                "Round 1: Screening DSA Assessment",
                "Round 2: Coding & Graph Algorithms",
                "Round 3: Advanced Data Structures & Memory Optimization",
                "Round 4: Googleyness & Leadership Principles"
            ),
            keySelectionCriteria = listOf(
                "Exceptional DSA proficiency (LeetCode Hard level).",
                "Open source contributions or AI/ML research papers.",
                "Clean code and optimal space/time complexity analysis."
            )
        ),
        "amazon" to CompanyProfile(
            name = "Amazon",
            logoEmoji = "📦",
            roleTitle = "SDE 1 - Consumer Robotics / AWS",
            ctcRange = "28 - 34 LPA",
            workCulture = "Customer obsessed, metrics-driven, 14 Leadership Principles integrated into every technical evaluation.",
            averageIntake = "25 - 30 Students / Year",
            keyTechStack = listOf("Java", "AWS", "DynamoDB", "Object Oriented Design", "Multi-threading"),
            interviewRounds = listOf(
                "Round 1: Online Assessment (Debugging + 2 Coding + Behavioral)",
                "Round 2: Data Structures & Algorithms",
                "Round 3: Object-Oriented Design (OOD) & Design Patterns",
                "Round 4: Bar Raiser (Behavioral + System Architecture)"
            ),
            keySelectionCriteria = listOf(
                "STAR method stories aligning with Amazon Leadership Principles.",
                "Solid Object-Oriented System Design foundation.",
                "Handling edge cases and writing production unit tests."
            )
        ),
        "goldman sachs" to CompanyProfile(
            name = "Goldman Sachs",
            logoEmoji = "🏦",
            roleTitle = "Quantitative & Systems Analyst",
            ctcRange = "25 - 30 LPA",
            workCulture = "High-frequency financial systems, low-latency computing, rigorous mathematical & algorithmic rigor.",
            averageIntake = "8 - 12 Students / Year",
            keyTechStack = listOf("C++", "Java", "Probability", "SQL", "Low Latency Systems"),
            interviewRounds = listOf(
                "Round 1: Mathematics & Aptitude + Coding Assessment",
                "Round 2: Advanced Data Structures & Dynamic Programming",
                "Round 3: Database Indexing, SQL & Operating Systems Core",
                "Round 4: Executive Partner Round"
            ),
            keySelectionCriteria = listOf(
                "Top percentile in Math & Probability.",
                "Flawless SQL query optimization & DB indexing.",
                "Low-latency concurrency primitives."
            )
        )
    )

    fun getCompany360(queryName: String, candidateResumeText: String): CompanyShortlistAnalysis {
        val key = queryName.trim().lowercase()
        val profile = companyDatabase[key] ?: CompanyProfile(
            name = if (queryName.isBlank()) "Target Tech Company" else queryName,
            logoEmoji = "🏢",
            roleTitle = "Software Development Engineer",
            ctcRange = "12 - 18 LPA",
            workCulture = "Collaborative development environment focusing on mobile applications, cloud services, and scalable web solutions.",
            averageIntake = "10 - 15 Students / Year",
            keyTechStack = listOf("Kotlin", "Java", "Data Structures", "SQL", "Git", "REST APIs"),
            interviewRounds = listOf(
                "Round 1: Online Technical & Aptitude Assessment",
                "Round 2: Problem Solving & Data Structures",
                "Round 3: System Design & Project Walkthrough",
                "Round 4: HR & Cultural Fit"
            ),
            keySelectionCriteria = listOf(
                "Strong foundation in Data Structures & Algorithms.",
                "Hands-on project experience in mobile or web tech stack.",
                "Good communication and team collaboration."
            )
        )

        // Perform Resume Gap & ATS Shortlist Analysis
        val resumeLower = candidateResumeText.lowercase()
        val missingSkills = profile.keyTechStack.filter { !resumeLower.contains(it.lowercase()) }
        val matchedSkillsCount = profile.keyTechStack.size - missingSkills.size
        val score = ((matchedSkillsCount.toFloat() / profile.keyTechStack.size) * 100).toInt().coerceIn(60, 98)

        val requiredChanges = mutableListOf<String>()
        if (missingSkills.isNotEmpty()) {
            requiredChanges.add("Add missing core technologies: ${missingSkills.joinToString(", ")}")
        }
        if (!resumeLower.contains("quantified") && !resumeLower.contains("%") && !resumeLower.contains("ms")) {
            requiredChanges.add("Quantify project impact metrics (e.g., 'Reduced app latency by 35%', 'Achieved 99.2% test coverage').")
        }
        requiredChanges.add("Format resume into single-column ATS-friendly layout without tables or image headers.")
        requiredChanges.add("Include a dedicated 'System Architecture' or 'Mobile AI' section detailing projects like Helply OS.")

        val strategies = listOf(
            "Highlight your Helply AI Student OS project — explicitly mention LiteRT, Gemma 4 E4B, and SQLCipher.",
            "Practice ${profile.interviewRounds.firstOrNull()?.substringBefore(":") ?: "Coding Assessment"} problems.",
            "Prepare 2 STAR-method stories emphasizing how you resolved complex multi-threading or memory bugs."
        )

        return CompanyShortlistAnalysis(
            company = profile,
            candidateMatchScore = score,
            requiredResumeChanges = requiredChanges,
            missingTechnicalSkills = missingSkills,
            actionableShortlistStrategy = strategies
        )
    }
}
