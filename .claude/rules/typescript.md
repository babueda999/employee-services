# TypeScript Rules

Use strict TypeScript.

## No Any

Avoid:

```typescript
any
```

unless absolutely unavoidable.

## Employee Types

Create:

```text
types/
└── employee.ts
```

Define types based on the actual backend API.

For example:

```typescript
export interface Employee {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}
```

Do not assume these are the actual fields. Verify the existing API.

## API Types

Where appropriate, separate:

```text
Employee
CreateEmployeeRequest
UpdateEmployeeRequest
```

## Functions

Use explicit types for important function parameters and return values.

## Null Handling

Handle:

* null
* undefined
* optional fields

explicitly.

Do not assume API data is always valid.
