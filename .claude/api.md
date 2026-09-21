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
  "salary": 75000.0
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

### Request body (POST / PUT) — `EmployeeRequest`

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "department": "Engineering",
  "salary": 75000.0
}
```

Validation rules:
- `firstName`, `lastName`, `department`: required, not blank
- `email`: required, must be a valid email format
- `salary`: required, must be a positive number

## Agent endpoint

Not a CRUD resource, so it doesn't follow the `/api/<resource-plural>` pattern — it's a single
natural-language action endpoint backed by `EmployeeAgent`, which answers using the `get_employee`,
`list_employees`, and `search_employees` tools (see [[architecture]]).

| Method | Path         | Description                                | Success | Error cases |
|--------|--------------|---------------------------------------------|---------|-------------|
| POST   | `/api/agent` | Ask the employee AI agent a question        | 200 OK  | 400 (validation), 500 (OpenAI/unexpected failure) |

### Request body — `AgentRequest`

```json
{
  "message": "Find employee 101"
}
```

Validation rules:
- `message`: required, not blank

### Response body — `AgentResponse`

```json
{
  "reply": "Employee 101 is John Doe, Engineering."
}
```

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
| `DuplicateEmployeeException`        | 409         | Duplicate Employee     |
| `MethodArgumentNotValidException`   | 400         | Validation Failed (message lists `field: reason` per invalid field, comma-separated) |
| any other `Exception`               | 500         | Internal Server Error  |

## H2 console

Available at `http://localhost:8080/h2-console` (in-memory DB, JDBC URL `jdbc:h2:mem:employeedb`, user `sa`,
no password). Data does not persist across restarts.

## Running locally

```
.\mvnw.cmd spring-boot:run
```

App starts on `http://localhost:8080`.
