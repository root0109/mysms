-- Test-only: the module-local idempotency store, shaped exactly like every module's idempotency_key.
CREATE TABLE coretest.idempotency_key (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    idempotency_key uuid NOT NULL,
    user_id uuid,
    operation_name varchar(120) NOT NULL,
    request_hash varchar(64),
    response_status smallint,
    response_body text,
    locked_at timestamptz,
    completed_at timestamptz,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_coretest_idempotency_key PRIMARY KEY (id),
    CONSTRAINT uq_coretest_idempotency_key_1 UNIQUE (tenant_id, idempotency_key)
);
ALTER TABLE coretest.idempotency_key ENABLE ROW LEVEL SECURITY;
ALTER TABLE coretest.idempotency_key FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON coretest.idempotency_key
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
