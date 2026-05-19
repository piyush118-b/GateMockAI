# GATE MockAI - AI-Powered Mock Exam Platform

GATE MockAI is a production-grade, AI-aligned assessment platform built to draft, manage, and simulate syllabus-aligned GATE Mock Exams. The system integrates advanced Spring components with a Retrieval-Augmented Generation (RAG) pipeline to ingestion and store technical questions, delivering dynamic assessment workflows.

## 🚀 Key Features
- **Semantic Vector RAG Pipeline**: Embeds and indexes seed questions using Google's `gemini-embedding-001` model into a JSON-backed simple vector store.
- **AI Generator Interface**: Admin capability to orchestrate target tests dynamically matching syllabus criteria.
- **Role-Based Security**: Complete access partitioning between `ADMIN` and `STUDENT` profiles.
- **System-Adaptive Design**: Dark/light themed responsive user interface with glassmorphic visuals and performance-focused Thymeleaf server-side templates.

---

## 🛠️ Technology Stack
- **Backend Framework**: Spring Boot 3.3.4 (Java 17)
- **AI Core**: Spring AI 1.1.1 BOM
- **LLM/Embeddings**: Google Gemini API (`gemini-2.0-flash`, `gemini-embedding-001`)
- **Database**: PostgreSQL (via Docker Compose)
- **Migrations**: Flyway Schema Migrations
- **Security**: Spring Security 6 (BCrypt hashing, Role-based success routing)
- **UI Engine**: Thymeleaf & Vanilla CSS (Fluid Glassmorphism)

---

## ⚙️ Quick Start

### 1. Prerequisites
- **Java**: JDK 17
- **Docker**: Docker Desktop (for PostgreSQL container)
- **API Key**: Gemini API Key (stored in environment variable `GEMINI_API_KEY`)

### 2. Run Database Container
Spin up the local PostgreSQL database mapped to host port `5439`:
```bash
docker-compose up -d
```

### 3. Build and Run the App
Export your Gemini API Key and run the Spring Boot application:
```bash
export GEMINI_API_KEY="your_api_key_here"
mvn clean compile spring-boot:run
```

Once running, access the portal at [http://localhost:8085](http://localhost:8085).

### 4. Default Seeded Credentials
- **Admin Login**: `admin@gate.com` / `Admin@123`
- **Student Login**: Register via the signup form or use the seeded student if applicable.
