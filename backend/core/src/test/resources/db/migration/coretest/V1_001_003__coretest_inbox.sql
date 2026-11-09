-- Test-only: the inbox, shaped like every module's (database/README "Events").
CREATE TABLE coretest.inbox_event (
    event_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    event_type varchar(120) NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    processed_at timestamptz,
    processing_error text,
    CONSTRAINT pk_coretest_inbox_event PRIMARY KEY (event_id)
);
ALTER TABLE coretest.inbox_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE coretest.inbox_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON coretest.inbox_event USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
