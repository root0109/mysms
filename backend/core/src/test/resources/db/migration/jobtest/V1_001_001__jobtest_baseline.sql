-- Test-only: a module's job schedule, shaped like privacy.job_schedule (no row-level security, as there).
CREATE TABLE jobtest.job_schedule (
    job_code varchar(60) NOT NULL,
    tenant_id uuid NOT NULL,
    next_run_at timestamptz NOT NULL,
    lease_owner uuid,
    lease_until timestamptz,
    last_run_at timestamptz,
    failures integer NOT NULL DEFAULT 0,
    last_error varchar(500),
    CONSTRAINT pk_job_schedule PRIMARY KEY (job_code, tenant_id),
    CONSTRAINT ck_job_schedule_lease CHECK ((lease_owner IS NULL) = (lease_until IS NULL)),
    CONSTRAINT ck_job_schedule_failures CHECK (failures >= 0)
);
CREATE INDEX ix_job_schedule_due ON jobtest.job_schedule (next_run_at);
