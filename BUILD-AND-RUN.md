# Build and run MySMS (developer guide)

This page gets a developer from a fresh checkout to a green build and a running system on one machine. Production
setup (roles with real passwords, secrets, TLS, microservices, backups) is in [`DEPLOYMENT.md`](DEPLOYMENT.md).

```
MySMS/
├── api/        contracts (OpenAPI per module): the source of the generated server interfaces and SDKs
├── database/   Flyway migrations per schema, data dictionary, ERDs, smoke test
├── backend/    the multi-module Maven project (21 modules + core, auth-client, messaging, contracts, sdk, app)
├── clients/    pnpm/turbo workspace: web app, platform console, phone and tablet apps, shared packages
├── tools/      generators and conformance checks (Python), not part of the runtime
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
| Python | 3.10+ | only when you re-run the generators or the conformance check in `tools/` | `python3 -V` |
| OpenSSL | any current | the local signing and encryption keys (§4) | `openssl version` |

**Memory:** the full backend build peaks at about 4.5 GB (the generated contracts module compiles ~11,000 files;
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

One command runs, for every module in order: contract and SDK generation from `api/`, compile, unit tests,
component tests (real PostgreSQL with row-level security, real RabbitMQ and the Pub/Sub emulator), Checkstyle,
SpotBugs and JaCoCo coverage
gates. It ends with `BUILD SUCCESS`; any test failure, style or bug-pattern violation fails the build.

Useful variants:

```bash
mvn -o install                                  # offline, after the first build
mvn -pl iam -am install                         # one module and what it depends on
mvn -pl iam test -Dtest=SignInServiceTest       # one test class
mvn -pl app -am package -DskipTests             # just the runnable jars, when the tests already ran
```

Outputs: `app/target/app-0.1.0-SNAPSHOT-exec.jar` (all modules in one process) and
`<module>/target/<module>-0.1.0-SNAPSHOT-exec.jar` (one service per module).

**In an IDE (Eclipse/STS with m2e, IntelliJ):**

1. If generated contracts or SDK sources are missing, run `mvn install -DskipTests -Djacoco.skip=true` once on the
   command line (from `backend/`). This generates those sources and installs every module's jar in `~/.m2`, so the IDE
   can resolve modules such as `messaging` even before they are imported. Run the full `mvn verify` afterwards.
2. Import `backend/` as *Existing Maven Projects* and select **all** modules (the parent, core, contracts, sdk,
   auth-client, messaging, the 21 modules and app).
3. Right-click any project → *Maven → Update Project…* (Alt+F5), select all, tick *Force Update of Snapshots/Releases*.
   m2e takes the Java level (17) from the parent pom; make sure a JDK 17 or newer is installed in
   *Window → Preferences → Java → Installed JREs* and mapped to the JavaSE-17 execution environment.

The quality gates (Checkstyle, SpotBugs, JaCoCo) are switched off for the IDE's automatic build (an
`org.eclipse.m2e:lifecycle-mapping` entry in `backend/pom.xml`, plus `<?m2e ignore?>` for newer m2e). They run only
on the command line (`mvn verify` / `mvn install`). If an old "Failed to execute mojo … spotbugs" marker stays, delete
it from the *Problems* view and update the projects once more.

If m2e reports `Could not find artifact com.mysms:messaging-rabbitmq` or `com.mysms:messaging`, its workspace still
has an old/incomplete Maven project set. In Project Explorer, remove the obsolete `messaging-rabbitmq` project from
the workspace **without deleting its contents**, then use *File → Import → Maven → Existing Maven Projects* and
select `backend/`. Import the parent, `messaging`, and every module; then run *Maven → Update Project…* with all projects
selected. All broker adapters now live in `backend/messaging`; `messaging-rabbitmq` is no longer a Maven artifact.

Optional rule check (no build needed): `python3 tools/backend/conformance.py` — enforces
[`JAVA-CODING-RULES.md`](JAVA-CODING-RULES.md) and must report 0 violations.

### 3.2 Clients — one pnpm/turbo build

```bash
cd clients
pnpm install
pnpm verify            # prettier check, SDK generation, typecheck, lint, unit tests, production builds — every package
```

It ends with `Tasks: N successful, N total`. Only the tests use mocks; the apps always talk to a real backend.
`pnpm format` fixes formatting failures reported by `pnpm verify`.

Single packages: `pnpm --filter @mysms/web test`, `pnpm --filter @mysms/web build`,
`pnpm --filter @mysms/platform-console build`.

### 3.3 Can it be one Maven build?

Yes, if wanted later: a root `pom.xml` aggregating `backend/` and a `clients` module that runs
`pnpm install` / `pnpm verify` through `frontend-maven-plugin` (or `exec-maven-plugin`), with profiles to skip either
side. Today the two builds are run separately, as above.

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
export MYSMS_EVENTS_BUS=in-process            # or MYSMS_RABBITMQ_URI=amqp://guest:guest@localhost:5672
# first start only: create a tenant and its administrator
export MYSMS_IAM_BOOTSTRAP_TENANT_CODE=demo
export MYSMS_IAM_BOOTSTRAP_ADMIN_USERNAME=admin
export MYSMS_IAM_BOOTSTRAP_ADMIN_PASSWORD='change-me-please'

java -jar backend/app/target/app-0.1.0-SNAPSHOT-exec.jar
```

Ready when `curl -s localhost:8080/actuator/health/readiness` answers `{"status":"UP"}`. Every module migrates its
own schema with Flyway on start. Drop the three `BOOTSTRAP` variables after the first sign-in.

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

| Symptom | Cause and fix |
|---|---|
| Maven ends with `EXIT 137` / `Killed`, no test failure | Out of memory: stop other builds (especially `pnpm verify`) and rerun. |
| Component tests fail to connect to `localhost:5432` / `5672` | Server not running, or the `MYSMS_TEST_*` variables point to a wrong port; unset them to use Testcontainers. |
| `permission denied to create role` in tests | `MYSMS_TEST_JDBC_USER` is not a superuser. |
| RabbitMQ suites fail with `PRECONDITION_FAILED - inequivalent arg` | Queues left by an old broker run; the tests remove their own queues, otherwise `docker rm -f mysms-rmq` and restart it. |
| iam refuses to start | `MYSMS_IAM_SIGNING_KEY` missing or not PKCS#8 PEM. |
| A service refuses to start mentioning the relay user | The relay user must differ from the application and migration users. |
| `… compiled by a more recent version of the Java Runtime (class file version 65.0)` | Classes left by an earlier Java 21 build: run `mvn clean install` once (in Eclipse/STS also *Project → Clean…*). |
| Checkstyle fails to start on JDK 17 after a version bump | Checkstyle 13+ needs Java 21; keep `checkstyle.version` at 12.3.x while the project targets Java 17. |
| `pnpm verify` fails at `prettier --check` | Run `pnpm format`, then verify again. |
| An API call answers `501 NOT_IMPLEMENTED` | The operation is in the contract but not built yet — see `api/operations-status.md`. |
