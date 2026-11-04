# BookShelf AI

BookShelf AI is a book management application that helps users organize their reading lists and discover new books through AI-powered recommendations.

## Overview

Users can:
- Sign in securely with Google authentication (via Firebase)
- Search for books from a catalog
- Maintain two lists: **Reading List** and **Already Read**
- Chat with an AI assistant powered by Claude to get personalized book recommendations

The AI recommendation feature uses a multi-agent workflow that considers your reading history and preferences to suggest books you haven't added to either list yet.

## Architecture

This is a local proof-of-concept application consisting of two main components:

### Backend
- **Framework:** Spring Boot 3 with Java 21
- **Database:** SQLite with Flyway migrations
- **Authentication:** Firebase Admin SDK for token verification
- **AI Integration:** Anthropic Claude API (server-side only)
- **Testing:** JUnit 5 and Mockito

### Frontend
- **Framework:** React 18 with TypeScript (strict mode)
- **Build Tool:** Vite
- **Authentication:** Firebase Auth (Google sign-in only)
- **Testing:** Vitest and React Testing Library

## Project Structure

```
/backend          Spring Boot service (Maven)
  /src/main/java/.../auth        Firebase ID token verification
  /src/main/java/.../books       Catalog search client, book entities
  /src/main/java/.../lists       Reading List and Already Read services and APIs
  /src/main/java/.../agents      Multi-agent orchestrator, agent prompts, guard step
  /src/main/resources/db/migration   Flyway SQL migrations
/frontend         React + TypeScript SPA (Vite)
  /src/auth        AuthPage.tsx, firebase.ts, getIdToken.ts
  /src/features    Search, lists, chat
  /src/components  Shared UI components
/docs             Architecture deck and notes
```

## Getting Started

### Prerequisites

- Java 21 or higher
- Node.js 18 or higher
- Maven (or use the included Maven wrapper)
- Firebase project with Google authentication enabled
- Anthropic API key

### Configuration

#### Backend Setup

1. Set up environment variables:
   ```bash
   export FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/firebase-service-account.json
   export ANTHROPIC_API_KEY=your-anthropic-api-key
   ```

2. Configure Firebase project ID in `backend/src/main/resources/application-local.yml`

#### Frontend Setup

1. Copy the environment template:
   ```bash
   cd frontend
   cp .env.example .env.local
   ```

2. Fill in your Firebase configuration in `.env.local`

### Running the Application

#### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

The application will be available at `http://localhost:5173`

### Testing

#### Backend Tests

```bash
cd backend
./mvnw test                     # Run unit tests
./mvnw verify                   # Run tests and static checks
```

#### Frontend Tests

```bash
cd frontend
npm test                        # Run Vitest tests
npm run typecheck               # TypeScript type checking
npm run lint                    # ESLint
```

## Security

- All authentication is handled through Firebase with Google sign-in
- Firebase ID tokens are verified server-side for every API request
- User identity is derived solely from verified tokens, never from client input
- All user data queries are scoped to the authenticated user's UID
- Sensitive data is encrypted at rest using AES-256-GCM
- The Anthropic API key is only used server-side and never exposed to the client
- No passwords are stored or handled by this application

## Key Features

### Multi-Agent AI Workflow

The recommendation system uses a pipeline of specialized agents:
1. **Intake Agent** - Processes user requests
2. **Preference Agent** - Analyzes reading preferences
3. **Candidate Agent** - Suggests potential books
4. **Guard Step** - Filters out books already in user's lists (deterministic, not AI-based)
5. **Response Agent** - Formats the final recommendation

### Data Isolation

Every API endpoint (except health checks) requires authentication. All queries are automatically scoped to the authenticated user's data, preventing cross-user data access.

## Development Guidelines

- See [CLAUDE.md](./CLAUDE.md) for detailed architecture rules, coding conventions, and testing expectations
- Backend changes require Flyway migrations for schema updates
- Frontend uses TypeScript strict mode with no `any` types
- All new endpoints must include authentication and authorization tests
- Agent tests use mocked Claude clients and never call the real API

## License

This is a proof-of-concept project for demonstration purposes.
