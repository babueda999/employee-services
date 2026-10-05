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
│   │   ├── AgentRequest.java        Inbound payload for /api/agent (message, role, conversationId)
│   │   ├── AgentResponse.java       Outbound payload (reply, conversationId, confirmationRequired/Token, toolsUsed)
│   │   └── AgentConfirmRequest.java Inbound payload for /api/agent/confirm (confirmationToken, approve,
│   │                                role — the *approver's* role, checked independently of the requester's)
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
│   │   ├── EmployeeSupervisorAgent.java  OpenAI Responses API orchestrator used by AgentController/A2aController;
│   │   │                                 discovers all agent.subagents.EmployeeSubAgent beans, loops tool calls
│   │   │                                 to completion (capped), replays conversation memory, and gates
│   │   │                                 CRITICAL-risk tools behind a human confirmation
│   │   ├── AgentReply.java          Record: text, conversationId, confirmationRequired, confirmationToken —
│   │   │                            the supervisor's return type for both process() and confirm()
│   │   ├── EmployeeTools.java       Tool implementations (get/list/search/update/delete/adjustSalary), called
│   │   │                            by the agent.subagents classes
│   │   ├── tools/                   FunctionTool name/description/Arguments + execute() for each of the six
│   │   │                            operations, called by the matching agent.subagents class
│   │   └── subagents/               One Spring-managed agent per tool, each implementing EmployeeSubAgent and
│   │       ├── EmployeeSubAgent.java    Shared interface: name(), definition() (OpenAI schema), authorize()
│   │       │                            (role check only), handle() (authorize() + execute)
│   │       ├── GetEmployeeAgent.java    get_employee     — also an @McpTool (direct MCP exposure)
│   │       ├── ListEmployeesAgent.java  list_employees   — also an @McpTool
│   │       ├── SearchEmployeeAgent.java search_employees — also an @McpTool
│   │       ├── UpdateEmployeeAgent.java update_employee  — also an @McpTool
│   │       ├── DeleteEmployeeAgent.java delete_employee  — also an @McpTool; CRITICAL risk, so the supervisor
│   │       │                            calls authorize() up front but routes execution through confirmation
│   │       └── AdjustSalaryAgent.java   adjust_salary    — also an @McpTool
│   ├── memory/                      Conversational memory, persisted to H2 so a conversation survives
│   │   │                            across separate HTTP requests
│   │   ├── ConversationMessage.java         Entity, table "conversation_messages" (conversationId, role, content)
│   │   ├── ConversationMessageRepository.java  findTop20By...OrderByCreatedAtDesc (bounds replayed context)
│   │   ├── ConversationMemoryService.java      Interface: getRecentHistory/appendUserMessage/appendAssistantMessage
│   │   └── ConversationMemoryServiceImpl.java
│   ├── confirmation/                Human-in-the-loop gate for CRITICAL-risk tool calls (see ToolGuardrail)
│   │   ├── PendingToolConfirmation.java        Entity, table "pending_tool_confirmations"; id IS the token
│   │   ├── PendingToolConfirmationRepository.java
│   │   ├── ToolConfirmationService.java        Interface: createPending/resolve (single-use, 5-min TTL)
│   │   └── ToolConfirmationServiceImpl.java
│   ├── a2a/
│   │   └── A2aController.java       Hand-rolled A2A (Agent2Agent) JSON-RPC endpoint over EmployeeSupervisorAgent;
│   │                                reuses the A2A message's contextId as the conversationId when present
│   ├── config/
│   │   └── JacksonConfig.java       com.fasterxml.jackson ObjectMapper bean (needed by agent/ and mcp/ classes,
│   │                                distinct from the Jackson 3 ObjectMapper Spring Boot 4 uses for the web layer)
│   └── exception/
│       ├── EmployeeNotFoundException.java
│       ├── DuplicateEmployeeException.java
│       ├── PendingConfirmationNotFoundException.java  Unknown/resolved/expired confirmation token -> 404
│       ├── ErrorResponse.java       Standard error payload shape
│       └── GlobalExceptionHandler.java  @RestControllerAdvice, maps exceptions to HTTP status
├── src/main/resources/
│   └── application.properties       Server port 8080, H2 in-memory DB, JPA/H2 console config
├── src/test/java/com/example/employee/
│   ├── EmployeeApplicationTests.java  Spring context load test
│   ├── controller/, service/, repository/  Layer tests mirroring src/main (see rules/testing.md)
│   ├── agent/EmployeeSupervisorAgentTest.java, agent/EmployeeToolsTest.java
│   ├── agent/subagents/*AgentTest.java  One test class per sub-agent (authorize()/handle()/its @McpTool method)
│   ├── memory/*Test.java            ConversationMemoryServiceImplTest, ConversationMessageRepositoryTest (@DataJpaTest)
│   └── confirmation/ToolConfirmationServiceImplTest.java
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
- `spring.ai.mcp.server.*` — Spring AI MCP server (protocol `STREAMABLE`), auto-discovering the six
  `@McpTool`-annotated methods on the `agent.subagents` classes

## Employee AI agent

`AgentController` (`POST /api/agent`) and `A2aController` (`/a2a`) both delegate to
`EmployeeSupervisorAgent`, which uses the OpenAI Responses API. Rather than owning all six tools itself, it
discovers every `agent.subagents.EmployeeSubAgent` bean (one per operation: get/list/search/update/delete/
adjustSalary), offers their combined OpenAI function schemas in one turn, and routes each tool call OpenAI
makes to the one sub-agent registered under that name — that sub-agent checks its own authorization
(`AuthorizationGuardrail`) and executes via `EmployeeTools`. Each sub-agent also independently exposes the
same operation as an `@McpTool`, so an external MCP client can call it directly without going through the
supervisor. `EmployeeSupervisorAgent` builds its `OpenAIClient` lazily from environment credentials
(`OpenAIOkHttpClient.fromEnv()`) on first use rather than in the constructor, so the app starts and `mvn test`
passes with no OpenAI credential configured — only an actual `/api/agent` request requires one.

Three things happen above that per-tool dispatch:

- **Multi-step tool use.** `process` loops — sending tool results back to OpenAI and letting it call
  another tool — until the model stops or a hard cap (5 rounds) is hit, so a request needing several
  tools in sequence actually completes in one call instead of being silently cut off after one round.
- **Conversational memory.** Each turn is persisted to H2 via `memory.ConversationMemoryService` and
  the recent history for that `conversationId` is replayed as context on the next turn, so a
  follow-up like "give them a raise instead" resolves "them" from an earlier turn. `A2aController`
  reuses the A2A message's `contextId` as the conversation ID when the caller sends one.
- **Human-in-the-loop confirmation, with two-person control.** `delete_employee` is CRITICAL risk
  (`ToolGuardrail.requiresConfirmation`), so it's never run inside the tool loop — the *requester* is
  authorized (`ADMIN`, via `checkDeleteAccess`), then the call is parked as a
  `confirmation.PendingToolConfirmation` (H2, 5-minute TTL, single-use) and only executed via a
  separate `POST /api/agent/confirm` call, never from the model's own interpretation of free-text like
  "yes". That confirm call checks its *own*, independent role — only `MANAGER` may approve or deny
  (`AuthorizationGuardrail.checkConfirmationApprovalAccess`), so the role that asked for a deletion can
  never also be the one that approves it. A rejected approval attempt doesn't consume the token.

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
