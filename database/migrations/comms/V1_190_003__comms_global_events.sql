-- Global tables written by platform staff (notification types and their properties): their changes publish events
-- without a tenant. Those rows are invisible to every tenant session, and only a transaction that binds no tenant may
-- write one (database/README "Global tables").

ALTER TABLE comms.outbox_event ALTER COLUMN tenant_id DROP NOT NULL;
COMMENT ON COLUMN comms.outbox_event.tenant_id IS 'Owning tenant (customer organisation). Row-level security key. NULL for global data changed by platform staff: such rows are invisible to every tenant session.';
DROP POLICY tenant_isolation ON comms.outbox_event;
-- a tenant's rows, to that tenant only (NULLIF: a pooled connection that bound a tenant before reads back '')
CREATE POLICY tenant_isolation ON comms.outbox_event
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
-- a row of global data, written only by a transaction that binds no tenant; no tenant session can forge one
CREATE POLICY global_insert ON comms.outbox_event FOR INSERT
    WITH CHECK (tenant_id IS NULL AND NULLIF(current_setting('app.tenant_id', true), '') IS NULL);
-- readable by a transaction that binds no tenant (the insert reads its row back); never by a tenant session
CREATE POLICY global_read ON comms.outbox_event FOR SELECT
    USING (tenant_id IS NULL AND NULLIF(current_setting('app.tenant_id', true), '') IS NULL);
