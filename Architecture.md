# Helply System Architecture Documentation

This document describes the high-level system architecture, component relationships, data boundaries, and technical blueprints for the **Helply - Student AI Operating System**.

---

## 1. System Architecture

Helply is structured around a **Local-First, Edge AI Driven Architecture** where the mobile application acts as the primary command center. The system minimizes cloud dependencies, keeping all personal academic data on the user's device while offering optional cloud integrations for synchronization, repository creation, and web portfolio deployment.

```mermaid
graph TD
    subgraph Mobile Device (Primary Command Center)
        UI[Jetpack Compose UI & ViewModels]
        AgentRuntime[Agentic State Engine]
        LiteRTEngine[LiteRT-LM Engine: Gemma 4 E4B]
        ToolReg[Kotlin Safety Tool Registry]
        LocalStore[(Room DB + SQLCipher + SQLite-VSS)]
    end

    subgraph Desktop / Laptop (Office Kit Bridge)
        OfficeBridge[Desktop Bridge Node]
        HeavyCompute[PPT / PDF / Heavy Research Engine]
    end

    subgraph Cloud / External Services
        GmailAPI[Gmail / Outlook OAuth API]
        CalendarAPI[Google / Device Calendar API]
        GitHubAPI[GitHub REST API & Actions]
        GHPages[GitHub Pages Hosting]
    end

    UI <--> AgentRuntime
    AgentRuntime <--> LiteRTEngine
    AgentRuntime <--> ToolReg
    ToolReg <--> LocalStore
    Mobile Device <== LAN WebSockets ==> OfficeBridge
    OfficeBridge <--> HeavyCompute

    ToolReg <== TLS 1.3 ==> GmailAPI
    ToolReg <== TLS 1.3 ==> CalendarAPI
    ToolReg <== TLS 1.3 ==> GitHubAPI
    GitHubAPI --> GHPages
```

---

## 2. Mobile Architecture

Built using Android **Clean Architecture** patterns combined with Unidirectional Data Flow (UDF) via ViewModels, StateFlow, and Jetpack Compose.

```mermaid
graph LR
    subgraph Presentation Layer
        ComposeUI[Compose Views] <--> ViewModel[ViewModels & MVI StateFlow]
    end

    subgraph Domain Layer
        ViewModel --> UseCases[Domain Use Cases & Interactors]
        UseCases --> Contracts[Repository Contracts & Interfaces]
    end

    subgraph Data Layer
        Contracts --> Repositories[Repository Implementation]
        Repositories --> RoomDAO[Room DB & SQLCipher DAO]
        Repositories --> NetworkClient[Ktor / Retrofit APIs]
        Repositories --> AIBridge[LiteRT Local LLM Manager]
    end
```

---

## 3. AI Architecture

Helply isolates LLM execution into an event-driven **Brain & Controller Pattern**:
- **Brain**: Gemma 4 E4B quantized model performing token reasoning and emitting tool-call JSON proposals.
- **Controller**: Kotlin Agent Engine validating input/output schemas, enforcing permissions, and executing domain tools.

```mermaid
graph TD
    UserInput[User Voice / Text Request] --> ContextBuilder[Prompt & Context Builder]
    ContextBuilder --> SystemPrompt[System Instructions & Tool Declarations]
    SystemPrompt --> GemmaModel[LiteRT Gemma 4 E4B Engine]
    GemmaModel -- Token Stream --> JSONParser[Structured Tool Call Parser]
    JSONParser --> Validator[Kotlin Type & Permission Validator]
    Validator -- Authorized --> ToolExecution[Kotlin Tool Registry]
    Validator -- Rejected --> ErrorFeedback[Re-Prompt LLM / Alert User]
    ToolExecution --> DomainAction[Update DB / Calendar / UI Action]
```

---

## 4. Edge Inference Architecture

To optimize thermal efficiency and battery consumption, Gemma 4 E4B is loaded on-demand and executed using hardware acceleration delegates.

```mermaid
graph TD
    AppEvent[Trigger Event: OCR, Email, User Query] --> ModelManager[Model Manager]
    ModelManager --> CheckRAM{RAM & Thermal OK?}
    CheckRAM -- Yes --> LoadModel[Map LiteRT OpenCL/NPU Delegates]
    CheckRAM -- Low Memory --> UnloadIdle[Unload Idle Components] --> LoadModel
    LoadModel --> IngestTokens[Ingest Context Tokens]
    IngestTokens --> ExecuteInference[Run Streaming Inference]
    ExecuteInference --> ResetTimer[Start 120s Idle Timer]
    ResetTimer -- Timeout --> ReleaseWeights[Release LiteRT Native Buffers]
```

---

## 5. Agent Architecture

Each domain agent acts as an autonomous state machine handling intent classification, tool invocation, and multi-step execution.

```mermaid
graph TD
    AgentRouter[Agent Router] --> AcademicAgent[Academic Agent]
    AgentRouter --> EmailAgent[Email Agent]
    AgentRouter --> PlacementAgent[Placement Agent]
    AgentRouter --> PortfolioAgent[Portfolio Agent]

    AcademicAgent --> OCRTool[OCR / Document Parse Tool]
    AcademicAgent --> TaskGenTool[Task Generator Tool]

    EmailAgent --> FilterTool[Email Classifier Tool]
    EmailAgent --> ExamDetectTool[Exam Extractor Tool]

    PlacementAgent --> ATSTool[ATS Evaluator Tool]
    PlacementAgent --> SkillGapTool[Skill Gap Analysis Tool]

    PortfolioAgent --> SiteGenTool[Site Compiler Tool]
    PortfolioAgent --> GitHubDeployTool[GitHub Pages Deployment Tool]
```

---

## 6. Memory Architecture

Personal Academic Memory combines structured entity persistence with semantic vector indexing for hybrid retrieval.

```mermaid
graph TD
    InputData[User Input / Extracted Document] --> StructuredExtractor[Structured Entity Extractor]
    InputData --> TextEmbedder[Lightweight Embedding Model 384-d]

    StructuredExtractor --> RoomStore[(Room Encrypted DB)]
    TextEmbedder --> VectorIndex[(SQLite-VSS Vector Index)]

    UserQuery[User Inquiry] --> QueryEmbedder[Query Text Embedder]
    QueryEmbedder --> VectorSearch[Cosine Vector Top-K Search]
    VectorSearch --> ContextMerger[Context Merger]
    RoomStore --> ContextMerger
    ContextMerger --> LLMPrompt[Inject into Gemma Context Window]
```

---

## 7. Database Architecture

The Room database (`helply_database.db`) uses strict foreign keys, index optimization, and SQLCipher AES-256 encryption.

```mermaid
erDiagram
    STUDENT_PROFILE ||--o{ ACADEMIC_MEMORY : owns
    ACADEMIC_MEMORY ||--o{ MEMORY_EVIDENCE : contains
    ACADEMIC_MEMORY ||--o{ PORTFOLIO_PROJECT : promotes
    ASSIGNMENT ||--o{ TASK_ITEM : generates
    PLACEMENT_COMPANY ||--o{ RESUME_VERSION : targets
    EMAIL_MESSAGE ||--o{ EXAM : triggers
    EXAM ||--o{ CALENDAR_EVENT : schedules

    STUDENT_PROFILE {
        string id PK
        string name
        string email
        string college
        string degree
    }

    ACADEMIC_MEMORY {
        string id PK
        string type
        string title
        string description
        float confidence_score
        boolean verified_status
    }

    ASSIGNMENT {
        string id PK
        string subject
        string requirements
        datetime deadline
        string status
    }

    RESUME_VERSION {
        string id PK
        string version_name
        string target_company
        float estimated_ats_score
    }
```

---

## 8. Email Architecture

Gmail / Outlook communication processing relies on OAuth 2.0 PKCE with local heuristic filters before invocation of Gemma classifier logic.

```mermaid
graph LR
    OAuth[AppAuth OAuth 2.0 PKCE] --> TokenStore[KeyStore Encrypted Store]
    TokenStore --> MailFetcher[Background Email Poller]
    MailFetcher --> HeaderFilter[Local Fast Heuristic Filter]
    HeaderFilter -- Suspicious / Irrelevant --> Purge[Ignore]
    HeaderFilter -- Academic Match --> GemmaClassifier[Gemma AI Classifier]
    GemmaClassifier --> CategoryOutput[Classify Category & Priority]
    CategoryOutput --> EventCreation[Schedule Notification & Calendar Event]
```

---

## 9. Calendar Architecture

Integrates bi-directionally with Android native `CalendarContract` and cloud providers.

```mermaid
graph TD
    CalendarSync[Calendar Intelligence Agent] --> NativeProvider[Android CalendarProvider]
    CalendarSync --> CloudCalendar[Google Calendar API]
    NativeProvider --> EventMonitor[Post-Event Monitor]
    EventMonitor --> PortfolioWorthiness[Evaluate Portfolio Worthiness Score]
    PortfolioWorthiness -- Score > Threshold --> PromptUser[Prompt: Add to Portfolio?]
```

---

## 10. GitHub Architecture

Uses minimum scope OAuth (`repo`, `user`) to verify developer activity, code statistics, and orchestrate web portfolio deployments.

```mermaid
graph TD
    GitHubAuth[GitHub OAuth PKCE] --> GitHubAPI[GitHub REST API]
    GitHubAPI --> RepoParser[Repository Metrics Parser]
    RepoParser --> SkillVerification[Verify Developer Tech Stack Evidence]
    SkillVerification --> AcademicMemory[(Academic Memory Update)]
    GitHubAPI --> RepoCreator[Repository Builder]
    RepoCreator --> ActionsSetup[GitHub Actions Deployment Config]
    ActionsSetup --> PagesHost[GitHub Pages Hosting]
```

---

## 11. Resume Architecture

Parses PDF and DOCX files into structured internal data schemas and evaluates job description alignment.

```mermaid
graph TD
    ImportedDoc[PDF / DOCX Resume] --> Parser[Document Structure Parser]
    Parser --> StructuredSchema[JSON Resume Representation]
    StructuredSchema --> JobMatcher[Placement Copilot Matcher]
    JobMatcher --> ATSScore[Estimated ATS Calculator]
    ATSScore --> SkillGaps[Generate Skill Gap Recommendations]
    SkillGaps --> UpdatedResume[Save Version-Controlled Resume]
```

---

## 12. Portfolio Architecture

Compiles structured academic memories into production-ready responsive static websites supporting 8 visual themes.

```mermaid
graph TD
    Memories[(Verified Academic Memories)] --> DataBinder[Portfolio JSON Data Binder]
    DataBinder --> SelectedTheme[Theme Compiler (e.g. Modern Developer)]
    SelectedTheme --> StaticBundle[Generate HTML5 / Vanilla CSS / JS]
    StaticBundle --> Previewer[In-App WebView Dynamic Preview]
    Previewer -- User Approved --> Deployment[GitHub Pages Deploy Engine]
```

---

## 13. Office Kit Architecture

Pairs mobile device with local desktop node via mDNS and encrypted WebSockets on local WiFi.

```mermaid
graph LR
    Phone[Helply Android Phone] <== mDNS Discovery ==> Laptop[Office Kit Desktop Node]
    Phone -- Encrypted WebSocket --> TaskPayload[Heavy Task Payload: PPT / Doc Generation]
    TaskPayload --> LaptopEngine[Desktop Compute Engine]
    LaptopEngine -- Rendered Asset --> Phone
```

---

## 14. Security Architecture

Enforces defense-in-depth security across authentication, storage, and AI tool execution.

```mermaid
graph TD
    subgraph Data at Rest
        KeyStore[Android Hardware KeyStore] --> EncryptedDB[Room DB with SQLCipher AES-256]
        KeyStore --> EncryptedPrefs[EncryptedSharedPreferences]
    end

    subgraph Data in Motion
        TLS[TLS 1.3 Encryption for Web APIs]
        PKCE[OAuth 2.0 PKCE Authentication]
    end

    subgraph AI Safety
        Sanitizer[Prompt Input Sanitizer] --> ToolAuth[Kotlin Tool Permission Check]
        ToolAuth --> DeterministicValidator[Schema & Logic Validator]
    end
```

---

## 15. Data Flow

End-to-end data trajectory from document ingestion to portfolio publication:

```
[Assignment / Email / GitHub]
            │
            ▼
   [Local Parsing & OCR]
            │
            ▼
 [Gemma AI Context Analysis]
            │
            ▼
[Personal Academic Memory (Room)]
            │
            ▼
 [Placement & Resume Alignment]
            │
            ▼
 [Dynamic Portfolio Compilation]
            │
            ▼
  [GitHub Pages Deployment]
```

---

## 16. Network Boundaries

- **Strict Offline Boundary**: All memory operations, local assignment parsing, resume analysis, and Gemma inference run without external network traffic.
- **Controlled Network Calls**: Network communication occurs exclusively when initiating OAuth authentication, fetching email headers, reading GitHub repos, or triggering GitHub Pages deployment over HTTPS.

---

## 17. Offline Architecture

```mermaid
graph TD
    OfflineTrigger[No Active Network Connection] --> LocalFallback[Enforce Offline Mode]
    LocalFallback --> Disables[Disable External Email / GitHub Sync]
    LocalFallback --> Enables[Enable On-Device LiteRT LLM & Local DB]
    Enables --> LocalOps[Local Academic Memory, Assignment Analysis, Local Resume ATS, Voice Intent Parsing]
```

---

## 18. Deployment Architecture

```mermaid
graph TD
    AppBuild[Helply Gradle Build] --> Proguard[R8 ProGuard Code Obfuscation]
    Proguard --> SignedAPK[Signed Release APK / App Bundle]
    SignedAPK --> TargetDevice[Android ARM64 Devices (Android 9.0+)]
```
