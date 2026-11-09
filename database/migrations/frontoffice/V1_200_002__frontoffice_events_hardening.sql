-- Event delivery hardening (PRINCIPAL-REVIEW P2-02, database/README "Events").
-- The inbox is keyed by tenant and event id: an event id recorded for one tenant can never stand in for another
-- tenant's event. The outbox can set aside a row the relay cannot turn into an event (publish_error), so one bad row
-- does not hold back every later event of the module.

ALTER TABLE frontoffice.inbox_event DROP CONSTRAINT pk_frontoffice_inbox_event;
ALTER TABLE frontoffice.inbox_event ADD CONSTRAINT pk_frontoffice_inbox_event PRIMARY KEY (tenant_id, event_id);
COMMENT ON COLUMN frontoffice.inbox_event.event_id IS 'Global id of the consumed event; recorded per tenant.';
CREATE INDEX ix_frontoffice_inbox_event_processed ON frontoffice.inbox_event (processed_at) WHERE processed_at IS NOT NULL;

ALTER TABLE frontoffice.outbox_event ADD COLUMN publish_error text;
COMMENT ON COLUMN frontoffice.outbox_event.publish_error IS 'Why the relay could not turn the row into an event (set aside, not published); NULL otherwise.';
DROP INDEX frontoffice.ix_frontoffice_outbox_event_pending;
CREATE INDEX ix_frontoffice_outbox_event_pending ON frontoffice.outbox_event (id) WHERE published_at IS NULL AND publish_error IS NULL;
