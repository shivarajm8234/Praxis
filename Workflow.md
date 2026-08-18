# Helply Workflows Documentation

This document outlines the detailed end-to-end workflows for all primary operations supported by the **Helply - Student AI Operating System**.

---

## 1. First Launch Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Jetpack Compose UI
    participant System as System Check UseCase
    participant KeyStore as Android KeyStore
    participant DB as Room Database

    Student->>UI: Launch Helply App
    UI->>System: Check First Launch Status & Permissions
    System->>KeyStore: Verify Security Keys Initialization
    alt First Launch Detected
        System->>DB: Initialize Default Schema & Preference Store
        System-->>UI: Navigate to Onboarding & Model Download Screen
    else Subsequent Launch
        System->>DB: Load Existing Student Profile & Academic Memory
        System-->>UI: Navigate to Home Dashboard
    end
```

---

## 2. AI Model Installation Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Onboarding UI
    participant Manager as Model Manager
    participant Storage as File Storage
    participant LiteRT as LiteRT AI Runtime

    Student->>UI: Click "Download Gemma 4 E4B (2.2 GB)"
    UI->>Manager: Initiate Storage Pre-check
    Manager->>Storage: Check Available Space (Req: >= 4.5 GB)
    alt Storage Sufficient
        Manager->>Manager: Start Resumeable Download over HTTPS
        loop Progress Update
            Manager-->>UI: Emit Download Progress (e.g. 45%)
        end
        Manager->>Storage: Save Model File (`gemma-4-e4b-q4.bin`)
        Manager->>Manager: Compute SHA-256 Checksum
        alt SHA-256 Valid
            Manager->>LiteRT: Warmup Native OpenCL GPU Delegate
            LiteRT-->>Manager: Initialized OK
            Manager-->>UI: Model Ready Signal -> Proceed to Onboarding
        else Hash Mismatch
            Manager-->>UI: Error: Checksum Failed. Prompt Retry.
        end
    else Storage Insufficient
        Manager-->>UI: Error: Insufficient Storage. Show Storage Manager Dialog.
    end
```

---

## 3. Student Onboarding Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Onboarding UI
    participant VM as Onboarding ViewModel
    participant Memory as Personal Academic Memory
    participant DB as Room Database

    Student->>UI: Enter Basic Details (Name, College, Major, Graduation Year)
    Student->>UI: Select Initial Academic Interests & Career Target
    UI->>VM: Submit Onboarding Payload
    VM->>Memory: Convert Profile into Structured Base Memory Records
    Memory->>DB: Persist Profile & Memory Records
    VM-->>UI: Onboarding Complete -> Navigate to Home Dashboard
```

---

## 4. Memory Creation Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Memory UI
    participant Extractor as Memory Extraction Engine
    participant Gemma as Gemma 4 E4B LLM
    participant DB as Room Database
    participant Vector as Local Vector Store

    Student->>UI: Input Project / Workshop / Skill Detail
    UI->>Extractor: Process Raw Input Text / Files
    Extractor->>Gemma: Request Structured Memory Extraction (JSON Schema)
    Gemma-->>Extractor: Return JSON (Title, Tags, Evidence, Confidence)
    Extractor->>DB: Insert `academic_memory` Entity
    Extractor->>Vector: Generate & Save 384-d Embedding Vector
    Extractor-->>UI: Memory Item Added & Tagged
```

---

## 5. Assignment Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Academics UI
    participant OCR as ML Kit OCR Engine
    participant Gemma as Gemma 4 E4B
    participant Planner as Task Planner
    participant DB as Room DB

    Student->>UI: Upload Assignment (Camera / PDF / Image)
    UI->>OCR: Extract Raw Text from Document
    OCR-->>Gemma: Pass Raw Text + Extraction Prompt
    Gemma-->>Gemma: Parse Subject, Requirements, Deadline, Deliverables
    Gemma-->>Planner: Output Structured JSON Requirements
    Planner->>DB: Save Assignment & Breakdown Tasks
    Planner-->>UI: Display Assignment Breakdown & Add Reminders
```

---

## 6. Research Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Research Assistance UI
    participant Agent as Academic Research Agent
    participant Memory as Personal Academic Memory
    participant Gemma as Gemma 4 E4B

    Student->>UI: Initiate Research Request on Assignment Topic
    UI->>Agent: Create Research Plan
    Agent->>Memory: Query Related Academic Notes & Past Projects
    Memory-->>Agent: Relevant Context Records
    Agent->>Gemma: Synthesize Literature Review & Structured Outline
    Gemma-->>Agent: Detailed Outline + Citation References
    Agent-->>UI: Display Interactive Research Plan with Distinguishment of Verified vs Generated Info
```

---

## 7. PPT Generation Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Academics UI
    participant Agent as Document Generation Agent
    participant Bridge as Office Kit Bridge (Optional)
    participant Generator as PPT Builder Library

    Student->>UI: Click "Generate Presentation (.pptx)"
    UI->>Agent: Request PPT Blueprint
    alt Laptop Bridge Connected
        Agent->>Bridge: Send Heavy PPT Generation Task to Laptop
        Bridge-->>Agent: Return Generated `.pptx` File Path
    else Local Mobile Mode
        Agent->>Generator: Compile Structured Slides into `.pptx`
    end
    Agent-->>UI: Return Downloadable & Previewable PPT File
```

---

## 8. PDF Generation Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Academics UI
    participant Generator as PDF Compiler Engine
    participant Memory as Personal Academic Memory

    Student->>UI: Request Assignment Report PDF Export
    UI->>Generator: Generate Document Layout
    Generator->>Memory: Attach Verified Citations & Student Header Metadata
    Generator->>Generator: Render Android `PdfDocument` Canvas
    Generator-->>UI: Export Completed PDF to Local Downloads
```

---

## 9. Email Ingestion Workflow

```mermaid
sequenceDiagram
    autonumber
    participant Gmail as Gmail / Outlook API
    participant Ingestion as Background Email Agent
    participant Classifier as Gemma Text Classifier
    participant DB as Room DB
    participant Notif as Notification Manager

    Ingestion->>Gmail: Poll Unread Emails (OAuth 2.0 PKCE)
    Gmail-->>Ingestion: Return Academic Circular Headers & Snippets
    Ingestion->>Classifier: Pass Email Snippet for Classification
    Classifier-->>Ingestion: Return Category (EXAMINATION, Urgent) & Priority
    Ingestion->>DB: Save Processed Email Record
    Ingestion->>Notif: Trigger High-Priority System Notification
```

---

## 10. Exam Detection Workflow

```mermaid
sequenceDiagram
    autonumber
    participant Agent as Email Agent
    participant Parser as Exam Circular Parser
    participant Planner as Preparation Timeline Planner
    participant Calendar as Calendar Provider
    participant DB as Room DB

    Agent->>Parser: Parse Email Payload ("END SEMESTER EXAMINATION")
    Parser->>Parser: Extract Subject, Start Date, End Date, Venue
    Parser->>Planner: Generate Day-by-Day Study Roadmap
    Planner->>DB: Save Exam Entities & Daily Revision Tasks
    Planner->>Calendar: Insert Exam Dates into System Calendar
```

---

## 11. Notification Workflow

```mermaid
sequenceDiagram
    autonumber
    participant System as Helply Core Engine
    participant Filter as Priority & Spam Filter
    participant Notif as Android Notification Engine
    actor Student

    System->>Filter: Evaluate Pending Event Notification
    alt Category == CRITICAL / HIGH
        Filter->>Notif: Trigger Actionable Notification with Custom Action Buttons
        Notif-->>Student: Display High Priority Alert (e.g., "DBMS Exam in 5 Days")
    else Category == LOW
        Filter->>Notif: Queue in Silent Daily Summary Batch
    end
```

---

## 12. Focus Mode Workflow

```mermaid
sequenceDiagram
    autonumber
    participant System as Exam Focus Engine
    participant Perms as Permission Validator
    participant DND as Android NotificationManager (DND)
    actor Student

    System->>Perms: Check `ACCESS_NOTIFICATION_POLICY` Permission
    alt Permission Granted
        System->>DND: Set Interruption Filter to Priority Only during Exam Session
        DND-->>Student: Focus Mode Activated Badge Displayed
    else Permission Denied
        System-->>Student: Request Permission Dialog with Privacy Explanation
    end
```

---

## 13. Placement Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Placement Copilot UI
    participant Agent as Placement Agent
    participant Memory as Personal Academic Memory
    participant Gemma as Gemma 4 E4B

    Student->>UI: Enter Target Company & Paste Job Description
    UI->>Agent: Analyze Role Eligibility & Skill Match
    Agent->>Memory: Query Verified Student Skills & Projects
    Agent->>Gemma: Compare Job Requirements vs Memory
    Gemma-->>Agent: Output Skill Gap Analysis, Missing Keywords, & Roadmap
    Agent-->>UI: Display Interactive Placement Dashboard
```

---

## 14. Resume Analysis Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Resume UI
    participant Parser as Resume Parser (PDF/DOCX)
    participant DB as Room DB

    Student->>UI: Import Resume Document
    UI->>Parser: Extract Text & Structure
    Parser->>Parser: Parse Contact, Work, Education, Projects, Skills
    Parser->>DB: Save New Version (`resume_version` V1/V2)
    Parser-->>UI: Render Parsed Resume Breakdown & Section Preview
```

---

## 15. ATS Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Resume UI
    participant Engine as Estimated ATS Engine
    participant Vector as Local Vector Engine

    Student->>UI: Click "Calculate ATS Compatibility for Job X"
    UI->>Engine: Pass Resume Text + Job Description
    Engine->>Vector: Compute Cosine Semantic Similarity Score
    Engine->>Engine: Run Keyword Matching Matrix
    Engine-->>UI: Render "Estimated ATS Score: 84%" with Transparent Breakdown
```

---

## 16. GitHub OAuth Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Settings UI
    participant Auth as AppAuth PKCE Engine
    participant GitHub as GitHub OAuth Server
    participant KeyStore as Encrypted Token Store

    Student->>UI: Click "Connect GitHub"
    UI->>Auth: Initiate Authorization Request (`repo`, `user` scopes)
    Auth->>GitHub: Redirect User to GitHub Authorization Page
    GitHub-->>Auth: Callback with Authorization Code
    Auth->>GitHub: Exchange Code for Access Token
    GitHub-->>Auth: Access Token Returned
    Auth->>KeyStore: Store Token securely in EncryptedSharedPreferences
    Auth-->>UI: Display "GitHub Connected Successfully"
```

---

## 17. GitHub Analysis Workflow

```mermaid
sequenceDiagram
    autonumber
    participant Engine as GitHub Intelligence Engine
    participant API as GitHub REST API
    participant Memory as Personal Academic Memory
    participant DB as Room DB

    Engine->>API: Fetch User Public Repositories, Star Counts, & READMEs
    API-->>Engine: Return Repositories JSON
    Engine->>Engine: Parse Primary Tech Stack & Language Distributions
    Engine->>Memory: Map High-Quality Repositories to `academic_memory`
    Engine->>DB: Update Portfolio-Eligible Project Records
```

---

## 18. Portfolio Generation Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Portfolio UI
    participant Engine as Portfolio Generator Engine
    participant Memory as Personal Academic Memory

    Student->>UI: Select Theme (e.g. "Modern Developer") & Click "Generate Portfolio"
    UI->>Engine: Synthesize Portfolio Structured JSON
    Engine->>Memory: Pull Top-Rated Verified Projects, Certifications, & Skills
    Engine->>Engine: Compile HTML5 / Vanilla CSS Static Site Bundle
    Engine-->>UI: Display Interactive In-App Portfolio Live Preview
```

---

## 19. Calendar Event Detection Workflow

```mermaid
sequenceDiagram
    autonumber
    participant Calendar as Calendar Sync Engine
    participant Evaluator as Portfolio Worthiness Evaluator
    participant Notif as Notification Engine
    actor Student

    Calendar->>Calendar: Detect Event Completion (e.g., "Smart City Hackathon")
    Calendar->>Evaluator: Evaluate Event Type & Metadata
    Evaluator->>Evaluator: Calculate Worthiness Score (Score > Threshold)
    Evaluator->>Notif: Trigger Prompt: "High-value portfolio update detected."
    Notif-->>Student: Display Notification with [Add to Portfolio] [Skip]
```

---

## 20. Portfolio Update Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Portfolio Manager UI
    participant Engine as Portfolio Engine
    participant DB as Room DB

    Student->>UI: Tap "Add Hackathon Win to Portfolio"
    UI->>Engine: Update Structured Portfolio JSON
    Engine->>DB: Save New Portfolio Version
    Engine-->>UI: Show Instant Dynamic Preview Update
```

---

## 21. Image Upload Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Portfolio UI
    participant Compressor as Image Optimization Engine
    participant Storage as App Local Storage

    Student->>UI: Select Certificate / Hackathon Photo (Optional)
    UI->>Compressor: Downscale & Compress Image (WebP format)
    Compressor->>Storage: Store Optimized Asset in `portfolio_assets/`
    Compressor-->>UI: Display Image Thumbnail in Portfolio Editor
```

---

## 22. GitHub Repository Creation Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Portfolio UI
    participant Deployer as GitHub Deployment Manager
    participant API as GitHub REST API

    Student->>UI: Confirm "Create & Deploy Repository"
    UI->>Deployer: Request Repo Creation (`username.github.io` or `student-portfolio`)
    Deployer->>API: POST /user/repos
    API-->>Deployer: Repository Created Successfully
    Deployer-->>UI: Step 1 Complete: Repo Ready
```

---

## 23. Portfolio Deployment Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant Deployer as GitHub Deployment Manager
    participant API as GitHub REST API
    participant Pages as GitHub Pages Service
    participant UI as Portfolio UI

    Deployer->>API: Upload Static HTML/CSS/JS Files via Git Tree API
    Deployer->>API: Commit `.github/workflows/deploy.yml`
    Deployer->>API: Enable GitHub Pages on `main` branch
    Pages-->>Deployer: Deployment Started
    Deployer-->>UI: Show Live Deployment Status & Production URL (`https://user.github.io/portfolio`)
```

---

## 24. Offline Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Helply UI
    participant Detector as Network Monitor
    participant LiteRT as Local Gemma 4 E4B Engine
    participant DB as Room DB

    Detector-->>UI: Offline Mode Detected
    UI->>UI: Display Offline Badge in Header
    Student->>UI: Perform Query / Assignment Parsing / Local Resume ATS
    UI->>LiteRT: Pass Request to On-Device LiteRT LLM
    LiteRT->>DB: Retrieve Local Academic Memory Context
    LiteRT-->>UI: Return Instant Local Response
```

---

## 25. Error Recovery Workflow

```mermaid
sequenceDiagram
    autonumber
    participant Component as Any Engine Component
    participant Handler as Sealed Error Handler
    participant UI as Jetpack Compose UI
    actor Student

    Component->>Handler: Throw `HelplyException.ModelMemoryPressure`
    Handler->>Handler: Log Diagnostic Stack Trace
    Handler-->>UI: Emit Actionable UI Error State
    UI-->>Student: Display Card: "Gemma unloaded to free memory. Tap [Reload]."
    Student->>UI: Tap [Reload] Button
    UI->>Component: Re-initialize with Fresh Memory Allocated
```

---

## 26. Memory Deletion Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant UI as Privacy & Settings UI
    participant Memory as Memory Engine
    participant DB as Room DB
    participant Vector as Local Vector Store

    Student->>UI: Select "Forget Memory Item" or "Delete All Memories"
    UI->>UI: Prompt Confirmation Dialog
    Student->>UI: Confirm Deletion
    UI->>Memory: Delete Target Memory Records
    Memory->>DB: Execute DELETE query on `academic_memory` & `memory_evidence`
    Memory->>Vector: Remove Corresponding Vector Embeddings
    Memory-->>UI: Display Toast: "Memories permanently purged."
```
