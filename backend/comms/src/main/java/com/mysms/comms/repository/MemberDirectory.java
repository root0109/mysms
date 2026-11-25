package com.mysms.comms.repository;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Who belongs to a school, as comms knows it from sis's and hr's events (comms.student_record, comms.staff_record,
 * comms.guardian_link): its students' and staff's login accounts, and the accounts of its students' active guardians.
 * An account comms knows no such record of (an administrator who is no member of staff, say) belongs to no school.
 */
@Repository
public class MemberDirectory {

    private static final String STUDENT_ACCOUNTS_IN = """
            SELECT r.user_id
              FROM comms.student_record r
             WHERE r.tenant_id = :tenantId
               AND (CAST(:institutionId AS uuid) IS NULL OR r.institution_id = :institutionId)
               AND r.user_id = ANY(:userIds)
            """;
    private static final String STAFF_ACCOUNTS_IN = """
            SELECT f.user_id
              FROM comms.staff_record f
             WHERE f.tenant_id = :tenantId AND NOT f.is_deleted
               AND (CAST(:institutionId AS uuid) IS NULL OR f.institution_id = :institutionId)
               AND f.user_id = ANY(:userIds)
            """;
    private static final String GUARDIAN_ACCOUNTS_IN = """
            SELECT g.guardian_user_id
              FROM comms.guardian_link g
              JOIN comms.student_record s ON s.tenant_id = :tenantId AND s.student_id = g.student_id
             WHERE g.tenant_id = :tenantId AND NOT g.is_deleted AND g.status = 'active'
               AND (CAST(:institutionId AS uuid) IS NULL OR s.institution_id = :institutionId)
               AND g.guardian_user_id = ANY(:userIds)
            """;
    private static final String STUDENT_IN = """
            SELECT EXISTS (SELECT 1 FROM comms.student_record r
                            WHERE r.tenant_id = :tenantId AND r.student_id = :studentId
                              AND r.institution_id = :institutionId)
            """;
    private static final String GUARDIAN_IS = """
            SELECT EXISTS (SELECT 1 FROM comms.guardian_link g
                            WHERE g.tenant_id = :tenantId AND NOT g.is_deleted AND g.status = 'active'
                              AND g.guardian_id = :guardianId
                              AND (g.guardian_user_id = :userId OR g.guardian_person_id = CAST(:personId AS uuid)))
            """;
    private static final String GUARDIAN_OF = """
            SELECT EXISTS (SELECT 1 FROM comms.guardian_link g
                            WHERE g.tenant_id = :tenantId AND NOT g.is_deleted AND g.status = 'active'
                              AND g.guardian_id = :guardianId AND g.student_id = :studentId
                              AND (g.guardian_user_id = :userId OR g.guardian_person_id = CAST(:personId AS uuid)))
            """;

    private final JdbcClient jdbc;

    public MemberDirectory(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The accounts among the ids that belong to the school (any school of the tenant for none). */
    public Set<UUID> accountsIn(Collection<UUID> userIds, UUID institutionId) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        UUID[] ids = userIds.toArray(UUID[]::new);
        Set<UUID> found = new HashSet<>();
        for (String query : List.of(STUDENT_ACCOUNTS_IN, STAFF_ACCOUNTS_IN, GUARDIAN_ACCOUNTS_IN)) {
            found.addAll(jdbc.sql(query).param("userIds", ids).param("institutionId", institutionId)
                    .query(UUID.class).list());
        }
        return found;
    }

    /** Whether the student is one of the school's. */
    public boolean studentIn(UUID studentId, UUID institutionId) {
        return Boolean.TRUE.equals(jdbc.sql(STUDENT_IN).param("studentId", studentId)
                .param("institutionId", institutionId).query(Boolean.class).single());
    }

    /** Whether the account or person (null = none) is the guardian record, linked to some student. */
    public boolean isGuardian(UUID guardianId, UUID userId, UUID personId) {
        return Boolean.TRUE.equals(jdbc.sql(GUARDIAN_IS).param("guardianId", guardianId).param("userId", userId)
                .param("personId", personId).query(Boolean.class).single());
    }

    /** Whether the account or person is the guardian record and that guardian is linked to the student. */
    public boolean isGuardianOf(UUID guardianId, UUID studentId, UUID userId, UUID personId) {
        return Boolean.TRUE.equals(jdbc.sql(GUARDIAN_OF).param("guardianId", guardianId).param("studentId", studentId)
                .param("userId", userId).param("personId", personId)
                .query(Boolean.class).single());
    }
}
