package com.mysms.people.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.people.config.PeopleDatabaseConfig;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * people.member_link: the students, staff and guardian links of persons, kept from sis's and hr's events. A row
 * changes only to a newer source row version, so a late or repeated event changes nothing, and a deleted record stays
 * as a tombstone a late event cannot bring back.
 */
@Repository
public class MemberLinkRepository {

    /** One link as an event describes it. */
    public record Link(String recordType, UUID personId, UUID institutionId, UUID subjectPersonId, boolean active,
            LocalDate joinedOn) {
    }

    /** A member of an institution whose birthday or anniversary may be listed: the directory's view only. */
    public record Member(UUID personId, String displayName, LocalDate dateOfBirth, String role, LocalDate joinedOn) {
    }

    private static final String APPLY = """
            INSERT INTO people.member_link (tenant_id, record_id, record_type, person_id, institution_id,
                                            subject_person_id, is_active, joined_on, is_deleted, source_version,
                                            synced_at)
            VALUES (:tenantId, :recordId, :recordType, :personId, :institutionId, :subjectPersonId, :active,
                    :joinedOn, :deleted, :version, :now)
            ON CONFLICT (tenant_id, record_id) DO UPDATE
               SET person_id = EXCLUDED.person_id, institution_id = EXCLUDED.institution_id,
                   subject_person_id = EXCLUDED.subject_person_id, is_active = EXCLUDED.is_active,
                   joined_on = EXCLUDED.joined_on, is_deleted = EXCLUDED.is_deleted,
                   source_version = EXCLUDED.source_version, synced_at = EXCLUDED.synced_at
             WHERE people.member_link.source_version < EXCLUDED.source_version
               AND people.member_link.record_type = EXCLUDED.record_type
            """;
    /**
     * The institutions a person belongs to: those of their own student and staff records, and as a guardian those of
     * the student records of the children they are actively linked to.
     */
    private static final String INSTITUTIONS_OF = """
            SELECT DISTINCT m.institution_id
              FROM people.member_link m
             WHERE m.tenant_id = :tenantId AND NOT m.is_deleted AND m.is_active AND m.institution_id IS NOT NULL
               AND (m.person_id = :personId
                 OR (m.record_type = 'student'
                     AND EXISTS (SELECT 1 FROM people.member_link g
                                  WHERE g.tenant_id = :tenantId AND NOT g.is_deleted AND g.is_active
                                    AND g.record_type = 'guardian' AND g.person_id = :personId
                                    AND g.subject_person_id = m.person_id)))
            """;

    /**
     * The live persons of an institution (a guardian through a child's student record there) in the roles asked for
     * whose birthday or joining day (month and day) falls in the window; each person once per role.
     */
    private static final String MEMBERS = """
            SELECT DISTINCT p.id, p.display_name, p.date_of_birth, m.record_type, m.joined_on
              FROM people.member_link m
              JOIN people.person p ON p.tenant_id = :tenantId AND p.id = m.person_id AND p.deleted_at IS NULL
             WHERE m.tenant_id = :tenantId AND NOT m.is_deleted AND m.is_active AND m.record_type = ANY(:roles)
               AND (m.institution_id = :institutionId
                 OR (m.record_type = 'guardian'
                     AND EXISTS (SELECT 1 FROM people.member_link s
                                  WHERE s.tenant_id = :tenantId AND NOT s.is_deleted AND s.is_active
                                    AND s.record_type = 'student' AND s.institution_id = :institutionId
                                    AND s.person_id = m.subject_person_id)))
               AND ((:birthdays AND p.date_of_birth IS NOT NULL
                     AND to_char(p.date_of_birth, 'MMDD') BETWEEN :fromDay AND :toDay)
                 OR (:anniversaries AND m.joined_on IS NOT NULL
                     AND to_char(m.joined_on, 'MMDD') BETWEEN :fromDay AND :toDay))
            """;

    private final JdbcClient jdbc;

    public MemberLinkRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one event of a record; whether the stored entry changed (false for an older or repeated version). */
    public boolean apply(UUID recordId, Link link, boolean deleted, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("recordId", recordId).param("recordType", link.recordType())
                .param("personId", link.personId()).param("institutionId", link.institutionId())
                .param("subjectPersonId", link.subjectPersonId()).param("active", link.active())
                .param("joinedOn", link.joinedOn()).param("deleted", deleted)
                .param("version", version).param("now", now).update() == 1;
    }

    public List<UUID> institutionsOf(UUID personId) {
        return jdbc.sql(INSTITUTIONS_OF).param("personId", personId).query(UUID.class).list();
    }

    /**
     * The members of an institution with a birthday or joining day between two month-days ({@code MMDD}, inclusive,
     * the first not after the second).
     */
    public List<Member> members(UUID institutionId, List<String> roles, boolean birthdays, boolean anniversaries,
            String fromDay, String toDay) {
        return jdbc.sql(MEMBERS).param("institutionId", institutionId).param("roles", roles.toArray(String[]::new))
                .param("birthdays", birthdays).param("anniversaries", anniversaries).param("fromDay", fromDay)
                .param("toDay", toDay)
                .query((rs, row) -> new Member(rs.getObject("id", UUID.class), rs.getString("display_name"),
                        rs.getObject("date_of_birth", LocalDate.class), rs.getString("record_type"),
                        rs.getObject("joined_on", LocalDate.class)))
                .list();
    }
}
