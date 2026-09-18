---
name: employee-frontend
description: Build or extend the Next.js/TypeScript frontend for the Employee Service, following the project's frontend plan and rules.
---

# Employee Frontend

Use this when asked to build, extend, or fix the Employee Service frontend (list/create/view/edit/delete
employees against the backend REST API).

Read first: [[employee-frontend-plan]] (the phased plan), [[api]] (endpoint contract), [[nextjs]], [[react]],
[[typescript]] (conventions), and [[api-conventions]] (error-response shape).

## Before writing code

1. Check `frontend/` (or `web/`) exists yet. If not, this is Phase 1 of [[employee-frontend-plan]] — scaffold
   it per that plan before writing feature code.
2. Verify the current backend DTO shapes against [[api]] — don't assume `EmployeeRequest`/`EmployeeResponse`
   fields; re-check if the backend has changed since the plan/rules were written.
3. Confirm whether the backend has CORS configured. If not, prefer Server Components/Route Handlers/Server
   Actions calling the API server-side, per [[nextjs]] — don't add client-side `fetch` calls straight to
   `localhost:8081` without CORS in place.

## Build order

Follow the phases in [[employee-frontend-plan]]:
1. Scaffold + shared types (`types/employee.ts`, matching `Employee`/`CreateEmployeeRequest`/
   `UpdateEmployeeRequest` to the real backend contract).
2. Read path — list page, detail page (Server Components).
3. Write path — create/edit forms (Server Actions or Route Handlers), with `ErrorResponse` (400/409) surfaced
   to the user, not just console-logged.
4. Delete — Server Action with a confirmation step.
5. Tests — React Testing Library, one success-path and one error-response-path test per form/page that talks
   to the API (per [[react]]).
6. Docs — update [[structure]] and [.claude/CLAUDE.md](../CLAUDE.md) with the new frontend layout and its
   build/run commands; flip [[employee-frontend-plan]]'s `STATUS` to `done` once complete (or update it if
   scope changed mid-implementation).

## Guardrails

- No pagination, auth, or CORS changes beyond minimal local-dev needs — those are explicitly out of scope in
  [[employee-frontend-plan]] unless the user asks to expand scope.
- Keep Java (`src/main/java/...`) and frontend code fully separate — don't put frontend files inside the
  Maven module tree.
- Any deviation from [[employee-frontend-plan]]'s scope or phases should be reflected back into that plan
  file, not left only in code/commit history.

## Verification

- `frontend`'s own type-check/build commands (e.g. `tsc --noEmit`, `next build`) must pass.
- Manually exercise all five operations against a running backend (`.\mvnw.cmd spring-boot:run`), including
  the validation, duplicate-email, and not-found error paths (see [[api]]).
