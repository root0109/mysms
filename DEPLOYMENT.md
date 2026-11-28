# Deploying and running MySMS

From a fresh checkout to a running installation: database, backend (one monolith or one service per module), web apps and phone and tablet apps. Build and local development: [`BUILD-AND-RUN.md`](BUILD-AND-RUN.md). Architecture: [`ARCHITECTURE.md`](ARCHITECTURE.md).

Diagram: [docs/diagrams/architecture/deployment.drawio](docs/diagrams/architecture/deployment.drawio) — monolith and per-module services, database roles, broker; open with draw.io / diagrams.net.

A contract operation without an implementation answers `501 NOT_IMPLEMENTED`, so clients can tell "not built" from "not found".

---

## 1. Prerequisites

Build tools (JDK 17, Maven, Node.js 22.12+, pnpm 10, OpenSSL): [`BUILD-AND-RUN.md`](BUILD-AND-RUN.md) §1. To run an installation:

| Service | Version | Used for |
|---|---|---|
| PostgreSQL | 16 (the version the tests use; no extensions) | the database |
| RabbitMQ | 4.1 (the version the tests use; quorum queues) | default event bus (§5.7) |
| Google Cloud Pub/Sub | — | optional managed event bus (§5.7) |
| Expo Go, a simulator or EAS Build | — | phone and tablet apps (§9) |

## 2. The database

One PostgreSQL database, **one schema per area** (23: `master`, `platform`, `iam`, `org`, `people`, `academics`, `sis`, `hr`, `payroll`, `time_attendance`, `admission`, `attendance`, `facility`, `timetable`, `assessment`, `lms`, `finance`, `library`, `transport`, `comms`, `frontoffice`, `careers`, `privacy`). No foreign key crosses modules (only hrms's sibling schemas link), so a module moves to its own database by pointing its `MYSMS_<MODULE>_DB_*` variables elsewhere and copying its schemas; the areas of one module (hrms: `hr`, `payroll`, `time_attendance`) stay on one instance.

### 2.1 Roles

Created once by operations, never by migrations:

| Role | Purpose | Attributes |
|---|---|---|
| `mysms_migrate` | Owns schemas and tables; Flyway and partition maintenance run as it. | `LOGIN` |
| `mysms_app` | Runtime login. Not the owner, so `FORCE`d row-level security binds it. | `LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB` |
| `mysms_relay` | Outbox relay (§5.7): reads every tenant's pending events. Granted per schema `USAGE`, `SELECT, DELETE` on `outbox_event` and `inbox_event`, and `UPDATE` of `outbox_event.published_at` and `.publish_error` only. A service refuses to start when it is the application or migration user. | `LOGIN BYPASSRLS` |

As a PostgreSQL superuser (passwords from your secret store):

```sql
CREATE ROLE mysms_migrate LOGIN PASSWORD '<migrate-password>';
CREATE ROLE mysms_app LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB PASSWORD '<app-password>';
CREATE ROLE mysms_relay LOGIN BYPASSRLS PASSWORD '<relay-password>';

CREATE DATABASE mysms OWNER mysms_migrate;
REVOKE ALL ON DATABASE mysms FROM PUBLIC;
GRANT CONNECT ON DATABASE mysms TO mysms_app, mysms_relay;
```

On every start each module runs its own Flyway history (`<schema>.flyway_schema_history`) as `mysms_migrate`; core's `ApplicationRoleGrants` then grants `mysms_app` `USAGE` on the schema, DML on its tables and use of its sequences, and `mysms_relay` its outbox and inbox rights. The first start logs a harmless `schema "<module>" already exists, skipping`.

Each module has one Flyway script, its baseline (`database/migrations/<module>/V1_<order>_001__<module>_baseline.sql`). Until the first release a schema change edits the baseline in place, so a database migrated by an earlier build fails checksum validation and must be recreated ([`database/README.md`](database/README.md), "Applying the schema").

`MYSMS_DB_MIGRATE_USER` left empty runs migrations as the application user: acceptable on a developer machine, **never in production** (the application would own its tables and bypass row-level security).

### 2.2 Schema smoke test (throwaway databases only)

`database/verification/smoke.sql` proves tenant isolation, money and evidence constraints and the distributed-deployment invariants. It **commits seed tenants**, so run it only against a scratch database the backend has migrated:

```bash
psql -v ON_ERROR_STOP=1 -d mysms_scratch -f database/verification/smoke.sql   # ends with "SMOKE4 OK"
```

### 2.3 Backups

`pg_dump -Fc mysms` backs up every schema with its Flyway history, row-level security and grants. Roles are not in it: add `pg_dumpall --roles-only`.

## 3. The token-signing key

iam issues RS256 access tokens. One key per installation, kept only in the secret store:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out iam-signing.pem
```

Pass the file's *contents* (PEM, PKCS#8, 2048 bits or more) in `MYSMS_IAM_SIGNING_KEY`. iam refuses to start without it and publishes the public half at `/oauth2/jwks`.

**Rotation:** set the new key in `MYSMS_IAM_SIGNING_KEY`, the old public half (`openssl pkey -in old.pem -pubout`) in `MYSMS_IAM_PREVIOUS_PUBLIC_KEY`, restart iam, and remove the previous key after one access-token lifetime (default 15 minutes). Refresh tokens are opaque and unaffected.

## 3a. Secrets: the field-encryption master key

Columns ending `_enc` (national ids, bank accounts, gateway and webhook secrets, meeting and tool secrets, MFA secrets) are encrypted by core's `FieldCipher` before they reach the database: AES-256-GCM, envelope `v1.<key id>.<iv>.<ciphertext>`, a per-tenant key derived with HKDF-SHA256 from the master key and bound to tenant and column. The API accepts these values write-only and shows them masked (`••••1234`). Responses kept for `Idempotency-Key` retries are encrypted the same way.

```bash
echo "k1:$(openssl rand -base64 32)"
```

Set `MYSMS_SECRETS_KEYS` to that value (`<key id>:<base64 key>`). Every module with an idempotency store (all except master) refuses to start without it, and so does the monolith. **Losing the key loses those values**: back it up as carefully as the database, never next to it.

**Rotation:** `MYSMS_SECRETS_KEYS=k1:<old>,k2:<new>` and `MYSMS_SECRETS_ACTIVE_KEY=k2` (default: the last key listed). New values use `k2`; `k1` values stay readable while `k1` is listed. Keep the old key until every value is rewritten (replacing a secret through the API rewrites it).

## 4. Configuration reference

Each module jar carries its own `application.properties` (JAVA-CODING-RULES §2.1) in which every value defaults from one of the variables below; the monolith combines every module's file under its own. Any variable, an `application.properties` beside the jar or `--key=value` overrides the file. No secret is in the repository.

### 4.1 Every service (and the monolith)

**Database**

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_DB_URL` | yes | JDBC URL, e.g. `jdbc:postgresql://db:5432/mysms` |
| `MYSMS_DB_USER`, `MYSMS_DB_PASSWORD` | yes | the application role (`mysms_app`) |
| `MYSMS_DB_MIGRATE_USER`, `MYSMS_DB_MIGRATE_PASSWORD` | in production | the schema owner (`mysms_migrate`) |
| `MYSMS_DB_RELAY_USER`, `MYSMS_DB_RELAY_PASSWORD` | in production | the relay role (`mysms_relay`); without it the relay runs only when the application role bypasses RLS (a local superuser), otherwise it logs an ERROR and events stay in the outbox |
| `MYSMS_DB_REPLICA_URL` (with `_USER`, `_PASSWORD`) | no | a streaming replica that read-only transactions use (pool `<module>-replica`); writes and reads inside writes stay on the primary. User and password default to the application role's. A stale read cannot overwrite newer data: updates carry the ETag (412). Never migrated; health component `replica` |
| `MYSMS_DB_SLOW_STATEMENT` | no | statements slower than this are logged (text with placeholders, never values); default `200ms`, `0` = off |
| `MYSMS_<MODULE>_DB_URL`, `_DB_USER`, `_DB_PASSWORD`, `_DB_MIGRATE_USER`, `_DB_MIGRATE_PASSWORD`, `_DB_RELAY_USER`, `_DB_RELAY_PASSWORD`, `_DB_REPLICA_URL`, `_DB_REPLICA_USER`, `_DB_REPLICA_PASSWORD`, `_DB_SLOW_STATEMENT` | no | per-area override of the shared value (e.g. `MYSMS_FINANCE_DB_URL`); the areas of one module read each other's schemas (payroll and time_attendance read `hr.staff`; time_attendance has a foreign key into `hr`), so keep their URL and application user equal |
| `MYSMS_<AREA>_DB_POOL_SIZE` | no | pool size per area, default 10. Every process opens one pool per area it serves (and one per replica); the monolith 23: 23 × pool size must stay below `max_connections` |

**Authentication and service identity**

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_AUTH_ISSUER` | yes | token issuer: the public base URL of iam (or of the monolith); a service refuses to start without it |
| `MYSMS_AUTH_JWK_SET_URI` | yes | `<iam base URL>/oauth2/jwks` |
| `MYSMS_AUTH_AUDIENCES` | no | accepted audiences, default `mysms`, `mysms-platform-admin`, `mysms-internal`; if set, keep `mysms-internal` |
| `MYSMS_AUTH_TOKEN_URI` | for service calls | `<iam base URL>/oauth2/token` (§5.9); blank = the module makes no service calls, one that needs to answers 503 |
| `MYSMS_<MODULE>_SERVICE_CLIENT_SECRET` | for service calls | **secret**, the module's client secret, at least 32 random characters; iam holds only its SHA-256 |
| `MYSMS_ROLE_CACHE_TTL` | no | how long the permissions filter keeps a role's permission codes (§5.10), default `PT1M` |
| `MYSMS_RELEASE` | no | release published with the module's API-permission catalogue (§5.10), default `development` |
| `MYSMS_CATALOGUE_INTERVAL` | no | catalogue republish interval, default `6h`; keep well below `MYSMS_IAM_API_PERMISSION_RETIRE_AFTER` |
| `MYSMS_API_DOCS` | no | `true` serves `/v3/api-docs.yaml` and `/swagger-ui.html` without a token; off by default (the document maps every operation and permission) |
| `MYSMS_SECRETS_KEYS` | yes, except master alone | **secret**, field-encryption key list (§3a) |
| `MYSMS_SECRETS_ACTIVE_KEY` | no | key id for new values, default the last listed |

**HTTP and gRPC**

| Variable | Required | Meaning |
|---|---|---|
| `PORT` | no | HTTP port, default 8080 |
| `MYSMS_GRPC_SERVER_PORT` | no | gRPC port (§5.8), default `9112`; one server per process |
| `MYSMS_NAMESPACE` | no | Kubernetes namespace of the service DNS names (`<module>-service.<namespace>.svc.cluster.local`), default `mysms` |
| `MYSMS_<MODULE>_SERVICE_PORT_RPC` | no | gRPC port of a called `<module>` service (the module serving the called area, ARCHITECTURE §1), default `9112` (Kubernetes sets it for a port named `rpc`) |
| `MYSMS_<AREA>_GRPC_ADDRESS` | no | full target of a called area's service (e.g. `static://10.0.0.5:9112`), overriding the two above; monolith default: the in-process server |
| `MYSMS_GRPC_CLIENT_DEADLINE` | no | deadline of a call whose stub sets none, default `10s` |
| `MYSMS_GRPC_REFLECTION` | no | `true` serves gRPC server reflection; off by default |

**Events, partitions and jobs**

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_EVENTS_BUS` | no | `rabbitmq` (default), `google-pubsub`, `in-process` (one JVM) or `none` (tests only: nothing relayed or consumed) |
| `MYSMS_RABBITMQ_URI` | with RabbitMQ | **secret**, `amqps://<user>:<password>@<host>:5671/<vhost>`; a service refuses to start without it |
| `MYSMS_RABBITMQ_PUBLISHER_USER` | no | broker user a module's events must come from (`{module}` = the module serving the event's source area, `payroll` → `hrms`): default `mysms-{module}` for a service, `self` for the monolith |
| `MYSMS_EVENTS_GCP_PROJECT_ID` | with Pub/Sub | Google Cloud project; falls back to `GOOGLE_CLOUD_PROJECT` |
| `PUBSUB_EMULATOR_HOST` | no | Pub/Sub emulator, e.g. `localhost:8085` (no credentials, resource creation on) |
| `MYSMS_EVENTS_GOOGLE_PUBSUB_CREATE_RESOURCES` | no | create topics and subscriptions at startup; default `false`, `true` with the emulator |
| `MYSMS_EVENTS_RELAY_INTERVAL` | no | pause between relay passes, default `500ms` |
| `MYSMS_EVENTS_OUTBOX_RETENTION` | no | published outbox rows kept for diagnosis, default `7d` |
| `MYSMS_EVENTS_INBOX_RETENTION` | no | processed inbox rows kept, default `30d`; longer than you would wait before replaying a dead-lettered event |
| `MYSMS_PARTITIONS_ENABLED` | no | partition maintenance (§5.6), default `true`; connects as `MYSMS_DB_MIGRATE_USER` |
| `MYSMS_PARTITIONS_MONTHS_AHEAD` | no | months after the current one that must have a partition, default 3, at most 24 |
| `MYSMS_PARTITIONS_RETENTION_MONTHS` | no | months kept before the current one; older months are **dropped**. Default 0 = never; otherwise at least 12 |
| `MYSMS_PARTITIONS_INTERVAL` | no | run interval after the run at startup, default `24h` |
| `MYSMS_JOBS_ENABLED` | no | scheduled-job runtime ([design](docs/design/BACKGROUND-JOB-PATTERN.md)), default `true`; a replica with `false` runs no jobs, others do |
| `MYSMS_JOBS_POLL_INTERVAL` | no | how often each module's `job_schedule` is polled, default `1m`; also the first retry delay |
| `MYSMS_JOBS_LEASE` | no | how long a claimed run is held before another replica may take it, default `30m`; longer than any run |
| `MYSMS_JOBS_BATCH_SIZE` | no | runs per module and poll, default 20 |

### 4.2 iam (and the monolith)

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_IAM_SIGNING_KEY` | yes | **secret**, PEM PKCS#8 RSA private key (§3) |
| `MYSMS_IAM_SIGNING_KEY_ID` | no | `kid`; default the key's RFC 7638 thumbprint |
| `MYSMS_IAM_PREVIOUS_PUBLIC_KEY` | no | retiring public key during rotation |
| `MYSMS_IAM_ACCESS_TOKEN_TTL`, `MYSMS_IAM_REFRESH_TOKEN_TTL` | no | ISO-8601, default `PT15M` and `P30D` |
| `MYSMS_IAM_SERVICE_TOKEN_TTL` | no | service-token lifetime, default `PT5M`, at most `PT1H` |
| `MYSMS_IAM_SERVICECLIENTS_<MODULE>_SECRETSHA256` | for service calls | lower-case hex SHA-256 of that module's client secret (property `mysms.iam.service-clients.<module>.secret-sha256`); one client per area, keyed by the area code. An environment variable cannot carry an underscore inside the key, so an area such as `time_attendance` is written without it (`MYSMS_IAM_SERVICECLIENTS_TIMEATTENDANCE_SECRETSHA256`); in properties `time_attendance` and `time-attendance` work too |
| `MYSMS_IAM_SERVICECLIENTS_<MODULE>_PERMISSIONS` | no | comma-separated permission codes the module's service tokens carry (operations of audience `internal`); default none |
| `MYSMS_IAM_API_PERMISSION_RETIRE_AFTER` | no | an operation no module has published for this long is retired, default `P3D` |
| `MYSMS_IAM_BOOTSTRAP_TENANT_CODE`, `_ADMIN_USERNAME`, `_ADMIN_PASSWORD` | first start only | §5.3 |

### 4.2a privacy (and the monolith)

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_PRIVACY_AGE_OF_MAJORITY` | no | age in years at which a person becomes an adult, default 18 |
| `MYSMS_PRIVACY_MAJORITY_JOB_INTERVAL` | no | per-tenant majority job interval (minors who came of age become adults), default `1h` |
| `MYSMS_PRIVACY_CONSENT_EXPIRY_JOB_INTERVAL` | no | per-tenant consent-expiry job interval, default `1h` |
| `MYSMS_PRIVACY_RIGHTS_SLA_ACCESS`, `_CORRECTION`, `_GRIEVANCE`, `_NOMINATION`, `_ERASURE`, `_EXPORT`, `_WITHDRAW_ALL` | no | response deadline from submission, ISO-8601 period, default `P30D` each (legal periods to be verified by counsel) |
| `MYSMS_PRIVACY_ERASURE_MODULES` | no | the modules an erasure or export request sends a task to (comma-separated module codes), default the modules with a privacy task handler: `iam,comms,people,sis,hr,payroll,time_attendance,admission,attendance,assessment,academics,careers,finance,frontoffice,library,platform,transport` (lms has none: it cannot map a person to its rows). Add a module only once it has a handler: a listed module without one leaves its task dispatched and the request in progress (PRIVACY-AND-CONSENT-CENTRE §9.1). Retention runs are sent to these modules only |
| `MYSMS_PRIVACY_ERASURE_FIRST_WAVE` | no | the modules an erasure reaches first, before the rest (stop messages and sign-in), default `iam,comms`; each must be in `MYSMS_PRIVACY_ERASURE_MODULES` |
| `MYSMS_PRIVACY_ERASURE_REDISPATCH_AFTER` | no | how long a task may stay unanswered before the sweep dispatches it again, default `PT24H` |
| `MYSMS_PRIVACY_ERASURE_RETRY_DELAY` | no | how long a failed task waits before its next attempt, doubled per attempt and at most `MYSMS_PRIVACY_ERASURE_REDISPATCH_AFTER`, default `PT5M` |
| `MYSMS_PRIVACY_ERASURE_SWEEP_INTERVAL` | no | per-tenant interval of that sweep (core job runtime), default `15m` |
| `MYSMS_PRIVACY_RETENTION_JOB_INTERVAL` | no | per-tenant interval of the retention run, which sends each retention policy's cutoff to the module holding the data (`privacy.retention.due`; once per UTC day per policy), default `24h` |

Read by every module that uses privacy-client (people, sis, iam, hr, payroll, time_attendance, admission, attendance, assessment, academics, careers, comms, finance, frontoffice, library, platform, transport): consent checks and the module's privacy tasks and
retention runs (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10):

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_PRIVACY_CLIENT_DEADLINE` | no | how long one consent check may wait, default `300ms`; past it the check refuses (503) |
| `MYSMS_PRIVACY_CLIENT_CACHE_TTL` | no | per-process cache of consent answers, default `60s`: a withdrawal shows everywhere within it |
| `MYSMS_PRIVACY_CLIENT_HOLDS_DEADLINE` | no | how long privacy's answer on the legal holds of a subject may take before an erasure or a retention sweep, default `2s`; without an answer nothing is erased and the task (or run) answers failed |
| `MYSMS_PRIVACY_CLIENT_TASK_INTERVAL` | no | per-tenant interval of the module's job `<module>.privacy_tasks` (core job runtime), which runs the tasks and runs received, default `5m` |
| `MYSMS_PRIVACY_GRPC_ADDRESS` | no | the privacy service (§4.1 `MYSMS_<MODULE>_GRPC_ADDRESS`) |

### 4.2b hrms (and the monolith)

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_HR_LEAVE_WEEKEND_DAYS` | no | the weekend of every institution, as leave, pay and staff attendance count working days: day names, comma-separated, default `SATURDAY,SUNDAY`; blank = a seven-day week. Holidays come from org's holiday events (hr's copy, §5.7) |

### 4.3 Web apps (build time)

| Variable | Meaning |
|---|---|
| `VITE_API_BASE_URL` | API origin; unset = the origin the app is served from (recommended, §7) |
| `VITE_TENANT_CODE` | pre-fills the school code on sign-in for single-school hosts |
| `VITE_DEFAULT_LOCALE` | `en` (default) or `hi` |
| `MYSMS_API_PROXY` | dev server only: where `/v1` is proxied, default `http://localhost:8080` |

### 4.4 Phone and tablet apps

| Variable | Meaning |
|---|---|
| `EXPO_PUBLIC_API_BASE_URL` | API origin reachable from the device (not `localhost` on a phone) |
| `APP_VARIANT` | phone app only: `student` (default), `parent` or `teacher` |

### 4.5 External providers

An unconfigured provider never breaks a service: the operations that need it answer `503` with a code naming what is missing.

| Variable | Module | Meaning |
|---|---|---|
| `MYSMS_SMTP_HOST`, `_PORT` (587), `_USERNAME`, `_PASSWORD` (**secret**), `_FROM`, `_STARTTLS_REQUIRED` (true) | iam, careers, lms | outgoing mail (sign-up, password reset, verification and module mail) |
| `MYSMS_APP_URL` | iam | web app URL the links in iam's mail point to |
| `MYSMS_FILES_BUCKET`, `_PROJECT`, `_CREDENTIALS_FILE`, `_ENDPOINT` | platform | Google Cloud Storage (signed upload and download URLs); `ENDPOINT` only for an emulator |
| `MYSMS_FILES_MAX_SIZE_BYTES` | platform | largest upload, default 100 MB |
| `MYSMS_PLATFORM_RAZORPAY_KEY_ID`, `_KEY_SECRET`, `_WEBHOOK_SECRET` (**secrets**), `_CURRENCY` (INR), `_URL` | platform | MySMS's own Razorpay account for tenant subscriptions |
| `MYSMS_RAZORPAY_URL` (default `https://api.razorpay.com/v1/`), `MYSMS_RAZORPAY_TIMEOUT` (20 s), `MYSMS_FINANCE_REFUND_RECONCILE_AFTER` (15 min) | finance | Razorpay API for each school's own gateway account (credentials stored encrypted in finance) |
| `MYSMS_PEXELS_API_KEY`, `MYSMS_PEXELS_URL` | platform | stock image search |
| `MYSMS_ZOOM_API_URL`, `MYSMS_ZOOM_OAUTH_URL`, `MYSMS_CONFERENCING_TIMEOUT` (20 s) | lms | Zoom live classes (credentials per tenant) |
| `MYSMS_GOOGLE_CALENDAR_CLIENT_ID`, `_AUTH_URL`, `_SCOPE`, `MYSMS_CALENDAR_STATE_LIFETIME` (15 min) | lms | evaluator calendars over Google OAuth |
| `MYSMS_LMS_CONVERSION_PROVIDER`, `_API_URL`, `_API_KEY`, `_CALLBACK_SECRET`, `_WAIT` (45 s) | lms | e-book document conversion |
| `MYSMS_LMS_PLAGIARISM_INTEGRATION_NAME`, `_VERSION` | lms | integration headers towards the plagiarism provider (account per tenant) |
| `MYSMS_LMS_LTI_OUTCOME_URL` | lms | public URL tools send LTI 1.1 grades to; blank = no grade return |
| `MYSMS_LMS_SCORM_CONTENT_BASE_URL` | lms | where extracted SCORM packages are served from |
| `MYSMS_LMS_FILES_TIMEOUT` | lms | transfer timeout of one file through platform's signed URLs, default 60 s |

**Razorpay webhooks.** Point each school's account at `<public base URL>/v1/finance/payment-gateways/razorpay:webhook` and MySMS's own account at `<public base URL>/v1/platform/tenant-subscriptions:webhook`, with the webhook secret above, subscribed to `order.paid`, `payment.captured`, `payment.failed`, `refund.processed` and `refund.failed`. Money is recorded only from a verified webhook or checkout signature.

## 5. The backend

### 5.1 Build

`mvn verify` in `backend/` ([`BUILD-AND-RUN.md`](BUILD-AND-RUN.md) §3); `mvn -pl app -am package -DskipTests` builds just the jars. Outputs: `app/target/app-0.1.0-SNAPSHOT-exec.jar` (monolith), `<module>/target/<module>-0.1.0-SNAPSHOT-exec.jar` (one service per module), each area's OpenAPI document `<module>/target/openapi/<area>/api-docs.yml` and gRPC descriptor set `<module>/target/docs/protos/grpc.protobin`.

### 5.2 Run as one monolith (recommended start)

All 10 modules (23 areas) in one process, each area with its own pool and Flyway history; gRPC between areas stays in-process.

```bash
export MYSMS_DB_URL=jdbc:postgresql://db:5432/mysms
export MYSMS_DB_USER=mysms_app MYSMS_DB_PASSWORD=<app-password>
export MYSMS_DB_MIGRATE_USER=mysms_migrate MYSMS_DB_MIGRATE_PASSWORD=<migrate-password>
export MYSMS_DB_RELAY_USER=mysms_relay MYSMS_DB_RELAY_PASSWORD=<relay-password>
export MYSMS_AUTH_ISSUER=https://sms.example.org
export MYSMS_AUTH_JWK_SET_URI=http://localhost:8080/oauth2/jwks
export MYSMS_IAM_SIGNING_KEY="$(cat /run/secrets/iam-signing.pem)"
export MYSMS_SECRETS_KEYS="$(cat /run/secrets/mysms-secrets-keys)"
export MYSMS_RABBITMQ_URI="$(cat /run/secrets/mysms-rabbitmq-uri)"   # or MYSMS_EVENTS_BUS=in-process
java -jar backend/app/target/app-0.1.0-SNAPSHOT-exec.jar
```

Ready when `GET /actuator/health/readiness` answers `{"status":"UP"}` (liveness: `/actuator/health/liveness`).

### 5.3 First start: the bootstrap administrator

A new installation has no tenant and no account. Add to the first start:

```bash
export MYSMS_IAM_BOOTSTRAP_TENANT_CODE=demo
export MYSMS_IAM_BOOTSTRAP_ADMIN_USERNAME=admin
export MYSMS_IAM_BOOTSTRAP_ADMIN_PASSWORD='<at least 12 characters>'
```

iam ensures the tenant exists with a system role `administrator` holding every permission, and an account holding it; it announces the tenant (`iam.tenant_directory.bootstrapped`) and platform registers it in `platform.tenant`. Start platform with or before iam (earlier events wait in `mysms.events.unrouted`, §5.7). Idempotent; never changes an existing password. **Remove the password from the environment after the first sign-in.**

```bash
curl -s -X POST http://localhost:8080/v1/iam/auth:login -H 'Content-Type: application/json' \
  -d '{"tenantCode":"demo","identifier":"admin","password":"<password>"}'
# → {"mfaRequired":false,"tokens":{"accessToken":"eyJ…",…}}; with an authenticator app: {"mfaRequired":true,"mfaToken":"…"},
#   then POST /v1/iam/mfa:complete {"mfaToken":"…","code":"<6-digit or recovery code>"}
curl -s http://localhost:8080/v1/iam/me -H "Authorization: Bearer <accessToken>"
```

### 5.4 Run as one service per module

Each module jar is a service that owns its areas' schemas and serves `/v1/<area>/…` for each of its areas (ARCHITECTURE §1). Give each the variables of §4.1 for every area it serves and its own `PORT`; identity (iam) also those of §4.2. Every service verifies tokens itself against iam's `/oauth2/jwks`, so start identity first.

```bash
PORT=8101 java -jar backend/identity/target/identity-0.1.0-SNAPSHOT-exec.jar
PORT=8102 java -jar backend/platform/target/platform-0.1.0-SNAPSHOT-exec.jar
# … one per module (10)
```

The gateway routes by the first path segment after `/v1/` (the area) to the module serving it and routes `/oauth2/jwks` to identity; `MYSMS_AUTH_ISSUER` is the same public URL in every service. Do **not** route `/oauth2/token` from the internet. gRPC: each service listens on `MYSMS_GRPC_SERVER_PORT` (9112); route `mysms.<area>.v1.*` to the module serving the area, HTTP/2 end to end.

### 5.5 Edge behaviour you can rely on

- Every request outside `/actuator/health/**`, `/actuator/info`, `/oauth2/jwks` and iam's `/oauth2/token` must match a contract operation (else 404), carry a valid bearer token (else 401), cover the requested institution (`X-Institution-Id`) and hold the operation's permission (else 403).
- Errors are `application/problem+json` with a stable `code`.
- Writes to existing rows need `If-Match` with the row's `ETag` (428 without, 412 when stale).
- Settings a user may not administer are invisible (404).
- **Audiences:** `public` (tenant users, or platform staff where the operation says so), `platform` (platform staff) and `internal` (services with a service token only). A service token is refused (403) on every other operation, a person's token on an internal one. Internal today: `org.ListActiveInstitutions` and privacy's `BatchCheckConsent`, `CheckConsent`, `GetPurposeStatus`, `ListSubjectsWithConsent`.
- The gRPC edge (§5.8) applies the same decision, with the same codes in `google.rpc.ErrorInfo.reason`.

### 5.6 Partition maintenance

Monthly partitions: `attendance.attendance_record`, `time_attendance.staff_attendance`, `comms.outbound_message`, `comms.in_app_notification`, `lms.content_view_session`, `lms.access_log`, `platform.audit_log`, `privacy.consent_evidence`. Each service creates the current month and the next `MYSMS_PARTITIONS_MONTHS_AHEAD` at startup and every `MYSMS_PARTITIONS_INTERVAL`, as the schema owner. It waits at most 5 s for a table lock and retries at its next run.

A row whose month has no partition lands in `…_default` and the job logs `partition … (DEFAULT) holds rows` at ERROR: alert on it, move those rows out, create the month, move them back (§11). Old months are dropped only with `MYSMS_PARTITIONS_RETENTION_MONTHS` set (12 or more); back up first. Lists over these tables must name a date range of at most 366 days.

### 5.7 Events

A change writes its event into the module's `outbox_event` in the same transaction; the module's relay publishes it and marks it published once the bus confirms; each consumer records it in its `inbox_event` and handles it exactly once. The bus interface `com.mysms.messaging.events.EventBus` and all adapters live in `messaging`; select one with `MYSMS_EVENTS_BUS`. Diagram: [docs/diagrams/architecture/events-outbox-sequence.drawio](docs/diagrams/architecture/events-outbox-sequence.drawio) — open with draw.io / diagrams.net.

**Delivery.** At least once, unordered across replicas; the inbox (tenant, event id) makes handling exactly once. Each relay pass gives a module at most 10 batches of up to 100 rows; a batch stops after 20 s and commits what the broker confirmed. A row the relay cannot turn into its module's event is set aside with the reason in `<schema>.outbox_event.publish_error` (ERROR log) and the rows after it go on. `/actuator/health` component `events` is DOWN while the broker connection is lost or a relay has failed 3 passes in a row.

#### RabbitMQ (default)

Services declare what they need at startup:

| Object | Kind | Purpose |
|---|---|---|
| `mysms.events` | topic exchange, durable | every event, routed by type (`hr.staff.deleted`); alternate exchange `mysms.events.unrouted` |
| `mysms.<area>.events` | quorum queue | the events an area consumes; its module's replicas share it |
| `mysms.events.unrouted` | fanout exchange + quorum queue | events no queue was bound to, kept 3 days for `replay-unrouted` |
| `mysms.events.dead` | direct exchange | where failing events go |
| `mysms.<area>.dead` | quorum queue | events that failed every delivery (first plus 5 returns), or were unreadable, unauthentic or rejected |

A failed handling is returned after 1 s, 2 s, 4 s … up to 60 s. Change `delivery-limit` or dead-letter settings of an existing queue with a RabbitMQ policy, or drain, delete and let the service declare it again; a service started with other arguments stops with `its queue … exists with other arguments`.

**One broker user per service.** RabbitMQ verifies each message's AMQP `user-id`. An event names its source *area* (`payroll.payslip.issued`); a consumer accepts it only from the user of the module serving that area (`mysms-hrms`; the area → module table is `MessagingModules` in `backend/messaging`), and topic permissions let each user publish only its areas' event types and `audit.recorded`. Each area keeps its own queues (`mysms.<area>.events`, `mysms.<area>.dead`). For each module (`hrms` below, areas `hr`, `payroll` and `time_attendance`), vhost `mysms`:

```bash
rabbitmqctl add_vhost mysms
rabbitmqctl add_user mysms-hrms '<hrms-rabbit-password>'
P='^mysms\.(events(\.dead|\.unrouted)?|(hr|payroll|time_attendance)\.(events|dead))$'
rabbitmqctl set_permissions -p mysms mysms-hrms "$P" "$P" "$P"
rabbitmqctl set_topic_permissions -p mysms mysms-hrms mysms.events '^((hr|payroll|time_attendance)\..+|audit\.recorded)$' '.*'
# hrms service: MYSMS_RABBITMQ_URI='amqps://mysms-hrms:<hrms-rabbit-password>@broker:5671/mysms'
```

| Module (user `mysms-<module>`) | Areas in its two regexes |
|---|---|
| `platform` | `platform`, `master`, `org` |
| `identity` | `iam`, `people` |
| `privacy` | `privacy` |
| `comms` | `comms` |
| `hrms` | `hr`, `payroll`, `time_attendance` |
| `students` | `sis`, `admission`, `careers` |
| `academics` | `academics`, `timetable`, `attendance`, `assessment` |
| `finance` | `finance` |
| `campus` | `library`, `transport`, `frontoffice`, `facility` |
| `learning` | `lms` |

The monolith publishes for every module over one connection: give its user `'.*' '.*' '.*'` on the vhost and keep `MYSMS_RABBITMQ_PUBLISHER_USER=self`. Operators replay with their own user tagged `impersonator` (replayed events keep the original `user-id`):

```bash
rabbitmqctl add_user mysms-ops '<ops-password>' && rabbitmqctl set_user_tags mysms-ops impersonator
rabbitmqctl set_permissions -p mysms mysms-ops '.*' '.*' '.*'
export MYSMS_RABBITMQ_URI='amqps://mysms-ops:<ops-password>@broker:5671/mysms'
java -cp academics-0.1.0-SNAPSHOT-exec.jar -Dloader.main=com.mysms.messaging.rabbitmq.EventsAdmin \
  org.springframework.boot.loader.launch.PropertiesLauncher replay-dead academics   # [max], default 10000
java -cp academics-0.1.0-SNAPSHOT-exec.jar -Dloader.main=com.mysms.messaging.rabbitmq.EventsAdmin \
  org.springframework.boot.loader.launch.PropertiesLauncher replay-unrouted
```

`replay-dead <area>` returns a consuming area's parked events to its queue (one call per area of a multi-area module) (the failure reason is in its `inbox_event.processing_error` and log); `replay-unrouted` publishes waiting events again.

Copies kept from events start empty on an install that had the source rows before the copy existed. **hrms's holiday copy** (`hr.holiday_record`, from `org.holiday.*`, release with BE-6): org's earlier holiday events carry only the id and change nothing in it, so on such an install save each existing holiday once: `PATCH /v1/org/holidays/{id}` with `If-Match` and one field re-sent with its current value (for example `{"title":"<its current title>"}`) bumps the row version and publishes `org.holiday.updated` with the row (an empty `{}` changes nothing and publishes nothing). Until then leave and pay count those days as working days, and leave approved meanwhile keeps the days it counted (`counted_days`). A local broker: `docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4.1-management-alpine`, then one user with `'.*'` permissions and `amqp://` in `MYSMS_RABBITMQ_URI` (`amqps://` in production).

#### Google Cloud Pub/Sub

`MYSMS_EVENTS_BUS=google-pubsub`, `MYSMS_EVENTS_GCP_PROJECT_ID`, Application Default Credentials (in production one service account per service, preferably through Workload Identity).

| Resource | Name | IAM |
|---|---|---|
| Source topic | `mysms-events-<source>` | producer: `roles/pubsub.publisher` on the topic of each of its areas (`<source>` is the source area) |
| Subscription | `mysms-<consumer>-from-<source>` | consumer: `roles/pubsub.subscriber` on its subscriptions |
| Dead-letter topic / subscription | `mysms-events-<consumer>-dead` / `mysms-<consumer>-dead` | consumer: publisher on its dead-letter topic; Pub/Sub service agent: publisher on dead-letter topics, subscriber on source subscriptions |

Provision with infrastructure-as-code and keep `MYSMS_EVENTS_GOOGLE_PUBSUB_CREATE_RESOURCES=false`; `true` needs create, get, update and seek permissions and validates existing topology. Topics retain events 3 days; retries back off 10 s to 60 s, 5 delivery attempts, then dead letter (best effort: monitor dead-letter metrics). A rejected event is copied to the dead-letter topic with `mysmsDeadLetterConsumer` and `mysmsDeadLetterReason` attributes; there is no automated replay (republish with the original body and `eventType` / `sourceModule` attributes). Local emulator:

```bash
gcloud beta emulators pubsub start --project=mysms-local --host-port=0.0.0.0:8085
export MYSMS_EVENTS_BUS=google-pubsub MYSMS_EVENTS_GCP_PROJECT_ID=mysms-local PUBSUB_EMULATOR_HOST=localhost:8085
```

The emulator does not support topic retention, so retention-window backfill needs a real project.

### 5.8 gRPC

Every operation is also a gRPC method (service `mysms.<module>.v1.<Root>Service`, [`api/README.md`](api/README.md)), answered by the same business service as REST; each module except privacy has a `<Module>GrpcParityComponentTest`. The process listens on `MYSMS_GRPC_SERVER_PORT` (plaintext HTTP/2; TLS at the proxy or mesh) and serves `grpc.health.v1.Health` (NOT_SERVING while shutting down; running calls get 20 s).

- Metadata: `authorization` (`Bearer …`), `x-institution-id`, `traceparent`; the trace id returns as `x-trace-id`.
- Refusals: `google.rpc.Status` with `ErrorInfo` (`reason` = REST problem `code`, metadata `status`, `traceId`), `BadRequest` for field errors, `PreconditionFailure` for 412/428.
- Idempotent operations take `request_id` (REST `Idempotency-Key`, same store and rules); a replay carries `idempotent-replayed: true`.
- An operation REST answers 501 answers `UNIMPLEMENTED`.

Module-to-module calls (JAVA-CODING-RULES §10): a split deployment addresses each callee by Kubernetes DNS name (`MYSMS_NAMESPACE`, `MYSMS_<MODULE>_SERVICE_PORT_RPC` or `MYSMS_<MODULE>_GRPC_ADDRESS`); the monolith uses its in-process server. Each module configures clients only for modules it calls, plus iam (permissions, §5.10).

```bash
grpcurl -plaintext -d '{"service":"mysms.org.v1.InstitutionService"}' localhost:9112 grpc.health.v1.Health/Check
```

### 5.9 Service identity (service-to-service calls)

A call no person makes (a background job, an operation of audience `internal`) uses the calling module's **service token**: iam issues it at `POST /oauth2/token` (OAuth 2.0 client credentials, `client_secret_post`, `client_id=<module>`). The token has `sub = service:<module>`, audience `mysms-internal`, lifetime `MYSMS_IAM_SERVICE_TOKEN_TTL`, the client's configured permissions and no tenant; the call names its tenant in `X-Tenant-Id` (gRPC `x-tenant-id`), accepted from service tokens only. The module refreshes its token a minute before expiry (half its lifetime if shorter). Every other cross-module call uses the caller's own token.

```bash
SECRET=$(openssl rand -base64 48)                 # into the module's secret store
printf %s "$SECRET" | sha256sum                   # its hash, into iam's configuration
# the finance service
MYSMS_AUTH_TOKEN_URI=http://iam.internal:8101/oauth2/token
MYSMS_FINANCE_SERVICE_CLIENT_SECRET="$SECRET"
# iam
MYSMS_IAM_SERVICECLIENTS_FINANCE_SECRETSHA256=<hex hash>
MYSMS_IAM_SERVICECLIENTS_FINANCE_PERMISSIONS=<permission codes, comma-separated>
```

Needed today by:

| Module | Permission | Without it |
|---|---|---|
| iam (self sign-up reads active schools from org) | `org.institution.list_active` | `iam.SignUp` and `iam.ListSignUpInstitutions` answer 503 |
| people (consent checks through privacy-client, design PRIVACY-AND-CONSENT-CENTRE §7) | `privacy.consent_decision.batch_check` | every consent check is refused (fail closed) |
| every module with a privacy task handler (`MYSMS_PRIVACY_ERASURE_MODULES`; privacy tasks and retention runs, design PRIVACY-AND-CONSENT-CENTRE §9.1: legal holds asked before erasing) | `privacy.legal_hold.check` | every erasure and retention sweep answers failed and erases nothing (fail closed) |

Rotation: set the new secret on the module and the new hash at iam, restart both; the cached token stays valid until it expires.

### 5.10 Permissions: catalogues and the role cache

Every module publishes its API-permission catalogue (its operation table) to iam as `<module>.api_permissions.published` at startup and every `MYSMS_CATALOGUE_INTERVAL`. iam keeps it in `iam.api_permission` (`iam.ListApiPermissions`), adds new tenant-user permission codes, and retires (never deletes) an operation no module has published for `MYSMS_IAM_API_PERMISSION_RETIRE_AFTER`. On Pub/Sub keep iam's source areas (`mysms.events.google-pubsub.source-modules`) at the default (every area).

Each module's edge decides every call itself. A person's token names role grants (`rgr`), direct grants and denies and the tenant's authorisation version (`azv`); the filter resolves permissions through a per-process cache of each role's codes, filled from iam's `IamAuthzService.GetRolePermissions` (in iam and the monolith, directly). A cached role older than the token's `azv` is fetched again; other changes show within `MYSMS_ROLE_CACHE_TTL`. When iam cannot answer what the cache lacks the call is refused with 503 `UNAVAILABLE`, never admitted: **run iam with at least two replicas and a rolling update.**

### 5.11 Upgrade notes

- **Sibling areas call each other in process (2026-11-27).** Admission's enrolment steps no longer reach sis over gRPC, so a step key kept by the earlier build no longer matches: an enrolment retry that spans the upgrade may answer 422 `IDEMPOTENCY_KEY_REUSED` for up to 24 hours (the ledger's retention). The same retry runs normally once that window has passed; to avoid it, upgrade when no enrolment is in flight.

## 6. Production checklist

- [ ] The three database roles of §2.1; the application connects as `mysms_app`, migrations as `mysms_migrate`, the relay as `mysms_relay` (without it events stay in the outboxes).
- [ ] `MYSMS_IAM_SIGNING_KEY` and `MYSMS_SECRETS_KEYS` from the secret store; the secrets key backed up apart from database backups.
- [ ] Bootstrap password removed after the first sign-in.
- [ ] TLS at the proxy; the backend unreachable from the internet; web apps and API on **one origin** (no CORS headers by design).
- [ ] `max_connections` ≥ 22 × pool size + headroom for the monolith.
- [ ] Only `/actuator/health/**` routed from the load balancer; the actuator exposes `health` and `info` only.
- [ ] Daily `pg_dump -Fc` plus `pg_dumpall --roles-only`, with a tested restore.
- [ ] `database/verification/smoke.sql` in CI against a scratch database, never production.
- [ ] Log shipping for standard output (requests carry the W3C `traceparent`).
- [ ] Alerts on the ERROR lines `partition … (DEFAULT) holds rows` and `set outbox row … aside`, on health component `events` DOWN, and on the WARN line `service token refused`.
- [ ] RabbitMQ: `amqps://`, one broker user per service (§5.7), alerts on `mysms.*.dead` and `mysms.events.unrouted`. Pub/Sub: per-service identities, topic and subscription IAM, dead-letter metrics.
- [ ] gRPC port reachable only through the proxy or mesh, reflection off, load-balancer health check on `grpc.health.v1.Health`.
- [ ] Service identity: one secret per module, only its SHA-256 at iam, `/oauth2/token` not routed from the internet.
- [ ] iam with at least two replicas (§5.10).

## 7. Web apps (school web app and platform console)

```bash
cd clients
pnpm install
pnpm verify                                   # format, SDK generation, typecheck, lint, test, build
pnpm --filter @mysms/web build                # → apps/web/dist
pnpm --filter @mysms/platform-console build   # → apps/platform-console/dist
```

Serve `dist` as static files from the same origin as the API, with a fallback to `index.html`:

```nginx
server {
  listen 443 ssl;
  server_name sms.example.org;
  root /srv/mysms/web;                          # apps/web/dist
  location /v1/          { proxy_pass http://127.0.0.1:8080; proxy_set_header X-Forwarded-Proto https; }
  location /oauth2/jwks  { proxy_pass http://127.0.0.1:8080; }
  location /assets/      { expires 1y; add_header Cache-Control "public, immutable"; }
  location /             { try_files $uri /index.html; }
}
```

Serve the platform console the same way on its own host name (e.g. `console.example.org`), for platform operators only.

## 8. Developing the web apps against a local backend

[`BUILD-AND-RUN.md`](BUILD-AND-RUN.md) §4. There is no mock API; mocking is done only in tests.

## 9. Phone and tablet apps

```bash
cd clients
EXPO_PUBLIC_API_BASE_URL=https://sms.example.org pnpm --filter @mysms/mobile start            # student app
EXPO_PUBLIC_API_BASE_URL=https://sms.example.org pnpm --filter @mysms/mobile start:parent
EXPO_PUBLIC_API_BASE_URL=https://sms.example.org pnpm --filter @mysms/mobile start:teacher
EXPO_PUBLIC_API_BASE_URL=https://sms.example.org pnpm --filter @mysms/tablet start
```

Scan the QR code with Expo Go, or press `a` / `i` for an emulator. Store builds: `npx eas build --platform android|ios` in `apps/mobile` or `apps/tablet`, with `APP_VARIANT` per phone variant (bundle identifiers in `app.config.ts`). Check a bundle compiles without a device: `npx expo export --platform android --output-dir /tmp/mobile-bundle` in the app's folder.

## 10. Tests

Suites, gates and commands: [`BUILD-AND-RUN.md`](BUILD-AND-RUN.md) §3.

## 11. Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `mysms.auth.issuer (MYSMS_AUTH_ISSUER) is required` or `… must be an absolute http(s) URL` | Set the issuer to iam's public base URL (§4.1). |
| `mysms.auth.jwk-set-uri (or, for development, mysms.auth.public-key) is required` | Set `MYSMS_AUTH_JWK_SET_URI`. |
| `mysms.iam.token.signing-key is required` or `… PKCS#8 …` | Pass the PEM *contents*, PKCS#8 (`-----BEGIN PRIVATE KEY-----`), 2048 bits or more. |
| `mysms.secrets.keys (MYSMS_SECRETS_KEYS) is not configured` | Set the field-encryption key (§3a). |
| `permission denied for schema …` at startup | `mysms_migrate` must own the database (or have `CREATE` on it). |
| `permission denied for table flyway_schema_history` | `MYSMS_DB_MIGRATE_USER` / `_PASSWORD` missing: Flyway ran as the application role. |
| `FATAL: sorry, too many clients already` | The monolith opens 23 pools: lower `MYSMS_<MODULE>_DB_POOL_SIZE` or raise `max_connections`. |
| `partition … (DEFAULT) holds rows` / `partition … could not be created` | Move the rows out of `…_default`, `CREATE TABLE <schema>.<table>_YYYY_MM PARTITION OF <schema>.<table> FOR VALUES FROM ('YYYY-MM-01') TO (…)`, move them back through the parent (§5.6). |
| A list answers 400 `INVALID_FILTER` "this list needs a range on …" | The table is partitioned: filter a range of at most 366 days. |
| `mysms.events.rabbitmq.uri (MYSMS_RABBITMQ_URI) is required` | Set the broker URI, or `MYSMS_EVENTS_BUS=in-process` for a single process. |
| `module … has no relay-username …` | Set `MYSMS_DB_RELAY_USER` / `_PASSWORD` (§2.1); pending events go out as soon as the relay runs. |
| `the relay role must be a role of its own` | The relay user is the application or migration user; create `mysms_relay`. |
| `set outbox row … aside, it is not published` | The row was not that module's event or was unreadable (`publish_error` says why); fix it and set `publish_error` to NULL. |
| Messages in `mysms.<area>.dead` | The handler failed every delivery (`inbox_event.processing_error`, WARN log), or the event was unreadable, from the wrong broker user (ERROR `claiming module … from broker user …`) or rejected. Fix, then `replay-dead <area>`. |
| Messages in `mysms.events.unrouted` | No queue was bound for that type: start the consumer, then `replay-unrouted`. |
| `ACCESS_REFUSED - write access to topic '…'` | A service published another module's event type: a bug; the event stays in its outbox. |
| `its queue … exists with other arguments` / `inequivalent arg` | §5.7, queue settings. |
| Every call answers 401 | `MYSMS_AUTH_ISSUER` differs from the token's `iss`, or `MYSMS_AUTH_JWK_SET_URI` is unreachable. |
| Sign-in answers 401 for the right password | Locked after 5 failures for 1, 2, 4 … up to 60 minutes; or the tenant is suspended. |
| Calls answer 503 `UNAVAILABLE` in every module | iam cannot answer role permissions (§5.10). |
| 501 `NOT_IMPLEMENTED` | The operation is in the contract but not served yet. |
| CORS errors in the browser | Web app and API on different origins (§7). |
| 403 changing a tenant-wide record (category, fee type …) | Tenant-level records need the permission granted at tenant scope. |
| A record of another school is "not found" | Rows outside the caller's institution grants are hidden (404) by design. |
| A setting is missing from "My settings" | It is an administrator setting, visible only to those who administer it. |
| Phone app cannot reach the API | `EXPO_PUBLIC_API_BASE_URL` must be reachable from the device. |
| 422 `IDEMPOTENCY_KEY_REUSED` | The same `Idempotency-Key` with a different request; use a new key per request, the same key only for an exact retry within 24 hours. |
| 409 `REQUEST_IN_PROGRESS` | The first request with that key is still running; a lock older than two minutes is taken over by the retry. |
| 500 with `unreadable secret envelope` or `does not open for this tenant` in the log | The key that wrote the value is no longer in `MYSMS_SECRETS_KEYS`; put it back. |
| `mfaRequired: true` and no tokens | Expected for accounts with an authenticator app: `POST /v1/iam/mfa:complete` with the `mfaToken` within five minutes. |
