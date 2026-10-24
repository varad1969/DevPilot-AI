# DevPilot Backend Architecture & API Technical Guide

Welcome to the comprehensive technical documentation for the **DevPilot Backend** — a production-grade, Retrieval-Augmented Generation (RAG) system built with **Spring Boot 3**, **Spring AI**, **OpenAI**, and **PostgreSQL + PgVector**.

This document is structured to give you both a deep technical understanding of the implementation and clear, conversational explanations suitable for technical interviews.

---

## 🚀 1. Executive Summary & Simple Terms Explanation

### What is DevPilot?
DevPilot is a **GitHub Code Intelligence System**. It allows developers to connect their GitHub repositories, convert codebase files into vector embeddings, and interact with an AI assistant that answers questions directly referenced from their actual codebase with exact file and line citations.

### How does RAG (Retrieval-Augmented Generation) work in simple terms?
Imagine you ask an expert a question about a giant book they've never read:
1. **Traditional LLM (Without RAG):** The AI guesses based on general knowledge, which leads to hallucinations or out-of-date answers.
2. **DevPilot RAG Approach:**
   - **Indexing Phase:** We read your GitHub repository, chop the code into small readable paragraphs ("chunks"), convert each chunk into a mathematical coordinate ("vector embedding"), and save it in a vector database.
   - **Retrieval Phase:** When you ask a question like *"How is authentication implemented?"*, we convert your question into a vector embedding and find the top 5 most mathematically similar code chunks in your repository.
   - **Generation Phase:** We feed your question **+** those exact code chunks as context to OpenAI (`gpt-4o-mini`), which generates a precise answer along with exact file path citations.

---

## 🏗️ 2. Core Architecture & Workflow

The backend follows a layered, modular architecture:

```
┌────────────────────────────────────────────────────────────────────────┐
│                              Client (Next.js)                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP / SSE Stream
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Spring Boot REST Controllers                    │
│        (AuthController, RepoController, ChatController)                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          ▼                         ▼                         ▼
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│  Security &      │      │ Indexing & RAG   │      │ GitHub REST      │
│  OAuth2 Module   │      │ Service Engine   │      │ API Client       │
└─────────┬────────┘      └─────────┬────────┘      └─────────┬────────┘
          │                         │                         │
          ▼                         ▼                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         Persistence Layer                              │
│       PostgreSQL (Relational Data) + PgVector (Vector Embeddings)       │
└────────────────────────────────────────────────────────────────────────┘
```

### Module 1: Security & Authentication (OAuth2 + GitHub)
- **OAuth2 Workflow:** Uses Spring Security OAuth2 Login. Users log in using their GitHub account (`/oauth2/authorization/github`).
- **Encrypted Token Storage:** GitHub Access Tokens are encrypted using Spring Security's `TextEncryptor` before being stored in the database.
- **Session Management:** Stateful session cookies (`DEVPILOT_SESSION`) are issued after successful authentication and verified on protected `/api/**` endpoints.

### Module 2: Repository Sync & File Ingestion
- **GitHub API Integration:** Fetches repository metadata, branch trees, and raw file contents via `GithubApiClient`.
- **Rate Limiting Protection:** Implements `GitHubRateLimiter` to pause execution between requests to prevent hitting GitHub's API rate limits.
- **File Filtering (`CodeFileFilter`):** Excludes binary files, lockfiles, build artifacts (`node_modules`, `target`, `.git`), and files exceeding the configurable size threshold (default 100 KB).

### Module 3: Code Chunking & Vector Storage
- **Code Chunker (`CodeChunker`):** Uses Spring AI's `TokenTextSplitter` to break code files into overlapping token windows (~800 characters / 200 tokens).
- **Metadata Enrichment:** Attaches metadata to every chunk: `repositoryId`, `filePath`, `language`, and `chunkIndex`.
- **Asynchronous Execution:** Indexing runs on a dedicated background thread pool using `@Async("indexingExecutor")`.
- **Vector Embedding & Storage:** Chunks are sent in batches to OpenAI `text-embedding-3-small` and stored in PostgreSQL using the **PgVector** extension.

### Module 4: RAG Retrieval & Prompt Engineering
- **Filtered Similarity Search (`CodeContextRetriever`):** Performs vector similarity search restricted to the specific `repositoryId` using PgVector metadata filter expressions.
- **Prompt Builder (`ChatPromptBuilder`):** Combines system instructions, retrieved code snippets, and the user question into an optimized LLM prompt.
- **Citation Mapping (`CitationMapper`):** Maps retrieved vector documents into structural citations containing `filePath`, `startLine`, `endLine`, and `language`.

### Module 5: Real-time SSE Streaming
- **Server-Sent Events (`ChatStreamHandler`):** Returns an `SseEmitter` to stream generated LLM response tokens directly to the frontend in real time.
- **Persistence:** Automatically saves both the user prompt and complete assistant reply (with JSON-encoded citations) to PostgreSQL upon completion.

---

## 🛠️ 3. Technology Stack & Key Dependencies

| Component | Technology / Library | Purpose |
| :--- | :--- | :--- |
| **Framework** | Spring Boot `3.3.5` (Java 17) | Core REST API and Application Framework |
| **AI Integration** | Spring AI `1.0.0-M6` | Vector stores, Document chunking, OpenAI integration |
| **LLM Model** | OpenAI `gpt-4o-mini` | High-performance conversational code intelligence |
| **Embedding Model** | OpenAI `text-embedding-3-small` | Fast, accurate 1536-dimensional vector embeddings |
| **Database** | PostgreSQL 16 + PgVector | Relational data + Vector embedding similarity search |
| **ORM & Data Access** | Spring Data JPA / Hibernate | Entity management and repository operations |
| **Security** | Spring Security + OAuth2 Client | GitHub OAuth login, Session management, Encryption |
| **Validation** | `spring-boot-starter-validation` | Request DTO validation (`@NotNull`, `@NotBlank`, `@Valid`) |
| **JSON Mapping** | Jackson 2 (`ObjectMapper`) | Serialization/Deserialization of citations & API models |
| **Code Boilerplate** | Project Lombok | Auto-generation of Getters, Setters, Builders, Constructors |

---

## 🔌 4. API Endpoints Reference

### 🔐 Auth Endpoints (`/api/auth`)

#### 1. Get GitHub OAuth Login URL
- **HTTP Method:** `GET`
- **Path:** `/api/auth/login-url`
- **Auth Required:** No
- **Response (`200 OK`):**
  ```json
  {
    "url": "/oauth2/authorization/github"
  }
  ```

#### 2. Get Current Authenticated User
- **HTTP Method:** `GET`
- **Path:** `/api/auth/me`
- **Auth Required:** Yes (Session Cookie)
- **Response (`200 OK`):**
  ```json
  {
    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "githubId": 12345678,
    "githubUsername": "octocat",
    "displayName": "Mona Lisa Octocat",
    "avatarUrl": "https://github.com/images/error/octocat_happy.gif"
  }
  ```

#### 3. Logout User
- **HTTP Method:** `POST`
- **Path:** `/api/auth/logout`
- **Auth Required:** Yes
- **Response (`204 No Content`)**

---

### 📁 Repository Endpoints (`/api/repos`)

#### 4. List User Repositories
- **HTTP Method:** `GET`
- **Path:** `/api/repos?refresh=true`
- **Query Params:** `refresh` (boolean, default: `true`)
- **Auth Required:** Yes
- **Response (`200 OK`):**
  ```json
  [
    {
      "id": "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
      "githubRepoId": 87654321,
      "owner": "octocat",
      "name": "Hello-World",
      "fullName": "octocat/Hello-World",
      "isPrivate": false,
      "defaultBranch": "main",
      "language": "Java",
      "htmlUrl": "https://github.com/octocat/Hello-World",
      "description": "My first repository",
      "indexStatus": "READY",
      "indexedAt": "2026-09-06T12:00:00Z",
      "chunkCount": 42,
      "filesTotal": 10,
      "filesProcessed": 10,
      "errorMessage": null
    }
  ]
  ```

#### 5. Get Single Repository Details
- **HTTP Method:** `GET`
- **Path:** `/api/repos/{id}`
- **Auth Required:** Yes
- **Response (`200 OK`):** Single `RepositoryResponse` object.

#### 6. Trigger Repository Indexing
- **HTTP Method:** `POST`
- **Path:** `/api/repos/{id}/index`
- **Auth Required:** Yes
- **Response (`202 Accepted`):**
  ```json
  {
    "id": "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
    "indexStatus": "INDEXING",
    "filesProcessed": 0,
    "filesTotal": 0,
    "chunkCount": 0
  }
  ```

#### 7. Get Repository Indexing Progress / Status
- **HTTP Method:** `GET`
- **Path:** `/api/repos/{id}/status`
- **Auth Required:** Yes
- **Response (`200 OK`):**
  ```json
  {
    "repositoryId": "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
    "status": "INDEXING",
    "filesTotal": 25,
    "filesProcessed": 15,
    "chunkCount": 60,
    "errorMessage": null,
    "updatedAt": "2026-09-06T12:05:00Z"
  }
  ```

---

### 💬 Chat & RAG Endpoints (`/api/chat`)

#### 8. Create Chat Session
- **HTTP Method:** `POST`
- **Path:** `/api/chat/sessions`
- **Auth Required:** Yes
- **Request Body:**
  ```json
  {
    "repositoryId": "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
    "title": "Discussion on Auth Flow"
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "id": "c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33",
    "repositoryId": "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
    "title": "Discussion on Auth Flow",
    "createdAt": "2026-09-06T12:10:00Z"
  }
  ```

#### 9. List Chat Sessions for Repository
- **HTTP Method:** `GET`
- **Path:** `/api/chat/sessions?repositoryId={repositoryId}`
- **Auth Required:** Yes
- **Response (`200 OK`):** List of `ChatSessionResponse` objects.

#### 10. Get Messages for a Chat Session
- **HTTP Method:** `GET`
- **Path:** `/api/chat/sessions/{id}`
- **Auth Required:** Yes
- **Response (`200 OK`):**
  ```json
  [
    {
      "id": "d3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44",
      "role": "USER",
      "content": "Where is the security filter configured?",
      "citations": [],
      "createdAt": "2026-09-06T12:11:00Z"
    },
    {
      "id": "e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55",
      "role": "ASSISTANT",
      "content": "The security filter is configured in `SecurityConfig.java`...",
      "citations": [
        {
          "filePath": "src/main/java/DevPilot/Backend/config/SecurityConfig.java",
          "startLine": 28,
          "endLine": 64,
          "language": "Java"
        }
      ],
      "createdAt": "2026-09-06T12:11:05Z"
    }
  ]
  ```

#### 11. Send Message & Stream AI Reply (SSE Stream)
- **HTTP Method:** `POST`
- **Path:** `/api/chat/sessions/{id}/messages`
- **Headers:** `Accept: text/event-stream`
- **Auth Required:** Yes
- **Request Body:**
  ```json
  {
    "content": "How does the token encryption work?"
  }
  ```
- **Response (`200 OK` - `text/event-stream`):**
  ```
  event: user_message
  data: {"id":"...","role":"USER","content":"How does the token encryption work?"}

  event: token
  data: "Token"

  event: token
  data: " encryption"

  event: assistant_message
  data: {"id":"...","role":"ASSISTANT","content":"Token encryption uses Spring TextEncryptor...","citations":[...]}

  event: done
  data: [DONE]
  ```

---

## 🎤 5. Technical Interview Questions & Talking Points

When explaining this project to an interviewer, focus on architecture decisions, scalability, and error handling:

### Q1: "Walk me through the architecture of DevPilot."
> **Answer:** "DevPilot is a full-stack RAG application for codebases. The backend is built with Spring Boot 3 and Java 17. It manages user authentication via GitHub OAuth2, syncs GitHub repositories, chunks code files into vector embeddings using Spring AI and OpenAI, stores them in PostgreSQL with PgVector, and streams real-time answers with source code citations using Server-Sent Events."

### Q2: "How did you design the indexing pipeline to handle large codebases?"
> **Answer:** "I implemented an asynchronous, non-blocking pipeline:
> 1. When a user requests indexing, the backend marks the status as `INDEXING` and immediately returns `202 Accepted`.
> 2. An `@Async` background worker fetches the file tree, filters out non-code/large files (`CodeFileFilter`), and processes raw code into chunks (`TokenTextSplitter`).
> 3. To avoid exceeding memory or API limits, vector embeddings are generated and inserted in batches (`VECTOR_BATCH_SIZE = 32`) with built-in rate limiting (`GitHubRateLimiter`).
> 4. Progress is tracked incrementally in the database (`filesProcessed`/`filesTotal`), allowing the frontend to poll status in real time."

### Q3: "How does vector search ensure multi-tenant security so users only search their own repos?"
> **Answer:** "Every vector document stored in PgVector is tagged with metadata including `repositoryId`. During retrieval (`CodeContextRetriever`), we build a strict metadata filter expression (`FilterExpressionBuilder().eq("repositoryId", repoId)`). PgVector executes the similarity search exclusively over vectors matching that specific repository ID."

### Q4: "Why did you choose Server-Sent Events (SSE) over WebSockets or traditional REST polling?"
> **Answer:** "SSE is lightweight and unidirectional (server-to-client streaming), making it ideal for LLM token streaming. Unlike WebSockets, SSE operates over standard HTTP/1.1 or HTTP/2, works seamlessly with standard Spring MVC `SseEmitter`, requires no complex connection handshake, and automatically handles client reconnections."

### Q5: "How is security handled for third-party OAuth access tokens?"
> **Answer:** "GitHub access tokens give read access to user repositories, so security is critical. Tokens are never stored as plain text. We use Spring Security's `TextEncryptor` to encrypt tokens before persisting them to PostgreSQL. When indexing a repository, the token is decrypted in-memory for the duration of the API call and immediately garbage collected."
