# React Rules

No React/frontend code exists in this repository yet — it is currently a Spring Boot backend only
(see [[structure]]). These are the conventions to follow **when a React frontend is added** to this project
(e.g. under a new `frontend/` or `ui/` directory), so the codebase stays consistent from the start.

## Project setup

- Use TypeScript, not plain JavaScript, for all new React code.
- Use functional components with hooks — no class components.
- Keep the frontend in its own top-level directory (e.g. `frontend/`) with its own `package.json`, separate
  from the Maven build. Do not mix Java and JS/TS build tooling in the same module.

## Talking to this API

- The backend base URL is `/api/employees` on port `8081` by default (see [[api]]). Centralize API calls in a
  single client module (e.g. `src/api/employeeClient.ts`) rather than scattering `fetch`/`axios` calls across
  components.
- Mirror the backend's DTO shapes (`EmployeeRequest`/`EmployeeResponse` fields: `firstName`, `lastName`,
  `email`, `department`, `salary`) in matching TypeScript types — keep field names identical to avoid mapping
  bugs.
- Handle the backend's standard `ErrorResponse` shape (`timestamp`, `status`, `error`, `message`, `path`) in
  one shared error-handling utility, matching the status codes documented in [[api]] (400/404/409/500).

## Component structure

- One component per file, named to match the file (`EmployeeList.tsx` exports `EmployeeList`).
- Presentational components (pure rendering) separate from container/data-fetching components — don't fetch
  data inside deeply nested leaf components.
- Co-locate a component's styles/tests with the component (`EmployeeList.tsx`, `EmployeeList.test.tsx`,
  `EmployeeList.module.css`) rather than separate global `styles/`/`tests/` trees.

## State management

- Prefer local component state (`useState`) and React Query/SWR-style server-state caching for API data over
  a global store, unless the app grows enough state complexity to justify one — don't add Redux/Zustand
  preemptively.

## Testing

- Use React Testing Library (not Enzyme) — test behavior/output, not implementation details.
- Every new component that fetches or submits data needs at least one test covering the success path and one
  covering an error response from the API.

## Docs

- Once a frontend exists, add a `frontend/` section to [[structure]] and a "Frontend" section to
  [.claude/CLAUDE.md](../CLAUDE.md) with its own build/run commands.
