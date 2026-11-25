-- Test-only: the tables a module needs for its privacy tasks (receipts, job schedule, outbox), shaped like people's,
-- one personal-data table and two tables kept under a retention policy for PrivacyTaskHandlerComponentTest.
CREATE TABLE ptasktest.person (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    name varchar(100) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz,
    CONSTRAINT pk_person PRIMARY KEY (id)
);
ALTER TABLE ptasktest.person ENABLE ROW LEVEL SECURITY;
ALTER TABLE ptasktest.person FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ptasktest.person USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE TABLE ptasktest.payslip (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    CONSTRAINT pk_payslip PRIMARY KEY (id)
);
ALTER TABLE ptasktest.payslip ENABLE ROW LEVEL SECURITY;
ALTER TABLE ptasktest.payslip FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ptasktest.payslip USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE TABLE ptasktest.contract (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    CONSTRAINT pk_contract PRIMARY KEY (id)
);
ALTER TABLE ptasktest.contract ENABLE ROW LEVEL SECURITY;
ALTER TABLE ptasktest.contract FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ptasktest.contract USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE TABLE ptasktest.privacy_task_receipt (
    task_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    kind varchar(10) NOT NULL,
    payload jsonb NOT NULL,
    status varchar(15) NOT NULL DEFAULT 'received',
    result jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    CONSTRAINT pk_privacy_task_receipt PRIMARY KEY (task_id),
    CONSTRAINT ck_privacy_task_receipt_kind CHECK (kind IN ('task', 'retention')),
    CONSTRAINT ck_privacy_task_receipt_status CHECK (status IN ('received', 'done', 'partial', 'failed',
        'not_applicable')),
    CONSTRAINT ck_privacy_task_receipt_completed CHECK ((status = 'received') = (completed_at IS NULL))
);
ALTER TABLE ptasktest.privacy_task_receipt ENABLE ROW LEVEL SECURITY;
ALTER TABLE ptasktest.privacy_task_receipt FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ptasktest.privacy_task_receipt
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE TABLE ptasktest.outbox_event (
    id bigint GENERATED ALWAYS AS IDENTITY,
    tenant_id uuid NOT NULL,
    event_type varchar(120) NOT NULL,
    aggregate_type varchar(80) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload text NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    publish_error text,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);
ALTER TABLE ptasktest.outbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE ptasktest.outbox_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ptasktest.outbox_event
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE TABLE ptasktest.job_schedule (
    job_code varchar(60) NOT NULL,
    tenant_id uuid NOT NULL,
    next_run_at timestamptz NOT NULL,
    lease_owner uuid,
    lease_until timestamptz,
    last_run_at timestamptz,
    failures integer NOT NULL DEFAULT 0,
    last_error varchar(500),
    CONSTRAINT pk_job_schedule PRIMARY KEY (job_code, tenant_id)
);
