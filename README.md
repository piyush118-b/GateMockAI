# GATE MockAI!
### AI-Powered Academic Assessment & Smart RAG Mock Exam Platform

Welcome to **GATE MockAI**, a production-grade, AI-aligned assessment platform built to ingest, draft, manage, and simulate syllabus-aligned GATE (Graduate Aptitude Test in Engineering) Mock Exams. By combining a split-architecture **React Single Page Application (SPA)** with a **Spring Boot REST API** and a **Retrieval-Augmented Generation (RAG)** pipeline powered by local LLMs (via Ollama), the system provides an end-to-end sandbox for creating, indexing, and emulating high-fidelity, NTA-style examinations.

---

## 🚀 Key Features

* **High-Fidelity NTA Emulation**: A faithful replica of the National Testing Agency (NTA) exam console, complete with interactive question palettes, virtual calculator, section navigation (General Aptitude & Computer Science), countdown timers, and strict submission rules.
* **Semantic RAG Ingestion Pipeline**: Ingests official GATE past papers from PDFs using a **3-page overlapping chunking window** to ensure questions spanning page boundaries are captured without truncation.
* **Advanced Answer Key Parser**: A Java-based regex parser that extracts answers, scoring models, and tolerances from official keys, linking options and NAT values programmatically.
* **Semantic Search Generation**: Similarity search powered by local vector embedding maps the admin's syllabus configuration to database context, grounding LLM exam generation in real historical questions.
* **Persistent Session Store**: Spring Session backed by JDBC (PostgreSQL) preserves administrator and student login sessions across application restarts or rebuilds.

---

## 🏗️ System Architecture

GATE MockAI is built as a split-architecture Web application utilizing a modern single-page React frontend, a Spring Boot backend API, a PostgreSQL database (supporting `pgvector` for embeddings), and a local Ollama AI engine.

### Visual Architecture Diagram
Below is the system architecture showing how each tier functions and interfaces with the next:

![System Architecture](file:///Users/piyush_18/Desktop/Projects/Java_SpringBoot_Project/GateMockAI/docs/images/system_architecture.png)

### Architecture Schema (Mermaid Layout)
For programmatic reference and rendering in Markdown viewers:

```mermaid
graph TD
    subgraph Client ["Client Tier (React SPA)"]
        UI["React SPA (Vite / React Router)"]
        Console["NTA Exam Console (Student)"]
        Dashboard["Admin / Student Dashboards"]
    end

    subgraph API ["Backend Service Tier (Spring Boot)"]
        Security["Spring Security (Auth & JDBC Session Guard)"]
        AdminController["Admin controllers (RAG, Editor, Stats)"]
        StudentController["Student controllers (Exam console, Results)"]
        RAGService["RagIngestionService (Embeddings / Vector Store)"]
        GenService["MockTestGenerationService (LLM Orchestrator)"]
        Parser["DocumentParserService (PDFBox / Parser)"]
    end

    subgraph Storage ["Storage Tier (PostgreSQL + PGVector)"]
        DB[("Relational DB (users, attempts, mock_tests)")]
        VS[("Vector Table (gate_vector_store - Cosine distance)")]
    end

    subgraph Models ["LLM Engine Tier (Ollama Local API)"]
        Ollama["Ollama Service (Port 11434)"]
        Qwen["qwen2.5-coder:7b (Transcription & Explanations)"]
        Nomic["nomic-embed-text (768-Dim Vector Embeddings)"]
    end

    UI -->|Proxy requests /api| Security
    Security --> AdminController
    Security --> StudentController
    
    AdminController --> Parser
    AdminController --> RAGService
    AdminController --> GenService
    
    GenService -->|Semantic Search| RAGService
    GenService -->|Generative Prompts| Ollama
    RAGService -->|Embed Batch Docs| Ollama
    
    Ollama --> Qwen
    Ollama --> Nomic
    
    RAGService --> VS
    GenService --> DB
    AdminController --> DB
    StudentController --> DB
```

---

## 📊 Database Schema & Relationships

The database is built on PostgreSQL, schema migrations are version-controlled with Flyway, and session states are persisted to prevent administrative timeouts during local AI inference.

### Core Schema ERD
The entity-relationship diagram below maps all active tables, primary keys, foreign keys, and fields:

![Database Schema](file:///Users/piyush_18/Desktop/Projects/Java_SpringBoot_Project/GateMockAI/docs/images/database_schema.png)

### Schema Code (Mermaid Format)

```mermaid
erDiagram
    users {
        uuid id PK
        varchar email UNIQUE
        text password_hash
        varchar full_name
        varchar role "ADMIN | STUDENT"
        timestamp created_at
    }
    
    mock_tests {
        uuid id PK
        varchar title
        varchar topic
        varchar subject
        varchar branch
        varchar year_label
        integer duration_minutes
        numeric total_marks
        boolean is_published
        timestamp created_at
    }
    
    questions {
        uuid id PK
        uuid test_id FK
        text question_text
        text image_path
        varchar type "MCQ | MSQ | NAT"
        double correct_nat_value
        double nat_tolerance
        numeric marks
        numeric negative_marks
        integer sequence_no
        text explanation
    }
    
    options {
        uuid id PK
        uuid question_id FK
        char option_label "A | B | C | D"
        text option_text
        text image_path
        boolean is_correct
    }
    
    attempts {
        uuid id PK
        uuid user_id FK
        uuid test_id FK
        numeric score
        timestamp started_at
        timestamp submitted_at
        varchar status "IN_PROGRESS | SUBMITTED | TIMED_OUT"
    }
    
    attempt_answers {
        uuid id PK
        uuid attempt_id FK
        uuid question_id FK
        text selected_option_ids "Comma-separated"
        double nat_value_entered
        boolean is_correct
        numeric marks_awarded
    }
    
    branches {
        uuid id PK
        varchar name
        varchar code UNIQUE
    }
    
    branch_subjects {
        uuid id PK
        uuid branch_id FK
        varchar name
        integer default_marks_weightage
        integer display_order
        boolean is_active
    }

    users ||--o{ attempts : places
    mock_tests ||--o{ questions : contains
    mock_tests ||--o{ attempts : receives
    questions ||--o{ options : has
    attempts ||--o{ attempt_answers : contains
    questions ||--o{ attempt_answers : answered-by
    branches ||--o{ branch_subjects : "defines subjects for"
```

### Table Definitions & Purposes

1. **`users`**: Registers user profiles and segregates privileges using role flags (`ROLE_ADMIN` vs. `ROLE_STUDENT`).
2. **`mock_tests`**: Serves as the parent metadata container for an exam session, linking its topic, duration, publishing state, and total compiled marks.
3. **`questions`**: Stores the questions. Supports Multiple Choice (`MCQ`), Multiple Select (`MSQ`), or Numerical Answer Type (`NAT`). Stores grading criteria (`marks`, `negative_marks`), correctness ranges (`correct_nat_value` and `nat_tolerance` for NATs), and detailed explanations.
4. **`options`**: Holds individual options for `MCQ` and `MSQ` questions, using standard labels (A, B, C, D) and a boolean `is_correct` indicator.
5. **`attempts` & `attempt_answers`**: Persists student actions during exam attempts. Tracks live state (answered options, entered numerical values), evaluates responses instantly upon submission, and awards marks based on paper negative marking matrices.
6. **`branches` & `branch_subjects`**: Domain catalog listing branches (e.g., CSE) and corresponding syllabus subjects. Used by generator modules to dynamically distribute subject weightages.
7. **`gate_vector_store`**: Dedicated table instantiated by Spring AI PGVector. Contains the columns:
   - `id`: Unique text chunk identifier.
   - `content`: Combined text corpus containing subject, topic, question text, and academic explanation.
   - `metadata`: JSON payload carrying document attributes (e.g., subject, topic, question type).
   - `embedding`: Vector data type (`vector(768)`) representing the 768-dimensional space generated by the `nomic-embed-text` model.

---

## 🧠 How RAG Works

The Retrieval-Augmented Generation (RAG) implementation consists of two core procedures: **Ingestion & Alignment** (parsing past papers into the vector database) and **Generation** (retrieving relevant context to compile new exams).

---

### Part A: Ingestion & Alignment Pipeline (PDF to PGVector)

This pipeline extracts unstructured past exams, matches options programmatically, and stores clean chunks into PGVector.

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant UI as Admin Dashboard (React)
    participant Ctrl as AdminRagController
    participant Parser as DocumentParserService
    participant LLM as Ollama (Qwen 2.5 Coder)
    participant RAG as RagIngestionService
    participant DB as Postgres & Vector Store

    Admin->>UI: Upload GATE 2020 PDF + Answer Key PDF
    UI->>Ctrl: POST /api/admin/rag/upload (Multipart)
    
    Ctrl->>Parser: parsePdf(AnswerKey)
    Parser-->>Ctrl: Raw Answer Key text
    Ctrl->>Parser: parseAnswerKeyToMap(KeyText)
    Parser-->>Ctrl: Map<Section_QNo, AnswerKeyEntry>

    Ctrl->>Parser: parsePdfPages(QuestionPaper)
    Parser-->>Ctrl: List<String> (individual page text contents)
    
    Note over Ctrl: Chunk pages using a 3-page window<br/>with a 1-page overlap
    
    loop For each 3-page Chunk
        Ctrl->>LLM: Request JSON transcription (Qwen 2.5 Coder)
        LLM-->>Ctrl: JSON with sequenceNo, section, type, text, options
    end

    Note over Ctrl: Programmatic Post-Processing in Java:<br/>1. Deduplicate by Section + QNo (longest text Wins)<br/>2. Bind options, marks, & negative marks from Answer Key Map<br/>3. Re-sort GA (Q1-10) and CS (Q1-55)<br/>4. Re-assign sequential sequenceNo (1 to 65)
    
    Ctrl-->>UI: Return aligned draft JSON
    UI->>Admin: Render Interactive Preview Grid
    Admin->>UI: Click "Confirm & Ingest"
    UI->>Ctrl: POST /api/admin/rag/confirm (draft JSON)
    
    Ctrl->>DB: Save MockTest, Questions, and Options (Relational JPA)
    
    Note over Ctrl, RAG: Convert Questions to Spring AI Document objects:<br/>Include Subject, Topic, Question, Explanation
    
    Ctrl->>RAG: ingestDocumentChunks(Documents)
    RAG->>DB: Write to gate_vector_store (PGVector)
    Ctrl-->>UI: Return Success status
    UI->>Admin: Show Success Toast / Refresh status
```

#### Ingestion Technical Details

* **Overlapping Page Window (3-Page Chunking)**: 
  Instead of slicing the PDF blindly, the system loads pages and groups them into 3-page chunks shifting by 2 pages at a time (e.g., Pages 1–3, 3–5, 5–7). This overlap ensures that if a question starts at the bottom of Page 2 and ends at the top of Page 3, it is fully present in at least one segment block.
* **Simplified LLM Prompts (Transcription Only)**:
  Earlier iterations failed because the LLM tried to parse options, assign marks, and align keys concurrently. This hit context output limits and generated truncated answers. In the new pipeline, the prompt instructs `qwen2.5-coder:7b` strictly to *transcribe* the physical text, options, and question types present in the segment.
* **Programmatic Java Alignment**:
  `DocumentParserService.java` parses the answer key using regular expressions:
  - **Tabular Table rows** (e.g., `1 6 MCQ GA D 1`)
  - **Prefixed manuals** (e.g., `GA 1: D` or `CS 17: 0.125 to 0.125`)
  - **Simple lists** (e.g., `11: A` mapped dynamically to `CS_1` if section is GA context)
  
  The controller maps the transcribed questions to their official answer key entries. Based on the answer key, options are marked `isCorrect`, NAT correct values and tolerances are calculated, and negative marks are assigned (e.g., MCQ 1M = `-0.33`, MCQ 2M = `-0.67`, MSQ/NAT = `0.0`).
* **Length-based Deduplication**:
  Overlapping chunks produce duplicate question copies. The system groups questions by `Section + SequenceNo` and selects the candidate with the longest question text and option count.
* **Sorting & Global Numbering**:
  Questions are sorted putting GA (1 to 10) first, followed by CS (1 to 55). Finally, global sequence numbers are rewritten sequentially from 1 to 65 for the final exam.

---

### Part B: RAG Test Generation Pipeline

When generating mock exams, the system performs dynamic vector searches to ground the AI model with actual GATE questions.

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    participant UI as Admin Dashboard (React)
    participant Ctrl as MockTestGenerationService
    participant RAG as RagIngestionService
    participant DB as Postgres (PGVector)
    participant LLM as Ollama (Local LLM)
    
    Admin->>UI: Select Branch (CSE) & Topic (Operating Systems)
    Admin->>UI: Set Weightages / Count (e.g. 5 MCQ, 2 NAT)
    UI->>Ctrl: POST /api/admin/generate
    
    Ctrl->>RAG: retrieveSimilarQuestions("Operating Systems", topK=5)
    RAG->>DB: Cosine similarity search on gate_vector_store
    DB-->>RAG: Top 5 Document chunks
    RAG-->>Ctrl: List<Document>
    
    Note over Ctrl: Load prompt template (generate_mock_test.st)<br/>Inject retrieved questions into template as context
    
    Ctrl->>LLM: Send context-grounded prompt
    LLM-->>Ctrl: Returns generated mock test questions JSON
    
    Ctrl->>DB: Map DTOs to relational JPA entities & persist
    Ctrl-->>UI: Return Success / Redirect to Editor
```

#### Generation Details
1. **Semantic Search retrieval**: The system queries the `gate_vector_store` using cosine similarity search on the topic query. The database returns the top 5 questions closest to the requested topic in semantic vector space.
2. **Context-Grounded Prompting**: The retrieved questions (including their types, structures, and mathematical formatting) are injected into the LLM system prompt under a `{contextQuestions}` placeholder.
3. **Few-Shot Learning**: The local LLM reads this context to understand the expected complexity, formatting, and notation of real GATE exam questions, minimizing generic and overly simplified AI questions.
4. **Relational Persistence**: The generated test JSON is parsed, mapped to database entities (`mock_tests`, `questions`, `options`), and saved to the relational database.

---

## 🔑 Session & Security Configuration

The platform implements persistent, stateful authentication using Spring Security and JDBC Spring Session.

* **Persistent Sessions**:
  By using `spring.session.store-type: jdbc`, active sessions are saved to the `SPRING_SESSION` table inside PostgreSQL instead of standard in-memory storage. 
  When the backend is rebuilt or restarted (e.g. during development), the admin or student does not lose their session and does not need to re-login.
* **Cookie Serializer**:
  The `SessionConfig` class overrides default cookie properties, setting cookie lifetime to **30 days** (`Max-Age = 2592000` seconds) with a `Lax` SameSite policy, ensuring the authentication state survives browser restarts.
* **Spring Security Rule Set**:
  - `/api/register` is exposed publicly.
  - `/api/admin/**` and `/admin/**` endpoints are protected under the `ADMIN` role.
  - `/api/exam/**`, `/api/student/**`, `/student/**`, and `/exam/**` are protected under the `STUDENT` role.
  - Form logins route users to their respective dashboards based on their role (`customSuccessHandler`).

---

## 🛠️ Technology Stack

* **Frontend**: React (Vite, React Router, TailwindCSS `@import`)
* **Backend**: Spring Boot 3.3.4 (Java 17/25)
* **AI Core**: Spring AI 1.1.1
* **LLM Engine**: Ollama (Local Server)
  - Text Model: `qwen2.5-coder:7b` (used for transcription and generation)
  - Embedding Model: `nomic-embed-text` (768 Dimensions, used for semantic vector database indexing)
* **Database**: PostgreSQL 16 + PGVector Extension
* **Migrations**: Flyway Schema Migrations
* **Sessions**: Spring Session JDBC (PostgreSQL Persistent Store)
* **Security**: Spring Security 6 (Persistent cookies, Role-based routing, BCrypt password hashing)

---

## ⚙️ Quick Start

### 1. Prerequisites
* **Java**: JDK 17 or higher
* **Node.js**: v18+ & npm
* **Docker**: Docker Desktop (for PostgreSQL database container)
* **Ollama**: Installed and running on host/container port `11434`
  ```bash
  # Pull the configured models
  ollama pull qwen2.5-coder:7b
  ollama pull nomic-embed-text
  ```

### 2. Spin up Database and Vector Container
Start the PostgreSQL container on port `5439` using Docker Compose:
```bash
docker compose up -d
```

### 3. Boot Up the Backend
Run the Spring Boot application (will auto-apply Flyway migrations and prepare vector table schemas):
```bash
mvn clean compile spring-boot:run
```

### 4. Boot Up the Frontend Dev Server
Navigate into the `frontend` directory, install dependencies, and launch Vite:
```bash
cd frontend
# Install package dependencies
npm install
# Boot dev server
npm run dev
```

The application is now accessible at **[http://localhost:5173](http://localhost:5173)**.

---

## 🔑 Default Credentials
* **Admin Account**: `admin@gate.com` / `Admin@123`
* **Student Account**: Register a new student account using the signup form (`/register`).
