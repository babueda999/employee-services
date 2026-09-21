# Plan: Employee Frontend

STATUS: in progress — Phases 1-2 (scaffold + read path) done, Phases 3-6 (write path, delete, tests, docs
polish) remaining

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

## Definition of done

- All five operations usable end-to-end through the UI against a running `.\mvnw.cmd spring-boot:run`
  backend.
- Types match the backend contract exactly (spot-check against [[api]]).
- Error states (validation, duplicate email, not found) are shown to the user, not just logged to console.
- `frontend/` documented in [[structure]] and [.claude/CLAUDE.md](../CLAUDE.md).
