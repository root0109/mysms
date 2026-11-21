package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.TenantPurposeEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC writes of a tenant's purpose settings (privacy.tenant_purpose; reads go through
 * {@link PurposeRepository}). The table has no soft delete: a setting is switched off, never removed. Every statement
 * filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class TenantPurposeRepository {

    private static final String FIND_BY_PURPOSE = """
            SELECT t.id, t.tenant_id, t.purpose_id, t.enabled, t.max_scope, t.notice_id, t.created_at, t.created_by,
                   t.updated_at, t.updated_by, t.row_version
              FROM privacy.tenant_purpose t
             WHERE t.tenant_id = :tenantId
               AND t.purpose_id = :purposeId
            """;
    /** A concurrent first write of the same purpose inserts nothing, which the service answers as a stale ETag. */
    private static final String INSERT = """
            INSERT INTO privacy.tenant_purpose
            (id, tenant_id, purpose_id, enabled, max_scope, notice_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :purposeId, :enabled, :maxScope, :noticeId, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            ON CONFLICT (tenant_id, purpose_id) DO NOTHING
            """;
    private static final String UPDATE = """
            UPDATE privacy.tenant_purpose
               SET enabled = :enabled,
                   max_scope = :maxScope,
                   notice_id = :noticeId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected
            """;
    private static final String EXISTS_NOTICE = """
            SELECT EXISTS (SELECT 1 FROM privacy.notice WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String NOTICE_LISTS_PURPOSE = """
            SELECT EXISTS (SELECT 1 FROM privacy.notice n
                             JOIN privacy.notice_version v ON v.tenant_id = :tenantId AND v.notice_id = n.id
                                  AND v.status = 'active'
                            WHERE n.tenant_id = :tenantId
                              AND n.id = :noticeId AND n.deleted_at IS NULL
                              AND :purposeCode = ANY(v.purpose_codes))
            """;

    private final JdbcClient jdbc;

    public TenantPurposeRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The tenant's setting of the purpose; empty while the tenant has never set it. */
    public Optional<TenantPurposeEntity> findByPurposeId(UUID purposeId) {
        return jdbc.sql(FIND_BY_PURPOSE).param("purposeId", purposeId).query(TenantPurposeRepository::map).optional();
    }

    /** Inserts the first setting of the purpose; 0 rows means another request set it first. */
    public int insert(TenantPurposeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("purposeId", e.getPurposeId())
                .param("enabled", e.getEnabled())
                .param("maxScope", e.getMaxScope())
                .param("noticeId", e.getNoticeId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Updates under the ETag guard; 0 rows means the row changed since it was read. */
    public int update(TenantPurposeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("enabled", e.getEnabled())
                .param("maxScope", e.getMaxScope())
                .param("noticeId", e.getNoticeId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced privacy.notice row exists (live). */
    public boolean existsNotice(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_NOTICE).param("id", id).query(Boolean.class).single());
    }

    /** Whether the live notice's active version lists the purpose code. */
    public boolean noticeListsPurpose(UUID noticeId, String purposeCode) {
        return Boolean.TRUE.equals(jdbc.sql(NOTICE_LISTS_PURPOSE).param("noticeId", noticeId)
                .param("purposeCode", purposeCode).query(Boolean.class).single());
    }

    private static TenantPurposeEntity map(ResultSet rs, int row) throws SQLException {
        return TenantPurposeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .purposeId(rs.getObject("purpose_id", UUID.class))
                .enabled(rs.getObject("enabled", Boolean.class))
                .maxScope(rs.getString("max_scope"))
                .noticeId(rs.getObject("notice_id", UUID.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }
}
