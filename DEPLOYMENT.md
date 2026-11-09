# Deploying and running MySMS

This guide takes MySMS from a fresh checkout to a running installation: the database, the backend (as one monolith or as 21 microservices), the web apps, the phone and tablet apps, local development and every test suite. Every command here was run as written against PostgreSQL 16 and the current build.

What is served today is listed in [`backend/README.md`](backend/README.md#status). A contract operation without an implementation answers `501 NOT_IMPLEMENTED`, so clients can always tell "not built" apart from "not found".

---

## 1. Prerequisites

| Tool | Version | Used for |
|---|---|---|
| JDK | 17 | backend build and runtime (the build targets Java 17; a newer JDK also works) |
| Maven | 3.9+ | backend build |
| PostgreSQL | 16 | the database (no extensions needed) |
| RabbitMQ | 4.1 (3.13+ works: quorum queues) | the event bus between modules (§5.7); `MYSMS_EVENTS_BUS=in-process` runs a single process without it |
| Node.js | 22.12+ | clients |
| pnpm | 10 (`corepack enable`) | clients |
| OpenSSL | any current | the token-signing key |
| Docker | optional | backend tests can start PostgreSQL and RabbitMQ through Testcontainers instead of using servers you provide |
| Expo Go or a device simulator | optional | phone and tablet apps |

Memory: a full backend build peaks at about 4.5 GB. Do not run the backend build and the client build at the same time on a machine with less than 8 GB.

## 2. The database

MySMS uses **one PostgreSQL database with one schema per module** (21 schemas: `master`, `platform`, `iam`, `org`, `people`, `academics`, `sis`, `hr`, `payroll`, `admission`, `attendance`, `facility`, `timetable`, `assessment`, `lms`, `finance`, `library`, `transport`, `comms`, `frontoffice`, `careers`). No foreign key crosses schemas, so any module can later move to its own database by pointing its `MYSMS_<MODULE>_DB_*` variables elsewhere.

### 2.1 Roles

Four roles are created once by operations, never by the migrations:

| Role | Purpose | Attributes |
|---|---|---|
| `mysms_migrate` | Owns the schemas and tables; Flyway runs as this role at startup. | `LOGIN` |
| `mysms_app` | The application's runtime login. It is **not** the table owner, so the `FORCE`d row-level security applies to it and the application can never switch it off. | `LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB` |
| `mysms_relay` | The outbox relay (§5.7): reads every tenant's pending events. On each schema Flyway grants it `USAGE`, `SELECT, DELETE` on `outbox_event` and `inbox_event`, and `UPDATE` of `outbox_event.published_at` and `.publish_error` only — no business table, and it can never write an event. A role of its own: a service refuses to start when the relay user is its application or migration user. | `LOGIN BYPASSRLS` |
| `mysms_platform_admin` | Platform console and support tooling, used by the platform service only; never handed to the module services. | `LOGIN BYPASSRLS` |

As a PostgreSQL superuser (replace the passwords with values from your secret store):

```sql
CREATE ROLE mysms_migrate LOGIN PASSWORD '<migrate-password>';
CREATE ROLE mysms_app LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB PASSWORD '<app-password>';
CREATE ROLE mysms_relay LOGIN BYPASSRLS PASSWORD '<relay-password>';
CREATE ROLE mysms_platform_admin LOGIN BYPASSRLS PASSWORD '<platform-password>';

CREATE DATABASE mysms OWNER mysms_migrate;
REVOKE ALL ON DATABASE mysms FROM PUBLIC;
GRANT CONNECT ON DATABASE mysms TO mysms_app, mysms_relay, mysms_platform_admin;
```

That is all. On every start each module runs its own Flyway history (`<schema>.flyway_schema_history`) as `mysms_migrate` and then grants `mysms_app` exactly `USAGE` on the schema, DML on its tables and use of its sequences. Flyway logs a harmless warning `schema "<module>" already exists, skipping` on the first start, because Flyway creates the schema just before the baseline's `CREATE SCHEMA IF NOT EXISTS`.

If `MYSMS_DB_MIGRATE_USER` is left empty the migrations run as the application user — acceptable on a developer machine, **never in production**, because the application would then own its tables and bypass row-level security.

### 2.2 Schema smoke test (throwaway databases only)

`database/verification/smoke.sql` proves tenant isolation, the money and evidence constraints and the distributed-deployment invariants. It **commits seed tenants** (`alpha`, `beta`), so run it only against a scratch database that the backend has migrated, never against a real installation:

```bash
psql -v ON_ERROR_STOP=1 -d mysms_scratch -f database/verification/smoke.sql   # ends with "SMOKE4 OK"
```

### 2.3 Backups

Back up the whole database (`pg_dump -Fc mysms`); every module's Flyway history lives in its own schema, so a restore brings code and data versions back together. Row-level security and grants are part of the dump; the three roles are not (`pg_dumpall --roles-only`).

## 3. The token-signing key

The iam module issues RS256 access tokens. Generate one key per installation and keep it only in the secret store:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out iam-signing.pem
```

Pass the file's *contents* (PEM, PKCS#8) in `MYSMS_IAM_SIGNING_KEY`. iam refuses to start without it and publishes the public half at `/oauth2/jwks`.

**Rotation:** generate a new key, set it as `MYSMS_IAM_SIGNING_KEY`, put the old key's public half (`openssl pkey -in old.pem -pubout`) in `MYSMS_IAM_PREVIOUS_PUBLIC_KEY`, restart iam, and remove the previous key after one access-token lifetime (15 minutes by default). Refresh tokens are opaque and are not affected.

## 3a. Secrets: the field-encryption master key

Columns ending `_enc` (national ids, bank account numbers, gateway and webhook secrets, meeting passwords and links, conferencing and tool secrets, MFA secrets) are encrypted by the application before they reach the database: AES-256-GCM in the envelope `v1.<key id>.<iv>.<ciphertext>`. Each tenant gets its own key, derived with HKDF-SHA256 from the master key and the tenant id, and every value is bound to its tenant and column, so a value copied to another tenant or column does not open. The API only accepts these values (write-only) and shows a masked form (`••••1234`).

Generate one 32-byte master key per installation and keep it only in the secret store:

```bash
echo "k1:$(openssl rand -base64 32)"
```

Set `MYSMS_SECRETS_KEYS` to that value (`<key id>:<base64 key>`). Every service except master refuses to start without it, and so does the monolith: besides the secret columns, the responses kept for `Idempotency-Key` retries are stored encrypted with it. **Losing the key loses those values**: back it up with the same care as the database backups, but never next to them.

**Rotation:** add a new key to the list and make it the active one, e.g. `MYSMS_SECRETS_KEYS=k1:<old>,k2:<new>` and `MYSMS_SECRETS_ACTIVE_KEY=k2` (the last key listed is active when the variable is not set). New values use `k2`; values written under `k1` stay readable as long as `k1` is listed. Keep the old key until every value has been rewritten under the new one (replacing a secret through the API rewrites it).

## 4. Configuration reference

Everything comes from the environment; no secret is in the repository.

### 4.1 Every service (and the monolith)

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_DB_URL` | yes | JDBC URL, e.g. `jdbc:postgresql://db:5432/mysms` |
| `MYSMS_DB_USER`, `MYSMS_DB_PASSWORD` | yes | the application role (`mysms_app`) |
| `MYSMS_DB_MIGRATE_USER`, `MYSMS_DB_MIGRATE_PASSWORD` | yes in production | the schema owner (`mysms_migrate`) Flyway runs as |
| `MYSMS_<MODULE>_DB_URL`, `_DB_USER`, `_DB_PASSWORD`, `_DB_MIGRATE_USER`, `_DB_MIGRATE_PASSWORD` | no | per-module override (e.g. `MYSMS_FINANCE_DB_URL`) to give one module its own database |
| `MYSMS_<MODULE>_DB_POOL_SIZE` | no | connection pool size per module, default 10. The monolith opens one pool per module: 21 × pool size must stay below PostgreSQL's `max_connections`. |
| `MYSMS_AUTH_ISSUER` | yes | the token issuer, i.e. the public base URL of iam (or of the monolith). A service refuses to start without it. |
| `MYSMS_AUTH_JWK_SET_URI` | yes | where the public keys are fetched: `<iam base URL>/oauth2/jwks` |
| `MYSMS_SECRETS_KEYS` | yes for every service except master, and for the monolith | **secret**, the field-encryption master key list from §3a |
| `MYSMS_SECRETS_ACTIVE_KEY` | no | the key id new values are encrypted with; default the last one listed |
| `MYSMS_PARTITIONS_ENABLED` | no | the partition-maintenance job (§5.6), default `true`; it connects as `MYSMS_DB_MIGRATE_USER` |
| `MYSMS_PARTITIONS_MONTHS_AHEAD` | no | months after the current one that must already have a partition, default 3, at most 24 |
| `MYSMS_PARTITIONS_RETENTION_MONTHS` | no | months kept before the current one; older monthly partitions are **dropped**. Default 0: nothing is ever dropped; otherwise at least 12 |
| `MYSMS_PARTITIONS_INTERVAL` | no | how often the job runs after its run at startup, default `24h` |
| `MYSMS_RABBITMQ_URI` | yes with the RabbitMQ bus | **secret**, `amqps://<user>:<password>@<host>:5671/<vhost>` (§5.7). A service refuses to start without it. |
| `MYSMS_EVENTS_BUS` | no | `rabbitmq` (default), `in-process` (one JVM: the monolith without a broker) or `none` (nothing relayed or consumed: tests only) |
| `MYSMS_RABBITMQ_PUBLISHER_USER` | no | the broker user events of a module must come from, `{module}` standing for the source module: default `mysms-{module}` for a service (§5.7), `self` for the monolith (this process's own user) |
| `MYSMS_DB_RELAY_USER`, `MYSMS_DB_RELAY_PASSWORD` | yes in production | the outbox relay's role (`mysms_relay`, §2.1); without it the relay runs only when the application role bypasses row-level security (a local superuser), otherwise it logs an ERROR and events stay in the outbox |
| `MYSMS_EVENTS_RELAY_INTERVAL` | no | pause between relay passes, default `500ms` |
| `MYSMS_EVENTS_OUTBOX_RETENTION` | no | how long published outbox rows are kept for diagnosis, default `7d` |
| `MYSMS_EVENTS_INBOX_RETENTION` | no | how long processed inbox rows (the ids that make handling exactly-once) are kept, default `30d`; keep it longer than you would wait before replaying a dead-lettered event |
| `PORT` | no | HTTP port, default 8080 |

### 4.2 iam (and the monolith, which contains it)

| Variable | Required | Meaning |
|---|---|---|
| `MYSMS_IAM_SIGNING_KEY` | yes | **secret**, the PEM private key from §3 |
| `MYSMS_IAM_SIGNING_KEY_ID` | no | `kid`; default the key's RFC 7638 thumbprint |
| `MYSMS_IAM_PREVIOUS_PUBLIC_KEY` | no | the retiring public key during rotation |
| `MYSMS_IAM_ACCESS_TOKEN_TTL`, `MYSMS_IAM_REFRESH_TOKEN_TTL` | no | ISO-8601 durations, default `PT15M` and `P30D` |
| `MYSMS_PEOPLE_URL` | no | base URL of the people service (for the monolith: its own URL) so `GetMe` includes the person record |
| `MYSMS_IAM_BOOTSTRAP_TENANT_CODE`, `MYSMS_IAM_BOOTSTRAP_ADMIN_USERNAME`, `MYSMS_IAM_BOOTSTRAP_ADMIN_PASSWORD` | first start only | see §5.3 |

### 4.3 Web apps (build time)

| Variable | Meaning |
|---|---|
| `VITE_API_BASE_URL` | API origin. Leave unset to call the same origin the app is served from (recommended, see §7). |
| `VITE_TENANT_CODE` | pre-fills the school code on sign-in for single-school hosts |
| `VITE_DEFAULT_LOCALE` | `en` (default) or `hi` |
| `MYSMS_API_PROXY` | dev server only: where `/v1` is proxied, default the local monolith `http://localhost:8080` |

### 4.4 Phone and tablet apps

| Variable | Meaning |
|---|---|
| `EXPO_PUBLIC_API_BASE_URL` | the API origin the app calls (must be reachable from the device, so not `localhost` on a phone) |
| `APP_VARIANT` | phone app only: `student` (default), `parent` or `teacher` |

## 5. The backend

### 5.1 Build

```bash
cd backend
mvn verify                          # generate contracts + SDK, compile, unit + component tests, Checkstyle, SpotBugs, JaCoCo
mvn -pl app -am package -DskipTests # just the monolith jar, when the tests already ran
```

Outputs: `app/target/app-0.1.0-SNAPSHOT-exec.jar` (the monolith) and `<module>/target/<module>-0.1.0-SNAPSHOT-exec.jar` for each of the 21 services. After a change under `/api`, the next build regenerates the contracts and the SDK.

### 5.2 Run as one monolith (recommended start)

All 21 modules in one process, each still with its own pool and Flyway history:

```bash
export MYSMS_DB_URL=jdbc:postgresql://db:5432/mysms
export MYSMS_DB_USER=mysms_app MYSMS_DB_PASSWORD=<app-password>
export MYSMS_DB_MIGRATE_USER=mysms_migrate MYSMS_DB_MIGRATE_PASSWORD=<migrate-password>
export MYSMS_AUTH_ISSUER=https://sms.example.org
export MYSMS_AUTH_JWK_SET_URI=http://localhost:8080/oauth2/jwks
export MYSMS_IAM_SIGNING_KEY="$(cat /run/secrets/iam-signing.pem)"
export MYSMS_SECRETS_KEYS="$(cat /run/secrets/mysms-secrets-keys)"
export MYSMS_PEOPLE_URL=http://localhost:8080
export MYSMS_DB_RELAY_USER=mysms_relay MYSMS_DB_RELAY_PASSWORD=<relay-password>
export MYSMS_RABBITMQ_URI="$(cat /run/secrets/mysms-rabbitmq-uri)"   # or MYSMS_EVENTS_BUS=in-process for one process
java -jar backend/app/target/app-0.1.0-SNAPSHOT-exec.jar
```

Ready when `GET /actuator/health/readiness` answers `{"status":"UP"}` (liveness: `/actuator/health/liveness`).

### 5.3 First start: the bootstrap administrator

A new installation has no tenant and no account. Add these three variables to the first start:

```bash
export MYSMS_IAM_BOOTSTRAP_TENANT_CODE=demo
export MYSMS_IAM_BOOTSTRAP_ADMIN_USERNAME=admin
export MYSMS_IAM_BOOTSTRAP_ADMIN_PASSWORD='<at least 12 characters>'
```

iam then makes sure the tenant code exists, that the tenant has a system role `administrator` holding every permission, and that the account exists and holds it. It is idempotent and never changes an existing account's password. **Remove the password from the environment once the administrator has signed in.**

Check it:

```bash
curl -s -X POST http://localhost:8080/v1/iam/auth:login -H 'Content-Type: application/json' \
  -d '{"tenantCode":"demo","identifier":"admin","password":"<password>"}'
# → {"mfaRequired":false,"tokens":{"accessToken":"eyJ…","refreshToken":"…","expiresInSeconds":900,"tokenType":"Bearer"},…}
# an account with an authenticator app gets {"mfaRequired":true,"mfaToken":"…"} instead; then
#   POST /v1/iam/mfa:complete {"mfaToken":"…","code":"<6-digit code or a recovery code>"} gives the tokens

curl -s http://localhost:8080/v1/iam/me -H "Authorization: Bearer <accessToken>"
curl -s http://localhost:8080/v1/iam/me/settings -H "Authorization: Bearer <accessToken>"
```

### 5.4 Run as microservices

Each module jar is a service that owns one schema and serves the paths `/v1/<module>/…`. Start each with the variables of §4.1 and its own `PORT`; iam also gets those of §4.2 (with `MYSMS_PEOPLE_URL` pointing at the people service). Every service verifies tokens itself against iam's `/oauth2/jwks`, so start iam first.

```bash
PORT=8101 java -jar backend/iam/target/iam-0.1.0-SNAPSHOT-exec.jar
PORT=8102 java -jar backend/people/target/people-0.1.0-SNAPSHOT-exec.jar
PORT=8103 java -jar backend/master/target/master-0.1.0-SNAPSHOT-exec.jar
# … one per module
```

Put a gateway or reverse proxy in front that routes by the first path segment after `/v1/` (`/v1/iam/` → iam, `/v1/finance/` → finance, …) and routes `/oauth2/jwks` to iam. `MYSMS_AUTH_ISSUER` must be the same public URL in every service.

### 5.5 Edge behaviour you can rely on

- Every request outside `/actuator/health/**`, `/actuator/info` and `/oauth2/jwks` must match a contract operation (else 404), carry a valid bearer token for the right tenant (else 401), cover the requested institution (`X-Institution-Id`) and hold the operation's permission (else 403).
- Errors are `application/problem+json` with a stable `code`.
- Writes to existing rows need `If-Match` with the row's `ETag` (428 without it, 412 on a stale one).
- Settings that users may not change (for example `security.sessionTimeoutMinutes`) are invisible — 404 — to anyone who does not administer settings at one of the scopes the setting allows.

### 5.6 Partition maintenance

Attendance records, staff attendance, outbound messages, in-app notifications, content views, access logs and the
audit log are partitioned by month (database/README.md). Each service runs a job at startup and then daily that
creates the current month and the next `MYSMS_PARTITIONS_MONTHS_AHEAD` months. It connects as the schema owner, so
`MYSMS_DB_MIGRATE_USER` must be set in production. A row whose month has no partition lands in the `…_default`
partition: the job then logs `partition … (DEFAULT) holds rows` at ERROR — alert on it, create the missing month by
hand after moving those rows out, because PostgreSQL cannot attach a month while the default partition holds rows
for it. Old months are dropped only when `MYSMS_PARTITIONS_RETENTION_MONTHS` is set (12 or more); take a backup first. The job waits at most 5 seconds for a table lock and retries at its next run, so it never stalls requests behind a long query. Lists over
these tables must name a date range (at most 366 days); the web app's Records screen asks for one.

### 5.7 Events (RabbitMQ)

Modules talk through events: a change writes its event into the module's own `outbox_event` in the same transaction;
the module's relay publishes it to the bus and marks it published once the broker confirms; each consuming module
records it in its `inbox_event` and handles it exactly once. The bus is MySMS's own interface (`core.events.EventBus`);
RabbitMQ is the default implementation (module `messaging-rabbitmq`), and another broker would be another module
behind the same interface, chosen with `MYSMS_EVENTS_BUS`.

What the services create on RabbitMQ (they declare it themselves at startup):

| Object | Kind | Purpose |
|---|---|---|
| `mysms.events` | topic exchange, durable | every event, routed by its type (`hr.staff.deleted`); its alternate exchange is `mysms.events.unrouted` |
| `mysms.<module>.events` | quorum queue | the events a module consumes; replicas of the module share it |
| `mysms.events.unrouted` | fanout exchange + quorum queue | events no queue was bound to when published (their consumer has never started yet), kept 3 days (`mysms.events.rabbitmq.unrouted-retention`) for `replay-unrouted` |
| `mysms.events.dead` | direct exchange | where a module's failing events go |
| `mysms.<module>.dead` | quorum queue | events that failed every delivery (the first one and 5 returns, `delivery-limit`), or that were unreadable, unauthentic or rejected: fix the cause, then `replay-dead` |

**One broker user per service, limited to its own module.** Each message carries the AMQP `user-id` of the
connection that published it, which RabbitMQ verifies; a consumer accepts an event only from the user of the module it
claims to come from (`mysms-<module>`), and the broker's topic permissions let each user publish only its own module's
event types and its audit records. A compromised service therefore cannot speak for another module or reach another
module's queue. For each module `<m>` (`hr` below), with the vhost `mysms`:

```bash
rabbitmqctl add_vhost mysms
rabbitmqctl add_user mysms-hr '<hr-rabbit-password>'
P='^mysms\.(events(\.dead|\.unrouted)?|hr\.(events|dead))$'   # configure, write and read: the shared exchanges, its own queues
rabbitmqctl set_permissions -p mysms mysms-hr "$P" "$P" "$P"
rabbitmqctl set_topic_permissions -p mysms mysms-hr mysms.events '^(hr\..+|audit\.recorded)$' '.*'
# the hr service: MYSMS_RABBITMQ_URI='amqps://mysms-hr:<hr-rabbit-password>@broker:5671/mysms'
```

The monolith publishes for every module over one connection: give its user (say `mysms`) `'.*' '.*' '.*'` on its vhost
and leave `MYSMS_RABBITMQ_PUBLISHER_USER` at its monolith default `self`. Operators replay with a user of their own
that carries the `impersonator` tag (replayed events keep their original publisher's `user-id`):

```bash
rabbitmqctl add_user mysms-ops '<ops-password>'
rabbitmqctl set_user_tags mysms-ops impersonator
rabbitmqctl set_permissions -p mysms mysms-ops '.*' '.*' '.*'
```

A broker for a first install:

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4.1-management-alpine
docker exec rabbitmq rabbitmqctl add_vhost mysms
docker exec rabbitmq rabbitmqctl add_user mysms '<rabbit-password>'
docker exec rabbitmq rabbitmqctl set_permissions -p mysms mysms '.*' '.*' '.*'
export MYSMS_RABBITMQ_URI='amqp://mysms:<rabbit-password>@localhost:5672/mysms'   # amqps:// with TLS in production
```

**Delivery.** At least once, and per module unordered across replicas; the inbox (keyed by tenant and event id) makes
handling exactly once. Until the broker confirms an event it stays pending in the outbox; an event published before
its consumer ever declared its queue waits in `mysms.events.unrouted` (3 days by default: start every consumer
service once, or replay within that time). A failed handling is returned after 1 s, then 2 s, 4 s … up to 60 s
(`retry-delay`, `max-retry-delay`), so a database restart of a few seconds does not use up the deliveries. An outbox
row the relay cannot turn into an event (not of its module, or unreadable) is set aside with the reason in
`<schema>.outbox_event.publish_error` (an ERROR log line) and the rows after it go on. A batch stops taking rows after
20 s and commits what the broker confirmed. Each relay pass gives a module at most 10 batches before the next module's
turn. `/actuator/health` (component `events`) is DOWN while the broker connection is lost or a relay has failed 3
passes in a row.

**Replaying** (run from any service jar, as the `impersonator` user):

```bash
export MYSMS_RABBITMQ_URI='amqps://mysms-ops:<ops-password>@broker:5671/mysms'
java -cp academics-0.1.0-SNAPSHOT-exec.jar -Dloader.main=com.mysms.messaging.rabbitmq.EventsAdmin \
  org.springframework.boot.loader.launch.PropertiesLauncher replay-dead academics      # [max], default 10000
java -cp academics-0.1.0-SNAPSHOT-exec.jar -Dloader.main=com.mysms.messaging.rabbitmq.EventsAdmin \
  org.springframework.boot.loader.launch.PropertiesLauncher replay-unrouted
```

`replay-dead` returns a module's parked events to its queue (each is handled once, through the inbox); the reason they
failed is in the module's `inbox_event.processing_error` and its log. `replay-unrouted` publishes the waiting events
again; one that is still unroutable waits again.

**Changing queue settings.** A queue keeps the arguments it was created with (`delivery-limit`, the dead-letter
exchange); a service started with other settings stops with `its queue … exists with other arguments`. Change them for
a running queue with a RabbitMQ policy, or drain the queue, delete it and let the service declare it anew. An exchange
declared by an earlier release without the alternate exchange is refused the same way (`inequivalent arg
'alternate-exchange'`): delete `mysms.events` once (the queues and their bindings are declared again at startup).

What flows today: every module's audit records (`audit.recorded`) into the platform's `platform.audit_log` (a module
may audit only its own entities), and the reference cleanup of database/logical-references.md (an owner's `*.deleted`
event clears or archives the rows that referenced it in other modules).

## 6. Production checklist

- [ ] Three database roles as in §2.1; the application connects as `mysms_app`, migrations as `mysms_migrate`.
- [ ] `MYSMS_IAM_SIGNING_KEY` from the secret store, 3072-bit RSA, never in an image or in git.
- [ ] `MYSMS_SECRETS_KEYS` from the secret store, backed up separately from the database backups.
- [ ] Bootstrap password removed after the first sign-in.
- [ ] TLS terminated at the proxy; the backend is not reachable directly from the internet.
- [ ] Web apps and API served from **one origin** (the backend sends no CORS headers by design).
- [ ] `max_connections` ≥ (21 × pool size) + headroom for the monolith.
- [ ] Only `/actuator/health/**` routed from the load balancer; the actuator exposes nothing beyond `health` and `info`.
- [ ] Daily `pg_dump -Fc` plus `pg_dumpall --roles-only`, with a tested restore.
- [ ] `database/verification/smoke.sql` run in CI against a scratch database, never against production.
- [ ] Log shipping for the backend's standard output (requests carry the W3C `traceparent`).
- [ ] An alert on the ERROR log line `partition … (DEFAULT) holds rows` (§5.6).
- [ ] RabbitMQ over TLS (`amqps://`), one broker user per service with the permissions of §5.7, and an alert on messages in any `mysms.*.dead` queue and on `mysms.events.unrouted`.
- [ ] `MYSMS_DB_RELAY_USER` set to `mysms_relay`: without it events stay in the outboxes (ERROR `has no relay-username`).
- [ ] An alert on the ERROR log line `set outbox row … aside` and on `/actuator/health` component `events` DOWN.

## 7. Web apps (school web app and platform console)

```bash
cd clients
pnpm install
pnpm verify                                   # prettier, generate the SDK, typecheck, lint, test, build — all packages
pnpm --filter @mysms/web build                # → apps/web/dist (fails if the initial JS exceeds 300 KB gzip)
pnpm --filter @mysms/platform-console build   # → apps/platform-console/dist
```

The `dist` folders are static files. Serve them from the same origin as the API, with a fallback to `index.html` for client-side routes. A minimal nginx site:

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

**Development against a real backend:**

```bash
MYSMS_API_PROXY=http://localhost:8080 pnpm --filter @mysms/web dev   # http://localhost:5173
```

Sign in with the bootstrap tenant code, username and password.

## 8. Developing the web apps against a local backend

There is no mock API: mocking is done only in tests. Run the monolith (§5.2) on `:8080`, then:

```bash
cd clients
pnpm --filter @mysms/web dev                 # http://localhost:5173, proxies /v1 to http://localhost:8080
pnpm --filter @mysms/platform-console dev    # http://localhost:5174
MYSMS_API_PROXY=http://other-host:8080 pnpm --filter @mysms/web dev   # another backend
```

Sign in with the bootstrap tenant code, username and password (§5.3).

## 9. Phone and tablet apps

```bash
cd clients
EXPO_PUBLIC_API_BASE_URL=https://sms.example.org pnpm --filter @mysms/mobile start            # student app
EXPO_PUBLIC_API_BASE_URL=https://sms.example.org pnpm --filter @mysms/mobile start:parent
EXPO_PUBLIC_API_BASE_URL=https://sms.example.org pnpm --filter @mysms/mobile start:teacher
EXPO_PUBLIC_API_BASE_URL=https://sms.example.org pnpm --filter @mysms/tablet start
```

Scan the QR code with Expo Go, or press `a` / `i` for an emulator. For store builds use EAS Build (`npx eas build --platform android|ios` in `apps/mobile` or `apps/tablet`, with `APP_VARIANT` set for each phone variant); each variant has its own bundle identifier from `app.config.ts`. To check that a bundle compiles without a device:

```bash
cd clients/apps/mobile && npx expo export --platform android --output-dir /tmp/mobile-bundle
cd clients/apps/tablet && npx expo export --platform ios --output-dir /tmp/tablet-bundle
```

## 10. Tests

### Backend

```bash
cd backend
mvn verify                       # uses Docker (Testcontainers, PostgreSQL 16) when no server is given
# or with your own PostgreSQL 16 (an administrative connection; each test class creates and drops its own database and roles):
MYSMS_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/postgres \
MYSMS_TEST_JDBC_USER=postgres MYSMS_TEST_JDBC_PASSWORD=<password> \
MYSMS_TEST_AMQP_URI=amqp://guest:guest@localhost:5672 mvn verify   # and your own RabbitMQ
mvn -pl iam -am verify           # one module and what it depends on
```

- Unit tests (`src/test`) are the only place test doubles are allowed.
- Component tests (`src/component-test`) boot the module over HTTP against real PostgreSQL as a non-owner role, so row-level security, grants and Flyway are exercised exactly as in production. Module tests run the events on the in-process bus (a real implementation of the bus interface); `messaging-rabbitmq` and the monolith's test run them on a real RabbitMQ.
- Gates: Checkstyle, SpotBugs, JaCoCo (80 % line coverage per module). A failing gate fails the build.

### Clients

```bash
cd clients
pnpm verify                      # everything
pnpm --filter @mysms/web test    # one package
```

Web components are rendered in jsdom against the fake backend and each passes an axe accessibility check; `@mysms/core` keeps 90 % line coverage.

### Contracts and schema

```bash
cd api/openapi && npx @redocly/cli lint mysms-api.yaml      # the joined OpenAPI contract
psql -v ON_ERROR_STOP=1 -d mysms_scratch -f database/verification/smoke.sql
```

## 11. Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `mysms.auth.issuer (MYSMS_AUTH_ISSUER) is required` or `… must be an absolute http(s) URL` at startup | Set the issuer to the public base URL of iam, e.g. `https://sms.example.org` (§4.1). |
| `mysms.auth.jwk-set-uri (or, for development, mysms.auth.public-key) is required` | Set `MYSMS_AUTH_JWK_SET_URI` (§4.1). |
| `mysms.iam.token.signing-key is required` or `… PKCS#8 …` | Pass the PEM *contents*, PKCS#8 (`-----BEGIN PRIVATE KEY-----`), 2048 bits or more. |
| `permission denied for schema …` at startup | `mysms_migrate` must own the database (or have `CREATE` on it). |
| `permission denied for table flyway_schema_history` | `MYSMS_DB_MIGRATE_USER` / `_PASSWORD` are missing, so Flyway ran as the application role, which cannot touch the owner's tables. Set them. |
| `partition … (DEFAULT) holds rows` or `partition … could not be created` in the log | A month had no partition when rows arrived (§5.6). Move those rows out of the `…_default` partition, create the month (`CREATE TABLE <schema>.<table>_YYYY_MM PARTITION OF <schema>.<table> FOR VALUES FROM ('YYYY-MM-01') TO (…)`), move the rows back through the parent table. |
| A list answers 400 `INVALID_FILTER` "this list needs a range on …" | The record type is partitioned: add a range of that field to `filter`, at most 366 days. |
| `mysms.events.rabbitmq.uri (MYSMS_RABBITMQ_URI) is required` at startup | Set the broker URI (§5.7), or `MYSMS_EVENTS_BUS=in-process` for a single process without a broker. |
| `module … has no relay-username …: its events stay in ….outbox_event` | Set `MYSMS_DB_RELAY_USER`/`_PASSWORD` to the `mysms_relay` role (§2.1). Pending events are published as soon as the relay runs. |
| `the relay role must be a role of its own` | `MYSMS_DB_RELAY_USER` is the application or migration user; create `mysms_relay` (§2.1). |
| Messages in `mysms.<module>.dead` | That module failed an event on every delivery (the reason is in the module's `inbox_event.processing_error` and its WARN log), or the event was unreadable, came from another broker user than its module's (ERROR `claiming module … from broker user …`) or was rejected. Fix the cause, then `replay-dead <module>` (§5.7). |
| Messages in `mysms.events.unrouted` | Events of a type no module had bound when they were published: start the consuming service (it binds its queue), then `replay-unrouted` (§5.7). Types nobody consumes also wait here until their retention. |
| `ACCESS_REFUSED - write access to topic '…'` | A service published an event type that is not its module's: a bug in that service; the event stays pending in its outbox. |
| `set outbox row … aside, it is not published` | The row in `<schema>.outbox_event` was not that module's own event or could not be read; its `publish_error` says why. Fix it and set `publish_error` back to NULL to publish it. |
| `its queue … exists with other arguments` or `inequivalent arg` at startup | See "Changing queue settings" in §5.7. |
| `FATAL: sorry, too many clients already` | The monolith opens 21 pools; lower `MYSMS_<MODULE>_DB_POOL_SIZE` or raise `max_connections`. |
| Every call answers 401 | `MYSMS_AUTH_ISSUER` differs between the service and the token's `iss`, or `MYSMS_AUTH_JWK_SET_URI` is unreachable from the service. |
| Sign-in answers 401 for the right password | After 5 failures password sign-in is locked for 1, 2, 4 … up to 60 minutes; wait for the lock to expire. A suspended tenant cannot sign in. |
| A call answers 501 `NOT_IMPLEMENTED` | The operation is in the contract but not served yet (see `backend/README.md`, Status). |
| The browser shows CORS errors | The web app and API are on different origins; serve both from one origin (§7). |
| A school administrator gets 403 changing a category, a fee type or another record of the whole school group | Tenant-level records change only through a permission held tenant-wide; grant the role at tenant scope or have a tenant-wide administrator make the change. |
| A record of another school is "not found" | Rows of institutions outside the caller's grants are hidden by design (404). |
| A setting is missing from "My settings" | It is an administrator setting; only those administering settings at its scopes see it. |
| Phone app cannot reach the API | `EXPO_PUBLIC_API_BASE_URL` must be reachable from the device (not `localhost`). |
| A service stops at start with `mysms.secrets.keys (MYSMS_SECRETS_KEYS) is not configured` | Set the field-encryption master key (§3a); every service except master needs it. |
| A retried POST answers 422 `IDEMPOTENCY_KEY_REUSED` | The client sent the same `Idempotency-Key` with a different request (body, path or user). Use a new key for a new request; the same key only for an exact retry, within 24 hours. |
| A retried POST answers 409 `REQUEST_IN_PROGRESS` | The first request with that key is still running; retry after a moment. A lock older than two minutes (a crashed request) is taken over by the retry. |
| Reads of a record type with secrets answer 500 and the log says `unreadable secret envelope` or `does not open for this tenant` | The key that wrote those values is no longer in `MYSMS_SECRETS_KEYS` (or the value was copied from elsewhere). Put the old key back in the list; it stays readable next to the new active key. |
| A user with an authenticator app gets `mfaRequired: true` and no tokens | Expected: the client sends the 6-digit code (or a recovery code) to `POST /v1/iam/mfa:complete` with the `mfaToken`, within five minutes. |
