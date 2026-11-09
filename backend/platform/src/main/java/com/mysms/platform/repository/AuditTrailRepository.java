package com.mysms.platform.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.platform.config.PlatformDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Writes platform.audit_log, the tenant's immutable audit trail, from the modules' `audit.recorded` events. */
@Repository
public class AuditTrailRepository {

    private static final String INSERT = """
            INSERT INTO platform.audit_log (id, tenant_id, log_name, description, event, subject_type, subject_id,
                                            causer_type, causer_id, properties, occurred_at)
            VALUES (:id, :tenantId, :logName, :description, :event, :subjectType, :subjectId,
                    :causerType, :causerId, CAST(:properties AS jsonb), :occurredAt)
            """;

    /**
     * A record of global data (no tenant: tenants, plans, master data…), changed by platform staff. Row-level security
     * lets only a transaction that binds no tenant insert it and shows it to no tenant session. Such events bypass the
     * inbox, so the insert itself is idempotent.
     */
    private static final String INSERT_GLOBAL = """
            INSERT INTO platform.audit_log (id, tenant_id, log_name, description, event, subject_type, subject_id,
                                            causer_type, causer_id, properties, occurred_at)
            VALUES (:id, NULL, :logName, :description, :event, :subjectType, :subjectId,
                    :causerType, :causerId, CAST(:properties AS jsonb), :occurredAt)
            ON CONFLICT (id, occurred_at) DO NOTHING
            """;

    private final JdbcClient jdbc;

    public AuditTrailRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** One audit record; the id is the event's, so a record exists at most once. */
    public void insert(UUID id, String module, String action, String subjectType, UUID subjectId, UUID causerId,
                       String properties, OffsetDateTime occurredAt) {
        write(INSERT, id, module, action, subjectType, subjectId, causerId, properties, occurredAt);
    }

    /** One audit record of global data, at most once per event. */
    public void insertGlobal(UUID id, String module, String action, String subjectType, UUID subjectId, UUID causerId,
                             String properties, OffsetDateTime occurredAt) {
        write(INSERT_GLOBAL, id, module, action, subjectType, subjectId, causerId, properties, occurredAt);
    }

    private void write(String sql, UUID id, String module, String action, String subjectType, UUID subjectId,
                       UUID causerId, String properties, OffsetDateTime occurredAt) {
        jdbc.sql(sql)
                .param("id", id)
                .param("logName", module)
                .param("description", action + " " + subjectType)
                .param("event", action)
                .param("subjectType", subjectType)
                .param("subjectId", subjectId)
                .param("causerType", causerId == null ? "system" : "user")
                .param("causerId", causerId)
                .param("properties", properties)
                .param("occurredAt", occurredAt)
                .update();
    }
}
