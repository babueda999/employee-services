# Code Style Rules

Conventions already established in this codebase (`src/main/java/com/example/employee/**`). Follow these when
adding or modifying code so new code matches the existing style.

## Layering

- Never call `EmployeeRepository` directly from `EmployeeController` — always go through the `EmployeeService`
  interface.
- Never put business rules (duplicate checks, not-found checks) in the controller or mapper — they belong in
  the service implementation (`EmployeeServiceImpl`).
- Entity ↔ DTO conversion only happens in `EmployeeMapper`. Controllers and services must not manually copy
  fields between an entity and a DTO.
- Controllers only: accept the request DTO, call the service, wrap the result in a `ResponseEntity` with the
  correct HTTP status. No business logic in controllers.

## Formatting

- Explicit getters/setters on entities/DTOs (no Lombok `@Data`/`@Getter`/`@Setter`), even though Lombok is on
  the classpath — match the existing style unless the user asks to switch to Lombok project-wide.
  See [[architecture]] for full stack notes.
- Multi-parameter method signatures are formatted one parameter per line (see `EmployeeController`,
  `EmployeeServiceImpl` constructors).
- Section comments like `// CREATE`, `// GET BY ID` mark each CRUD method in `EmployeeController` — keep this
  pattern when adding new endpoints.

## New DTOs / entities

- Every new request DTO must carry Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Positive`,
  etc.) with an explicit `message`.
- Every new entity field that must be unique needs an explicit `@UniqueConstraint` with a descriptive name
  (pattern: `uk_<table>_<column>`), matching `uk_employee_email`.

## Exceptions

- New domain error conditions get their own exception class under `exception/`, plus a handler method in
  `GlobalExceptionHandler` that returns the standard `ErrorResponse` shape. Don't throw generic
  `RuntimeException` for expected error conditions.
