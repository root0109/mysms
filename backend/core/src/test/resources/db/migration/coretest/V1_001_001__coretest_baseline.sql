-- Test-only schema for core's component tests: one tenant-scoped table and an outbox, shaped like a real module.
CREATE SCHEMA IF NOT EXISTS coretest;

CREATE TABLE coretest.item (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    name varchar(60) NOT NULL,
    row_version integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_item PRIMARY KEY (id)
);
ALTER TABLE coretest.item ENABLE ROW LEVEL SECURITY;
ALTER TABLE coretest.item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON coretest.item USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE TABLE coretest.outbox_event (
    id bigint GENERATED ALWAYS AS IDENTITY,
    tenant_id uuid,
    event_type varchar(120) NOT NULL,
    aggregate_type varchar(80) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload text NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    CONSTRAINT pk_coretest_outbox_event PRIMARY KEY (id)
);
ALTER TABLE coretest.outbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE coretest.outbox_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON coretest.outbox_event
    USING (tenant_id IS NULL OR tenant_id = current_setting('app.tenant_id', true)::uuid);
