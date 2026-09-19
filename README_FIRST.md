# ZPantry Codex Context Pack — Read This First

This pack is designed to be copied into the **root of the Java ZPantry backend repository**.

It intentionally does **not** include or replace:

- your existing `.agents/skills/` directory;
- your existing `API_MIGRATION.md`.

Those two resources are already part of your project and should remain the primary sources for:

- generic Java/Spring implementation guidance (`.agents/skills/`);
- API-by-API migration tracking/specification (`API_MIGRATION.md`).

## Installation

After extracting this ZIP, merge the files into your Java repository root so the structure becomes:

```text
ZPantry-Java/
├── .agents/
│   └── skills/
│       └── ... existing skills ...
├── AGENTS.md
├── PROJECT_CONVENTIONS.md
├── API_MIGRATION.md              # keep your existing file
├── README_FIRST.md
├── docs/
│   ├── PROJECT_CONTEXT.md
│   ├── LEGACY_SYSTEM.md
│   ├── DATABASE_SCHEMA.md
│   ├── ARCHITECTURE.md
│   ├── INTEGRATIONS.md
│   ├── MIGRATION_STATUS.md
│   ├── ARCHITECTURE_DECISIONS.md
│   ├── KNOWN_RISKS.md
│   ├── TEST_STRATEGY.md
│   ├── SOURCE_BASELINE.md
│   └── CHANGELOG.md
├── pom.xml
└── src/
```

If your repository already has an `AGENTS.md`, merge the rules instead of blindly overwriting useful instructions.

## How the Agent Should Use the Files

The intended responsibility split is:

```text
.agents/skills/          HOW to write good Java/Spring code
API_MIGRATION.md         WHAT API behavior must be migrated
AGENTS.md                RULES for the agent
PROJECT_CONVENTIONS.md    HOW ZPantry Java code must be structured/named
PROJECT_CONTEXT.md        WHAT ZPantry is as a product/domain
LEGACY_SYSTEM.md         HOW the old backend behaves structurally
DATABASE_SCHEMA.md       WHAT data model must remain compatible
ARCHITECTURE.md          WHAT the Java target architecture is
MIGRATION_STATUS.md      WHERE the migration currently stands
ARCHITECTURE_DECISIONS   WHY important technical decisions were made
KNOWN_RISKS.md           WHAT can break compatibility/security
TEST_STRATEGY.md         HOW parity should be proven
CHANGELOG.md             WHAT has changed over time
```

## First Agent Session

A recommended first prompt after installing this pack is:

```text
Read AGENTS.md first, then PROJECT_CONVENTIONS.md, the existing
API_MIGRATION.md, and every file under docs/ that is relevant to this task.

Audit the current Java repository against docs/MIGRATION_STATUS.md.
Do not implement new features yet.

Update docs/MIGRATION_STATUS.md so it accurately reflects what already exists
in the Java codebase. Record any conflict between current code, API_MIGRATION.md,
and the documented legacy behavior in docs/KNOWN_RISKS.md.

Run the current build/tests and report the baseline state.
Do not overwrite my existing .agents/skills or API_MIGRATION.md.
```

After that baseline audit, give Codex one migration slice at a time.

## Important

The documents in this pack describe the known legacy backend baseline. They are not a substitute for inspecting the actual source code.

The legacy GitHub repository baseline used for this pack is documented in `docs/SOURCE_BASELINE.md`.

When the legacy project changes, the agent must re-check the changed legacy code and update the corresponding project docs before relying on this pack.
