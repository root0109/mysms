# Build and run MySMS (developer guide)

This page gets a developer from a fresh checkout to a green build and a running system on one machine. Production
setup (roles with real passwords, secrets, TLS, microservices, backups) is in [`DEPLOYMENT.md`](DEPLOYMENT.md).

```
MySMS/
├── api/        the API guide: conventions, operation list and status, state machines (the contracts are
│               owned source inside each backend module: com.mysms.<module>.api and its protos)
├── database/   Flyway migrations per schema, data dictionary, ERDs, smoke test
├── backend/    the multi-module Maven project (22 domain modules + core, auth-client, messaging,
│               privacy-client, app, conformance)
├── clients/    pnpm/turbo workspace: web app, platform console, phone and tablet apps, shared packages
└── ui/         screen and navigation specs
```

---

## 1. Prerequisites

| Tool | Version | Needed for | Check |
|---|---|---|---|
| JDK | 17 (newer also works; the build targets 17) | backend build and run | `java -version` |
| Maven | 3.9+ | backend build | `mvn -v` |
| PostgreSQL | 16 | backend tests and running | `psql --version` |
| RabbitMQ | 4.1 (3.13+ works) | default event bus; RabbitMQ component tests | `rabbitmq-diagnostics ping` |
| Google Cloud CLI | current | optional Pub/Sub emulator for local development and component tests | `gcloud version` |
| Docker | any current | *instead of* installing PostgreSQL, RabbitMQ and the Pub/Sub emulator yourself (Testcontainers or §2) | `docker info` |
| Node.js | 22.12+ | clients | `node -v` |
| pnpm | 10 (`corepack enable`) | clients | `pnpm -v` |
| OpenSSL | any current | the local signing and encryption keys (§4) | `openssl version` |

**Memory:** the full backend build peaks at about 4.5 GB (the contract packages add ~1,700 files;
`backend/.mvn/jvm.config` sets `-Xmx4g`). **Never run the backend build and the client build at the same time on a
machine with 8 GB or less** — the operating system kills Maven (exit 137) half way through.

Network: the first build downloads Maven and npm dependencies; later builds can run offline (`mvn -o`).

## 2. Local PostgreSQL and event brokers

The backend tests need a PostgreSQL 16 superuser connection (they create throwaway databases and the MySMS roles),
and the messaging module's component tests use RabbitMQ and a Google Pub/Sub emulator. Pick Docker or provide the
services yourself:

**A. Let the tests start them (needs Docker).** Do nothing: without the variables below, the tests start
PostgreSQL 16, RabbitMQ and the Pub/Sub emulator through Testcontainers, one container per test JVM.

**B. Use servers you run yourself (faster, works without Docker in CI):**

```bash
docker run -d --name mysms-pg  -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:16
docker run -d --name mysms-rmq -p 5672:5672 -p 15672:15672 rabbitmq:4.1-management
gcloud beta emulators pubsub start --project=mysms-test --host-port=localhost:8085

export MYSMS_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/postgres
export MYSMS_TEST_JDBC_USER=postgres
export MYSMS_TEST_JDBC_PASSWORD=postgres
export MYSMS_TEST_AMQP_URI=amqp://guest:guest@localhost:5672
export MYSMS_TEST_PUBSUB_EMULATOR_HOST=localhost:8085
```

(Natively installed PostgreSQL 16, RabbitMQ or a Pub/Sub emulator works the same; the JDBC user must be a superuser.)

## 3. Build everything

### 3.1 Backend — one multi-module Maven build

```bash
cd backend
mvn install            # or `mvn verify`
```

For every module (two at a time, `-T 2` in `.mvn/maven.config`): protos, compile, unit tests, component tests (real
PostgreSQL with row-level security, RabbitMQ, Pub/Sub emulator; each module's OpenAPI test writes
`target/openapi/api-docs.yml`), Checkstyle, SpotBugs, JaCoCo. Any failure fails the build; success ends with
`BUILD SUCCESS`.

Useful variants:

```bash
mvn -o install                                  # offline, after the first build; unchanged modules come from the build cache
mvn install -Dmaven.build.cache.enabled=false   # the gate build: every module built and tested
mvn -pl identity -am install                    # one module and what it depends on
mvn -pl identity test -Dtest=SignInServiceTest  # one test class
mvn -pl app -am package -DskipTests             # just the runnable jars, when the tests already ran
```

Outputs: `app/target/app-0.1.0-SNAPSHOT-exec.jar` (all modules in one process) and
`<module>/target/<module>-0.1.0-SNAPSHOT-exec.jar` (one service per module).

**In an IDE (Eclipse/STS with m2e, IntelliJ):**

1. If the generated gRPC sources are missing, run `mvn install -DskipTests -Djacoco.skip=true` once on the
   command line (from `backend/`). This compiles every module's protos and installs every module's jar in `~/.m2`, so the IDE
   can resolve modules such as `messaging` even before they are imported. Run the full `mvn verify` afterwards.
2. Import `backend/` as *Existing Maven Projects* and select **all** modules (the parent, core,
   auth-client, messaging, privacy-client, the 22 domain modules, app and conformance).
3. Right-click any project → *Maven → Update Project…* (Alt+F5), select all, tick *Force Update of Snapshots/Releases*.
   m2e takes the Java level (17) from the parent pom; make sure a JDK 17 or newer is installed in
   *Window → Preferences → Java → Installed JREs* and mapped to the JavaSE-17 execution environment.

The quality gates (Checkstyle, SpotBugs, JaCoCo) are switched off for the IDE's automatic build (an
`org.eclipse.m2e:lifecycle-mapping` entry in `backend/pom.xml`, plus `<?m2e ignore?>` for newer m2e). They run only
on the command line (`mvn verify` / `mvn install`). If an old "Failed to execute mojo … spotbugs" marker stays, delete
it from the *Problems* view and update the projects once more.

Rule check: the last module, `conformance`, enforces the mechanical rules of
[`JAVA-CODING-RULES.md`](JAVA-CODING-RULES.md) on every `mvn verify`; any violation fails the build. Run it alone with
`mvn -pl conformance test`.

### 3.2 Clients — one pnpm/turbo build

Build the backend first, with its tests: the SDK generator reads the OpenAPI documents the component tests write and
refuses to run until all 22 `backend/<module>/target/openapi/api-docs.yml` exist. The OpenAPI Generator needs a JDK on
`PATH`; without network, set `OPENAPI_GENERATOR_JAR` to a local copy of its jar.

```bash
cd clients
pnpm install
pnpm verify            # prettier check, TypeScript SDK generation from the backend's api-docs.yml, typecheck, lint, unit tests, production builds — every package
```

It ends with `Tasks: N successful, N total`. Only the tests use mocks; the apps always talk to a real backend.
`pnpm format` fixes formatting failures reported by `pnpm verify`.

Single packages: `pnpm --filter @mysms/web test`, `pnpm --filter @mysms/web build`,
`pnpm --filter @mysms/platform-console build`.

### 3.3 Reports written by the build

After serving or adding operations, refresh the generated status report:
`mvn -pl conformance test -Dmysms.write-reports=true` (writes [`api/operations-status.md`](api/operations-status.md)).

### 3.4 Build time and how to shorten it

The slowest modules are lms, iam, finance and library. What the time goes on:

- **Proto copies.** Each module compiles its callees' protos again (`people.proto` in 9 callers, `org.proto` in 8,
  `iam.proto` in 6); `lms.proto` alone is about 16.5k lines.
- **Component tests** start a full Spring context and, by default, their own Testcontainers PostgreSQL in every test
  JVM. JaCoCo, Checkstyle and SpotBugs run on every build.

The build settings, and why:

- `backend/.mvn/jvm.config`: `-Xmx4g` (the build peaks at about 4.5 GB, §1).
- `backend/.mvn/maven.config`: `-T 2`, two modules at a time, which is what the test-JVM memory is sized for
  (JAVA-CODING-RULES). `-T 1C` would run one module per CPU core — on a 10–12-core laptop that many javac runs in one
  heap, plus a 1 GB test JVM and a PostgreSQL container per module — and ends in out-of-memory failures. On a 32 GB
  machine try `-T 4` and keep whichever is faster.
- `backend/pom.xml`: `-Xlint:all,-processing,-serial,-deprecation`. Spring 7 deprecates
  `org.springframework.lang.Nullable`, used across the contract DTOs (`api/model`); with deprecation warnings on, javac keeps one
  diagnostic per use in memory and the flood hides real errors behind the 100-message cap. Turn deprecation back on
  after moving to `org.jspecify.annotations.Nullable`.

What else shortens it, by effort:

| Effort | Action | Expected gain |
|---|---|---|
| none | Use **external PostgreSQL and RabbitMQ** (§2 option B, the `MYSMS_TEST_*` variables) instead of a Testcontainers database per test JVM | Container start-up removed from every module |
| none | **Inner loop:** `mvn install -o -DskipTests -Dcheckstyle.skip -Dspotbugs.skip -Djacoco.skip -Dmaven.build.cache.skipSave=true` | Compile only, a few minutes. Does not write the OpenAPI documents, so the client SDK keeps the last ones. |
| none | **Build only what changed:** `mvn install -o -pl learning -am` (a module and what it needs), or `-pl core -amd` (core and everything that uses it) | Skips unrelated modules |
| none | **Keep the build cache on** (it is: `.mvn/extensions.xml`). A second run with no changes restores modules instead of rebuilding them. Use `-Dmaven.build.cache.enabled=false` only for the gate build. | Unchanged modules are near-instant |
| low | **Maven daemon (`mvnd`)** instead of `mvn`, to keep a warm JVM between builds | Warm JVM between builds |
| low | **Testcontainers reuse:** `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` plus `.withReuse(true)` on the containers | Containers survive between test runs |
| low | **One Spring context per module in tests.** Make every component test of a module share the same configuration so Spring's context cache reuses it, and avoid `@DirtiesContext` and per-test `@MockitoBean`. | Usually the biggest single test-time win |
| low | **SpotBugs and JaCoCo only in CI or the gate build**, in a `gate` profile, instead of every local build | Minutes per run on lms, iam and finance |
| medium | **One proto artifact per callee** instead of copies (owner decision [ROADMAP D2](ROADMAP.md#21-architecture-and-platform)) | Removes repeated protoc and javac |
| medium | **Merge modules** ([`ARCHITECTURE.md`](ARCHITECTURE.md) merge proposal) | Fewer Spring contexts, proto copies and test JVMs |
| medium | **CI with a remote build cache** ([ROADMAP BLD-1, BLD-2](ROADMAP.md#37-build-ci-and-testing)) | Most builds rebuild only what changed |

### 3.5 Test suites and gates

Backend (run by `mvn verify` / `mvn install`; the database and brokers come from §2; each test class creates and drops
its own database and roles):

- Unit tests (`src/test`) are the only place test doubles are allowed.
- Component tests (`src/component-test`) boot the module over HTTP against real PostgreSQL as a non-owner role, so row-level security, grants and Flyway are exercised exactly as in production. Module tests run the events on the in-process bus; `messaging` tests RabbitMQ and Google Pub/Sub against real brokers/emulator, and the monolith test runs against RabbitMQ.
- Gates: Checkstyle, SpotBugs, JaCoCo (80 % line coverage per module). A failing gate fails the build.

Clients (run by `pnpm verify`): Web components are rendered in jsdom against the fake backend and each passes an axe accessibility check; `@mysms/core` keeps 90 % line coverage.

Contracts and schema:

```bash
npx @redocly/cli lint backend/academics/target/openapi/attendance/api-docs.yml   # any area's contract, after mvn verify
psql -v ON_ERROR_STOP=1 -d mysms_scratch -f database/verification/smoke.sql
```

## 4. Run it locally

### 4.1 One-time setup

```bash
# database and roles (as the PostgreSQL superuser; local passwords only)
psql -h localhost -U postgres <<'SQL'
CREATE ROLE mysms_migrate LOGIN PASSWORD 'migrate';
CREATE ROLE mysms_app LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB PASSWORD 'app';
CREATE ROLE mysms_relay LOGIN BYPASSRLS PASSWORD 'relay';
CREATE ROLE mysms_platform_admin LOGIN BYPASSRLS PASSWORD 'platform';
CREATE DATABASE mysms OWNER mysms_migrate;
REVOKE ALL ON DATABASE mysms FROM PUBLIC;
GRANT CONNECT ON DATABASE mysms TO mysms_app, mysms_relay, mysms_platform_admin;
SQL

# local keys (keep them out of the repository)
mkdir -p ~/.mysms
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out ~/.mysms/iam-signing.pem
echo "k1:$(openssl rand -base64 32)" > ~/.mysms/secrets-keys
```

### 4.2 Start the backend (all modules in one process)

```bash
export MYSMS_DB_URL=jdbc:postgresql://localhost:5432/mysms
export MYSMS_DB_USER=mysms_app MYSMS_DB_PASSWORD=app
export MYSMS_DB_MIGRATE_USER=mysms_migrate MYSMS_DB_MIGRATE_PASSWORD=migrate
export MYSMS_DB_RELAY_USER=mysms_relay MYSMS_DB_RELAY_PASSWORD=relay
export MYSMS_AUTH_ISSUER=http://localhost:8080
export MYSMS_AUTH_JWK_SET_URI=http://localhost:8080/oauth2/jwks
export MYSMS_IAM_SIGNING_KEY="$(cat ~/.mysms/iam-signing.pem)"
export MYSMS_SECRETS_KEYS="$(cat ~/.mysms/secrets-keys)"
export MYSMS_PEOPLE_URL=http://localhost:8080
export MYSMS_TIMETABLE_URL=http://localhost:8080 MYSMS_SIS_URL=http://localhost:8080 MYSMS_ORG_URL=http://localhost:8080
export MYSMS_EVENTS_BUS=in-process            # or MYSMS_RABBITMQ_URI=amqp://guest:guest@localhost:5672
# first start only: create a tenant and its administrator
export MYSMS_IAM_BOOTSTRAP_TENANT_CODE=demo
export MYSMS_IAM_BOOTSTRAP_ADMIN_USERNAME=admin
export MYSMS_IAM_BOOTSTRAP_ADMIN_PASSWORD='change-me-please'

java -jar backend/app/target/app-0.1.0-SNAPSHOT-exec.jar
```

Ready when `curl -s localhost:8080/actuator/health/readiness` answers `{"status":"UP"}`. Every module migrates its
own schema with Flyway on start. Drop the three `BOOTSTRAP` variables after the first sign-in.
Until the first release each module's schema is one baseline script that is edited in place, so after pulling a
schema change Flyway refuses the existing database (checksum mismatch): drop and recreate `mysms` (the
`CREATE DATABASE` and `GRANT` lines of §4.1) and start again with the `BOOTSTRAP` variables.

### 4.3 Start the web apps

```bash
cd clients
pnpm --filter @mysms/web dev                  # http://localhost:5173  (school app; proxies /v1 to :8080)
pnpm --filter @mysms/platform-console dev     # http://localhost:5174  (platform staff)
MYSMS_API_PROXY=http://other-host:8080 pnpm --filter @mysms/web dev    # another backend
```

Sign in with school code `demo`, user `admin` and the bootstrap password. In the app, press `/` or Ctrl/⌘-K to find
any screen. Phone and tablet apps: see [`DEPLOYMENT.md` §9](DEPLOYMENT.md).

## 5. Troubleshooting

Build and local-development problems. Problems of a running installation (startup refusals, keys, events, 401/403/501 answers): [`DEPLOYMENT.md` §11](DEPLOYMENT.md#11-troubleshooting).

| Symptom | Cause and fix |
|---|---|
| Maven ends with `EXIT 137` / `Killed`, no test failure | Out of memory: stop other builds (especially `pnpm verify`) and rerun. |
| Component tests fail to connect to `localhost:5432` / `5672` | Server not running, or the `MYSMS_TEST_*` variables point to a wrong port; unset them to use Testcontainers. |
| `permission denied to create role` in tests | `MYSMS_TEST_JDBC_USER` is not a superuser. |
| RabbitMQ suites fail with `PRECONDITION_FAILED - inequivalent arg` | Queues left by an old broker run; the tests remove their own queues, otherwise `docker rm -f mysms-rmq` and restart it. |
| `… compiled by a more recent version of the Java Runtime (class file version 65.0)` | Classes left by an earlier Java 21 build: run `mvn clean install` once (in Eclipse/STS also *Project → Clean…*). |
| Checkstyle fails to start on JDK 17 after a version bump | Checkstyle 13+ needs Java 21; keep `checkstyle.version` at 12.3.x while the project targets Java 17. |
| `pnpm verify` fails at `prettier --check` | Run `pnpm format`, then verify again. |
| m2e: `Could not find artifact com.mysms:messaging-rabbitmq` (or `messaging`) | Stale workspace: remove any `messaging-rabbitmq` project (keep its contents), re-import `backend/` with all modules, *Update Project…*. All broker adapters are in `backend/messaging`. |
