-- iam.tenant_directory follows platform.tenant from its events (created, updated, deleted). Events can arrive out of
-- order and more than once, so the directory keeps the tenant row version it last applied and ignores older ones.
ALTER TABLE iam.tenant_directory ADD COLUMN source_version integer NOT NULL DEFAULT 0;
COMMENT ON COLUMN iam.tenant_directory.source_version IS 'platform.tenant row version this entry reflects; an event with an older version is ignored.';
