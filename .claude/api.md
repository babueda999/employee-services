# API Reference

Base path: `/api/employees` · Default server port: `8080`

## Employee object (response shape)

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "department": "Engineering",
  "salary": 75000.0,
  "remoteWorkEligible": true
}
```

## Endpoints

| Method | Path                  | Description          | Success | Error cases |
|--------|-----------------------|-----------------------|---------|-------------|
| POST   | `/api/employees`      | Create an employee    | 201 Created | 400 (validation), 409 (duplicate email) |
| GET    | `/api/employees/{id}` | Get employee by ID    | 200 OK  | 404 (not found) |
| GET    | `/api/employees`      | List all employees    | 200 OK  | — |
| PUT    | `/api/employees/{id}` | Update an employee    | 200 OK  | 400 (validation), 404 (not found), 409 (email used by another employee) |
| DELETE | `/api/employees/{id}` | Delete an employee    | 204 No Content | 404 (not found) |
| GET    | `/api/employees/search?name={name}` | Search employees by first/last name (case-insensitive, substring match) | 200 OK | 400 (missing `name` param) |

### Request body (POST / PUT) — `EmployeeRequest`

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "department": "Engineering",
  "salary": 75000.0,
  "remoteWorkEligible": true
}
```

Validation rules:
- `firstName`, `lastName`, `department`: required, not blank
- `email`: required, must be a valid email format
- `salary`: required, must be a positive number
- `remoteWorkEligible`: optional boolean; omitted/`null` means "not specified" (nullable DB column,
  no default). Left optional rather than required so existing callers that don't send it — the
  Employee Agent's `update_employee`/`adjust_salary` tools included — keep working without a 400 or a
  NOT NULL constraint failure.

## Agent endpoint

Not a CRUD resource, so it doesn't follow the `/api/<resource-plural>` pattern — it's a single
natural-language action endpoint backed by `EmployeeSupervisorAgent`, which answers using the
`get_employee`, `list_employees`, `search_employees`, `update_employee`, `delete_employee`, and
`adjust_salary` tools (see [[architecture]]). Each tool is its own single-purpose Spring bean under
`agent.subagents` (`GetEmployeeAgent`, `ListEmployeesAgent`, `SearchEmployeeAgent`,
`UpdateEmployeeAgent`, `DeleteEmployeeAgent`, `AdjustSalaryAgent`), implementing a shared
`EmployeeSubAgent` interface — the supervisor discovers all of them, offers their combined tool
definitions to OpenAI in one turn, and routes each tool call OpenAI makes to the one sub-agent that
owns it; that sub-agent enforces its own authorization before executing. The same six operations are
also independently exposed as MCP tools directly on each sub-agent class (an `@McpTool`-annotated
method alongside its OpenAI-facing one), so an external MCP client can call e.g. `get_employee`
without going through the supervisor at all. MCP calls accept the same optional `role` parameter as
the agent endpoint, enforced the same way (see Role permissions below) — unlike the
function-calling path, there is no un-gated default MCP caller.

**Multi-step tool use.** `process` loops — sending tool results back to OpenAI and letting it ask
for another tool — until the model stops calling tools or a hard cap (5 rounds) is hit, so a request
needing several tools in sequence (e.g. search, then adjust each match's salary) actually completes
instead of being cut off after one round.

**Conversational memory.** Each turn is persisted to H2 (`conversation_messages`, via
`ConversationMemoryService`) and the most recent turns for that `conversationId` are replayed as
context on the next call, so a follow-up like "give them a 10% raise instead" can resolve "them"
from an earlier turn. Pass back the `conversationId` from the response to continue a conversation;
omit it to start a new one.

**Human-in-the-loop delete confirmation, with two-person control.** `delete_employee` is CRITICAL risk
(see `ToolGuardrail.requiresConfirmation`), so the supervisor never runs it directly: it authorizes the
*requester* (`ADMIN`, via `checkDeleteAccess`), then parks the call as a `PendingToolConfirmation` (H2,
`pending_tool_confirmations`, 5-minute TTL, single-use) and returns `confirmationRequired: true` plus a
`confirmationToken` instead. The actual deletion only happens via a separate call to
`POST /api/agent/confirm` — never from the model interpreting free-text like "yes" in the next message —
and that call requires its *own* role, checked independently via
`AuthorizationGuardrail.checkConfirmationApprovalAccess`: only `MANAGER` may approve or deny, regardless
of which role requested the deletion. An `ADMIN` trying to confirm their own request gets `403` and the
token is left untouched (not burned), so the legitimate `MANAGER` approval can still go through.

**Role permissions:**
- `USER`: read only (`get_employee`, `list_employees`, `search_employees`)
- `MANAGER`: update only (`update_employee`, `adjust_salary`) — no read, no delete
- `ADMIN`: read, update (`update_employee`), and delete (`delete_employee`) — everything except
  `adjust_salary`, which stays MANAGER-exclusive

So a `MANAGER` still cannot read or delete employees through the agent (each gets `403` outside
its own lane), and salary adjustments specifically are the one action `ADMIN` doesn't get. Omitting
`role` defaults to `USER` (read only).

| Method | Path                 | Description                              | Success | Error cases |
|--------|----------------------|-------------------------------------------|---------|-------------|
| POST   | `/api/agent`         | Ask the employee AI agent a question      | 200 OK  | 400 (validation), 403 (authorization), 500 (OpenAI/unexpected failure) |
| POST   | `/api/agent/confirm` | Approve/deny a pending CRITICAL tool call | 200 OK  | 400 (validation), 403 (approver role isn't MANAGER), 404 (unknown/expired token) |

### Request body — `AgentRequest`

```json
{
  "message": "Find employee 101",
  "role": "ADMIN",
  "conversationId": "12cf975b-3a78-4f2d-ad02-4b3e8b210e24"
}
```

Validation rules:
- `message`: required, not blank
- `role`: optional; one of `USER`, `MANAGER`, `ADMIN` (case-sensitive). Defaults to `USER` when omitted.
  Checked twice: `AuthorizationGuardrail.validateRecognizedRole` up front (any of the three
  recognized roles passes this — it only rejects a null/blank/unrecognized role, before the agent
  makes any OpenAI call), then again per tool the model decides to call, with the actual
  per-operation permission — `checkReadAccess` (`USER`/`ADMIN`) for
  `get_employee`/`list_employees`/`search_employees`, `checkUpdateAccess` (`MANAGER`/`ADMIN`) for
  `update_employee`, `checkSalaryAdjustmentAccess` (`MANAGER` only) for `adjust_salary`, and
  `checkDeleteAccess` (`ADMIN` only) for `delete_employee`. This is a lightweight, unauthenticated
  role signal (no login/session), not real authentication — see [[architecture]].
- `conversationId`: optional. Continues an existing conversation (its recent turns are replayed as
  context) when provided; omit it to start a new one — the response's `conversationId` is then the
  one to send back on the next turn.

### Response body — `AgentResponse`

```json
{
  "reply": "Employee 101 is John Doe, Engineering.",
  "conversationId": "12cf975b-3a78-4f2d-ad02-4b3e8b210e24",
  "confirmationRequired": false,
  "confirmationToken": null,
  "toolsUsed": ["get_employee"]
}
```

`confirmationRequired`/`confirmationToken` are only populated when the turn's reply is about a
CRITICAL-risk tool call (currently just `delete_employee`) that was authorized but held back —
pass `confirmationToken` to `POST /api/agent/confirm` to approve or deny it. `toolsUsed` names the
`EmployeeSubAgent`(s) that actually ran this turn, in order (empty when the model answered without
calling a tool) — lets a UI show which agent did the work instead of just the prose reply.

### Request body — `AgentConfirmRequest`

```json
{
  "confirmationToken": "ebed0d83-2a55-41b8-8f6a-f1877ca20c4e",
  "approve": true,
  "role": "MANAGER"
}
```

Validation rules:
- `confirmationToken`: required, not blank
- `approve`: required
- `role`: the *approver's* role — independent of whoever requested the action. Optional in the sense
  that omitting it defaults to `USER`, which is then rejected; in practice it must be `MANAGER`
  (`AuthorizationGuardrail.checkConfirmationApprovalAccess`) or the call returns `403` *before* the
  token is touched — a rejected attempt never consumes it, so the real approver can still use it.

Once the approver role clears that check, executes the held tool call directly (no OpenAI call
involved) when `approve` is `true`, or discards it when `false`. Either way the token is single-use
and removed once resolved; a token that's unknown, already resolved, or older than 5 minutes returns
`404 Pending Confirmation Not Found`. Response body is the same `AgentResponse` shape, with
`confirmationRequired` always `false` and `toolsUsed` naming the tool that was (or would have been)
executed.

Requires an OpenAI credential to be configured in the environment (see `OpenAIOkHttpClient.fromEnv()`);
without one, a request to this endpoint returns `500` (the client is built lazily on first use, so the
rest of the app starts and runs fine either way).

## Error response shape

All errors (validation, not-found, duplicate, unexpected) are returned as `ErrorResponse` by
`GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-09-16T10:15:30",
  "status": 404,
  "error": "Employee Not Found",
  "message": "Employee not found with id: 5",
  "path": "/api/employees/5"
}
```

| Exception                          | HTTP status | `error` field         |
|-------------------------------------|-------------|------------------------|
| `EmployeeNotFoundException`         | 404         | Employee Not Found     |
| `PendingConfirmationNotFoundException` | 404      | Pending Confirmation Not Found (unknown, already-resolved, or expired `confirmationToken`) |
| `DuplicateEmployeeException`        | 409         | Duplicate Employee     |
| `MethodArgumentNotValidException`   | 400         | Validation Failed (message lists `field: reason` per invalid field, comma-separated) |
| `MissingServletRequestParameterException` | 400   | Validation Failed (e.g. `GET /api/employees/search` without `name`) |
| `IllegalArgumentException`          | 400         | Bad Request (e.g. `/api/agent` guardrail rejection — empty message, over length, or a blocked instruction pattern; see [[architecture]]) |
| `SecurityException`                 | 403         | Forbidden (e.g. `/api/agent` with an unrecognized `role`; see [[architecture]]) |
| `HttpRequestMethodNotSupportedException` | 405    | Method Not Allowed (e.g. `GET /api/agent`, which only accepts `POST`) |
| any other `Exception`               | 500         | Internal Server Error  |

## H2 console

Available at `http://localhost:8080/h2-console` (in-memory DB, JDBC URL `jdbc:h2:mem:employeedb`, user `sa`,
no password). Data does not persist across restarts.

## Running locally

```
.\mvnw.cmd spring-boot:run
```

App starts on `http://localhost:8080`.
