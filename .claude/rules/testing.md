# Testing Rules

Current state: only one test exists — `EmployeeApplicationTests` (`src/test/java/com/example/employee/`), a
`@SpringBootTest` smoke test (`contextLoads()`) with no assertions. There is no unit/service/controller test
coverage yet. See the gap noted in [[architecture]].

## Available test infrastructure

From `pom.xml` (test scope):
- `spring-boot-starter-data-jpa-test` — for repository-layer tests (`@DataJpaTest`, H2 in-memory DB).
- `spring-boot-starter-webmvc-test` — for controller-layer tests (`@WebMvcTest`, `MockMvc`).
- JUnit 5 (Jupiter) is the test framework in use — `@Test` from `org.junit.jupiter.api`.

No Mockito/AssertJ dependency is declared explicitly; if a test needs mocking or fluent assertions, add the
appropriate starter/dependency rather than hand-rolling stubs, and add it to [[architecture]]'s stack list
once introduced.

## Test placement and naming

- Mirror the main package structure under `src/test/java/com/example/employee/...` (e.g. a test for
  `service/EmployeeServiceImpl.java` goes in `src/test/java/com/example/employee/service/`).
- Test class name = `<ClassUnderTest>Test` (e.g. `EmployeeServiceImplTest`, `EmployeeControllerTest`,
  `EmployeeRepositoryTest`).
- Test method names describe behavior, not mechanics: `createEmployee_throwsDuplicateException_whenEmailExists`
  style (method_condition_expectedResult), not `test1`/`testCreate`.

## What to test per layer

- **Repository** (`@DataJpaTest`): the derived query methods actually used by the service —
  `existsByEmail`, `findByEmail` — against the real H2 schema, including the unique-email constraint.
- **Service** (plain unit test with mocked `EmployeeRepository`/`EmployeeMapper`, or a Mockito-based test):
  every branch in `EmployeeServiceImpl` — successful create/get/update/delete, `DuplicateEmployeeException` on
  create with an existing email, `DuplicateEmployeeException` on update when the new email belongs to another
  employee (but not when it's the same employee's own email), `EmployeeNotFoundException` on get/update/delete
  with an unknown ID.
- **Controller** (`@WebMvcTest(EmployeeController.class)` + `MockMvc`, service mocked): correct HTTP status per
  endpoint (see [[api]]: 201/200/200/200/204), request validation failures return 400 with the expected
  `ErrorResponse` shape, and that the controller delegates to the service rather than containing logic itself.
- **Exception handling**: at least one test per `GlobalExceptionHandler` method confirming the mapped status
  code and `ErrorResponse` fields (see [[api-conventions]]).

## Rules

- Every new service method or business rule (see [[code-style]]) must ship with at least one test covering
  the success path and one covering each error path it can produce.
- Every new endpoint (see [[api]]) must ship with a controller test covering its success status and its
  documented error statuses.
- Don't rely solely on `contextLoads()` as coverage for new functionality — it only proves the Spring context
  wires up, not that behavior is correct.
- Keep tests independent of each other (no shared mutable state, no ordering dependencies) — `@DataJpaTest`
  is transactional/rolled-back per test by default; don't disable that without a reason.

## Running tests

```
.\mvnw.cmd test
```

Test compilation/execution is unaffected by the known `mvn package` SSL/PKIX issue noted in [[architecture]].
