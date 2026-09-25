# Plan: Employee Frontend

STATUS: in progress — Phases 1-4 (scaffold, read path, write path, delete) and the agent chat box (see below)
done; Phases 5-6 (full test coverage, docs polish) remaining

## Goal

Add a web frontend that consumes the existing Employee Service REST API (see [[api]]) to list, view, create,
update, and delete employees, instead of relying on Postman/curl/H2 console for interaction.

## Scope

In scope:
- A Next.js (App Router) + TypeScript frontend under a new top-level `frontend/` directory, per
  [[nextjs]], [[react]], and [[typescript]].
- Pages/routes covering all five backend operations (see [[api]]):
  - `app/employees/page.tsx` — list all employees (`GET /api/employees`)
  - `app/employees/[id]/page.tsx` — view one employee (`GET /api/employees/{id}`)
  - `app/employees/new/page.tsx` — create form (`POST /api/employees`)
  - `app/employees/[id]/edit/page.tsx` — edit form (`PUT /api/employees/{id}`)
  - delete action from the list or detail view (`DELETE /api/employees/{id}`)
- Shared TypeScript types mirroring `EmployeeRequest`/`EmployeeResponse`/`ErrorResponse` (see [[typescript]]).
- Client-side display of validation/duplicate/not-found errors returned by `GlobalExceptionHandler`
  (see [[api-conventions]]).

Out of scope (not part of this plan):
- Authentication/authorization (none exists on the backend either — see gap in [[architecture]]).
- Pagination (backend doesn't support it yet — see gap in [[architecture]]).
- CORS configuration changes on the Spring Boot side beyond what's minimally needed for local dev.

## Backend prerequisite

The Spring Boot API currently has no CORS configuration. Per [[nextjs]], prefer calling the backend from
Next.js Server Components / Route Handlers (server-to-server) to avoid needing CORS changes at all. If any
client-side (`"use client"`) component needs to call the API directly, CORS must be added to the backend
first — that's a separate backend change, not part of this plan, and should get its own plan entry if needed.

## Phases

1. ~~**Scaffold**~~ DONE — `frontend/` created via `create-next-app` (App Router, TypeScript, no Tailwind,
   npm, no nested git repo). `types/employee.ts` has `Employee`, `CreateEmployeeRequest`,
   `UpdateEmployeeRequest`, `ApiErrorResponse`, verified against the live backend.
2. ~~**Read path**~~ DONE — `app/employees/page.tsx` (list) and `app/employees/[id]/page.tsx` (detail), both
   Server Components using `lib/employees.ts` (`fetch` with `cache: "no-store"`, `API_BASE_URL` from
   `.env.local`, defaulting to `http://localhost:8080`). 404 from the backend maps to `notFound()`.
   Verified in-browser against a running backend: list renders a created employee, detail page renders by id,
   unknown id renders Next's 404 page. `npm run build` type-checks and builds cleanly.
3. **Write path** (not started) — create and edit forms (Server Actions or Route Handlers calling
   `POST`/`PUT`), with client-side display of `ErrorResponse` (400/409) and redirect-on-success.
4. **Delete** (not started) — delete action (Server Action calling `DELETE`), with a confirmation step before
   the request.
5. **Tests** — component tests per [[react]]/[[nextjs]] covering success and error-response paths for each
   form.
6. **Docs** — update [[structure]] and [.claude/CLAUDE.md](../CLAUDE.md) with the new `frontend/` layout and
   its build/run commands; update this plan's `STATUS` to `done`.

## Open questions (resolve before/while implementing)

- Where does the frontend get deployed/run relative to the backend (same host, reverse proxy, separate
  deployment)? Determines whether `API_BASE_URL` needs to be configurable per environment.
- Should delete require a confirmation modal, or is a simple `confirm()` acceptable for a first version?

## Addendum: Employee Agent chat box (added, out of original scope)

DONE — `/agent` page with a client-side chat UI (`app/agent/AgentChatBox.tsx`) that talks to the backend's
`POST /api/agent` (OpenAI-backed natural-language endpoint, see [[api]]). Same server-side-proxy pattern as
the rest of the app: the client component calls the same-origin Route Handler `app/api/agent/route.ts`, which
forwards to `lib/agent.ts` (`askAgent`) on the server, avoiding any need for backend CORS changes. Linked from
the home page ("Chat with the agent"). Types in `types/agent.ts`. Tests: `tests/lib/agent.test.ts` (success +
`ApiError` path) and `tests/app/agent/AgentChatBox.test.tsx` (success reply, error display, empty-input
no-op), following the existing `tests/lib/employees.test.ts` / component-test conventions.

Known dependency: replies require the backend's OpenAI credential *and* outbound HTTPS to succeed — see the
SSL/PKIX trust-store issue noted in [.claude/CLAUDE.md](../CLAUDE.md) (Norton AV SSL inspection root not in
the JDK's `cacerts`). The chat box itself works correctly end-to-end regardless — it faithfully surfaces
whatever the backend returns, including that error, rather than failing silently.

## Definition of done

- All five operations usable end-to-end through the UI against a running `.\mvnw.cmd spring-boot:run`
  backend.
- Types match the backend contract exactly (spot-check against [[api]]).
- Error states (validation, duplicate email, not found) are shown to the user, not just logged to console.
- `frontend/` documented in [[structure]] and [.claude/CLAUDE.md](../CLAUDE.md).
