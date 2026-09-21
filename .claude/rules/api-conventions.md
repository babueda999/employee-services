run all test case available here # API Conventions

Rules for anything exposed under `/api/**`. See [[api]] for the current endpoint reference.

## HTTP status codes

- `201 Created` on successful resource creation (POST).
- `200 OK` on successful GET/PUT.
- `204 No Content` on successful DELETE (no response body).
- `400 Bad Request` for Bean Validation failures.
- `404 Not Found` when a path-variable ID doesn't resolve to an existing resource.
- `409 Conflict` for uniqueness violations (e.g. duplicate email).
- `500 Internal Server Error` only for unexpected/unhandled exceptions — never used intentionally.

## Error responses

Every error response must use the shared `ErrorResponse` shape (`timestamp`, `status`, `error`, `message`,
`path`), produced through `GlobalExceptionHandler`. Do not return ad-hoc error bodies from a controller.

## Resource routes

- Base path per resource: `/api/<resource-plural>` (e.g. `/api/employees`).
- `{id}` path variables are `Long`.
- Collection endpoints (`GET /api/employees`) return a JSON array, not a wrapped object — no pagination
  envelope currently exists in this project (see gap noted in [[architecture]]). If pagination is added later,
  keep it opt-in via query params (`page`, `size`) rather than breaking the existing unpaginated contract.

## Adding a new endpoint

1. Add/extend the request/response DTOs with validation.
2. Add the method to the service interface, then implement it in `EmployeeServiceImpl` (or the analogous impl
   for a new resource).
3. Add the controller method with the correct `@...Mapping` and HTTP status.
4. If it introduces a new failure mode, add an exception class + handler (see [[code-style]]).
5. Update `.claude/api.md` with the new endpoint.
