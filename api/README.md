# MySMS API design

MySMS exposes every capability through two equivalent interfaces: a **REST/JSON API** (OpenAPI 3.1) for web, mobile and partner integrations, and a **gRPC API** (proto3) for service-to-service calls and high-throughput clients. Both are thin adapters over the same module contract and services, so they always stay in step: each REST operation corresponds to exactly one gRPC method with the same request and response fields.

## At a glance

22 domain modules, one contract each; every operation is served over both REST and gRPC. Counts per module, and
which operations are served or answer 501: [operations-status.md](operations-status.md) (generated; do not copy its
numbers into other docs).

## Files

| Path | Contents |
|---|---|
| `README.md` | This design guide. |
| `operations.md` | Reference, kept by hand: every operation's REST method and path, gRPC service and method, permission code, public flag and description (ROADMAP DOC-2: generate it). |
| `../backend/<module>/target/openapi/api-docs.yml` | REST contract of one module (OpenAPI 3.1), written by the module's build from its code (springdoc, `<Module>OpenApiComponentTest`); not kept in git. |
| `../backend/<module>/src/main/resources/proto/mysms/<module>/v1/<module>.proto` | gRPC contract of one module (`package mysms.<module>.v1`), owned by the module; a module that calls another carries a byte-identical copy of the callee's proto. |
| `../backend/core/src/main/resources/proto/mysms/common/v1/` | Shared gRPC messages (Operation, upload/download targets, search hits..., the `ErrorMessage` of refused calls). |
| `../backend/conformance/src/main/resources/field-numbers.lock.json` | Assigned proto field numbers (keeps the wire format stable; checked by the conformance build, G2). |
| `standard-resources.json` | Owned data, checked by the build (`OperationStatusReportTest`): the resources whose standard operations and state transitions the backend serves (read by the clients' record screens). |
| `operations-status.md` | Generated: whether this build serves each operation, and why not when it answers 501. |
| `state-machines.md` | Reference, kept by hand in step with each service: the state transitions the backend serves, with the states each is allowed from and what it sets. |

Everything about the API lives in this one folder; paths are relative to it.

## Design principles

1. **Resource-oriented.** Each database table is a resource with the standard methods List, Get, Create, Update (partial) and Delete (soft). Rows that cannot exist without a parent are nested under it (`/v1/sis/students/{studentId}/guardians`). Nesting is at most one level deep; deeper children are addressed directly with the parent id as a field (e.g. `lms.lesson` carries `chapterId`).
2. **Custom methods for behaviour.** State changes that are not plain field edits are explicit operations named with a verb, using the `:verb` suffix: `POST /v1/admission/applications/{applicationId}:enroll`. On every resource that has workflow operations, the workflow-controlled fields (`status`, `isPublished`, `decision`, `reviewStatus`, `isCompleted`, `isSnoozed`, `enrollmentStatus`) are read-only in the contract, so they can only change through those operations. Business rules and audit therefore run in one place.
3. **Module = bounded context.** URL prefix `/v1/<module>/`, proto package `mysms.<module>.v1`, Java package `com.mysms.<module>.v1`. A module only references another module's resources by id, matching the database boundaries.
4. **One contract, two transports.** REST and gRPC are implemented as thin adapters over the same application services (Spring Boot: Spring MVC controllers + grpc-java services). Neither transport has behaviour the other lacks.

## URL and naming conventions

| Element | Convention | Example |
|---|---|---|
| Base path | `/v1/<module>/<resource-plural>` (kebab-case) | `/v1/attendance/attendance-sheets` |
| Item path | `.../{<resource>Id}` | `/v1/sis/students/{studentId}` |
| Nested child | `/<parent>/{parentId}/<child>` | `/v1/sis/students/{studentId}/guardians/{studentGuardianId}` |
| Singleton | no id | `/v1/lms/settings` |
| Custom method on item | `POST .../{id}:verb` (GET for queries) | `POST /v1/assessment/exams/{examId}:recordMarks` |
| Custom method on collection | `.../<plural>:verb` | `POST /v1/timetable/sessions:generate` |
| Module-level view | `/v1/<module>/<noun>` or `/v1/<module>/me/<noun>` | `GET /v1/finance/students/{studentId}/feeStatement`, `GET /v1/lms/me/dashboard` |
| Custom method on a nested child | addressed by the child id alone (ids are globally unique) | `GET /v1/lms/chapters/{chapterId}:scormLaunch` |
| JSON fields | lowerCamelCase | `academicYearId`, `createTime` |
| Proto fields | lower_snake_case (JSON name = lowerCamelCase) | `academic_year_id` |
| Operation id | `<module>.<GrpcMethod>` | `sis.ListStudents` |
| gRPC service | one per aggregate root, `<Root>Service` | `mysms.sis.v1.StudentService` |

## Standard methods

| Method | REST | gRPC | Notes |
|---|---|---|---|
| List | `GET /…/{plural}` | `List<Plural>` | `page` (one-based, default 1), `size` (1-500, default 50), `cursor`, `filter`, `sort`, `fields`; response `PagingInfo` = `data` + `paging` (`NavigationInfo`). gRPC: `pageable` (`PageableRequest`) in, `items` + `pagination` out. |
| Get | `GET /…/{id}` | `Get<Resource>` | `ETag` response header = row version. |
| Create | `POST /…/{plural}` → 201 + `Location` | `Create<Resource>` | `Idempotency-Key` header / `request_id`; `validateOnly` (runs every check, persists nothing, answers 200 with the would-be resource). |
| Update | `PATCH /…/{id}` | `Update<Resource>` | Partial update with `updateMask`; `If-Match` (REST) / `etag` field (gRPC) is mandatory → 412 on a stale version. |
| Delete | `DELETE /…/{id}` → 204 | `Delete<Resource>` | Soft delete (sets `deleted_at`); `If-Match` required. |

Output-only fields (`id`, `createTime`, `updateTime`, `createdBy`, `updatedBy`, `etag`, the parent id of a nested child, and workflow-controlled fields) are marked `readOnly` in OpenAPI and "Output only" in proto comments; values sent for them are ignored (ROADMAP DEC-9 decides whether to reject them instead). `tenantId`, `deletedAt` and `rowVersion` are never exposed: tenant comes from the access token and the row version travels as the ETag.

### Field behaviour (who may write what)

Every schema separates four field classes, and the generated SDKs enforce them at compile time:

- **Client-settable** fields appear in the resource schema and in its `<Resource>Update` schema.
- **Create-only** fields (identity of a row: parents, person links) appear in the resource schema but not in `<Resource>Update`.
- **Output-only** fields are `readOnly: true` (REST) / `Output only.` (proto): audit columns, computed totals, counters, statuses owned by workflows (gradebook points, payment status, exam marks, enrollment history columns). Values sent for them are ignored (DEC-9).
- **Workflow-owned rows** have no generic write operations at all: gradebook entries (`GradeEntry`, `BatchGradeEntries`, `PublishItemGrades`), invoice lines and dues (`CreateInvoiceFromDues`, `GenerateFeeDues`), payments (`StartCheckout`, `RecordPayment`, `RefundPayment`), payslips and their lines (`GeneratePayslips`, `MarkPayslipPaid`, `CancelPayslip`), certificates (issue flows), badge awards (`AwardBadge`), exam results and marksheet lines (`PopulateExamResults`, `RecordExamMarks`, `GenerateResults`), attendance records (`RecordAttendance`, `TakeAttendanceForSession`), quiz submissions (`StartQuizAttempt`, `SaveQuizAnswers`, `SubmitQuiz`) and LMS/cohort enrollments (`EnrollIn*` / `BatchEnrollIn*`). Documents (posted invoices, paid payslips, issued certificates, published marksheets) are immutable; corrections are new documents or logged workflow changes.

### Sensitive fields

- Credential material is never part of any resource: password hashes, token hashes and storage keys/locations are not exposed. Passwords change only through `iam.ChangePassword`, `iam.ResetPassword` and `iam.SetUserCredentials`; API tokens are created with `iam.CreateApiToken`, which returns the secret once.
- Columns stored encrypted (`…_enc` in the database: national ids, bank account numbers, tax identity, provider client secrets, meeting passwords, API keys) appear as **write-only plaintext inputs** (e.g. `nationalId`, `accountNumber`; OpenAPI `writeOnly`, proto "Input only") plus an output-only masked field (`nationalIdMasked` = "••••1234"). The plaintext is never returned.
- Push tokens (`pushToken`) are write-only.

### Filtering and ordering

`filter` uses a subset of the AIP-160 grammar over the resource's fields (JSON names): comparison `= != < <= > >=`, `:` (has / contains for strings and repeated fields), `AND`, `OR`, `NOT`, parentheses, quoted strings, `null`. Example: `status = "active" AND academicYearId = "0190…" AND createTime >= "2026-04-01T00:00:00Z"`. `sort` takes `field,asc|desc` (the direction is optional, ascending by default) and is repeated for several criteria in priority order (`sort=status&sort=createTime,desc`; one comma list `sort=status,asc,createTime,desc` reads the same). Each resource declares the fields it sorts on; the row id always breaks ties, so the order is total. Unsupported filter fields return 400 with `code = INVALID_FILTER`, unsupported sort fields 400.

### Pagination

Every operation that returns a list page by page (the standard List and the custom queries) answers the same shape, the owner's `PagingInfo`:

```json
{
  "data": [ … ],
  "paging": {
    "page": 3,
    "size": 50,
    "first": "/v1/hr/staff?page=1&size=50&sort=name",
    "previous": "/v1/hr/staff?page=2&size=50&sort=name",
    "next": "/v1/hr/staff?page=4&size=50&sort=name&cursor=eyJ2Ijoy…"
  }
}
```

- `page` is one-based; `paging.size` is the number of items in this page. The links are relative and keep the request's filter, sort and field mask; a link that does not apply is absent (`previous` on page 1, `next` on the last page).
- **`next` carries a cursor**: the sort keys of this page's last row, bound to the sort, the filter (and parent or query parameters) and the page it opens, valid for 24 hours. Following it reads the next page by keyset — an index seek, as fast on page 10,000 as on page 2, and stable while rows are added in front. A cursor presented for another page, sort or view, or after expiry, is 400.
- **A page number without a cursor** (`first`, `previous`, a jump to page n) is read by offset, which reads and discards every skipped row; it is therefore limited to the first 10,000 rows (400 beyond, "follow the next links"). Deeper pages are reached by `next` links, and `previous` is omitted where it would lie beyond that limit.
- Lists are not counted: there is no total (a count of a large table costs as much as reading it). `next` tells whether more rows exist.
- gRPC: the request's `pageable` (`mysms.common.v1.PageableRequest`: `page`, `size`, repeated `sort`, `cursor`) and the response's `pagination` (`mysms.common.v1.Pagination`: `page`, `size`, `first`, `previous`, `next` as page numbers, 0 where absent, and `next_cursor`).
- An operation that writes several items and returns them (`RecordAttendance`, `SetUserRoles`, `GenerateTerms`…) returns them all at once as `{"items": [...]}`; it is not a page of a list. So do the two bounded reads of the caller's own data, `iam.ListMySettings` and `iam.ListMfaCredentials`.

## Data types

| Database | JSON (REST) | Proto | Notes |
|---|---|---|---|
| uuid | string, format uuid | string | IDs are UUIDv7. |
| varchar / text / inet | string | string | `maxLength` from the column. |
| integer / smallint | integer int32 | int32 | |
| bigint | string, format int64 | int64 | Proto3 JSON maps int64 to a string. |
| numeric | string, format decimal | string | Exact decimal as a string (money, marks, percentages). Never a float. |
| boolean | boolean | bool | |
| date | string, format date | string `YYYY-MM-DD` | |
| time | string `HH:MM[:SS]` | string | |
| timestamptz | string, format date-time (RFC 3339, UTC) | google.protobuf.Timestamp | |
| interval | string `"<seconds>s"` | google.protobuf.Duration | |
| jsonb | any JSON | google.protobuf.Value | |
| enum (CHECK) | string enum with the database value, e.g. `"present"` | nested proto enum, e.g. `STATUS_PRESENT` | Every proto enum starts with `<ENUM>_UNSPECIFIED = 0`. The REST value is the lowercase database value shown in the proto comment above each enum value. Adapters map them 1:1. |
| nullable column | property may be absent or null | proto3 `optional` (scalars/enums) or unset message | |

## Authentication and tenancy

- **Tokens:** OAuth 2.0 / OpenID Connect bearer JWT in `Authorization: Bearer …` (REST) or the `authorization` metadata key (gRPC). `iam.Login` and `iam.RefreshToken` issue tokens for first-party apps. `iam.SignUp` creates the account at the school the student names (one of the active schools `iam.ListSignUpInstitutions` offers; the sign-up role is granted at that school only, never tenant-wide) and sends a verification; the student then signs in with `iam.Login`. Partners use the OAuth client-credentials flow. Access tokens live 15 minutes and refresh tokens rotate on every use. Logout revokes refresh tokens and push devices.
- **Claims:** `sub` (user account id), `tid` (tenant id), `roles` (codes with scope), `perms` (the effective permission codes, resolved by the issuer from the roles), `ins` (institutions the roles are scoped to; `*` when a tenant-wide grant covers every institution), `jti` (token id, for audit and revocation), `amr` (authentication methods; platform staff tokens carry `mfa`), `act` (impersonating administrator, when present), `pid` (the person record linked to the account, when there is one; `people.GetMyPerson` and `people.UpdateMyPerson` act on it), `sid` (the sign-in session, which `iam.Logout` ends). Every module verifies tokens locally against the issuer's keys; no call to the issuer per request. The tenant is always taken from the token. A request can never address another tenant, and PostgreSQL row-level security on `tenant_id` enforces the same rule.
- **Institution context:** the optional `X-Institution-Id` header (`x-institution-id` metadata) selects which of the caller's institutions a request acts in. It must fall within the caller's role scopes. Claim `tperms` lists the permissions carried by tenant-wide (or direct) grants; only those reach every institution and tenant-level records.
- **Authorization:** every operation declares a permission code (`x-permission`, e.g. `attendance.attendance_sheet.update`, `admission.application.enroll_applicant`). Codes are rows of `iam.permission`; roles bundle them, and `iam.user_role` scopes grants (institution, class section, course, cohort...). Data-level rules apply on top of the permission check, for example:
  - a guardian sees only their linked children;
  - a teacher sees only their class sections;
  - a student sees only their own submissions.
  - personal resources marked with `x-owner-field` (such as notification preference sets) are always restricted to the
    authenticated account, even when a tenant-wide permission is present; nested resources inherit that owner boundary.
- **Platform administration:** operations that act across tenants are reserved to platform staff and use the separate `platformAdminAuth` scheme (audience `mysms-platform-admin`, MFA required). They cover tenant management, SaaS plans, system settings, user types, reserved keywords, sales-enquiry handling, reference-data maintenance and clearing demo data. A tenant user's token can never call them.
- **Impersonation:** `iam.StartImpersonation` requires a reason and returns a single non-refreshable access token valid for at most 30 minutes (15 today). The target must have strictly lower privileges and lie within the administrator's institution scope. Every call carries both identities in the audit log (`act.sub` in the token, `impersonatorUserId` on every audit record), and `iam.StopImpersonation` ends the session; the token itself stays verifiable until it expires (tokens are verified locally, there is no revocation list). Nothing that outlives the impersonation — passwords, second factors, API tokens, terms, another impersonation, a role switch — can be done with it.
- **Personal API tokens:** `iam.CreateApiToken` answers the secret (`mysms_pat_…`) once; only its SHA-256 is stored. Presented as the `refreshToken` of `iam.RefreshToken`, it is exchanged for a 15-minute access token of its owner, limited to the token's `abilities`; it is not rotated. Owners list and revoke their own tokens; administrators need the permission tenant-wide.
- **Mailed secrets:** password-reset, sign-up and e-mail verification codes are `<tenant>.<256 random bits>`, stored as SHA-256, single use; a reset code works 30 minutes, a sign-up or verification code 24 hours. `iam.RequestPasswordReset` and `iam.SignUp` answer the same whether or not the account exists. At most three codes of a kind per account in 30 minutes.
- **Role grants:** `iam.SetUserRoles` and user-role creation only grant roles whose permissions are a subset of the caller's own, within the caller's scopes, so no privilege escalation is possible.
- **Public operations** (callable without a token): login, refresh, sign-up (and the schools it offers) and password reset; branding and institution finder; the published LMS catalogue (course and cohort lists, course outline, preview lessons, cohort courses, reviews, related courses, categories, programs, statistics, public settings, public member profiles); certificate verification; downloads of files owned by published catalogue content; job listings; reference lists (countries, states, cities, currencies); SaaS plans; creating a sales enquiry. Guest reads return published rows only, with internal fields omitted (guest projection).
- **Guests:** a request without a token names its school with `X-Tenant-Code` (`x-tenant-code` metadata). The edge of a module that serves guest reads resolves it through `iam.ResolveTenantCode` over gRPC (answers kept for a minute) and binds that tenant only; an unknown, suspended or closed school is 404, and 503 when iam cannot answer. The header is ignored next to a token and by modules without guest reads. A guest read that names no school is 404. A signed-in caller without the operation's read permission gets the same guest projection.
- **Inbound webhooks** (`finance.ReceiveGatewayWebhook`, `platform.ReceiveSubscriptionWebhook`, `lms.ReceiveConferencingWebhook`, `lms.ReceivePlagiarismWebhook`, `lms.ReceiveEbookConversion`, `lms.ReceiveToolOutcome`) carry no token. They are authenticated by the provider's signature over the raw request body, which is passed through unchanged. The tenant is resolved from the stored reference (gateway order id, meeting id, provider submission id, conversion job reference or LTI result id), and events are de-duplicated by the provider event id.

## Errors

REST errors use RFC 9457 `application/problem+json` with a stable `code`, a `traceId`, and `errors[]` for field-level validation. gRPC errors use `google.rpc.Status` with `ErrorInfo` (same `code` as `reason`), `BadRequest` (field violations) and `PreconditionFailure` details.

| Situation | HTTP | gRPC code | `code` examples |
|---|---|---|---|
| Malformed request / bad filter | 400 | INVALID_ARGUMENT | `INVALID_ARGUMENT`, `INVALID_FILTER` |
| Field validation failed | 422 | INVALID_ARGUMENT | `VALIDATION_FAILED` |
| Not authenticated | 401 | UNAUTHENTICATED | `UNAUTHENTICATED`, `TOKEN_EXPIRED`, `INVALID_CREDENTIALS` (sign-in and refresh: one answer for every failure, so accounts cannot be probed) |
| Not permitted | 403 | PERMISSION_DENIED | `PERMISSION_DENIED` |
| Not found / not visible | 404 | NOT_FOUND | `NOT_FOUND` |
| Duplicate / invalid state transition | 409 | ALREADY_EXISTS / FAILED_PRECONDITION | `DUPLICATE`, `INVALID_STATE_TRANSITION`, `SETTING_LOCKED`, `CONSENT_FOUR_EYES` (a paper consent verified by the staff member who recorded it), `REQUEST_IN_PROGRESS` (a retry while the first request with its `Idempotency-Key` still runs) |
| `Idempotency-Key` reused for a different request | 422 | INVALID_ARGUMENT | `IDEMPOTENCY_KEY_REUSED` |
| An entity type the contract lists that no module imports yet (`platform.StartImport`, `platform.GetImportTemplate`: `lms_course_package`) | 422 | INVALID_ARGUMENT | `IMPORT_TYPE_UNAVAILABLE` |
| Stale ETag | 412 | ABORTED | `ETAG_MISMATCH` |
| If-Match missing (or `*`) on an update or delete | 428 | FAILED_PRECONDITION | `PRECONDITION_REQUIRED` |
| Rate limited | 429 | RESOURCE_EXHAUSTED | `RATE_LIMITED` (with `Retry-After`) |
| Operation in the contract but not served by this release | 501 | UNIMPLEMENTED | `NOT_IMPLEMENTED` |
| Server error / unavailable | 500 / 503 | INTERNAL / UNAVAILABLE | `NOT_CONFIGURED` (503: an external provider this operation needs — SMTP, SMS, file store, payment gateway, stock photos — is not configured for the deployment) |

## Concurrency, idempotency and long-running work

- **Optimistic locking:** the ETag is the row version. Update and delete require it (`If-Match` / `etag`). Workflow operations accept it optionally and fail with 412 when it is stale.
- **Idempotency:** every POST except webhooks and the `master` creates accepts an `Idempotency-Key` header (REST) or a `request_id` field (gRPC; present on every other create and every custom POST request). `master` holds global reference data written only by platform operators and has no idempotency store: a retried create is detected by the unique natural key (ISO code, name) and answered with 409. Retrying with the same key within 24 h returns the original response (status, body, `Location`, `ETag`), marked `Idempotent-Replayed: true`; the same key with a different request (body, path, user) is 422 `IDEMPOTENCY_KEY_REUSED`, and a retry while the first request still runs is 409 `REQUEST_IN_PROGRESS`. A server failure (5xx) and a try-again answer (408, 425, 429) are not kept, so their retry runs again; a response body larger than 1 MB is not kept (the replay carries the status and headers, such as `Location`). Expired keys are removed as the tenant uses new ones. The kept response lives in the owning module's `idempotency_key`, encrypted for the tenant. It is **required** (requests without it are rejected with 400 `INVALID_ARGUMENT`) on money and bulk operations: `finance.StartCheckout`, `finance.RecordPayment`, `finance.RefundPayment`, `finance.GenerateFeeDues`, `finance.CreateInvoiceFromDues`, `payroll.GeneratePayslips`, `admission.EnrollApplicant`, `platform.StartSubscriptionCheckout`, `lms.BatchEnrollInCourse`, `lms.BatchEnrollInCohort`, `comms.SendMessage`.
- **Asynchronous operations:** reports, imports, exports, bulk generation (fee dues, payslips, timetable, certificates) and large message sends return **202** with an `Operation`. Each such module owns its operations: poll `GET /v1/<module>/operations/{operationId}` at the module that returned it (the `module` field says which; cancel with `POST …/{operationId}:cancel`), or receive the `operation.completed` notification. `platform.ListOperations` is the cross-module job centre — an index the platform builds from every module's operation events, so a row can lag the owner by a few seconds; live status and cancellation always go to the owning module. Report and export output is a file, downloaded through `platform.GetDownloadUrl`.
- **Files:** a two-step upload.
  1. `platform.CreateUploadUrl` returns a pre-signed URL; the client PUTs the bytes to it.
  2. `platform.CompleteUpload` finalises the file (virus scan, metadata).
  - Resources then reference files by `…FileId`, and multi-file fields use `platform.attachments`. Downloads always go through permission-checked pre-signed URLs.
- **Webhooks in:** payment gateways (`finance.ReceiveGatewayWebhook`), conferencing providers (`lms.ReceiveConferencingWebhook`), plagiarism-check providers (`lms.ReceivePlagiarismWebhook`), the e-book conversion service (`lms.ReceiveEbookConversion`) and LTI tools returning scores (`lms.ReceiveToolOutcome`) post signed callbacks. Signatures are verified and events are processed idempotently.
- **Events out:** every state change publishes a domain event through the module outbox (e.g. `sis.student.enrolled`, `attendance.sheet.completed`, `finance.payment.received`). Other modules react to these events instead of reading each other's tables.

## Rate limits, versioning and deprecation

- Every operation declares a `429` response. *Planned* (no limiter is built yet): limits per token and per tenant, `429`s carrying `Retry-After` plus `RateLimit-Limit`, `RateLimit-Remaining` and `RateLimit-Reset`.
- The major version is in the path (`/v1`) and in the proto package (`.v1`). Additive changes (new fields, operations or enum values) do not change the version, so clients must ignore unknown fields and tolerate unknown enum values (every enum is marked `x-extensible-enum: true`; render unknown values neutrally). Breaking changes ship as `/v2` / `.v2`, with both versions served for at least 12 months. Today conformance G2 (field-number lock) blocks wire breaks; `buf breaking` and an OpenAPI diff in CI are planned (BLD-1).
- Deprecated operations are marked `deprecated: true` and `option deprecated = true`; responses will carry a `Sunset` header (not built; no operation is deprecated yet).
- **Wire stability:** `backend/conformance/src/main/resources/field-numbers.lock.json` records the field numbers of **every** message (map fields included), so any regeneration preserves the wire format.

## Deep links

- Every notification, message and e-mail that points at a screen carries one app-neutral path in `actionUrl` (`comms.in_app_notification.action_url`): `mysms://<module>/<resource path>`, using the same resource paths as this API without `/v1` (for example `mysms://lms/cohorts/{cohortId}/gradebook`, `mysms://sis/students/{studentId}`, `mysms://comms/notices/{noticeId}`).
- The React Native apps register the `mysms` scheme and, for links opened outside the app, universal links (iOS) / app links (Android) on `https://<tenant host>/app/<same path>`; the React web app serves the same path, so one link works on every client. A path the current app cannot show (e.g. a teacher screen in the parent app) falls back to the web URL.
- Links never carry tokens or personal data; the app authenticates and loads the record through the normal operations.

## Consent-gated operations

Consent lives in the `privacy` module; modules ask it through `privacy-client` (design:
[PRIVACY-AND-CONSENT-CENTRE.md](../docs/design/PRIVACY-AND-CONSENT-CENTRE.md) §7–§8). Consent is checked per
processing activity (`<module>.<activity>`), not per operation; for a minor only an active legal guardian's decision
counts, and some purposes are forbidden for minors whatever a guardian says.

- Enforced today: `people.CreatePublicProfile` needs the person's consent to `public_profile` (422 `CONSENT_REQUIRED`);
  `people.GetPublicProfile` and `UpdatePublicProfile` show another person's profile only with it (404 otherwise);
  privacy unreachable → 503 (fail closed).
- Each other consent-based activity of design §8 (photo and video publication, naming in posts, marketing, optional
  SMS/WhatsApp, learning analytics, external AI, proctoring, biometrics, sharing with employers) adds its check when
  its processing is built.

## Partitioned resources

- Audit log entries, access log entries, outbound messages, in-app notifications, attendance records and content view sessions are stored in monthly partitions. Their ids stay globally unique (UUIDv7 generated by the server), so Get / Update / Delete by id work as for any resource; List operations on them should always be filtered by a time range (or, for attendance, by sheet / date) so that only the relevant months are read.

## Modules

Counts per module: [operations-status.md](operations-status.md).

| Module | REST base | gRPC package |
|---|---|---|
| Master Data | `/v1/master/` | `mysms.master.v1` |
| Platform & Tenancy | `/v1/platform/` | `mysms.platform.v1` |
| Identity & Access | `/v1/iam/` | `mysms.iam.v1` |
| Organisation & Calendar | `/v1/org/` | `mysms.org.v1` |
| People & Profiles | `/v1/people/` | `mysms.people.v1` |
| Academic Structure | `/v1/academics/` | `mysms.academics.v1` |
| Student Information | `/v1/sis/` | `mysms.sis.v1` |
| Workforce / HR | `/v1/hr/` | `mysms.hr.v1` |
| Payroll | `/v1/payroll/` | `mysms.payroll.v1` |
| Admissions | `/v1/admission/` | `mysms.admission.v1` |
| Attendance | `/v1/attendance/` | `mysms.attendance.v1` |
| Rooms & Facilities | `/v1/facility/` | `mysms.facility.v1` |
| Timetable | `/v1/timetable/` | `mysms.timetable.v1` |
| Assessment & Results | `/v1/assessment/` | `mysms.assessment.v1` |
| Learning Management | `/v1/lms/` | `mysms.lms.v1` |
| Fees & Payments | `/v1/finance/` | `mysms.finance.v1` |
| Library | `/v1/library/` | `mysms.library.v1` |
| Transport | `/v1/transport/` | `mysms.transport.v1` |
| Communication & Community | `/v1/comms/` | `mysms.comms.v1` |
| Front Office | `/v1/frontoffice/` | `mysms.frontoffice.v1` |
| Careers | `/v1/careers/` | `mysms.careers.v1` |
| Privacy and Consent | `/v1/privacy/` | `mysms.privacy.v1` |

## Key workflows

The custom operations, grouped by business process. Each runs in one transaction, checks the allowed source state (409 `INVALID_STATE_TRANSITION` otherwise), writes the audit log and emits a domain event.

- **`iam.mfa_credential`**: `EnrollMfa`, `CompleteMfa`, `DisableMfa`, `ListMfaCredentials`, `RegenerateRecoveryCodes`
- **`iam.user_account`**: `Login`, `RefreshToken`, `Logout`, `SignUp`, `RequestPasswordReset`, `ResetPassword`, `ChangePassword`, `RequestVerification`, `ConfirmVerification`, `GetMe`, `StartImpersonation`, `StopImpersonation`, `ActivateUser`, `DeactivateUser`, `UnlockUser`, `ForcePasswordReset`, `CreateApiToken`, `SetUserCredentials`, `SetUserRoles`, `SwitchRole`, `AcceptTerms`, `CheckUsernameAvailable`, `ListSignUpInstitutions`, `ImportUsers`, `ListMySettings`, `SetMySetting`, `ResetMySetting`
- **`platform.file_object`**: `CreateUploadUrl`, `CompleteUpload`, `GetDownloadUrl`, `CopyFile`
- **`platform.tenant`**: `GetBranding`, `ClearDemoData`, `GlobalSearch`, `ListReportDefinitions`, `RunReport`, `GetImportTemplate`, `StartImport`, `SearchStockImages`
- **`org.institution`**: `FindInstitutions`, `ListActiveInstitutions` (internal: MySMS services only)
- **`platform.tenant_subscription`**: `StartSubscriptionCheckout`, `ConfirmSubscriptionCheckout`, `ReceiveSubscriptionWebhook`, `CancelSubscription`
- **`platform.sales_enquiry`**: `MarkEnquiryContacted` (platform staff)
- **`org.academic_year`**: `ActivateAcademicYear`, `CloseAcademicYear`, `GenerateTerms`
- **`people.person`**: `ListBirthdays`, `SendGreetings`, `GetMyPerson`, `UpdateMyPerson` (the caller's own record; contact and self-description fields only)
- **`privacy.notice_version`**: `PublishNoticeVersion`
- **`privacy.consent_record`**: `ListMyConsents`, `RecordMyConsent`, `WithdrawMyConsent`, `GetMyNotice`, `RecordConsentOnBehalf`, `VerifyConsentRecord`
- **`privacy.consent_decision`** (internal, service principals): `CheckConsent`, `BatchCheckConsent`, `GetPurposeStatus`, `ListSubjectsWithConsent`
- **`privacy.rights_request`**: `SubmitMyRequest`, `ListMyRequests`, `GetMyRequest`, `GetMyAccessSummary`, `GetAccessSummary`, `VerifyRightsRequestIdentity`, `AssignRightsRequest`, `ExtendRightsRequest`, `RejectRightsRequest`, `CloseRightsRequest`, `FulfilAccessRequest`, `StartRightsRequestFulfilment`, `FulfilWithdrawAllRequest`, `RetryRightsRequestTask`
- **`privacy.legal_hold`**: `ReleaseLegalHold`; `ListActiveHolds` (internal, service principals)
- **`sis.student`**: `ChangeStudentStatus`, `GetStudentSummary`, `IssueBonafideCertificate`, `CreateStudentPortalAccount`
- **`sis.guardian`**: `CreatePortalAccount`, `ListMyChildren`
- **`sis.promotion`**: `ProposePromotions`, `ApprovePromotion`, `RejectPromotion`, `ApplyPromotions`
- **`sis.subject_registration`**: `SubmitSubjectRegistration`, `ApproveSubjectRegistration`, `RejectSubjectRegistration`, `ResetSubjectRegistration`, `LoadCourseSubjects`
- **`sis.discipline_incident`**: `MarkIncidentActionTaken`, `MarkIncidentSeen`
- **`academics.lesson_plan`**: `SubmitLessonPlan`, `ApproveLessonPlan`, `RejectLessonPlan`, `PublishLessonPlan`, `ArchiveLessonPlan`
- **`academics.class_section`**: `ListClassRoster`
- **`hr.staff`**: `ChangeStaffStatus`, `CreateStaffPortalAccount`, `GetLeaveBalance`
- **`hr.leave_application`**: `ApproveLeave`, `RejectLeave`, `CancelLeave`
- **`payroll.payslip`**: `GeneratePayslips`, `MarkPayslipPaid`, `CancelPayslip`
- **`admission.admission_cycle`**: `ConfirmAdmissionCycle`, `OpenApplications`, `StartAdmission`, `CloseAdmissionCycle`, `CancelAdmissionCycle`, `ResetAdmissionCycle`
- **`admission.application`**: `SubmitApplication`, `ConfirmApplication`, `MarkApplicationPending`, `RejectApplication`, `CancelApplication`, `ResetApplication`, `ApproveApplication`, `ConfirmAdmission`, `EnrollApplicant`
- **`attendance.attendance_sheet`**: `StartAttendanceSheet`, `CompleteAttendanceSheet`, `CancelAttendanceSheet`, `ResetAttendanceSheet`, `RecordAttendance`, `TakeAttendanceForSession`
- **`attendance.attendance_record`**: `SummarizeAttendance`
- **`timetable.session`**: `ConfirmSession`, `CompleteSession`, `CancelSession`, `ResetSession`, `BatchConfirmSessions`, `GenerateSessions`, `GetTimetable`
- **`assessment.homework`**: `PublishHomework`, `ApproveHomework`, `RejectHomework`
- **`assessment.homework_submission`**: `CheckHomeworkSubmission`, `ReplyToHomeworkFeedback`
- **`assessment.assignment`**: `PublishAssignment`, `ApproveAssignment`, `RejectAssignment`, `FinishAssignment`, `CancelAssignment`, `ResetAssignment`
- **`assessment.assignment_submission`**: `SubmitAssignmentSubmission`, `AcceptAssignmentSubmission`, `RequestSubmissionChanges`, `RejectAssignmentSubmission`, `ResetAssignmentSubmission`, `GradeAssignmentSubmission`
- **`assessment.exam_session`**: `ScheduleExamSession`, `MarkExamSessionHeld`, `CompleteExamSession`, `CancelExamSession`, `ResetExamSession`
- **`assessment.exam`**: `ScheduleExam`, `MarkExamHeld`, `MarkExamResultsUpdated`, `CompleteExam`, `CancelExam`, `ResetExam`, `PopulateExamResults`, `AllocateExamRooms`, `RecordExamMarks`
- **`assessment.result_template`**: `GenerateResults`
- **`assessment.marksheet`**: `ValidateMarksheet`, `CancelMarksheet`, `ResetMarksheet`
- **`assessment.marksheet_line`**: `GetStudentResults`
- **`lms.course`**: `PublishCourse`, `UnpublishCourse`, `SubmitCourseForReview`, `ApproveCourse`, `GetCourseOutline`, `ReorderChapters`, `EnrollInCourse`, `BatchEnrollInCourse`, `IssueMyCertificate`, `GetCourseProgressDistribution`, `GetEvaluatorSlots`, `ExportCourse`, `CreateCourseVersion`, `DeactivateCourse`, `ReactivateCourse`, `PublishCatalogListing`, `HideCatalogListing`, `CheckCourseDependencies`, `GetCourseSyllabus`, `GetCatalogHome`, `CompareCourses`, `GetTeacherDashboard`
- **`lms.chapter`**: `GetScormLaunch`, `SaveScormProgress`, `MoveChapter`
- **`lms.lesson`**: `GetLessonContent`, `MoveLesson`, `MarkLessonComplete`, `TrackVideoWatch`
- **`lms.quiz`**: `StartQuizAttempt`, `CheckQuizAnswer`, `SubmitQuiz`, `SetQuizQuestions`
- **`lms.question`**: `SearchQuestionBank`, `ImportQuestions`
- **`lms.assignment_submission`**: `GradeLearningSubmission`, `FinalizeSubmission`
- **`lms.programming_exercise`**: `SubmitExercise`
- **`lms.cohort`**: `PublishCohort`, `UnpublishCohort`, `EnrollInCohort`, `BatchEnrollInCohort`, `GetCohortTimetable`, `ApplyTimetableTemplate`, `SendCohortAnnouncement`, `GetCohortProgress`, `GenerateCohortCertificates`, `ActivateCohort`, `DeactivateCohort`, `ArchiveCohort`, `CopyCourseOutlineToCohort`, `GetCohortRoster`, `SetCohortMembers`, `ImportCohortMembers`, `SetStudentAccess`, `GetCohortSyllabus`, `GetGradebook`, `GetCohortPerformance`, `ExportGrades`, `PublishFinalGrades`, `EvaluateReleaseRules`
- **`lms.live_class`**: `ScheduleLiveClass`, `GetLiveClassStartUrl`, `JoinLiveClass`, `RetryLiveClassInvites`, `EndLiveClass`, `SyncLiveClassAttendance`
- **`lms.conferencing_account`**: `ReceiveConferencingWebhook`
- **`lms.evaluator`**: `ConnectEvaluatorCalendar`, `DisconnectEvaluatorCalendar`
- **`lms.certificate_request`**: `CancelCertificateRequest`, `StartEvaluation`
- **`lms.certificate_evaluation`**: `IssueCertificateFromEvaluation`
- **`lms.certificate`**: `VerifyCertificate`, `DownloadCertificate`
- **`lms.badge`**: `AwardBadge`
- **`lms.learning_program`**: `EnrollInLearningProgram`
- **`lms.enrollment`**: `GetStudentDashboard`, `GetLmsStatistics`, `GetMemberProfile`
- **`lms.settings`**: `GetPublicSettings`, `SendContactMessage`
- **`finance.student_fee_due`**: `GenerateFeeDues`
- **`finance.invoice`**: `CreateInvoiceFromDues`, `PostInvoice`, `CancelInvoice`, `SendFeeReminders`, `GetFeeStatement`
- **`finance.fee_structure`**: `PublishFeeStructure`, `ArchiveFeeStructure`
- **`finance.payment`**: `QuoteCheckout`, `StartCheckout`, `ReceiveGatewayWebhook`, `RecordPayment`, `RefundPayment`
- **`library.loan`**: `RequestLoanReturn`, `ReturnLoan`, `MarkLoanLost`, `InvoiceLoanFine`
- **`library.reservation`**: `AcceptReservation`, `RejectReservation`, `RequestReservationAgain`
- **`library.purchase_request`**: `SubmitPurchaseRequest`, `AcceptPurchaseRequest`, `RejectPurchaseRequest`
- **`comms.notice`**: `PublishNotice`, `UnpublishNotice`, `CloseNoticeReplies`, `DeactivateNotice`, `ReactivateNotice`
- **`comms.event`**: `ActivateEvent`, `DeactivateEvent`, `GetMyCalendar`
- **`comms.outbound_message`**: `SendMessage`, `CancelOutboundMessage`, `RetryOutboundMessage`
- **`comms.in_app_notification`**: `MarkNotificationRead`, `MarkAllNotificationsRead`, `ArchiveNotifications`
- **`comms.mailbox_message`**: `ComposeMail`, `MarkMailRead`
- **`comms.conversation`**: `MarkConversationRead`
- **`comms.conversation_message`**: `HideMessageForMe`
- **`comms.feedback_thread`**: `CloseFeedbackThread`, `ReopenFeedbackThread`
- **`comms.feedback_message`**: `MarkFeedbackMessage`
- **`comms.audience_group`**: `AddGroupMembers`
- **`comms.task`**: `CompleteTask`, `ReopenTask`, `SnoozeTask`, `ClaimTask`
- **`comms.post`**: `SubmitPost`, `ApprovePost`, `CancelPost`, `GetFeed`
- **`comms.class_page`**: `FollowClassPage`
- **`frontoffice.visitor_log`**: `CheckOutVisitor`
- **`careers.job_opening`**: `CloseJobOpening`, `ReopenJobOpening`, `ReportJobOpening`
- **`careers.job_application`**: `EmailApplicant`, `UpdateApplicationStatus`
- **`lms.content_item`**: `MoveContentItem`, `CopyContentItem`, `AddFromRepository`, `SetContentItemVisibility`, `GetWebResourceLaunch`, `PublishItemGrades`, `ReassignItem`, `DownloadSubmissions`, `GetReleaseStatus`, `GrantItemAccess`, `SetReleaseRulesEnabled`, `ReorderReleaseRules`, `RecordItemView`
- **`lms.gradebook_entry`**: `GradeEntry`, `BatchGradeEntries`
- **`lms.release_rule`**: `EnableReleaseRule`, `DisableReleaseRule`, `CopyReleaseRule`
- **`lms.assignment_group`**: `RequestToJoinGroup`, `SetGroupMembers`, `SubmitPeerReview`, `FinalizePeerReview`
- **`lms.assignment_group_member`**: `AcceptGroupRequest`, `RejectGroupRequest`
- **`lms.plagiarism_report`**: `SubmitForPlagiarismCheck`, `GetPlagiarismReportLink`, `ReceivePlagiarismWebhook`
- **`lms.quiz_submission`**: `SaveQuizResponses`, `PauseQuizAttempt`, `ResumeQuizAttempt`, `GradeQuizAnswers`
- **`lms.forum_topic`**: `MarkTopicRead`, `HideForumTopic`, `UnhideForumTopic`
- **`lms.forum_post`**: `HideForumPost`, `UnhideForumPost`, `ConfirmPostAbuse`, `FlagForumPost`, `MarkPostSubstantive`
- **`lms.forum`**: `GetUnreadCounts`
- **`comms.event_occurrence`**: `CancelEventOccurrence`, `CompletePersonalEvent`
- **`comms.calendar_export`**: `ExportCalendar`
- **`lms.ebook`**: `ConvertEbook`, `ReceiveEbookConversion`, `PublishEbookCuration`, `GetMyRelatedMaterials`
- **`lms.external_tool_link`**: `LaunchExternalTool`, `ReceiveToolOutcome`
- **`lms.external_tool`**: `ActivateExternalTool`
- **`lms.enrollment_request`**: `ApproveEnrollmentRequest`, `RejectEnrollmentRequest`
- **`lms.import_export_job`**: `CancelImportExportJob`, `RestoreCohortArchive`, `GetJobPackage`
- **`lms.drive_item`**: `ShareDriveItem`
- **`platform.storage_quota`**: `GetMyStorageUsage`
- **`comms.notification_channel`**: `SendTestMessage`

## The Java contracts, and what is generated

The server-side Java contracts are NOT generated: each backend module owns its API interfaces and DTOs
(`com.mysms.<module>.api` / `.api.model`, edited directly) and
its edge operation table (`src/main/resources/META-INF/mysms/operations/<module>.json`: method, path, `x-permission`,
security schemes, token optional or not, idempotency — enforced by the auth filter). springdoc writes each running
service's OpenAPI document from that code (`/v3/api-docs.yaml`; core's `OpenApiConfiguration` restores the contract
operationIds, `x-permission`, security and the contract tags), and every module's `<Module>OpenApiComponentTest`
writes it to `backend/<module>/target/openapi/api-docs.yml` on `mvn verify`. The conformance build fails when a
module's controllers, protos or `operations-status.md` drift from its operation table.

The TypeScript clients for the React and React Native apps (`@mysms/sdk`, one namespace per module) are generated by
[OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) 7.26.0 (`typescript-fetch`) from those written
documents, so the backend build runs before client generation. Modules call each other over gRPC only, through the
`@GrpcClient` stubs of their `integration` package; there is no Java REST client.

The gRPC stubs and protobuf messages (`com.mysms.<module>.v1`) are compiled from each module's own
`src/main/resources/proto` into its jar (`protobuf-maven-plugin` with OS-resolved `protoc` and
`protoc-gen-grpc-java`, the reference mechanism); the shared `mysms/common` protos once, in core.

```bash
cd backend && mvn install                            # every module, its stubs and its OpenAPI document
cd clients && pnpm --filter @mysms/sdk generate      # TypeScript SDK from backend/*/target/openapi/api-docs.yml
```

## Internal tables with no API resource

Every module schema except `master` has its own `idempotency_key` (backs the Idempotency-Key machinery for that module's operations); every module schema has its own `outbox_event` / `inbox_event` (eventing) and, where the module issues numbers, `number_sequence`. They are written only by the owning module's server, never by another module, and deliberately have no REST/gRPC resource. Each module's `operation` table is exposed through that module's `OperationService` (`GetOperation`, `ListOperations`, `CancelOperation`); `platform.operation` additionally holds the job-centre index behind `platform.ListOperations`.

## Guarantees kept by the build

- **REST ↔ gRPC parity:** each REST operation maps through `x-grpc` to exactly one RPC and every RPC is reachable from REST.
- **OpenAPI:** each module's document is written by its build and passes `redocly lint` (BUILD-AND-RUN §3.5). There is no joined all-modules file; a portal reads each service's own document (`/v3/api-docs.yaml`, written to `backend/<module>/target/openapi/api-docs.yml` by every build).
- **Generator-ready schemas:** every JSON request and response body is a named schema (no inline objects; free-form webhook payloads excepted), and no schema name is defined in two files, so generated clients get stable class names.
- **Generated code compiles:** every module compiles its own protos and its copies of its callees' on every build; the TypeScript SDK is generated from the written OpenAPI documents and compiled by `pnpm verify`; the owned server contracts compile in their modules.
- **Wire stability:** `backend/conformance/src/main/resources/field-numbers.lock.json` records the field numbers of **every** message (map fields included), so a proto change never renumbers an existing field (conformance G2).
- **Terminology:** the contracts use *student* and *teacher* throughout — schema names, field names, paths, permissions and descriptions; each concept has exactly one name.
- **Writable surfaces:** MFA-challenge logins return no tokens or user object, encrypted links are write-only, tenant subscriptions are read-only to tenants (checkout is the only write), fee-due status and enrollment grade/certificate fields are workflow-only.
