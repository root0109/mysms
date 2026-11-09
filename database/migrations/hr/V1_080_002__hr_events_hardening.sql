-- Event delivery hardening (PRINCIPAL-REVIEW P2-02, database/README "Events").
-- The inbox is keyed by tenant and event id: an event id recorded for one tenant can never stand in for another
-- tenant's event. The outbox can set aside a row the relay cannot turn into an event (publish_error), so one bad row
-- does not hold back every later event of the module.

ALTER TABLE hr.inbox_event DROP CONSTRAINT pk_hr_inbox_event;
ALTER TABLE hr.inbox_event ADD CONSTRAINT pk_hr_inbox_event PRIMARY KEY (tenant_id, event_id);
COMMENT ON COLUMN hr.inbox_event.event_id IS 'Global id of the consumed event; recorded per tenant.';
CREATE INDEX ix_hr_inbox_event_processed ON hr.inbox_event (processed_at) WHERE processed_at IS NOT NULL;

ALTER TABLE hr.outbox_event ADD COLUMN publish_error text;
COMMENT ON COLUMN hr.outbox_event.publish_error IS 'Why the relay could not turn the row into an event (set aside, not published); NULL otherwise.';
DROP INDEX hr.ix_hr_outbox_event_pending;
CREATE INDEX ix_hr_outbox_event_pending ON hr.outbox_event (id) WHERE published_at IS NULL AND publish_error IS NULL;
