# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project overview

BookShelf AI is a book management app. Users sign in with Google, search for books, and keep two lists: **Reading List** (books to read, unrated) and **Collection** (books already read, rated). Both live in the `collection` table, split by `list_type`; a book can be in only one list. A chat feature uses a multi-agent workflow, backed by the Claude API, to recommend a book that is not already in either list.

Scope: this is a **local proof of concept**. Do not add cloud hosting, deployment pipelines, or infrastructure-as-code unless explicitly asked.

## Tech stack

- **Backend:** Java 21, Spring Boot 3, Lombok, Spring Data JPA, SQLite, Flyway migrations, JUnit 5, Mockito, Firebase Admin SDK, Spring Security
- **Frontend:** React 18, TypeScript (strict mode), Vite, Vitest, React Testing Library
- **Auth:** Firebase Auth with Google sign-in only. No other providers. The backend turns the Google ID token into an HttpOnly session cookie (Spring Security, stateless).
- **AI:** Claude API called server-side only, via the Anthropic Messages API

## Repository layout

```
/backend          Spring Boot service (Maven)
  /src/main/java/.../auth        Session cookie auth: AuthService, filters, SecurityConfig, AuthController
  /src/main/java/.../books       Catalog search client, book entities
  /src/main/java/.../lists       Reading List and Already Read services and APIs
  /src/main/java/.../agents      Multi-agent orchestrator, agent prompts, guard step
  /src/main/resources/db/migration   Flyway SQL migrations
/frontend         React + TypeScript SPA (Vite)
  /src/auth        AuthPage.tsx, AuthProvider.tsx (useAuth), firebase.ts
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
npm run dev                     # start SPA on http://localhost:3000
npm test                        # Vitest
npm run typecheck               # tsc --noEmit
npm run lint
```

Before reporting a task as done, run the tests and type check for the part you changed.

## Configuration and secrets

- Frontend config lives in `frontend/.env.local`, created from `.env.example`. Never commit `.env.local`.
- Backend Firebase project ID is set in `application-local.yml` (`firebase.project-id`). The Firebase Admin service account JSON path comes from the `FIREBASE_SERVICE_ACCOUNT_PATH` environment variable. Never commit the JSON file.
- The Claude API key comes from `ANTHROPIC_API_KEY`. Never hard-code it, log it, or send it to the frontend.
- Add all secrets and local DB files to `.gitignore`. The SQLite file lives at `backend/data/bookshelf.db` and must not be committed.

## Architecture rules

1. **Identity comes from the verified session cookie only.** Sign-in: the SPA sends the Firebase ID token once to `POST /api/auth/session`; the backend verifies it (Google provider only, recent sign-in) and sets a Firebase session cookie (`bookshelf_session`, HttpOnly, SameSite=Strict, Path=/api, 7 days, `Secure` via `app.auth.cookie-secure`). Every endpoint except `POST /api/auth/session` and `POST /api/auth/logout` requires that cookie, verified with revocation checking. Take the user ID from the verified principal (`@AuthenticationPrincipal AuthenticatedUser`). Never accept a user ID from the request body, path, or query string. Logout revokes the user's refresh tokens so the cookie stops working immediately. The frontend uses relative `/api` URLs (Vite proxy) so the cookie stays same-origin and never stores tokens in browser storage. All state-changing requests must send `X-Requested-With: bookshelf` (CSRF defence); the shared `apiFetch` in `src/api/client.ts` does this.
2. **Users are keyed by Firebase UID.** Each collection row references `user_id` (the Firebase UID). Every query on user data must filter by the authenticated user's UID. Add a test for this whenever a new user-data endpoint is added.
3. **The model never enforces list exclusion.** Books already in either of a user's lists are removed by `ExclusionGuard`, a deterministic guard step in Java, after the candidate agent runs and before the response agent. Do not rely on prompt instructions for this. Normalize titles and ISBNs before comparing.
4. **Agents are a plain Spring orchestration.** The pipeline is intake, preference, candidate, guard, response. Each agent call uses a strict JSON schema for its output, a timeout, and one retry. Keep agent prompts in resource files, not inline strings.
   `CandidateValidator` (deterministic, in Java) also drops candidates the model leaves incomplete or fills with placeholders (blank or "Unknown" author, placeholder description) before the guard.
   The chat also has a **Reading List mode** (`mode: BACKLOG`): intake, preference, `RankingAgent`, then `BacklogGuard`. The ranking agent only returns opaque references (`b1`, `b2`, ...) to books on the user's Reading List; `BacklogGuard` drops unknown or repeated references and builds each pick from the database row, never from model output. An empty Reading List answers without calling Claude.
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
- **Lombok is used for all Java code** (version managed by Spring Boot, `optional` dependency, excluded from the packaged jar). Do not hand-write boilerplate:
  - Injected beans (controllers, services, components): `@RequiredArgsConstructor` with `private final` fields.
  - Request/response DTOs: `@Data`, plus `@NoArgsConstructor` / `@AllArgsConstructor` when needed.
  - JPA entities: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`. Never `@Data` or `@EqualsAndHashCode` on entities (it breaks equality and lazy loading); create instances with the builder.
  - Never put `@ToString` on a type holding secrets or encrypted fields. Use `@ToString.Exclude` or omit it.
- **Deletes are soft with a 5-second undo window.** Removing a book sets `deletion_scheduled_at`; the book is hidden immediately and a scheduled purge deletes it for good, so the removal completes even if the user refreshes. Undo is `POST /api/collection/{id}/undo-removal`. Active-collection queries must exclude books pending deletion.
- Name tests `<ClassName>Test` for unit tests and `<ClassName>IT` for integration tests.

### Frontend
- TypeScript strict mode. No `any` unless there is a comment explaining why.
- Components are function components with typed props.
- Feature code lives in `src/features/<feature>`. Shared UI goes in `src/components`.
- Auth-related code stays in `src/auth`. Components read the signed-in user through `useAuth()`; there is no token in JS (the session is an HttpOnly cookie). Make API calls only through `src/api/client.ts`.
- **Icons live in the icon library, `src/components/icons`.** Never define an icon or inline an `<svg>` inside a component. Add a new icon as its own file there (built on the shared `Icon` wrapper), export it from `icons/index.ts`, and import it with `import { LibraryIcon } from './icons'`.
- Show loading, empty, and error states for every data view.
- **Notifications are toasts, always.** Never use `alert()`, `confirm()`, or `prompt()`. Show every success, error, and undo message through `useToast()` from `src/components/ToastProvider`, which stacks styled toasts in the bottom-left corner. Do not render `Toast` directly or create page-specific notification styles; all notifications share the look in `Toast.css`. Destructive actions are not confirmed with a dialog: perform them, then offer an Undo action on the toast.

## Testing expectations

- Every service method with branching logic needs a JUnit test.
- Backend unit tests use JUnit 5 with Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`). Mock collaborators (repositories, HTTP clients, the Claude client) instead of hand-written fakes or real network/DB access. Services must receive collaborators through constructor injection so they can be mocked. Use real objects only for pure logic and simple value types.
- The exclusion guard must have tests covering: exact match, case difference, ISBN match, and no match.
- Every user-data endpoint needs a test showing that user A cannot read or modify user B's data.
- Agent tests use a mocked Claude client. Tests must never call the real API.
- Frontend: test the auth page's sign-in and error paths, and the lists view's empty and error states.

## Security checklist for changes

Before finishing any change that touches auth, data, or the API, check:
- [ ] Every new endpoint requires an authenticated session (the default in `SecurityConfig`).
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
