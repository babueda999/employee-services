# Project Structure

Spring Boot 4.1.1 / Java 17 Maven project implementing a layered CRUD REST service for employees.

```
employee-servicves-main/
├── pom.xml                          Maven build (parent: spring-boot-starter-parent 4.1.1)
├── mvnw / mvnw.cmd                  Maven wrapper
├── README.md                        Full API reference and usage docs
├── src/main/java/com/example/employee/
│   ├── EmployeeApplication.java     @SpringBootApplication entry point
│   ├── controller/
│   │   └── EmployeeController.java  REST endpoints, /api/employees
│   ├── dto/
│   │   ├── EmployeeRequest.java     Inbound payload, Bean Validation annotations
│   │   └── EmployeeResponse.java    Outbound payload
│   ├── entity/
│   │   └── Employee.java            JPA entity, table "employees", unique email constraint
│   ├── repository/
│   │   └── EmployeeRepository.java  Spring Data JPA repository
│   ├── service/
│   │   ├── EmployeeService.java     Service interface
│   │   └── EmployeeServiceImpl.java Business logic, duplicate-email checks
│   ├── mapper/
│   │   └── EmployeeMapper.java      Manual DTO <-> Entity mapping (no MapStruct)
│   └── exception/
│       ├── EmployeeNotFoundException.java
│       ├── DuplicateEmployeeException.java
│       ├── ErrorResponse.java       Standard error payload shape
│       └── GlobalExceptionHandler.java  @RestControllerAdvice, maps exceptions to HTTP status
├── src/main/resources/
│   └── application.properties       Server port 8080, H2 in-memory DB, JPA/H2 console config
├── src/test/java/com/example/employee/
│   └── EmployeeApplicationTests.java  Spring context load test
└── frontend/                        Next.js (App Router) + TypeScript frontend, separate npm project
    ├── app/
    │   ├── page.tsx                 Home, links to /employees
    │   └── employees/
    │       ├── page.tsx             List all employees (Server Component)
    │       └── [id]/page.tsx        Employee detail (Server Component, 404 on missing id)
    ├── lib/employees.ts             Server-side API client (getAllEmployees, getEmployeeById), ApiError class
    ├── types/employee.ts            Employee, CreateEmployeeRequest, UpdateEmployeeRequest, ApiErrorResponse
    └── .env.local                   API_BASE_URL=http://localhost:8080
```

## Layer flow

```
HTTP request
  -> EmployeeController      (validates @RequestBody via @Valid, delegates to service)
  -> EmployeeService/Impl    (business rules: duplicate email checks, not-found checks)
  -> EmployeeMapper           (Entity <-> DTO conversion)
  -> EmployeeRepository      (Spring Data JPA, backed by H2)
  -> Employee entity          (persisted to "employees" table)
```

Errors thrown from the service layer (`EmployeeNotFoundException`, `DuplicateEmployeeException`) and validation
failures (`MethodArgumentNotValidException`) are caught centrally by `GlobalExceptionHandler` and turned into a
consistent `ErrorResponse` JSON body.

## Key configuration (`application.properties`)

- `server.port=8080`
- H2 in-memory DB: `jdbc:h2:mem:employeedb`, H2 console at `/h2-console`
- `spring.jpa.hibernate.ddl-auto=update` — schema is auto-generated/updated from the `Employee` entity
- `spring.jpa.show-sql=true` — SQL logged to console

## Frontend

`frontend/` is a standalone Next.js 16 (App Router) + TypeScript app, built with `create-next-app` (no
Tailwind, npm, no nested git repo). It currently implements the **read path only** (list + detail pages) —
create/update/delete forms are not yet built. See [.claude/plans/employee-frontend-plan.md](plans/employee-frontend-plan.md)
for remaining phases, and [rules/nextjs.md](rules/nextjs.md) / [rules/react.md](rules/react.md) /
[rules/typescript.md](rules/typescript.md) for conventions.

Pages call the backend server-side (Server Components, `fetch` with `cache: "no-store"`) via
`lib/employees.ts`, reading `API_BASE_URL` from `frontend/.env.local` — no CORS configuration exists on the
backend, so client-side (`"use client"`) calls directly to the API are not set up.

```
cd frontend
npm run dev      # http://localhost:3000
npm run build    # production build + typecheck
```

See [architecture.md](architecture.md) for design details and [api.md](api.md) for the endpoint reference.
