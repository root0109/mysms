package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.NomineeEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to privacy.nominee (no soft delete: a revoked nomination stays as evidence). A principal has at
 * most one active nominee (partial unique index); every change is one guarded statement on the active row and its
 * row_version. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class NomineeRepository {

    private static final String SELECT = """
            SELECT n.id, n.tenant_id, n.principal_person_id, n.nominee_person_id, n.nominee_name_enc,
                   n.nominee_contact_enc, n.status, n.activated_at, n.activated_by_request_id, n.revoked_at,
                   n.created_at, n.created_by, n.updated_at, n.updated_by, n.row_version
              FROM privacy.nominee n
             WHERE n.tenant_id = :tenantId
            """;
    private static final String ACTIVE_OF = SELECT + " AND n.principal_person_id = :principalPersonId"
            + " AND n.status = 'active'";
    private static final String ACTIVE_NOMINEE = ACTIVE_OF + " AND n.nominee_person_id = :nomineePersonId";
    private static final String INSERT = """
            INSERT INTO privacy.nominee
            (id, tenant_id, principal_person_id, nominee_person_id, nominee_name_enc, nominee_contact_enc, status,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :principalPersonId, :nomineePersonId, :nomineeNameEnc, :nomineeContactEnc, 'active',
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            """;
    private static final String REVOKE = """
            UPDATE privacy.nominee
               SET status = 'revoked',
                   revoked_at = :at,
                   updated_at = :at,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'active'
            """;
    private static final String ACTIVATE = """
            UPDATE privacy.nominee
               SET activated_at = :at,
                   activated_by_request_id = :requestId,
                   updated_at = :at,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND principal_person_id = :principalPersonId AND nominee_person_id = :nomineePersonId
               AND status = 'active' AND activated_at IS NULL
            """;

    private final JdbcClient jdbc;

    public NomineeRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The principal's active nominee; empty when they name none. */
    public Optional<NomineeEntity> findActive(UUID principalPersonId) {
        return jdbc.sql(ACTIVE_OF).param("principalPersonId", principalPersonId).query(this::map).optional();
    }

    /** The principal's active nominee when it is this person; empty otherwise. */
    public Optional<NomineeEntity> findActive(UUID principalPersonId, UUID nomineePersonId) {
        return jdbc.sql(ACTIVE_NOMINEE).param("principalPersonId", principalPersonId)
                .param("nomineePersonId", nomineePersonId).query(this::map).optional();
    }

    public void insert(NomineeEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("principalPersonId", e.getPrincipalPersonId())
                .param("nomineePersonId", e.getNomineePersonId())
                .param("nomineeNameEnc", e.getNomineeNameEnc())
                .param("nomineeContactEnc", e.getNomineeContactEnc())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** active → revoked; 0 rows means the row changed since it was read. */
    public int revoke(UUID id, int expectedVersion, OffsetDateTime at, UUID actor) {
        return jdbc.sql(REVOKE).param("id", id).param("expected", expectedVersion).param("at", at)
                .param("updatedBy", actor).update();
    }

    /**
     * Lets the principal's active nominee act for them from now on, recording the nomination request whose fulfilment
     * did it; 0 rows when this person is no longer the principal's active nominee, or already acts for them.
     */
    public int activate(UUID principalPersonId, UUID nomineePersonId, UUID requestId, OffsetDateTime at,
            UUID actor) {
        return jdbc.sql(ACTIVATE).param("principalPersonId", principalPersonId)
                .param("nomineePersonId", nomineePersonId).param("requestId", requestId).param("at", at)
                .param("updatedBy", actor).update();
    }

    private NomineeEntity map(ResultSet rs, int row) throws SQLException {
        return NomineeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .principalPersonId(rs.getObject("principal_person_id", UUID.class))
                .nomineePersonId(rs.getObject("nominee_person_id", UUID.class))
                .nomineeNameEnc(rs.getString("nominee_name_enc"))
                .nomineeContactEnc(rs.getString("nominee_contact_enc"))
                .status(rs.getString("status"))
                .activatedAt(rs.getObject("activated_at", OffsetDateTime.class))
                .activatedByRequestId(rs.getObject("activated_by_request_id", UUID.class))
                .revokedAt(rs.getObject("revoked_at", OffsetDateTime.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }
}
