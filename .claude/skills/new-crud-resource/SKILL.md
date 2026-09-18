# Employee Frontend Skill

## Purpose

Develop and maintain the Employee Management React/Next.js frontend.

## Workflow

When implementing a frontend feature:

1. Understand the existing API.
2. Identify the required page/component.
3. Update TypeScript types if necessary.
4. Update API service if necessary.
5. Implement reusable React components.
6. Implement UI state.
7. Implement validation.
8. Implement error handling.
9. Add tests.
10. Run lint and build.

## Architecture

Follow:

```text
Page
 ↓
Component
 ↓
Hook
 ↓
Service
 ↓
API Client
 ↓
Spring Boot API
```

## Rules

Never:

* modify Java backend
* create duplicate backend logic
* hardcode API URLs
* use unnecessary global state
* use `any` unnecessarily
* duplicate API calls
* put all logic into one component

## Definition of Done

Every completed frontend feature should be:

* strongly typed
* reusable
* responsive
* accessible
* tested
* lint-clean
* build-clean
