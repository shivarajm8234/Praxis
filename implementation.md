# Implementation Guide: Helply - Student AI Operating System

## 1. Project Overview
Helply is an autonomous, privacy-first, on-device AI-powered Student Operating System. Built natively for Android (Kotlin, Jetpack Compose, LiteRT/LiteRT-LM with Gemma 4 E4B model) and backed by persistent Personal Academic Memory, Helply acts as an intelligent co-pilot for a student's academic and career journey. It unifies academic work, college communication, placement intelligence, GitHub activity, resume customization, calendar management, and web portfolio auto-deployment without relying on continuous cloud LLMs or compromising privacy.

## 2. Goals
- **Autonomous Memory & Continuity**: Maintain structured local academic history so students never have to re-explain their background.
- **On-Device Edge AI**: Execute reasoning and agentic workflows locally via Gemma 4 E4B quantized models using LiteRT, operating completely offline when internet is unavailable.
- **Academic Autopilot**: Seamlessly process assignments (OCR, PDF, camera), extract deadlines/deliverables, generate research plans, and export polished PPT/PDF/DOCX reports.
- **College Communication Intelligence**: Parse incoming college circulars (Gmail / Microsoft Outlook), extract critical dates, schedule exam focus modes, and automate task planning.
- **Placement & Resume Co-pilot**: Calculate transparent estimated ATS scores, highlight skill gaps against targeted company job descriptions, and tailor resumes per role.
- **Dynamic Portfolio Engine**: Automatically identify high-value achievements (hackathons, workshops, GitHub repos) and auto-deploy web portfolios to GitHub Pages via GitHub Actions.
- **Phone + Office/Laptop Bridge**: Offload heavy computational tasks (large doc generation, PPT builds, deep research) to a local desktop/laptop environment when paired.

## 3. Functional Requirements
- **Module A (AI Academic Autopilot)**: OCR/Document processing, requirement extraction, research planning, markdown/PPT/PDF export, progress tracking.
- **Module B (College Intelligence Agent)**: Secure OAuth for Gmail/Outlook, smart message classification (EXAMINATION, ASSIGNMENT, PLACEMENT, etc.), key date/action item extraction.
- **Module C (Exam Focus / Productivity Mode)**: Preparation timeline generation, schedule management, Android system focus/do-not-disturb toggle (with explicit user permission).
- **Module D (Personal Academic Memory)**: Structured SQLite/Room entity store (Projects, Skills, Certificates, Exams, Resumes, Repos) with local vector embeddings for semantic search.
- **Module E (Placement Copilot)**: Job description parsing, ATS compatibility calculation, skill gap analysis, interview preparation roadmap generation.
- **Module F (Resume Intelligence)**: Version-controlled PDF/DOCX resume parsing, keyword optimization, diff tracking across target roles.
- **Module G (GitHub Intelligence)**: GitHub OAuth (read:user, repo scopes), repository metric analysis, code technology/language verification.
- **Module H & I (AI Portfolio Generator & Deployment)**: Structured JSON to dynamic HTML/CSS template compilation, GitHub API repo creation, GitHub Actions deployment to GitHub Pages.
- **Module J (Calendar Intelligence)**: Native Android Calendar Provider and Google/Outlook calendar synchronization, post-event portfolio evaluation.
- **Module K (Optional Image Intelligence)**: Image classification, metadata tagging, optimization for project/certificate portfolio assets.
- **Module L (Voice AI)**: Local Speech-to-Text, intent parsing, Kotlin tool call execution with voice responses.

## 4. Non-Functional Requirements
- **Privacy & Security**: Zero raw telemetry or background voice eavesdropping; encrypted local SQLite (SQLCipher); secure keystore token handling; zero model prompt injection vector risks via strict schema validation.
- **Performance**:
  - Warm-start LLM latency < 1.2s; streaming output at >= 15 tokens/sec.
  - UI frame rates locked to 60/120 FPS via Jetpack Compose unbundled state rendering.
  - Efficient battery/thermal usage with event-driven AI invocation (no spinning background polling loops).
- **Offline Capabilities**: Full functionality for memory retrieval, assignment parsing, resume parsing, local AI chat, and local portfolio previews without internet connectivity.
- **Accessibility**: Compliance with WCAG 2.1 AA guidelines, dynamic font scaling support, screen reader content descriptions, contrast ratio >= 4.5:1.

## 5. Technology Stack
- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material Design 3 (Vanilla CSS + HTML templates for deployed portfolios)
- **Architecture**: Clean Architecture + MVVM / MVI + Android Architecture Components
- **Dependency Injection**: Hilt (Dagger Hilt)
- **Database & Storage**: Room Persistence Library with SQLCipher (Encrypted SQLite) + SQLite-VSS / Local Vector Storage
- **Asynchronous / Reactive**: Kotlin Coroutines + Flow
- **AI / LLM Runtime**: LiteRT / LiteRT-LM (TensorFlow Lite / Google AI Edge for Android) running quantized Gemma 4 E4B
- **Networking & Serialization**: Retrofit2 + Ktor Client + kotlinx.serialization
- **Auth & Security**: AppAuth for Android (OAuth 2.0 + PKCE), Android KeyStore API
- **Document & Image Processing**: ML Kit Vision (OCR & Text Recognition), Apache POI / Android PdfDocument / Android Docx generation libraries

## 6. Android Architecture
The app strictly follows Clean Architecture principles divided into three distinct layers per feature module:
- **Presentation Layer**: Jetpack Compose Composables, ViewModels emitting immutable UI States (`StateFlow`), and handling UI Intents.
- **Domain Layer**: Pure Kotlin Use Cases (`UseCase` / `Interactor`), Domain Models, Repository Interfaces, and Business Validation logic.
- **Data Layer**: Repository implementations, Local Data Sources (Room DAOs, Encrypted Shared Preferences), Remote Data Sources (GitHub API, Gmail API), and DTO Mapping.

```
+-------------------------------------------------------------------+
|                        Presentation Layer                         |
|         (Jetpack Compose UI, ViewModels, UI State & Events)       |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                           Domain Layer                            |
|             (Use Cases, Business Models, Validation)              |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                            Data Layer                             |
|    (Repositories, Room Database, Network APIs, LiteRT Gemma)      |
+-------------------------------------------------------------------+
```

## 7. AI Runtime
- **Engine**: Google LiteRT-LM (Mobile AI Edge Runtime for Android).
- **Model**: Gemma 4 E4B (4-bit quantized model format, e.g., `.bin` / `.tflite` optimized for ARM64 NPU/GPU hardware acceleration).
- **Execution Engine**: OpenCL GPU Delegate / NNAPI / Hexagon DSP delegate with fallback to multithreaded CPU execution.
- **Inference Lifecycle**: Single-session streaming inference with strict context-window management (sliding window token buffer + dynamic system prompt injection).

## 8. Gemma Deployment
- Model binaries are not hardcoded into the initial APK installer (to maintain APK size < 40MB).
- Dynamic Model Delivery: Upon first launch, the app initiates a verified, background resume-capable download of the Gemma 4 E4B 4-bit quantized weights (~2.2 GB) directly to encrypted app storage (`context.filesDir/models/gemma-4-e4b-q4.bin`).
- SHA-256 integrity verification guarantees model file validity before instantiation.

## 9. Model Installation
1. Check device storage availability (Requires minimum 4.5 GB free space).
2. Download compressed model chunk archive with pause/resume support via Android `DownloadManager` or custom Ktor chunked downloader.
3. Compute and check SHA-256 hash against remote manifest signature.
4. Extract model assets into `filesDir/gemma/` directory marked with `NO_BACKUP` flags.
5. Initialize test invocation to benchmark hardware delegates (GPU vs CPU).

## 10. Model Lifecycle
- **Unloaded**: Model weights reside solely on disk. Minimal memory overhead (< 15 MB).
- **Loading**: Heavy weight mapping into RAM/VRAM via LiteRT native memory mapping (`mmap`).
- **Active / Streaming**: System prompt + User context injected; token-by-token output streamed via Kotlin `Flow<String>`.
- **Idle Timeout**: After 120 seconds of inactivity, memory manager releases native memory references, reverting state to Unloaded to conserve system RAM and battery.
- **Thermal / Memory Warning**: Responds to `onTrimMemory()` and thermal throttle callbacks by gracefully cancelling inference and unloading native buffers.

## 11. Memory Implementation
Personal Academic Memory uses a hybrid storage model:
1. **Structured Store**: SQLite / Room Database holding verified entity records (Projects, Certificates, Skills, Grades, Resumes).
2. **Semantic Vector Index**: SQLite-VSS / Local Vector Store holding 384-dimensional dense vector embeddings generated by an on-device lightweight text embedding model (e.g., All-MiniLM-L6-v2 TFLite).
3. **Retrieval Workflow**:
   - Query -> Embedding Model -> Cosine Similarity Top-K Retrieval -> Prompt Context Injection -> Gemma Inference.

## 12. Database Schema
Main Room Database Entities (`helply_database.db`):
- `student_profile` (id, name, email, college, degree, graduation_year, bio)
- `academic_memory` (id, type, title, description, source, created_at, updated_at, confidence_score, verified_status, tags)
- `memory_evidence` (id, memory_id, evidence_type, file_path, uri, extracted_text)
- `assignment` (id, subject, title, requirements, deadline, priority, status, report_path)
- `email_message` (id, sender, subject, body_snippet, category, priority, received_at, is_processed)
- `exam` (id, subject, exam_date, duration_minutes, syllabus_topics, prep_schedule_json)
- `placement_company` (id, company_name, role, job_description, required_skills_json, ats_score)
- `resume_version` (id, version_name, raw_content, formatted_pdf_path, created_at, target_role, target_company)
- `portfolio_project` (id, memory_id, title, description, repo_url, live_demo_url, image_urls_json, is_published)
- `calendar_event` (id, title, event_type, start_time, end_time, location, portfolio_worthiness_score)

## 13. Agent Architecture
Agents operate autonomously over the Tool Registry using structured system prompts and deterministic state machines:
- **Academic Agent**: Manages OCR parsing, task decomposition, research outline synthesis, and report compilation.
- **College Intelligence Agent**: Processes email payloads, applies classifier rules, extracts date structures, and creates system reminders.
- **Placement & Resume Agent**: Computes keyword frequency matrices, performs vector similarity between resume content and job descriptions, generates targeted resume variations.
- **Portfolio & Deployment Agent**: Synthesizes structured portfolio JSON from top academic memories, injects theme parameters, controls GitHub repository management API.

```
                   +-----------------------+
                   |   User / Event Input  |
                   +-----------------------+
                               |
                               v
                   +-----------------------+
                   |     Agent Router      |
                   +-----------------------+
                               |
      +------------------------+------------------------+
      |                        |                        |
      v                        v                        v
+------------+          +------------+           +------------+
| Academic   |          |  College   |           | Placement  |
|   Agent    |          | Intelligence|          | & Portfolio|
+------------+          +------------+           +------------+
      |                        |                        |
      +------------------------+------------------------+
                               |
                               v
                   +-----------------------+
                   |  Gemma 4 E4B (Brain)  |
                   +-----------------------+
                               |
                               v
                   +-----------------------+
                   | JSON Tool-Call Parser |
                   +-----------------------+
                               |
                               v
                   +-----------------------+
                   | Kotlin Tool Registry  |
                   +-----------------------+
```

## 14. Tool Registry
All AI-driven actions are safely wrapped inside Kotlin classes implementing a standard `AgentTool` interface:
- `createTask(title, deadline, priority)`
- `readAssignments(filterStatus)`
- `createReminder(title, timestamp)`
- `createCalendarEvent(title, startTime, endTime)`
- `updateMemory(entityType, content, tags)`
- `getMemory(query, category)`
- `generateReport(assignmentId, templateType)`
- `analyzeResume(resumeId, jobDescription)`
- `calculateATS(resumeId, jobId)`
- `analyzeGitHub(username)`
- `createRepository(repoName, visibility)`
- `updatePortfolio(portfolioDataJson)`
- `deployPortfolio(repoName)`
- `enableFocusMode(startTime, endTime)`

Each execution checks runtime permissions, sanitizes inputs, validates parameters against Kotlin Data Contracts, and logs output for security audit.

## 15. Gmail Integration
- Authenticates using AppAuth for Android using OAuth 2.0 with PKCE.
- Scopes: `https://www.googleapis.com/auth/gmail.readonly`
- Fetches incoming headers and snippets securely over TLS.
- Local heuristics check for academic markers (keywords like "Exam", "Circular", "Submission", "Schedule", "Hall Ticket") before triggering full Gemma NLP classification.

## 16. Calendar Integration
- Direct synchronization with native Android `CalendarContract` API and Google Calendar API (`https://www.googleapis.com/auth/calendar.events`).
- Automatically checks for schedule conflicts before placing study/exam preparation sessions.
- Prompts user confirmation before inserting or modifying any existing calendar entries.

## 17. GitHub OAuth
- Uses GitHub OAuth App flow with PKCE (`repo`, `user` scopes).
- Securely exchanges authorization code for access token.
- Stores token inside Android KeyStore backed `EncryptedSharedPreferences`.
- Fetches user public repositories, commit statistics, primary languages, and README files to update GitHub Intelligence metrics.

## 18. Resume Analysis
- Parses imported PDF documents using `PdfReader` / ML Kit Vision OCR and `.docx` using Apache POI.
- Extracts sections: Contact, Education, Work Experience, Projects, Skills, Certifications.
- Standardizes content into structured JSON representations stored in `resume_version` table.

## 19. ATS Engine
Calculates "Estimated ATS Compatibility Score" (0–100%):
- **Formula**: Score = (0.45 * Keyword_Match_Score) + (0.35 * Vector_Semantic_Similarity) + (0.10 * Structural_Completeness) + (0.10 * Project_Relevance)
- Explains scoring components transparently without pretending to be a specific proprietary ATS vendor.
- Provides actionable line-by-line recommendations (e.g., "Add missing keyword 'Docker'", "Quantify achievement impact under Project X").

## 20. Portfolio Generation
- Synthesizes dynamic portfolio data models from verified high-confidence records in `academic_memory`.
- Supports 8 distinct themes: Minimal Developer, Modern Developer, AI/ML Portfolio, Corporate Professional, Research Portfolio, Fresher Portfolio, Creative Developer, Dark Developer.
- Generates a standalone, responsive static web application (HTML5, Vanilla CSS, JS) complete with dynamic project cards, dark mode toggle, and responsive layouts.

## 21. GitHub Deployment
Automated step-by-step pipeline:
1. Validates local portfolio web build assets.
2. Checks/Creates public GitHub repository (e.g., `username.github.io` or `student-portfolio`).
3. Commits static files and auto-generated `.github/workflows/deploy.yml` workflow file.
4. Triggers GitHub Actions pipeline to deploy static build directly to GitHub Pages.
5. Returns live production URL (`https://<username>.github.io/<repo>/`).

## 22. Office Kit Integration
- Pair phone with laptop/desktop on the local area network (LAN) over encrypted WebSockets / mDNS local service discovery.
- Hands off heavy tasks (e.g., rendering multi-page PPT files with embedded vector graphics, compiling large LaTeX reports, running heavy build tasks) to the desktop node.
- Keeps phone UI updated with real-time status notifications without draining mobile battery.

## 23. Security
- OAuth tokens strictly stored in hardware-backed KeyStore.
- Input/Prompt Validation against prompt injection: user input is wrapped in isolated data blocks; tools strictly validate structured parameters before execution.
- No plain-text password or credential logging; output logs sanitized.
- One-click privacy management: "Forget this memory", "Clear AI Cache", "Revoke OAuth Tokens".

## 24. Privacy
- 100% Local-First data storage and on-device model execution.
- Zero analytics tracking or data monetization.
- User retains full control over which memories are marked `Verified` or `Private`.

## 25. Error Handling
- Comprehensive sealed error hierarchy (`HelplyError.NetworkError`, `HelplyError.ModelUnavailable`, `HelplyError.InsufficientStorage`, etc.).
- Granular, user-friendly actionable dialogs with direct fix buttons (e.g., "Storage Low: Tap to clear 1.2 GB temporary cache").
- Retries with exponential backoff for network-bound operations.

## 26. Testing Strategy
- **Unit Tests**: Mockk & JUnit5 testing for UseCases, Repositories, and ATS calculation algorithms.
- **Integration Tests**: Room DB migration tests, LiteRT Tool-Call JSON parser validation.
- **UI Tests**: Jetpack Compose UI tests with `ComposeTestRule`.
- **Security Audit**: OAuth PKCE verification tests and token revocation validation.

## 27. Performance Optimization
- Lazy load Compose lists with `LazyColumn` and key indexing.
- SQLite indexing on frequently queried columns (`memory_type`, `created_at`, `status`).
- Model lifecycle management releasing RAM during background pauses.
- Background asynchronous image compression before web portfolio packaging.

## 28. Build Instructions
1. Clone repository: `git clone https://github.com/helply/helply-android.git`
2. Open in Android Studio Jellyfish / Ladybug or newer.
3. Supply local OAuth Client IDs in `local.properties` (`GMAIL_CLIENT_ID`, `GITHUB_CLIENT_ID`).
4. Sync Gradle project dependencies (`./gradlew assembleDebug`).
5. Run on physical device (ARM64 with minimum 6GB RAM recommended for local LiteRT Gemma execution).

## 29. Release Instructions
1. Run full test suite: `./gradlew check test`
2. Generate signed Release APK/AAB: `./gradlew assembleRelease`
3. Verify proguard rules (`proguard-rules.pro`) preserve LiteRT native bindings and kotlinx.serialization schemas.
