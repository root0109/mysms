-- Test-only: the event delivery hardening every module's migrations apply (database/README "Events").
ALTER TABLE coretest.inbox_event DROP CONSTRAINT pk_coretest_inbox_event;
ALTER TABLE coretest.inbox_event ADD CONSTRAINT pk_coretest_inbox_event PRIMARY KEY (tenant_id, event_id);
ALTER TABLE coretest.outbox_event ADD COLUMN publish_error text;
