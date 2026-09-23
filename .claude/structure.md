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
│   │   ├── EmployeeController.java  REST endpoints, /api/employees
│   │   └── AgentController.java     REST endpoint, POST /api/agent (natural-language employee queries)
│   ├── dto/
│   │   ├── EmployeeRequest.java     Inbound payload, Bean Validation annotations
│   │   ├── EmployeeResponse.java    Outbound payload
│   │   ├── AgentRequest.java        Inbound payload for /api/agent (message, @NotBlank)
│   │   └── AgentResponse.java       Outbound payload for /api/agent (reply)
│   ├── entity/
│   │   └── Employee.java            JPA entity, table "employees", unique email constraint
│   ├── repository/
│   │   └── EmployeeRepository.java  Spring Data JPA repository
│   ├── service/
│   │   ├── EmployeeService.java     Service interface
│   │   └── EmployeeServiceImpl.java Business logic, duplicate-email checks
│   ├── mapper/
│   │   └── EmployeeMapper.java      Manual DTO <-> Entity mapping (no MapStruct)
│   ├── agent/
│   │   ├── EmployeeAgent.java       OpenAI Responses API agent used by AgentController; wires the tools below
│   │   ├── EmployeeTools.java       Tool implementations (get/list/search), called by EmployeeAgent
│   │   └── tools/                   FunctionTool name/description constants for GetEmployee/ListEmployees/SearchEmployee
│   ├── mcp/
│   │   └── EmployeeMcpTools.java    Spring AI @McpTool-annotated employee tools, exposed via the MCP server
│   ├── config/
│   │   └── JacksonConfig.java       com.fasterxml.jackson ObjectMapper bean (needed by agent/ and mcp/ classes,
│   │                                distinct from the Jackson 3 ObjectMapper Spring Boot 4 uses for the web layer)
│   └── exception/
│       ├── EmployeeNotFoundException.java
│       ├── DuplicateEmployeeException.java
│       ├── ErrorResponse.java       Standard error payload shape
│       └── GlobalExceptionHandler.java  @RestControllerAdvice, maps exceptions to HTTP status
├── src/main/resources/
│   └── application.properties       Server port 8080, H2 in-memory DB, JPA/H2 console config
├── src/test/java/com/example/employee/
│   ├── EmployeeApplicationTests.java  Spring context load test
│   ├── controller/, service/, repository/  Layer tests mirroring src/main (see rules/testing.md)
│   ├── agent/EmployeeAgentTest.java, agent/EmployeeToolsTest.java
│   └── mcp/EmployeeMcpToolsTest.java
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
- `spring.ai.mcp.server.*` — Spring AI MCP server (protocol `STREAMABLE`), exposing `EmployeeMcpTools`

## Employee AI agent

`AgentController` (`POST /api/agent`) delegates to `EmployeeAgent`, which uses the OpenAI Responses API with
three tools (`get_employee`, `list_employees`, `search_employees`) implemented in `EmployeeTools`, which in
turn calls `EmployeeService`. `EmployeeAgent` builds its `OpenAIClient` lazily from environment credentials
(`OpenAIOkHttpClient.fromEnv()`) on first use rather than in the constructor, so the app starts and `mvn test`
passes with no OpenAI credential configured — only an actual `/api/agent` request requires one.

See [api.md](api.md#agent-endpoint) for the request/response shape.

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
