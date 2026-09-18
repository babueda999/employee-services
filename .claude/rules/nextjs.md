# Next.js Development Rules

## Framework

Use Next.js with:

* App Router
* TypeScript
* React Server Components where appropriate
* Client Components only when necessary

## Routes

Implement:

```text
/employees
/employees/new
/employees/[id]
/employees/[id]/edit
```

## Layout

Create a common application layout with:

* application header
* navigation
* main content area
* responsive design

## Navigation

Use Next.js navigation APIs.

Do not use plain browser navigation unnecessarily.

## Environment Variables

Use:

```text
NEXT_PUBLIC_API_BASE_URL
```

Example:

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## API Integration

Do not put repeated fetch logic inside page components.

Use:

```text
lib/apiClient.ts
services/employeeService.ts
```

## Error Handling

Handle:

* API errors
* network errors
* invalid employee IDs
* employee not found
* server errors

Provide user-friendly UI messages.

## Loading

Use appropriate Next.js loading patterns and React loading state where needed.
