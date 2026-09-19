# Codex Migration Task Template

Use this template when assigning one migration slice to Codex.

```text
Read AGENTS.md first.

Then read:
- API_MIGRATION.md
- docs/PROJECT_CONTEXT.md
- docs/LEGACY_SYSTEM.md
- docs/MIGRATION_STATUS.md
- [add task-specific docs]

Task:
[one concrete migration goal]

Legacy behavior to inspect:
[list exact legacy files/controllers/services if known]

Constraints:
- preserve existing API behavior unless explicitly stated otherwise;
- do not modify unrelated modules;
- do not change the database schema unless required and approved;
- use the relevant skills under .agents/skills/;
- do not copy any legacy secrets.

Verification:
- run the Maven build;
- run relevant tests;
- verify API compatibility;
- verify persistence side effects if applicable.

Documentation:
Before finishing, update:
- docs/MIGRATION_STATUS.md
- API_MIGRATION.md if API migration state changed
- docs/CHANGELOG.md
- any other affected docs
- docs/KNOWN_RISKS.md if you discover conflicts or uncertainty

Do not mark the task complete only because it compiles.
```

## Example: Foundation Audit

```text
Read AGENTS.md and the required documentation.

Audit the current Java project foundation only.

Check:
- Java version
- Spring Boot version
- Maven configuration
- package structure
- PostgreSQL configuration
- schema-generation setting
- response wrapper
- exception handling
- security skeleton
- test baseline

Do not implement feature modules.

Run the build and tests.

Update docs/MIGRATION_STATUS.md with the actual current state and record any
conflicts in docs/KNOWN_RISKS.md.
```

## Example: User Vertical Slice

```text
Migrate the User module as one vertical slice.

Inspect the legacy User entity/repository/service/controller and the relevant
section in API_MIGRATION.md before changing Java code.

Preserve:
- UUID identity
- soft deletion
- admin-only list/detail/delete behavior where legacy behavior requires it
- owner-only profile update behavior
- response wrapper and pagination behavior

Do not migrate authentication in this task except interfaces strictly required
by User authorization.

Add tests and update migration documentation before finishing.
```
