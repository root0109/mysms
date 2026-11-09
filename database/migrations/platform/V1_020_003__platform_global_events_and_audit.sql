-- Global tables written by platform staff (tenants, SaaS plans, reserved keywords, sales enquiries, system settings):
-- their changes publish events and audit records without a tenant. Those rows are invisible to every tenant session,
-- and only a transaction that binds no tenant may write one (database/README "Global tables").

ALTER TABLE platform.outbox_event ALTER COLUMN tenant_id DROP NOT NULL;
COMMENT ON COLUMN platform.outbox_event.tenant_id IS 'Owning tenant (customer organisation). Row-level security key. NULL for global data changed by platform staff: such rows are invisible to every tenant session.';
DROP POLICY tenant_isolation ON platform.outbox_event;
-- a tenant's rows, to that tenant only (NULLIF: a pooled connection that bound a tenant before reads back '')
CREATE POLICY tenant_isolation ON platform.outbox_event
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
-- a row of global data, written only by a transaction that binds no tenant; no tenant session can forge one
CREATE POLICY global_insert ON platform.outbox_event FOR INSERT
    WITH CHECK (tenant_id IS NULL AND NULLIF(current_setting('app.tenant_id', true), '') IS NULL);
-- readable by a transaction that binds no tenant (the insert reads its row back); never by a tenant session
CREATE POLICY global_read ON platform.outbox_event FOR SELECT
    USING (tenant_id IS NULL AND NULLIF(current_setting('app.tenant_id', true), '') IS NULL);

ALTER TABLE platform.audit_log ALTER COLUMN tenant_id DROP NOT NULL;
COMMENT ON COLUMN platform.audit_log.tenant_id IS 'Owning tenant (customer organisation). Row-level security key. NULL for global data changed by platform staff: such rows are invisible to every tenant session.';
DROP POLICY tenant_isolation ON platform.audit_log;
-- a tenant's rows, to that tenant only (NULLIF: a pooled connection that bound a tenant before reads back '')
CREATE POLICY tenant_isolation ON platform.audit_log
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
-- a row of global data, written only by a transaction that binds no tenant; no tenant session can forge one
CREATE POLICY global_insert ON platform.audit_log FOR INSERT
    WITH CHECK (tenant_id IS NULL AND NULLIF(current_setting('app.tenant_id', true), '') IS NULL);
-- the same rows, readable by a transaction that binds no tenant (platform staff): the idempotent insert
-- (ON CONFLICT DO NOTHING) needs it, and a tenant session still sees none of them
CREATE POLICY global_read ON platform.audit_log FOR SELECT
    USING (tenant_id IS NULL AND NULLIF(current_setting('app.tenant_id', true), '') IS NULL);
