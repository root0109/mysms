# Background job pattern

Scheduled per-tenant work in core (`com.mysms.core.jobs`). Binding rules: [`CLAUDE.md`](../../CLAUDE.md) and [`JAVA-CODING-RULES.md`](../../JAVA-CODING-RULES.md); configuration: [DEPLOYMENT.md §4.1](../../DEPLOYMENT.md#41-every-service-and-the-monolith); open work (user-triggered long operations): [ROADMAP.md](../../ROADMAP.md) ARC-4.

Diagram: [docs/diagrams/architecture/background-jobs-sequence.drawio](../diagrams/architecture/background-jobs-sequence.drawio) — open with draw.io / diagrams.net.

## 1. Constraints

- Every tenant table has `FORCE` row-level security; the application role sees one tenant per transaction, so a run with no caller must first know which tenants to visit.
- The only BYPASSRLS pool is the outbox relay's, never used for business queries.
- Modules run as one process or as separate services, each with several replicas: a run must happen once per tenant and interval across replicas, survive a crash mid-run and never mix tenants.
- Free open-source libraries only, otherwise built in-house: no job-scheduler library (JobRunr was not adopted).

## 2. Design

| Part | What it is |
|---|---|
| `<module>.job_schedule` | One row per (`job_code`, `tenant_id`): `next_run_at`, `lease_owner`, `lease_until`, `last_run_at`, `failures`, `last_error`. Job codes, tenant ids and times only, so **no RLS** (like `iam.tenant_directory`); a BYPASSRLS role would widen what a bug could read. In the module's baseline migration. |
| `JobSchedule` | One bean per module database (qualified). `enrol`, `claim`, `complete`, `fail`. |
| `ScheduledJob` | A module bean: `module()`, `code()` (`<module>.<name>`), `interval()`, `run()`. |
| `JobRuntime` | `SmartLifecycle`, one `scheduled-jobs` thread per process; exists only with `mysms.jobs.enabled=true`. |

**Enrolment by work.** A tenant joins a job's schedule the first time it has work for it, inside the transaction that commits that work (`JobSchedule.enrol`, `ON CONFLICT DO NOTHING`, due at once). No tenant list, no missed tenant-creation event, no backfill; a tenant without work never runs the job.

**Claim.** Every `mysms.jobs.poll-interval`, per module, up to `mysms.jobs.batch-size` times: lease one due row of this process's job codes in a global transaction — `UPDATE … SET lease_owner, lease_until WHERE (job_code, tenant_id) IN (SELECT … WHERE next_run_at <= now AND (lease_until IS NULL OR lease_until < now) ORDER BY next_run_at LIMIT 1 FOR UPDATE SKIP LOCKED) RETURNING …`. One runner per row across replicas; an expired lease (`mysms.jobs.lease`) is claimed again after a crash.

**Run.** The runtime binds the tenant's call context (tenant, no actor, new trace) and calls `run()`. The job works through its module's services in bounded batches, each its own `doInWriteTransaction` (tenant bound, RLS applies), and must be safe to repeat.

**Finish.** Success: `next_run_at = now + interval`, `failures = 0`, lease cleared — only while this runner still holds the lease (otherwise a WARN that the run outlived it). Failure: `next_run_at = now + min(poll-interval × 2^failures, interval)`, `failures + 1`, `last_error`. One tenant's failure never stops another's run or the runtime.

## 3. Jobs

| Job | Module | Enrolled when | Work per transaction | Interval |
|---|---|---|---|---|
| `privacy.majority` (`MajorityJob`) | privacy | privacy keeps a minor of the tenant | one person whose majority date has come: status adult, `majority_lapsed` evidence for guardians' and nominees' live decisions, consent answers recomputed, `privacy.subject.majority_reached` published | `mysms.privacy.majority-job.interval`, default 1h |
| `privacy.consent_expiry` (`ConsentExpiryJob`) | privacy | a decision with a last valid day is recorded | one decision past its last day: `expired` evidence, `privacy.consent_record.expired`, recompute | `mysms.privacy.consent-expiry-job.interval`, default 1h |
| `privacy.task_sweep` (`TaskSweepJob`) | privacy | an erasure or export request's fulfilment starts | one rights-request task dispatched longer ago than `mysms.privacy.erasure.redispatch-after` (default PT24H) and unanswered: dispatched again, `privacy.rights_request.task_assigned` | `mysms.privacy.erasure.sweep-interval`, default 15m |
| `privacy.retention` (`RetentionJob`) | privacy | privacy learns of a person of the tenant | every retention policy, in one transaction: a `retention_run` per policy and UTC day, `privacy.retention.due` | `mysms.privacy.retention-job.interval`, default 24h |

An hourly interval bounds how long a guardian's decision still counts after the majority date.

## 4. Adding a job

1. Add `job_schedule` to the module's baseline (same columns and constraints as `privacy.job_schedule`) and declare a qualified `JobSchedule` bean for the module database.
2. Implement `ScheduledJob`; name it `<module>.<name>`; give its interval a `mysms.<module>.…interval` property over an environment variable.
3. Call `enrol` from the service whose committed work creates the need, inside that transaction.
4. Keep each unit of work its own transaction and idempotent; test with a component test like `ScheduledPrivacyJobsComponentTest`.
