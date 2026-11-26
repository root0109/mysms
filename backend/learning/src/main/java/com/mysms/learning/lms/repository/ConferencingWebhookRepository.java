package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain-JDBC effects of a conferencing callback: the live class behind a meeting, its account's secret, the writes. */
@Repository
public class ConferencingWebhookRepository {

    /** The live class a meeting reference names. */
    public record Meeting(UUID id, UUID hostUserId, UUID accountId, int rowVersion) {
    }

    private static final String MEETING = """
            SELECT id, host_user_id, conferencing_account_id, row_version FROM lms.live_class
             WHERE tenant_id = :tenantId AND conferencing_provider = :provider
               AND (meeting_id = :ref OR meeting_uuid = :ref) AND deleted_at IS NULL
             ORDER BY created_at DESC LIMIT 1
            """;
    private static final String SECRET = """
            SELECT client_secret_enc FROM lms.conferencing_account
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String ENDED = """
            UPDATE lms.live_class SET ended_at = COALESCE(ended_at, :endedAt), updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String RECORDING = """
            UPDATE lms.live_class SET recording_url = :url, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String HOST_JOINED = """
            UPDATE lms.live_class SET host_joined = true, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND NOT host_joined AND deleted_at IS NULL
            """;
    private static final String JOINED = """
            UPDATE lms.live_class_participant SET joined_at = LEAST(joined_at, :at), attended = true,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND live_class_id = :id AND user_id = :userId AND deleted_at IS NULL
            """;
    private static final String INSERT_JOINED = """
            INSERT INTO lms.live_class_participant (id, tenant_id, live_class_id, user_id, joined_at, attended,
                   created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:participantId, :tenantId, :id, :userId, :at, true, :now, :actor, :now, :actor, 1)
            """;
    private static final String LEFT = """
            UPDATE lms.live_class_participant SET left_at = GREATEST(joined_at, :at),
                   duration_minutes = GREATEST(0, CEIL(EXTRACT(EPOCH FROM (:at - joined_at)) / 60))::int,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND live_class_id = :id AND user_id = :userId AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public ConferencingWebhookRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<Meeting> meeting(String provider, String ref) {
        return jdbc.sql(MEETING).param("provider", provider).param("ref", ref)
                .query((rs, n) -> new Meeting(rs.getObject("id", UUID.class),
                        rs.getObject("host_user_id", UUID.class),
                        rs.getObject("conferencing_account_id", UUID.class), rs.getInt("row_version"))).optional();
    }

    public Optional<String> accountSecret(UUID accountId) {
        return jdbc.sql(SECRET).param("id", accountId).query(String.class).optional()
                .filter(secret -> !secret.isBlank());
    }

    public int ended(UUID id, OffsetDateTime endedAt, OffsetDateTime now, UUID actor) {
        return jdbc.sql(ENDED).param("id", id).param("endedAt", endedAt).param("now", now).param("actor", actor)
                .update();
    }

    public int recording(UUID id, String url, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RECORDING).param("id", id).param("url", url).param("now", now).param("actor", actor).update();
    }

    public int hostJoined(UUID id, OffsetDateTime now, UUID actor) {
        return jdbc.sql(HOST_JOINED).param("id", id).param("now", now).param("actor", actor).update();
    }

    public int joined(UUID id, UUID userId, OffsetDateTime at, OffsetDateTime now, UUID actor) {
        return jdbc.sql(JOINED).param("id", id).param("userId", userId).param("at", at).param("now", now)
                .param("actor", actor).update();
    }

    public void insertJoined(UUID participantId, UUID id, UUID userId, OffsetDateTime at, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(INSERT_JOINED).param("participantId", participantId).param("id", id).param("userId", userId)
                .param("at", at).param("now", now).param("actor", actor).update();
    }

    public int left(UUID id, UUID userId, OffsetDateTime at, OffsetDateTime now, UUID actor) {
        return jdbc.sql(LEFT).param("id", id).param("userId", userId).param("at", at).param("now", now)
                .param("actor", actor).update();
    }
}
