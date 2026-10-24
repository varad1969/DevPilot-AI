# DevPilot - RAG-Based GitHub Code Intelligence System

A full-stack application that indexes GitHub repositories and lets you chat with them using RAG (Retrieval-Augmented Generation).

## Architecture

- **Backend**: Spring Boot 4.1.0, Java 21, Spring AI, PostgreSQL + pgvector
- **Frontend**: Next.js 16, React 19, TypeScript, Tailwind CSS, shadcn/ui
- **Database**: PostgreSQL with pgvector extension (Docker)
- **AI**: OpenAI GPT-4o-mini for chat, text-embedding-3-small for embeddings

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21
- Node.js 20+
- OpenAI API key
- GitHub OAuth App credentials

### 1. Start PostgreSQL with pgvector
```bash
docker-compose up -d
```

### 2. Configure Backend
```bash
cd Backend
cp .env.example .env
# Edit .env with your OPENAI_API_KEY, GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET
```

### 3. Run Backend
```bash
# Linux/Mac
./run-backend.sh

# Windows PowerShell
.\start-dev.ps1
```

### 4. Run Frontend
```bash
cd client
npm install
npm run dev
```

### 5. Access Application
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080

## Features
- GitHub OAuth login
- Repository sync from GitHub
- Code indexing with vector embeddings
- RAG-powered chat with code citations
- Real-time streaming responses

## Project Structure
```
├── Backend/          # Spring Boot backend
├── client/           # Next.js frontend
├── docker/           # PostgreSQL initialization
├── docker-compose.yml
└── README.md
```