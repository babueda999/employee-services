# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Employee Service — a Java 17 / Spring Boot 4.1.1 CRUD REST API for managing employees, backed by an in-memory
H2 database via Spring Data JPA/Hibernate. Layered architecture: Controller → Service → Mapper → Repository →
Entity.

Detailed docs live alongside this file under `.claude/`:

- [structure.md](structure.md) — directory layout and request flow
- [architecture.md](architecture.md) — stack, design patterns, business rules, data model, known gaps
- [api.md](api.md) — full endpoint reference, request/response/error shapes
- [rules/code-style.md](rules/code-style.md) — coding conventions to follow
- [rules/api-conventions.md](rules/api-conventions.md) — REST/HTTP status conventions
- [rules/react.md](rules/react.md), [rules/nextjs.md](rules/nextjs.md), [rules/typescript.md](rules/typescript.md) — frontend conventions
- [plans/](plans/) — saved implementation plans for non-trivial changes (see [plans/employee-frontend-plan.md](plans/employee-frontend-plan.md) for frontend status)
- [skills/new-crud-resource/SKILL.md](skills/new-crud-resource/SKILL.md) — scaffold a new CRUD resource matching the Employee pattern
- [skills/employee-frontend.md](skills/employee-frontend.md) — build/extend the frontend

Read the relevant file above before making non-trivial changes rather than re-deriving conventions from
scratch.

## Build / run

```
.\mvnw.cmd compile              # compile
.\mvnw.cmd test                 # run tests
.\mvnw.cmd spring-boot:run       # run the app (http://localhost:8080)
.\mvnw.cmd clean package         # full build incl. jar packaging
```

**Known issue**: `mvn package` (jar assembly) can fail in this environment with a PKIX/SSL trust-chain error
when Maven downloads `maven-jar-plugin` transitive dependencies from Maven Central — a local trust-store issue,
not a code problem. `compile` and `test` are unaffected; use `spring-boot:run` to run/verify the app if
packaging fails. Do not "fix" this by disabling SSL certificate verification without the user's explicit
sign-off.

### Frontend (`frontend/`)

```
cd frontend
npm run dev      # http://localhost:3000, requires the backend running on http://localhost:8080
npm run build    # production build + typecheck
```

Read-path only (list/detail pages) so far — see [structure.md](structure.md#frontend) and
[plans/employee-frontend-plan.md](plans/employee-frontend-plan.md) for what's built vs. remaining.

## Working conventions

- Follow [rules/code-style.md](rules/code-style.md) and [rules/api-conventions.md](rules/api-conventions.md)
  for any new or modified code.
- No Lombok annotations on entities/DTOs even though Lombok is on the classpath — match the existing explicit
  getter/setter style unless told otherwise.
- Keep `api.md` and `structure.md` in sync when endpoints or package layout change.
- This is not currently a git repository (no `.git` directory) — check before assuming git commands apply.
