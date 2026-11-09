-- Test-only: two range-partitioned tables shaped like the MySMS ones (a date key and a timestamptz key), each with
-- one month and a DEFAULT partition, under forced row-level security.
CREATE TABLE parttest.daily_row (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    happened_on date NOT NULL,
    CONSTRAINT pk_parttest_daily_row PRIMARY KEY (id, happened_on)
) PARTITION BY RANGE (happened_on);
CREATE TABLE parttest.daily_row_default PARTITION OF parttest.daily_row DEFAULT;
CREATE TABLE parttest.daily_row_2026_10 PARTITION OF parttest.daily_row FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
ALTER TABLE parttest.daily_row ENABLE ROW LEVEL SECURITY;
ALTER TABLE parttest.daily_row FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON parttest.daily_row USING (tenant_id = current_setting('app.tenant_id', true)::uuid);

CREATE TABLE parttest.event_log (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_parttest_event_log PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE TABLE parttest.event_log_default PARTITION OF parttest.event_log DEFAULT;
CREATE TABLE parttest.event_log_2026_09 PARTITION OF parttest.event_log
    FOR VALUES FROM ('2026-09-01 00:00:00+00') TO ('2026-10-01 00:00:00+00');
ALTER TABLE parttest.event_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE parttest.event_log FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON parttest.event_log USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
