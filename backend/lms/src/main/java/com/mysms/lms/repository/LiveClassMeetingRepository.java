package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Statements of a live class's meeting: ending it, and the participants who joined or attended it. */
@Repository
public class LiveClassMeetingRepository {

    private static final String END = """
            UPDATE lms.live_class SET ended_at = :now, is_active = false, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String RECORD_JOIN = """
            INSERT INTO lms.live_class_participant (id, tenant_id, live_class_id, user_id, joined_at, is_presenter,
                                                    created_at, created_by, updated_at, updated_by, row_version)
            SELECT :id, :tenantId, :classId, :userId, :now, :presenter, :now, :userId, :now, :userId, 1
             WHERE NOT EXISTS (SELECT 1 FROM lms.live_class_participant WHERE tenant_id = :tenantId
                                  AND live_class_id = :classId AND user_id = :userId AND deleted_at IS NULL)
            """;
    private static final String ATTENDED = """
            UPDATE lms.live_class_participant
               SET joined_at = LEAST(joined_at, :joinedAt), left_at = :leftAt, duration_minutes = :minutes,
                   attended = true, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND live_class_id = :classId AND user_id = :userId AND deleted_at IS NULL
            """;
    private static final String INSERT_ATTENDED = """
            INSERT INTO lms.live_class_participant (id, tenant_id, live_class_id, user_id, joined_at, left_at,
                                                    duration_minutes, attended, created_at, created_by, updated_at,
                                                    updated_by, row_version)
            VALUES (:id, :tenantId, :classId, :userId, :joinedAt, :leftAt, :minutes, true, :now, :actor, :now,
                    :actor, 1)
            """;
    private static final String QUEUE_INVITES = """
            UPDATE lms.live_class SET invite_status = 'retrying', invite_attempts = invite_attempts + 1,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String COUNT_ATTENDEES = """
            UPDATE lms.live_class SET attendee_count = (SELECT count(*) FROM lms.live_class_participant p
                                                         WHERE p.tenant_id = :tenantId AND p.live_class_id = :id
                                                           AND p.attended AND p.deleted_at IS NULL),
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public LiveClassMeetingRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public int end(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(END).param("id", id).param("expected", expected).param("now", now).param("actor", actor)
                .update();
    }

    /** Marks the class's calendar invites retrying and counts the attempt. */
    public int queueInvites(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(QUEUE_INVITES).param("id", id).param("expected", expected).param("now", now)
                .param("actor", actor).update();
    }

    /** Records that the user asked to join (once per class); answers 1 when this was the first time. */
    public int recordJoin(UUID id, UUID classId, UUID userId, boolean presenter, OffsetDateTime now) {
        return jdbc.sql(RECORD_JOIN).param("id", id).param("classId", classId).param("userId", userId)
                .param("presenter", presenter).param("now", now).update();
    }

    /** Records the provider's attendance of one user, on their join row when they have one. */
    public void attended(UUID id, UUID classId, UUID userId, OffsetDateTime joinedAt, OffsetDateTime leftAt,
            int minutes, OffsetDateTime now, UUID actor) {
        int updated = jdbc.sql(ATTENDED).param("classId", classId).param("userId", userId).param("joinedAt", joinedAt)
                .param("leftAt", leftAt).param("minutes", minutes).param("now", now).param("actor", actor).update();
        if (updated == 0) {
            jdbc.sql(INSERT_ATTENDED).param("id", id).param("classId", classId).param("userId", userId)
                    .param("joinedAt", joinedAt).param("leftAt", leftAt).param("minutes", minutes).param("now", now)
                    .param("actor", actor).update();
        }
    }

    public void countAttendees(UUID id, OffsetDateTime now, UUID actor) {
        jdbc.sql(COUNT_ATTENDEES).param("id", id).param("now", now).param("actor", actor).update();
    }
}
