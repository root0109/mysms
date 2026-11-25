package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.BreachNotificationEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to privacy.breach_notification: append-only, so rows are inserted and read, never changed. Every
 * read filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class BreachNotificationRepository {

    private static final String OF_BREACHES = """
            SELECT n.id, n.tenant_id, n.breach_id, n.audience, n.kind, n.sent_at, n.document_file_id, n.created_at,
                   n.created_by, n.updated_at, n.updated_by, n.row_version
              FROM privacy.breach_notification n
             WHERE n.tenant_id = :tenantId
               AND n.breach_id = ANY(CAST(:breachIds AS uuid[]))
             ORDER BY n.sent_at, n.id
            """;
    private static final String INSERT = """
            INSERT INTO privacy.breach_notification
            (id, tenant_id, breach_id, audience, kind, sent_at, document_file_id, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :breachId, :audience, :kind, :sentAt, :documentFileId, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            """;

    private final JdbcClient jdbc;

    public BreachNotificationRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The notices of the breaches, in the order they were sent. */
    public List<BreachNotificationEntity> ofBreaches(List<UUID> breachIds) {
        return jdbc.sql(OF_BREACHES).param("breachIds", breachIds.toArray(UUID[]::new))
                .query(BreachNotificationRepository::map).list();
    }

    public void insert(BreachNotificationEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("breachId", e.getBreachId())
                .param("audience", e.getAudience())
                .param("kind", e.getKind())
                .param("sentAt", e.getSentAt())
                .param("documentFileId", e.getDocumentFileId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    private static BreachNotificationEntity map(ResultSet rs, int row) throws SQLException {
        return BreachNotificationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .breachId(rs.getObject("breach_id", UUID.class))
                .audience(rs.getString("audience"))
                .kind(rs.getString("kind"))
                .sentAt(rs.getObject("sent_at", OffsetDateTime.class))
                .documentFileId(rs.getObject("document_file_id", UUID.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }
}
