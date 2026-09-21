# API Design Rules

General rules for designing and evolving this project's REST API. For the current endpoint reference see
[[api]] (top-level `.claude/api.md`); for HTTP status/error-shape rules see [[api-conventions]].

## Consistency

- Every new resource follows the same shape as `Employee`: plural noun base path (`/api/<resource>`),
  `{id}` as a `Long` path variable, and the same five operations (create/get-by-id/get-all/update/delete)
  unless the resource genuinely doesn't need one of them.
- Field names in request/response DTOs use `camelCase` and must match the entity field names — no renaming
  between layers (e.g. don't expose `first_name` in JSON while the entity has `firstName`).
- Don't introduce a different error-response shape for a new resource — always reuse `ErrorResponse` via
  `GlobalExceptionHandler` (see [[code-style]]).

## Backwards compatibility

- Don't remove or rename a field on an existing response DTO (`EmployeeResponse`, etc.) without checking for
  consumers — additive changes (new optional fields) are safe, removals/renames are breaking.
- Don't change an existing endpoint's success status code or URL once documented in [[api]] — add a new
  endpoint/version instead if behavior must diverge.
- If a breaking change is unavoidable, note it in a [[plans]] entry before implementing.

## Versioning

- No API versioning scheme exists yet (all routes are unprefixed `/api/...`). If versioning becomes
  necessary, prefer a URL prefix (`/api/v2/...`) over header-based versioning, and do it resource-by-resource
  rather than a big-bang migration.

## Documentation

- Any change to a route, request/response shape, status code, or error condition must be reflected in
  [[api]] in the same change — don't let the reference doc drift from the controller code.
- No OpenAPI/Swagger generation exists yet (see gap in [[architecture]]); until it's added, [[api]] is the
  source of truth for the contract and must be kept accurate by hand.

## Consumption (frontend)

- Frontend code calling this API must follow [[react]] / [[nextjs]] / [[typescript]] as applicable — in
  particular, mirror DTO field names exactly and handle the shared `ErrorResponse` shape rather than assuming
  every call succeeds.
