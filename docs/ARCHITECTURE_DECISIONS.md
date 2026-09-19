# ZPantry Architecture Decision Log

Use this file for decisions that affect compatibility, architecture, security, persistence, or long-term maintainability.

Do not use ADRs for trivial code-style choices already covered by Java skills.

---

## ADR-001 — Java Runtime

**Status:** Accepted baseline  
**Decision:** Java 21  
**Context:** The backend migration is being built as a new Java service.  
**Consequence:** Build, CI, container, IDE, and dependencies must support Java 21.

---

## ADR-002 — Spring Boot Baseline

**Status:** Accepted baseline  
**Decision:** Spring Boot 4.0.8  
**Context:** This is the selected baseline for the Java migration repository.  
**Consequence:** Dependency/library choices must be validated against this Spring Boot generation rather than copied blindly from Spring Boot 3 tutorials.

---

## ADR-003 — Build Tool

**Status:** Accepted baseline  
**Decision:** Maven  
**Consequence:** Repository build instructions and dependency management should use the Maven wrapper where available.

---

## ADR-004 — Architecture Style

**Status:** Accepted baseline  
**Decision:** Modular monolith using package-by-feature.  
**Rationale:** The legacy backend is modular but deployed as one API process. A modular monolith preserves operational simplicity while allowing cleaner boundaries in Java.  
**Consequence:** Avoid premature microservice decomposition during language migration.

---

## ADR-005 — API Compatibility Before Redesign

**Status:** Accepted  
**Decision:** Preserve existing externally observable API behavior during initial migration.  
**Consequence:** Spring conventions do not automatically justify route, response, status, field, or authorization changes.

---

## ADR-006 — Preserve Existing PostgreSQL Data Model Initially

**Status:** Accepted  
**Decision:** Map the existing schema before redesigning it.  
**Consequence:** Hibernate should validate rather than automatically rebuild the schema. Flyway requires a controlled baseline.

---

## ADR-007 — AI Service Remains External

**Status:** Accepted  
**Decision:** Do not merge the AI service implementation into the Spring Boot backend during language migration.  
**Consequence:** Recreate the HTTP client boundary and contract.

---

## ADR-008 — Preserve Legacy Password Format During Shared-Table Migration

**Status:** Accepted  
**Decision:** During C#/Java coexistence, Java User password updates must generate and verify the
exact Microsoft.AspNet.Identity.Core 2.2.4 marker-0 format: PBKDF2-HMAC-SHA1, 1,000 iterations,
16-byte random salt, 32-byte subkey and standard-Base64 49-byte payload. Do not write BCrypt or
another format while the legacy login must consume the same table.  
**Evidence:** Actual package execution, JDK 21 cross-verification, disposable bootstrap/login and
the synthetic fixture under contracts/auth.  
**Consequence:** This weak legacy cost is transitional compatibility debt. A stronger format,
progressive rehash or reset policy requires a later Authentication ADR and coordinated rollout.

---

# ADR Template

## ADR-XXX — Title

**Status:** Proposed / Accepted / Rejected / Superseded  
**Date:** YYYY-MM-DD

### Context

What problem/constraint led to this decision?

### Decision

What was decided?

### Alternatives Considered

What realistic alternatives were considered?

### Consequences

What becomes easier/harder? What compatibility impact exists?

### Migration Impact

Which modules/docs/tests must change?

## ADR-009 — Enforce Java 21 for compilation and test execution

**Status:** Accepted  
**Date:** 2026-09-16

Keep Boot 4.0.8 and Maven wrapper 3.9.16. Maven Enforcer requires [21,22); compiler release
is explicitly 21. Surefire and Failsafe use the same Maven JDK, with Boot-managed versions
3.5.6. .java-version records the portable major-version preference; it does not change the
host environment. Set JAVA_HOME to a JDK 21 before invoking Maven. No hardcoded personal
JDK path or host-wide PATH modification is committed. Tests assert their actual Java major.

## ADR-010 — Disposable PostgreSQL tests and non-mutating runtime defaults

**Status:** Accepted  
**Date:** 2026-09-16

Use Boot-managed Testcontainers 2.0.5 and pgvector/pgvector:0.8.2-pg16 pinned by manifest
digest in IsolatedPostgres. A test configuration owns container start/stop, disables reuse,
and constructs its DataSource exclusively from that container. Environment datasource
URLs/passwords are not used by persistence tests. There is no external-DB fallback and no
skip-on-missing-Docker behavior. Failsafe runs *IT during verify; Surefire runs unit tests.

Create the test-only schema with the container init script, then run Hibernate validate.
Runtime also uses validate; Flyway and Spring SQL initialization remain disabled. Flyway
clean is disabled and automatic baselining is off. No EF migrations/history are replayed,
renamed or replaced. Before any future baseline: inspect a read-only catalog/export, compare
EF mappings, rehearse on an isolated clone, document baseline version and separate Flyway
history, then explicitly enable approved migrations. Do not baseline production on startup.

Remove the unused Spring AI vector-store starter rather than create a second vector schema
or require an inference model. The test probes existing PostgreSQL vector types via JDBC;
it does not implement vector search or choose a future entity mapping library.

## ADR-011 — Source-derived envelopes without speculative HTTP policy

**Status:** Accepted  
**Date:** 2026-09-16

Provide record envelopes based on the pinned legacy ApiResponse, ApiErrorDetail and
PagedResponse sources. Pagination is flat and preserves the legacy page-count calculation,
including non-positive page-size behavior. Explicit null inclusion preserves data/errors.
Do not add controllers, blanket exception mapping, fabricated endpoint fixtures, or an
unspecified request-ID header. Error/status/tracing adapters await feature-level evidence.
No public API migration status changes in this slice; API_MIGRATION.md is unchanged.

## ADR-012 — Correct Legacy Owner Identity Extraction

**Status:** Accepted (2026-09-19; Option B explicitly approved)

**Context:** The legacy JWT includes email `sub`, UUID `userId`, and UUID under the literal
NameIdentifier URI. JwtBearer 10.0.9 uses `JsonWebTokenHandler` 8.0.1 with inbound mapping enabled,
which maps `sub` to an earlier email-valued NameIdentifier. The controller selects the first
NameIdentifier, cannot parse the email as UUID and rejects matching owners with 403.

**Decision:** After complete JWT validation, derive authenticated UUID deterministically
from validated `userId`, falling back only to a UUID-valued NameIdentifier. Permit PUT only when
it equals the route UUID. Keep wrong-owner and admin-other updates forbidden, with no admin bypass.
Record observed legacy 403 separately in parity evidence.

**Alternatives considered:** Reproduce owner PUT 403 exactly; search duplicate NameIdentifier
values without preferring issuer-intended `userId`; migrate all Authentication flows first.

**Consequences:** This restores intended self-update but deliberately differs from captured
runtime behavior. MIG-004, frontend expectations and tests must state the deviation. The identity
converter can serve later slices while issuance, refresh and OTP remain out of scope.
## ADR-013 — Cross-runtime access-token revocation (PROPOSED)

**Context:** Runtime evidence proves the legacy static JTI blacklist is process-local and is lost
on restart. During coexistence, neither C# nor Java can observe the other's logout.

**Options:** (A) preserve local memory, which is incompatible with shared-token coexistence;
(B) adapt both runtimes to a shared expiry-aware revocation store; (C) route all bearer traffic and
logout to one runtime, cut over atomically, and drain for the maximum access-token lifetime.

**Recommendation:** Use C for a short controlled cutover when routing guarantees are enforceable;
otherwise use B, with PostgreSQL as the smallest already-operated dependency subject to schema
approval. No option is accepted or implemented by this analysis. Production cutover remains blocked.

## ADR-014 — Flyway Owns Empty-Database Schema Only

**Status:** Accepted  
**Date:** 2026-09-19

**Decision:** V1 is the Java schema authority for empty/new databases. Flyway is enabled only by an
explicit fresh-database profile or test configuration. Hibernate validates and never creates or
updates schema. Seed data remains outside production migrations. Existing EF databases require a
read-only catalog comparison and an approved baseline action; `baseline-on-migrate` stays false.

**Consequences:** Clean environments are reproducible without weakening legacy-database safety.
Production adoption remains blocked until the real catalog is verified.
