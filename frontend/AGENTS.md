# Carizmi Frontend — AI Agent Guide (`frontend/**`)

This file supplements root [`../AGENTS.md`](../AGENTS.md) with frontend-specific rules for `frontend/`.

## 1. Tech Stack & Directory Discovery
- **Source of Truth for Versions & Scripts**: Inspect [`package.json`](package.json) and [`tsconfig.json`](tsconfig.json) for exact library versions (React, TypeScript, Vite, Ant Design, React Router, Orval, Axios, Vitest, ESLint, Prettier) and npm scripts.
- **Directory Routing (`frontend/src/`)** — Discover modules dynamically by listing these directories rather than assuming static file lists:
  - `api/openapi.json` — Committed OpenAPI 3 contract generated from the backend.
  - `api/generated/` — **Read-only** Orval-generated TypeScript clients and DTO types (`gitignored`).
  - `api/client/` — [`ApiClient.ts`](src/api/client/ApiClient.ts) (Axios instance, `withCredentials: true`, 401 refresh queue) and [`apiMutator.ts`](src/api/client/apiMutator.ts).
  - `features/<context>/` — Domain feature modules (each with `pages/`, `modals/`, `hooks/`, `components/`).
  - `contexts/` — Shared React Context state providers (`*Context.tsx`).
  - `hooks/` — Shared custom hooks (`use*.ts`, `use*.tsx`).
  - `components/` — Reusable UI and form components.
  - `styles/` — CSS custom properties and shared styles (`global.css`, `design-system.css`, `ant-overrides.css`, plus scoped subdirectories).
  - `test/` — Vitest setup and unit test suites (`*.test.ts`, `*.test.tsx`).

## 2. API Contract & Orval Client Rules
- **Never Edit `src/api/generated/**` Manually**: Any manual edits are wiped by `rm -rf src/api/generated` during `npm run generate:api`.
- **Regeneration Pipeline** (see [`../docs/API_GENERATION.md`](../docs/API_GENERATION.md)):
  1. When backend controllers or DTOs change, regenerate `src/api/openapi.json` via `./mvnw verify -pl framework,backend -DskipTests` (requires Docker). If Docker is unavailable, ask the maintainer before proceeding.
  2. Run `npm --prefix frontend run generate:api` (runs `orval` followed by `postgenerate:api` → `node clean-orval.mjs`).
  3. [`clean-orval.mjs`](clean-orval.mjs) flattens Springdoc's per-entity `GlobalResponse*` wrappers into generic `GlobalResponse<T>`, normalizes `SortOrder`, and exports `<tag>Api` singleton clients under `src/api/generated/<tag>/<tag>.ts`.
- **Consuming APIs**: Discover available `<tag>Api` singletons and DTO types by inspecting `src/api/generated/` (after running `generate:api`).

## 3. Authentication & `GlobalResponse<T>` Error Handling
- **`httpOnly` Cookie Auth**: Access and refresh JWTs are stored in `httpOnly` cookies (`withCredentials: true` in `ApiClient.ts`). Never store JWT tokens in `localStorage` or `sessionStorage`.
- **Silent 401 Refresh**: `ApiClient.ts` automatically intercepts HTTP 401 responses, queues concurrent requests, calls `/auth/refresh`, and retries or dispatches `auth:unauthorized`. Do not build custom 401 retry loops in components.
- **`GlobalResponse<T>` Envelope**:
  - Happy path reads `response.responseData`, `response.meta` (pagination), and `response.globalMessages`.
  - Error path (`400`/`409`/`404`) uses [`useApiMessages`](src/hooks/useApiMessages.ts) to map `fieldMessages` to form fields and `globalMessages` to notifications.

## 4. Styling, Design System & Responsiveness
- **CSS Token Architecture** (see [`../docs/UI_UX_ARCHITECTURE.md`](../docs/UI_UX_ARCHITECTURE.md)):
  1. [`src/styles/global.css`](src/styles/global.css) — Light and `[data-theme='dark']` CSS custom properties (`--color-primary`, `--bg-card`, `--text-primary`, etc.).
  2. [`src/styles/design-system.css`](src/styles/design-system.css) & [`src/styles/ant-overrides.css`](src/styles/ant-overrides.css) — Shared layout, utility classes, and Ant Design token overrides.
  3. Component/page stylesheets under `src/styles/`.
- **No Hardcoded Hex Colors**: Always use CSS variables (`var(--...)`) so Light and Dark themes stay synchronized.
- **Mobile-First `<576px` Breakpoint**: Financial tables, member directories, and modals must remain usable on mobile viewports (`<576px`).

## 5. Verification & Testing Commands
Run from the repository root before completing any frontend task:
```bash
npm --prefix frontend run generate:api   # Ensure generated client is present and up-to-date
npm --prefix frontend run type-check     # Must exit 0 (tsc --noEmit)
npm --prefix frontend run lint           # Must exit 0 with 0 errors
npm --prefix frontend run test           # Runs Vitest unit tests
```
> **Formatting Note:** Do not run `npm --prefix frontend run format` across the whole repository unless requested by the maintainer; if formatting, scope Prettier strictly to the files you modified (`npx --prefix frontend prettier --write <changed-file>`).