package com.mysms.students.sis.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.SqlFragment;
import com.mysms.students.sis.config.SisDatabaseConfig;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Who is whose guardian, read for the persona rules of sis ("a guardian sees only their own children"): the active
 * links (sis.student_guardian) of the guardian record (sis.guardian) that belongs to a login account or to its
 * person. An inactive or deleted link, and a deleted guardian record, link nobody. Every statement filters the
 * caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class FamilyLinkRepository {

    /** Stands for "no person": a token without a person record matches no guardian by person. */
    private static final UUID NOBODY = new UUID(0L, 0L);

    private static final String IS_GUARDIAN_OF = """
            SELECT EXISTS (SELECT 1 FROM sis.student_guardian sg
                             JOIN sis.guardian g ON g.tenant_id = :tenantId AND g.id = sg.guardian_id
                                  AND g.deleted_at IS NULL
                            WHERE sg.tenant_id = :tenantId
                              AND sg.student_id = :studentId AND sg.deleted_at IS NULL AND sg.status = 'active'
                              AND (g.user_id = :userId OR g.person_id = :personId))
            """;
    private static final String RELATIONSHIPS = """
            SELECT sg.student_id, t.name
              FROM sis.student_guardian sg
              JOIN sis.guardian g ON g.tenant_id = :tenantId AND g.id = sg.guardian_id AND g.deleted_at IS NULL
              JOIN sis.relationship_type t ON t.tenant_id = :tenantId AND t.id = sg.relationship_type_id
                   AND t.deleted_at IS NULL
             WHERE sg.tenant_id = :tenantId
               AND sg.student_id = ANY(:studentIds) AND sg.deleted_at IS NULL AND sg.status = 'active'
               AND (g.user_id = :userId OR g.person_id = :personId)
            """;
    private static final String INSTITUTIONS_OF_CHILDREN = """
            SELECT DISTINCT s.institution_id
              FROM sis.student_guardian sg
              JOIN sis.student s ON s.tenant_id = :tenantId AND s.id = sg.student_id AND s.deleted_at IS NULL
             WHERE sg.tenant_id = :tenantId
               AND sg.guardian_id = :guardianId AND sg.deleted_at IS NULL AND sg.status = 'active'
            """;
    private static final String CHILDREN = """
             (r.id IN (SELECT sg.student_id
                         FROM sis.student_guardian sg
                         JOIN sis.guardian g ON g.tenant_id = :tenantId AND g.id = sg.guardian_id
                              AND g.deleted_at IS NULL
                        WHERE sg.tenant_id = :tenantId
                          AND sg.deleted_at IS NULL AND sg.status = 'active'
                          AND (g.user_id = :familyUserId OR g.person_id = :familyPersonId)))
            """;

    private final JdbcClient jdbc;

    public FamilyLinkRepository(@Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Whether the account, or its person, is an active guardian of the student. */
    public boolean isGuardianOf(UUID studentId, UUID userId, Optional<UUID> personId) {
        return Boolean.TRUE.equals(jdbc.sql(IS_GUARDIAN_OF).param("studentId", studentId).param("userId", userId)
                .param("personId", personId.orElse(NOBODY)).query(Boolean.class).single());
    }

    /** What the account's (or its person's) guardian record is to each of these students: student id → relationship. */
    public Map<UUID, String> relationships(Collection<UUID> studentIds, UUID userId, Optional<UUID> personId) {
        Map<UUID, String> relationships = new HashMap<>();
        jdbc.sql(RELATIONSHIPS).param("studentIds", studentIds.toArray(UUID[]::new)).param("userId", userId)
                .param("personId", personId.orElse(NOBODY))
                .query((rs, row) -> relationships.put(rs.getObject("student_id", UUID.class), rs.getString("name")))
                .list();
        return relationships;
    }

    /** The institutions of the students the guardian record is actively linked to. */
    public Collection<UUID> institutionsOfChildren(UUID guardianId) {
        return jdbc.sql(INSTITUTIONS_OF_CHILDREN).param("guardianId", guardianId)
                .query((rs, row) -> rs.getObject("institution_id", UUID.class)).list();
    }

    /** A filter over sis.student rows ({@code r}): the students the account, or its person, is an active guardian of. */
    public static SqlFragment childrenOf(UUID userId, Optional<UUID> personId) {
        return new SqlFragment(CHILDREN, Map.of("familyUserId", userId, "familyPersonId", personId.orElse(NOBODY)));
    }
}
