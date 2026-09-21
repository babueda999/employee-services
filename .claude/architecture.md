# Architecture

## Stack

- Java 17
- Spring Boot 4.1.1 (starter-parent)
- Spring Web MVC (`spring-boot-starter-webmvc`)
- Spring Data JPA + Hibernate ORM 7.4.5 (`spring-boot-starter-data-jpa`)
- Bean Validation (`spring-boot-starter-validation`)
- H2 in-memory database (`com.h2database:h2`), with `spring-boot-h2console`
- Lombok (optional, available but current entity/DTO classes use explicit getters/setters, not Lombok annotations)
- DevTools for hot reload during development
- Maven build, `spring-boot-maven-plugin` for packaging/running

## Design patterns in use

- **Layered architecture**: Controller → Service (interface + impl) → Repository, with a dedicated Mapper
  layer between DTOs and the JPA entity. `EmployeeService` is an interface implemented by
  `EmployeeServiceImpl`, allowing the implementation to be swapped/mocked independently of the controller.
- **DTO pattern**: `EmployeeRequest` (input, validated) and `EmployeeResponse` (output) are separate from the
  `Employee` JPA entity, so persistence concerns never leak into the API contract.
- **Mapper component**: `EmployeeMapper` centralizes all Entity↔DTO conversion (`toEntity`, `toResponse`,
  `updateEntity`) instead of scattering mapping logic across the service.
- **Global exception handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) centralizes translation of
  domain exceptions and validation errors into a uniform `ErrorResponse` JSON shape with timestamp, status,
  error, message, and request path.
- **Repository pattern**: `EmployeeRepository extends JpaRepository<Employee, Long>` with derived query
  methods (`existsByEmail`, `findByEmail`) — no custom SQL/JPQL needed for current use cases.

## Business rules enforced in the service layer

- **Duplicate email prevention on create**: `createEmployee` checks `existsByEmail` before insert and throws
  `DuplicateEmployeeException` (→ HTTP 409) if the email is already in use.
- **Duplicate email prevention on update**: `updateEmployee` looks up any existing employee with the new email
  and throws `DuplicateEmployeeException` only if that email belongs to a *different* employee ID — so an
  employee can be updated without changing their own email without tripping the check.
- **Not-found handling**: `getEmployeeById`, `updateEmployee`, and `deleteEmployee` all throw
  `EmployeeNotFoundException` (→ HTTP 404) when the ID doesn't exist.
- **Transactional boundaries**: `EmployeeServiceImpl` is `@Transactional` at the class level; read-only
  methods (`getEmployeeById`, `getAllEmployees`) are additionally annotated `@Transactional(readOnly = true)`.

## Data model

`Employee` entity (table `employees`):

| Field       | Type    | Constraints                          |
|-------------|---------|---------------------------------------|
| id          | Long    | PK, identity/auto-increment           |
| firstName   | String  | not null                              |
| lastName    | String  | not null                              |
| email       | String  | not null, unique (`uk_employee_email`)|
| department  | String  | not null                              |
| salary      | Double  | not null                              |

`EmployeeRequest` (input DTO) additionally enforces via Bean Validation: `@NotBlank` on name/email/department,
`@Email` format on email, `@NotNull @Positive` on salary.

## Known gaps / not yet implemented

- No pagination/sorting on `GET /api/employees` — returns the full list.
- No authentication/authorization layer.
- No persistent database — H2 is in-memory, data is lost on restart (`ddl-auto=update` will recreate/update
  schema each run against a fresh in-memory DB).
- No API documentation generation (no springdoc/OpenAPI dependency).
- Full `mvn package` (jar assembly) may fail in this environment due to a local TLS trust-chain issue when
  Maven downloads `maven-jar-plugin` transitive dependencies from Maven Central — use `mvn spring-boot:run` to
  run the app directly from compiled classes if that happens.
