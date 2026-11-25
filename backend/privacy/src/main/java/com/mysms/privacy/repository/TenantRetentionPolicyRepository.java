package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.TenantRetentionPolicyEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to privacy.tenant_retention_policy: the first setting of a policy inserts the tenant's row, later
 * ones update it under the row_version read. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class TenantRetentionPolicyRepository {

    private static final String FIND_BY_POLICY = """
            SELECT t.id, t.tenant_id, t.policy_id, t.period_days, t.action, t.created_at, t.created_by, t.updated_at,
                   t.updated_by, t.row_version
              FROM privacy.tenant_retention_policy t
             WHERE t.tenant_id = :tenantId
               AND t.policy_id = :policyId
            """;
    private static final String INSERT = """
            INSERT INTO privacy.tenant_retention_policy
            (id, tenant_id, policy_id, period_days, action, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :policyId, :periodDays, :action, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            ON CONFLICT (tenant_id, policy_id) DO NOTHING
            """;
    private static final String UPDATE = """
            UPDATE privacy.tenant_retention_policy
               SET period_days = :periodDays,
                   action = :action,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected
            """;

    private final JdbcClient jdbc;

    public TenantRetentionPolicyRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The tenant's setting of the policy; empty while the tenant has never set it. */
    public Optional<TenantRetentionPolicyEntity> findByPolicyId(UUID policyId) {
        return jdbc.sql(FIND_BY_POLICY).param("policyId", policyId).query(TenantRetentionPolicyRepository::map)
                .optional();
    }

    /** Inserts the first setting of the policy; 0 rows means another request set it first. */
    public int insert(TenantRetentionPolicyEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("policyId", e.getPolicyId())
                .param("periodDays", e.getPeriodDays())
                .param("action", e.getAction())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Writes the tenant's period and action; 0 rows means the setting changed since it was read. */
    public int update(TenantRetentionPolicyEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("periodDays", e.getPeriodDays())
                .param("action", e.getAction())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static TenantRetentionPolicyEntity map(ResultSet rs, int row) throws SQLException {
        return TenantRetentionPolicyEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .policyId(rs.getObject("policy_id", UUID.class))
                .periodDays(rs.getInt("period_days"))
                .action(rs.getString("action"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }
}
