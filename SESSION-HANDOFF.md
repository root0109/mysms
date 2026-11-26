# Session handoff — ARC-7 reorganisation + PATCH rule (written 2026-11-26, end of session 2)

> **Temporary working note** (committed so it travels with the repo). Read it fully, act on it, then delete it at
> the start of the next session. The lasting record is: `docs/design/MODULE-AND-PRODUCT-REORGANISATION.md` §11,
> `ROADMAP.md` (ARC-7, ARC-8, ARC-9), `JAVA-CODING-RULES.md` (§7.2 PATCH, §14 foreign keys), `DEPLOYMENT.md` (RabbitMQ).

## 0. Start here (first hour of the next session)

1. `git log --oneline -8` — expect the commits of §1 on top of `df03488`. Delete `_to_delete/` (patch copies only).
2. Local gate (CLAUDE.md §9): `mvn clean install` in `backend/` (JDK 17; PostgreSQL 16, RabbitMQ, Pub/Sub emulator,
   Docker for the GCS emulator — or the `MYSMS_TEST_*` variables of BUILD-AND-RUN §2) and `pnpm verify` in `clients/`.
   Everything was green in the cloud copy, but five modules (learning, platform, identity, academics, students) were
   not re-run after the last tiny `CrudContract` change (`patch.fixed` exemption) and `pnpm verify` ran as its parts
   (prettier, `turbo run generate typecheck lint test`, `turbo run build --concurrency=1`), never as one command.
   `PlatformFilesComponentTest` / `PlatformImportsComponentTest` need Docker and never ran in the cloud.
3. Then work §4 in order: next task is **ARC-9 gRPC PATCH** (owner-decided), then reorganisation 2c → 2d → 3 → 4.

## 1. What this session committed (on top of `df03488`)

| Commit | Content | Verification |
|---|---|---|
| `f7671a3` | Previous session's backend restructure (22 → 10 modules of 22 areas), committed as it was | cloud build; later full gate |
| `6217ec1` | **Step 1 — RabbitMQ split deployment.** `messaging/MessagingModules` = area → module table (`AREAS`, `moduleOf`); `RabbitEventBus.expectedPublisher` expects `mysms-{module}` of the module serving the event's area (unknown area → dead-lettered); conformance G9 checks the table equals every module's areas; DEPLOYMENT §RabbitMQ: one broker user per module with `(hr|payroll)`-style regexes and a module → areas table, queues stay per area (`mysms.<area>.events/.dead`), `replay-dead <area>` | messaging + conformance green; Sonnet-reviewed |
| `d192d4e` | **Step 2a — drop `payroll.staff_member`.** payroll reads `hr.staff` (sibling area) for owner-only payslip reads (`PayslipRepository.isOwnStaffRecord`, `READABLE_BY` subqueries) and privacy erasure; `StaffMemberSync`/`StaffMemberRepository` deleted; DEPLOYMENT: areas of one module keep one DB URL/user | hrms + conformance green; Sonnet-reviewed |
| `9feea8a` | **Step 2b — `hr.staff_subject` → `academics.teacher_subject`.** `/v1/academics/teacher-subjects` (5 ops, `academics.teacher_subject.*`), row scoped by its subject's institution, FK to `academics.subject` (RESTRICT; SubjectService refuses deleting a referenced subject), `staff_id` logical ref archived on `hr.staff.deleted` (AcademicsReferenceCleanup); hr side, protos, lock, operation tables, iam permission seeds, docs, clients (`useStaffAssignments('subjects')` uses `TeacherSubjectApi`) moved | all modules + app + clients green; Sonnet-reviewed |
| `26eb21e` | **PATCH rule §7.2 (owner decision), modules finance, learning, privacy, comms, campus, platform, identity, students** | each batch green + Sonnet-reviewed |
| `7153075` | **PATCH: academics + hrms**, transition removed (`UpdateMask`, `ProtoBridge.updateMask`, D2 allow-list, client masked branch); `CrudContract` patch checks; JAVA-CODING-RULES §14 FK rule; ROADMAP ARC-8 | full gate green (counts below); Sonnet-reviewed |

Full gate counts (cloud copy): core 167, conformance 9, messaging 34, auth-client 71, privacy-client 30, learning
2075, privacy 149, platform 380, identity 445, academics 714, students 494, hrms 292, finance 257, campus 431,
comms 581, app 14 (monolith; table count 742, operations 2123), demo-seed 8; clients 37/37 tasks + build 5/5.

### 1.1 The PATCH rule as built (JAVA-CODING-RULES §7.2, §8.3, D2)

- `<Resource>Update` DTOs: every patchable field `private @Nullable Optional<CONSTRAINTS T> x;` — absent (null) =
  untouched, JSON null (`Optional.empty()`) = clear, value = set; `@NotNull` inside for required columns; write-only
  secrets are `Optional<@NotNull …>` (replaced, never wiped) except lms `Settings.unsplashAccessKey` (documented
  clearable). No `updateMask` on REST PATCH. `@Valid` on type arguments (`List<@Valid X>`), never on containers
  (HV000271/HV000272 fixed everywhere).
- Services: `List<String> fields = apply(entity, changes)` with one `patch.apply("name", changes.getName(),
  entity::setName)` line per field through core `com.mysms.core.web.Patches` (`applied()` feeds the audit list;
  `Patches.value(...)` for pre-apply shape checks); then the merged DTO is re-validated and business checks run as
  before. Never reflection, never MapStruct.
- gRPC (until ARC-9): `Update<X>Request` + `FieldMask`; `ProtoBridge.changes` maps masked-set → value, masked-unset →
  null, unmasked → absent; camelCase or snake_case mask names; a masked empty required list stays null (refused).
- OpenAPI (`OpenApiConfiguration.mysmsContractSchemaCustomizer`): no `*Update` property is required; clearable ones
  carry `"null"` (`anyOf` for `$ref`, null in enums).
- Tests: `auth-client` `CrudContract` checks the three states per updatable resource from each crud-spec entry's
  `"patch": {"keeps": [...], "clears": [...], "fixed": [...]}` and fails if the entry is missing (`fixed` = fields the
  service never lets change after create; only hr LeaveBalance `academicYearId`).
- Clients: `core/src/api/patch.ts` `patchOf(changes)` sends only the keys given, `undefined` → null (clear);
  `records/api.ts` `toPatch`; `fixtures/src/patch.ts` `patched` applies a body like the server; admission
  `applicationChanges(saved, form)` diffs the saved application.
- Behaviour notes (accepted): unknown PATCH properties are ignored (no 400 any more); an empty body `{}` still writes
  (see §3 open decision).

## 2. Owner decisions (all binding)

1. **No foreign keys across modules — sacred, never relaxed** (JAVA-CODING-RULES §14). Inside one module (its
   areas' schemas) foreign keys are allowed. The earlier open question about CLAUDE.md §10 is closed.
2. **PATCH = reference-service `Optional` pattern** for every PATCH API now and in future (done, §1.1).
3. **ARC-9: gRPC PATCH moulded on the reference service** (`/Users/VaibhavSingh/Development/Repos/plt-organization-service`,
   READ-ONLY — never change it). No comparison analysis wanted. See §4 row 1.
4. **time_attendance = a NEW area in hrms** (schema `time_attendance`, `/v1/time-attendance/...`, permissions
   `time_attendance.*`, own operation table, OpenAPI document, SDK namespace) → 23 areas.
5. **teacher_subject in academics** (done, `9feea8a`).
6. **Holidays / master data:** ARC-8 (ROADMAP) — every master table gets a nullable `tenant_id` (NULL = global,
   value = tenant row, same table); holidays become one `master.holiday` (global statutory + tenant rows, optional
   institution/location scope) replacing the earlier plan of a separate `master.public_holiday`. **ARC-8 is picked up
   after the PATCH work and the reorganisation.** hrms reads holidays through an **event-fed copy in hr** (no runtime
   call). Consequence for step 2d: do the leave/pay parts of BE-6 now; wire holidays once ARC-8 exists (or do ARC-8's
   holiday slice first if the owner prefers — ask).
7. **Front end:** do the client restructure after backend steps 2c, 2d and 3 (owner approved).
8. **Models/agents (CLAUDE.md §8):** Opus implements, a separate Sonnet agent reviews each batch, no Fable/Haiku,
   at most 2–3 agents. Owner stops sessions at ~90 % of the 5-hour usage limit — checkpoint (commit) after every batch.

## 3. Open owner decisions (ask, do not assume)

1. **Empty PATCH body `{}`** still bumps row version + writes audit + event. Option: in every service, return the
   current row when `apply(...)` returns no fields; `CrudContract` would then expect the version unchanged.
2. **Reference-service style items** (from the read-only review of `plt-organization-service`): per-response error-code
   examples in OpenAPI (worth adopting?); company logger (`@CustomLog`, `@GrpcServiceLogging`) vs our SLF4J. Our
   gRPC auth interceptor and reflection-off default stay (security). Everything else (layering, transactions, JDBC
   repositories, Flyway, problem+json, paging, idempotency, outbox, testing) stays ours.
3. **Pre-existing issues found by reviews** (not introduced here; decide/fix): `assessment` `AssignmentApproval` is
   directly PATCHable for `decision`, `decidedByUserId`, `decidedAt` (`AssignmentService.java:~760`) although
   approvals should be workflow-only (CLAUDE.md §2 says so for homework approvals); client-writable server-owned
   fields `ResultTemplate.status`, `Exam.resultsEntered`, `Marksheet.generatedByUserId/totalPassed/totalFailed`,
   `StaffAttendance.leaveApplicationId`, `TeachingAssignment.periodsRemaining`; `teacher_subject.staff_id` and
   `teaching_assignment.staff_id` are not validated against hr (logical references; could check `academics.staff_record`).
4. Older, still open: design §9 product vocabulary, §10 packaging.

## 4. Next, in order (one change set each; green build + Sonnet review + commit before the next)

| # | Work | Details |
|---|---|---|
| 1 | **ARC-9 gRPC PATCH like the reference** | Each update RPC takes the partial resource directly, like the reference's `rpc patch(OrganizationPartial) returns (Organization)` (keep our RPC naming `Update<X>`; decide request message name `<X>Partial` or `<X>Update`); clearable scalars as `Nullable*` `oneof` wrappers (`NullableString { google.protobuf.NullValue null = 1; string value = 2; }`, `NullableBool`, …, in core's common proto); a field the client does not set = absent; lists in wrapper messages so absence is preserved (NOT the reference's always-replace flaw); drop `FieldMask`/`update_mask`/`validate_only`? (keep `validate_only` + `request_id` unless owner says otherwise); explicit proto → `<X>Update` mapping (MapStruct proto mappers like the reference's `OrganizationProtoMapper`/`ProtoMapper`, or ProtoBridge — owner chose the reference: use explicit mappers) into the same `Optional` DTO and the same business service; update `field-numbers.lock.json`, every caller's verbatim proto copy (G1), gRPC parity tests, `ProtoBridge`, JAVA-CODING-RULES §7.2/§10 (add/adjust only, keep § numbers). Batch per module like the PATCH migration. |
| 2 | **2c staff attendance → new hrms area `time_attendance`** | Create `database/migrations/time_attendance/V1_<order>_001__time_attendance_baseline.sql` (move `attendance.staff_attendance` with its monthly partitions, outbox/inbox/idempotency/operation infra tables, RLS); hrms pom migrations dir; `mysms.auth.modules[2]=time_attendance` in hrms properties; `mysms.time_attendance.datasource.*` keys; config/DB/events classes `com.mysms.hrms.timeattendance.*` (package rule: `com.mysms.<module>.<area>.<layer>`; check the area-name → package mapping for underscores); move `StaffAttendance*` (api, model, controller, business, repository, mapper, domain, grpc, events) from `academics/.../attendance`; proto `mysms/time_attendance/v1` + lock; operation table `time_attendance.json` (operation ids/permissions `time_attendance.*`); iam permission catalogue rows; `TestDatabase.SERVICES`; `MessagingModules` (+ G9); app `application.properties` sorted modules + in-process client; `OpenApiGroupsEnvironmentPostProcessor`/area OpenAPI test `TimeAttendanceOpenApiComponentTest`; crud-spec/reference-cleanup; table counts (attendance 63 → fewer, new area count, app 742 unchanged); demo-seed (attendance seeder staff part); docs (database README/data dictionary/logical-references/ERD, DEPLOYMENT area lists and RabbitMQ table, ARCHITECTURE, design §11, ui/02); clients: SDK gets a `time_attendance` namespace (sdk/core generators find each area's document — check `clients/packages/{sdk,core}/scripts/generate.mjs`), hooks/screens/fixtures for staff attendance move to the new path. hr leave ↔ staff attendance link (`StaffAttendance.leaveApplicationId`) becomes an in-module reference (FK allowed inside hrms). |
| 3 | **2d BE-6 inside hrms** | Leave days exclude weekends and holidays (`hr/repository/LeaveLedgerRepository.java` note at ~line 31, `LeaveDays`); payroll counts unpaid leave (leave types without pay) and percentage pay components (payslip run in `payroll/business/PayslipService`); holidays via an hr event-fed projection — source per decision 6 (ARC-8 `master.holiday`) — ask whether to do ARC-8's holiday slice first. |
| 4 | **Step 3 sibling-area gRPC → direct Java calls** | admission→sis, attendance→timetable, assessment/attendance/timetable→academics, iam→people, org/platform→master: replace `integration/*GrpcClient` calls between areas of the SAME module by calls to the sibling area's business service (JAVA-CODING-RULES §2.2 allows and prefers it); remove the now-unused in-process `grpc.client.*` keys and conformance G9 expectations accordingly. |
| 5 | **Front-end reorganisation (approved)** | design §4: `clients/packages/kernel` + `packages/features/*` (per product feature area), ESLint boundaries, split `ui/02-navigation-and-screens.md` per product; the SDK keeps one namespace per area. |
| 6 | Test-log noise in `platform` (from session 1, still open) | `PlatformAuditTrail` consumes every area's `audit.recorded`; org/master/platform component tests write rows for made-up tenants, so `platform.audit_log` inserts fail `fk_audit_log_tenant_id` and the in-process bus redelivers (~250 warnings). Fix: a component-test helper in `backend/platform/src/component-test/.../platform/` inserting the test tenant into `platform.tenant` (`INSERT … (id, code, name)` as `PlatformPrivacyTaskComponentTest:66`), used wherever org/master/platform tests choose a tenant; confirm `grep -c "could not handle event" backend/platform/target/surefire-reports/*.xml` is 0. |
| later | students ↔ academics module cycle (event-fed projection one way); product entitlements (ask owner first); HRMS apps (`apps/web-hrms`, employee phone variant, console entitlements, `app-hrms`/`app-learning`, company/branch/office institution types); ARC-8 master data | design §11.2, ROADMAP ARC-7/ARC-8 |

## 5. How the work was done (environment notes)

- The owner's Mac VM (device shell) has JDK 11, no Maven, ~3 GB RAM: edit files there, build elsewhere. Git commits
  need delete permission in the folder (git removes its lock/tmp files); commit with
  `git -c user.name="Vaibhav Singh" -c user.email="vaibhav.singh@brightinsight.com" commit …` (no git config stored).
  The repo has **no git remote** — the owner pushes manually.
- Cloud build recipe: JDK 17 (`apt-get install openjdk-17-jdk-headless`), native PostgreSQL 16 (`postgres`/`postgres`),
  `rabbitmq-server -detached`, `gcloud beta emulators pubsub start --project=mysms-test --host-port=localhost:8085`;
  env `MYSMS_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/postgres`, `MYSMS_TEST_JDBC_USER/PASSWORD=postgres`,
  `MYSMS_TEST_AMQP_URI=amqp://guest:guest@localhost:5672`, `MYSMS_TEST_PUBSUB_EMULATOR_HOST=localhost:8085`; no Docker:
  `-Dtest='!PlatformFilesComponentTest,!PlatformImportsComponentTest' -Dsurefire.failIfNoSpecifiedTests=false`.
  On 2 cores a full reactor build takes well over an hour; background jobs die on environment restarts — run modules
  in the foreground with `timeout`. Web build needs `NODE_OPTIONS=--max-old-space-size=3072` and `--concurrency=1`.
- Package rule: area named like its module → `com.mysms.<module>.<layer>`; else `com.mysms.<module>.<area>.<layer>`.
  Component tests of a multi-area module register every area's DB; Surefire caps cached contexts at 4.
