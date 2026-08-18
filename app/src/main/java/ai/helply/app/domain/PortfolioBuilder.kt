package ai.helply.app.domain

import ai.helply.app.data.entities.AcademicMemoryEntity

enum class PortfolioTheme(val themeName: String, val primaryColor: String, val bgStyle: String) {
    MINIMAL_DEVELOPER("Minimal Developer", "#2563EB", "#F8FAFC"),
    MODERN_DEVELOPER("Modern Developer", "#6366F1", "linear-gradient(135deg, #0F172A 0%, #1E1B4B 100%)"),
    AIML_PORTFOLIO("AI/ML Portfolio", "#06B6D4", "#090D16"),
    CORPORATE_PROFESSIONAL("Corporate Professional", "#1E40AF", "#FFFFFF"),
    RESEARCH_PORTFOLIO("Research Portfolio", "#059669", "#F3F4F6"),
    FRESHER_PORTFOLIO("Fresher Portfolio", "#EC4899", "#FAFAFA"),
    CREATIVE_DEVELOPER("Creative Developer", "#8B5CF6", "#18181B"),
    DARK_DEVELOPER("Dark Developer", "#10B981", "#000000")
}

object PortfolioBuilder {
    fun buildHtmlPortfolio(
        studentName: String,
        degree: String,
        college: String,
        bio: String,
        theme: PortfolioTheme,
        memories: List<AcademicMemoryEntity>
    ): String {
        val projects = memories.filter { it.type.lowercase().contains("project") }
        val skills = memories.filter { it.type.lowercase().contains("skill") }

        val projectCardsHtml = if (projects.isEmpty()) {
            """<div class="card"><p>No projects recorded in Helply Memory yet.</p></div>"""
        } else {
            projects.joinToString("\n") { p ->
                """
                <div class="card">
                    <h3>${p.title}</h3>
                    <p>${p.description}</p>
                    <span class="badge">${p.source}</span>
                </div>
                """.trimIndent()
            }
        }

        val skillsHtml = if (skills.isEmpty()) {
            """<p style="opacity: 0.7;">No technical skills recorded in Helply Memory yet.</p>"""
        } else {
            skills.joinToString(" ") { s ->
                """<span class="skill-tag">${s.title}</span>"""
            }
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$studentName | Academic & Engineering Portfolio</title>
            <style>
                :root {
                    --primary: ${theme.primaryColor};
                    --bg: ${if (theme.bgStyle.startsWith("linear")) "transparent" else theme.bgStyle};
                }
                body {
                    margin: 0;
                    padding: 0;
                    font-family: 'Inter', system-ui, -apple-system, sans-serif;
                    background: ${theme.bgStyle};
                    color: ${if (theme == PortfolioTheme.DARK_DEVELOPER || theme == PortfolioTheme.MODERN_DEVELOPER || theme == PortfolioTheme.AIML_PORTFOLIO) "#E2E8F0" else "#1E293B"};
                    line-height: 1.6;
                }
                .container {
                    max-width: 900px;
                    margin: 0 auto;
                    padding: 40px 20px;
                }
                header {
                    border-bottom: 2px solid var(--primary);
                    padding-bottom: 24px;
                    margin-bottom: 32px;
                }
                h1 { margin: 0 0 8px 0; color: var(--primary); font-size: 2.4rem; }
                .subtitle { font-size: 1.1rem; opacity: 0.85; }
                .section-title { font-size: 1.5rem; margin-top: 36px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 8px; }
                .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; margin-top: 16px; }
                .card {
                    background: rgba(255, 255, 255, 0.05);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    border-radius: 12px;
                    padding: 20px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
                    backdrop-filter: blur(8px);
                }
                .skill-tag {
                    display: inline-block;
                    background: var(--primary);
                    color: #FFF;
                    padding: 4px 12px;
                    border-radius: 20px;
                    font-size: 0.85rem;
                    margin: 4px;
                }
                .badge {
                    display: inline-block;
                    font-size: 0.75rem;
                    background: rgba(255,255,255,0.1);
                    padding: 2px 8px;
                    border-radius: 4px;
                    margin-top: 8px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <header>
                    <h1>$studentName</h1>
                    <div class="subtitle">$degree | $college</div>
                    <p>$bio</p>
                </header>

                <h2 class="section-title">Verified Skills & Expertise</h2>
                <div>$skillsHtml</div>

                <h2 class="section-title">Featured Projects</h2>
                <div class="grid">
                    $projectCardsHtml
                </div>

                <footer style="margin-top: 60px; text-align: center; font-size: 0.85rem; opacity: 0.6;">
                    Auto-generated and deployed by Helply Student AI Operating System
                </footer>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
