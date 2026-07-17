# GATE MockAI: Technology Stack, Architecture & Troubleshooting Guide

This document provides a concise overview of the technologies used in the **GATE MockAI** project, the rationale behind their selection, their versions, and a record of the technical problems we faced and resolved during development.

---

## 1. Core Technology Stack & Versions

The platform is designed around a split-architecture Web application featuring a modern React frontend and a robust Spring Boot REST API. Below are the details of the service tiers and versions:

### Containerized Infrastructure (Docker)
We use **Docker Compose** to spin up and orchestrate the local infrastructure.
* **Database: PostgreSQL 16 + `pgvector`** (`pgvector/pgvector:pg16`)
  * *Why*: Rather than integrating a separate, costly, and complex vector database (like Pinecone, Milvus, or Chroma), we use the `pgvector` extension directly in PostgreSQL. This allows us to store relational schemas (users, test attempts, results) and 768-dimensional text embeddings in a single, ACID-compliant database.
* **LLM Engine: Ollama Local Server** (`ollama/ollama:latest`)
  * *Why*: Running LLM inference locally ensures 100% data privacy for assessment materials, has zero recurring API token costs during heavy batch ingestion, and operates offline.
  * *Models*:
    * **Text/Code Model**: `qwen2.5-coder:7b` (used for code-heavy transcriptions, questions, and explanations).
    * **Embedding Model**: `nomic-embed-text` (produces 768-dimensional vector embeddings for semantic search).
* **Cache & Session: Redis 7** (`redis:7-alpine` - optional under `redis` profile)
  * *Why*: Provides high-performance session validation and caching. Utilizes AOF (Append-Only File) persistence to survive container restarts.

### Backend Tier (Spring Boot)
* **Framework**: Spring Boot `3.3.4` (Java `17`)
* **AI Core**: Spring AI `1.1.1` (manages communication with Ollama and maps vector operations to PostgreSQL).
* **Database Migrations**: Flyway Core (enforces version-controlled, reproducible SQL database updates).
* **Session Store**: Spring Session JDBC (persists active login session keys inside PostgreSQL table `SPRING_SESSION`).
* **Security**: Spring Security 6 (secures REST endpoints, manages cookie serialization, hashes passwords using BCrypt, and performs role-based routing).

### Cloud AI Engine
* **Model**: Google Gemini (`gemini-2.5-flash`)
  * *Why*: Used for high-fidelity multi-modal PDF parsing and digitizing. Gemini's massive context window (up to 1M+ tokens) allows us to send entire 65-question PDFs in a single call, replacing brittle local chunking logic.

### Client Tier (React SPA)
* **Build System**: Vite (high-speed bundling).
* **Styling**: Vanilla CSS & TailwindCSS (via `@import` directives).
* **Core Components**: React Router DOM (client-side routing), NTA-style Exam Console (mimics official Indian National Testing Agency simulator).

---

## 2. Advanced Architectural Features

* **3-Page Overlapping Chunking**: Slices PDFs using a moving window (e.g., Pages 1–3, 3–5) to ensure questions that span across page breaks are not truncated.
* **HyDE (Hypothetical Document Embeddings)**: Converts a short topic search query (e.g., "Deadlock Prevention") into a fully fleshed-out hypothetical exam question before embedding. This hypothetical text embeds closer to actual exam questions in the vector database than a simple search term, improving similarity matching.
* **Multi-Query Fusion**: Generates 3 paraphrased variations of a search topic, retrieves vector matches for each, deduplicates the results, and picks the highest-ranking questions.
* **HNSW Index Optimization**: Configures `SET LOCAL hnsw.ef_search = 64` before each vector retrieval to widen the candidate search pool for maximum recall.

---

## 3. Problems Faced & How They Were Solved

### Problem 1: LaTeX Backslashes Breaking JSON Serialization
* **Symptom**: GATE CS questions contain extensive mathematical formulas, sets, and formatting written in LaTeX (e.g., `\sum`, `\theta`, `\cup`). When Gemini or Ollama returned these symbols inside JSON strings, standard JSON parsers failed to serialize/deserialize them, raising syntax and parsing exceptions.
* **Root Cause**: Unescaped backslashes in non-standard JSON escape sequences (e.g., `\t` is a tab, but `\theta` is invalid JSON).
* **Solution**: Implemented a custom helper method `escapeJsonBackslashes(String json)` in the ingestion controllers. It loops through characters and checks if a backslash initiates a valid JSON escape sequence (`\"`, `\\`, `\/`, `\n`, etc.). If it is a mathematical LaTeX command, it escapes the backslash (`\\theta`), converting it into a valid JSON string while preserving LaTeX renderer compatibility in the frontend.

### Problem 2: RestClient & Connection Timeouts during LLM Inference
* **Symptom**: Transcribing complete papers or generating mock tests takes several minutes. The default HTTP clients in Spring Boot threw `SocketTimeoutException` or `ResourceAccessException` and aborted mid-generation.
* **Root Cause**: Spring AI's default RestClient timeout config is too low for heavy local LLM generation or complex PDF tasks.
* **Solution**: 
  1. Manually declared the `OllamaApi` bean in `AppConfig.java` and injected a custom `SimpleClientHttpRequestFactory` with a `connectTimeout` of **30 seconds** and a `readTimeout` of **10 minutes** (`600,000ms`).
  2. Increased Spring MVC's async request timeout in `application.yml` via `spring.mvc.async.request-timeout: 180000` (3 minutes) to support Server-Sent Events (SSE) progress streams.

### Problem 3: Ollama GPU/VRAM OOM Crashes under Concurrency
* **Symptom**: Generating descriptions or transcribing multiple chunks concurrently in parallel worker threads caused the host machine's VRAM to run out of memory, causing Ollama to crash.
* **Root Cause**: Too many concurrent model inference streams running simultaneously.
* **Solution**: Registered a dedicated `ExecutorService` bean named `ollamaChunkExecutor` configured as a fixed thread pool of size 3 (`Executors.newFixedThreadPool(3)`). This limits concurrent calls to Ollama to exactly 3 at any given time, achieving a ~3x speedup in ingestion throughput compared to sequential execution, without crashing the local GPU.

### Problem 4: Duplicate Questions from Overlapping Windows
* **Symptom**: Using the overlapping 3-page window strategy to prevent split questions resulted in duplicate copies of questions being indexed.
* **Root Cause**: The same question was parsed in consecutive page overlapping blocks.
* **Solution**: Developed a programmatic post-processing alignment layer in Java. The system groups generated questions by `Section` (GA/CS) and `SequenceNo`, and deduplicates them by selecting the candidate containing the longest question text and option count.

### Problem 5: Session Loss on Server Rebuilds
* **Symptom**: During local development, editing backend code triggered hot-rebuilds, which immediately logged out the administrator or student, interrupting testing.
* **Root Cause**: Default Spring Security session state is stored in-memory (JVM heap) and is cleared on server restart.
* **Solution**: Integrated **Spring Session JDBC**. Active sessions are now written to PostgreSQL tables (`SPRING_SESSION`). Sessions persist across backend restarts, and a custom cookie serializer configures browser session cookies to remain active for **30 days**.

---

## 4. Docker Interview Prep Guide (Q&A)

Use this guide to confidently explain the use and architectural purpose of Docker in your project during interviews:

### Q1: "Where did you use Docker in your project?"
**Answer**:
"I used Docker to containerize and orchestrate all the core backing services of the application. Instead of running these services directly on my host system, I ran them inside lightweight Docker containers managed by a single `docker-compose.yml` file. Specifically, I containerized:
1. **PostgreSQL 16 with the `pgvector` extension** to handle relational data (users, mock tests, attempts) and 768-dimensional text embeddings in a single database.
2. **Ollama Local LLM server** to load and run LLM models (`qwen2.5-coder:7b` for text generation and `nomic-embed-text` for embedding generation).
3. **Redis 7** (configured with AOF persistence) to serve as a fast session cache and data store."

### Q2: "Why did you use Docker? What value did it add?"
**Answer**:
"I chose Docker for four primary engineering reasons:
1. **Environment Consistency and Zero-Setup Onboarding**: PostgreSQL and local LLM runtimes have complex system-level dependencies. By using Docker, we avoid the 'works on my machine' syndrome. Any new developer can spin up the entire database, Redis cache, and Ollama server with a single command: `docker compose up -d`.
2. **Simplified Vector Store Setup (`pgvector`)**: Vanilla PostgreSQL does not support vector embeddings out of the box. Building the `pgvector` extension from source on a developer's host machine requires compilation tools and can be very error-prone. Pulling the pre-built `pgvector/pgvector:pg16` Docker image gives us an optimized vector database instantly with zero manual configuration.
3. **Resource Isolation & GPU Access**: Running Ollama in a container isolates heavy model execution from host processes. We can mount host storage volumes to save model weights (`ollama_data`), keeping our workspace clean.
4. **Data Persistence**: We used named volumes (like `gate_pgdata` and `gate_redis_data`) so that even if the containers are stopped, destroyed, or upgraded, our testing database records, user history, and session states are safely persisted on the host disk."

### Q3: "How did you use Docker? Explain the setup."
**Answer**:
"We implemented it using **Docker Compose** (`docker-compose.yml`). The file defines three services:
* **`postgres`**: Configured using `pgvector/pgvector:pg16`, mapping container port `5432` to host port `5439` (to prevent conflicts with any pre-existing PostgreSQL instances on the developers' machines) and mounting a named volume `gate_pgdata`.
* **`ollama`**: Bound to host port `11434` and configured with a bind mount `./ollama_data:/root/.ollama` so that once model weights are pulled (e.g. via `ollama pull`), they remain cached on the host system.
* **`redis`**: Configured with Alpine-based Redis and ran with command overrides `redis-server --appendonly yes --appendfsync everysec` to enable AOF (Append-Only File) logging for crash recovery.

In our Spring Boot backend, we integrated these services simply by pointing our connection strings to the mapped host ports (e.g., `jdbc:postgresql://localhost:5439/gate_db` for database and `http://localhost:11434` for Ollama)."

