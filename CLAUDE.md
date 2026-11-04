# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project overview

BookShelf AI is a book management app. Users sign in with Google, search for books, and keep two lists: **Reading List** and **Already Read**. A chat feature uses a multi-agent workflow, backed by the Claude API, to recommend a book that is not already in either list.

Scope: this is a **local proof of concept**. Do not add cloud hosting, deployment pipelines, or infrastructure-as-code unless explicitly asked.

## Tech stack

- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, SQLite, Flyway migrations, JUnit 5, Mockito, Firebase Admin SDK
- **Frontend:** React 18, TypeScript (strict mode), Vite, Vitest, React Testing Library
- **Auth:** Firebase Auth with Google sign-in only. No other providers.
- **AI:** Claude API called server-side only, via the Anthropic Messages API

## Repository layout

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
/docs             Architecture deck and notes
```

## Commands

Run these from the relevant directory. If a command fails because the tooling is not set up yet, say so and ask before changing build files.

```bash
# Backend
cd backend
./mvnw spring-boot:run          # start API on http://localhost:8080
./mvnw test                     # JUnit tests
./mvnw verify                   # tests plus static checks

# Frontend
cd frontend
npm install
npm run dev                     # start SPA on http://localhost:5173
npm test                        # Vitest
npm run typecheck               # tsc --noEmit
npm run lint
```

Before reporting a task as done, run the tests and type check for the part you changed.

## Configuration and secrets

- Frontend config lives in `frontend/.env.local`, created from `.env.example`. Never commit `.env.local`.
- Backend Firebase project ID is set in `application-local.yml`. The Firebase Admin service account JSON path comes from the `FIREBASE_SERVICE_ACCOUNT_PATH` environment variable. Never commit the JSON file.
- The Claude API key comes from `ANTHROPIC_API_KEY`. Never hard-code it, log it, or send it to the frontend.
- Add all secrets and local DB files to `.gitignore`. The SQLite file lives at `backend/data/bookshelf.db` and must not be committed.

## Architecture rules

1. **Identity comes from the verified token only.** Every backend endpoint except health checks requires a Firebase ID token in `Authorization: Bearer <token>`. Verify it with the Firebase Admin SDK. Take the user ID from the verified token's UID. Never accept a user ID from the request body, path, or query string.
2. **Users are keyed by Firebase UID.** Each collection row references `user_id` (the Firebase UID). Every query on user data must filter by the authenticated user's UID. Add a test for this whenever a new user-data endpoint is added.
3. **The model never enforces list exclusion.** Books already in a user's lists are removed by a deterministic guard step in Java, after the candidate agent runs and before the response agent. Do not rely on prompt instructions for this. Normalize titles and ISBNs before comparing.
4. **Agents are a plain Spring orchestration.** The pipeline is intake, preference, candidate, guard, response. Each agent call uses a strict JSON schema for its output, a timeout, and one retry. Keep agent prompts in resource files, not inline strings.
5. **Claude calls happen only on the backend.** The frontend never calls the Anthropic API directly.
6. **Sensitive fields are encrypted at rest.** Use AES-256-GCM with a fresh random 12-byte IV per value. The key comes from configuration outside the database. Do not invent a custom crypto scheme.
7. **Passwords are never handled by this app.** Sign-in is delegated to Google through Firebase.

## Coding conventions

### Backend
- Controllers stay thin. Business logic goes in services. Persistence goes through repositories.
- Return a consistent error body (`timestamp`, `status`, `error`, `message`, `path`). Do not leak stack traces or internal exception messages.
- Validate all request input with Bean Validation annotations.
- Schema changes go through a new Flyway migration. Never edit an applied migration.
- Use constructor injection. Avoid field injection.
- Name tests `<ClassName>Test` for unit tests and `<ClassName>IT` for integration tests.

### Frontend
- TypeScript strict mode. No `any` unless there is a comment explaining why.
- Components are function components with typed props.
- Feature code lives in `src/features/<feature>`. Shared UI goes in `src/components`.
- Auth-related code stays in `src/auth`. Read the token only through `getApiToken()`.
- Show loading, empty, and error states for every data view.

## Testing expectations

- Every service method with branching logic needs a JUnit test.
- The exclusion guard must have tests covering: exact match, case difference, ISBN match, and no match.
- Every user-data endpoint needs a test showing that user A cannot read or modify user B's data.
- Agent tests use a mocked Claude client. Tests must never call the real API.
- Frontend: test the auth page's sign-in and error paths, and the lists view's empty and error states.

## Security checklist for changes

Before finishing any change that touches auth, data, or the API, check:
- [ ] Every new endpoint verifies the Firebase ID token.
- [ ] No user ID is taken from client input.
- [ ] Queries are scoped to the authenticated UID.
- [ ] No secrets, API keys, or service account contents appear in code, logs, or responses.
- [ ] Errors returned to the client are generic.
- [ ] Input is validated.

## Working style

- Make small, focused changes. Ask before changing the architecture, adding dependencies, or changing the Firebase, Claude, or SQLite choices.
- When a task is ambiguous, state the assumption you are making before implementing.
- When you add a dependency, say why in your summary.
- Do not add features beyond the current task's scope.
- Update this file when conventions or commands change.

## Current status

Planning is complete (see the solution deck). Work is organized into four workstreams: Auth and security, Backend, Frontend, and Agents and QA. Tasks have no dates; pick the next unchecked task that its dependencies allow.
