package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Sign-in sessions (iam.user_session): one per successful sign-in; its id is the access tokens' `sid`. */
@Repository
public class SignInSessionRepository {

    private static final int USER_AGENT_MAX = 500;
    private static final String INSERT = """
            INSERT INTO iam.user_session
                   (id, tenant_id, user_id, session_ref, ip_address, user_agent, started_at, last_seen_at, created_at,
                    created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenant, :user, :ref, CAST(:ip AS inet), :userAgent, :now, :now, :now, :user, :now, :user, 1)
            """;
    private static final String IS_OPEN = """
            SELECT EXISTS (SELECT 1 FROM iam.user_session WHERE tenant_id = :tenantId
                AND id = :id AND ended_at IS NULL AND deleted_at IS NULL)
            """;
    private static final String TOUCH =
            "UPDATE iam.user_session SET last_seen_at = :now WHERE tenant_id = :tenantId AND id = :id";
    private static final String END = """
            UPDATE iam.user_session SET ended_at = :now WHERE tenant_id = :tenantId AND id = :id AND ended_at IS NULL
            """;
    private static final String END_ALL = """
            UPDATE iam.user_session SET ended_at = :now WHERE tenant_id = :tenantId
                AND user_id = :user AND ended_at IS NULL
            """;
    private static final String END_ALL_EXCEPT = """
            UPDATE iam.user_session SET ended_at = :now WHERE tenant_id = :tenantId
                AND user_id = :user AND id <> :keep AND ended_at IS NULL
            """;

    /** Push devices of the user stop receiving notifications at sign-out (iam.Logout). */
    private static final String DEACTIVATE_DEVICES = """
            UPDATE iam.user_device SET is_active = false, updated_at = :now, updated_by = :user
             WHERE tenant_id = :tenantId
               AND user_id = :user AND is_active AND deleted_at IS NULL
            """;
    private static final String DEACTIVATE_SESSION_DEVICE = """
            UPDATE iam.user_device SET is_active = false, updated_at = :now, updated_by = :user
             WHERE tenant_id = :tenantId
               AND is_active AND deleted_at IS NULL
               AND id = (SELECT device_id FROM iam.user_session WHERE tenant_id = :tenantId
                   AND id = :session AND user_id = :user)
            """;

    private final JdbcClient jdbc;

    public SignInSessionRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public void insert(UUID id, UUID tenant, UUID user, String ip, String userAgent, OffsetDateTime now) {
        String agent = userAgent == null || userAgent.length() <= USER_AGENT_MAX ? userAgent
                : userAgent.substring(0, USER_AGENT_MAX);
        jdbc.sql(INSERT).param("id", id).param("tenant", tenant).param("user", user).param("ref", id.toString())
                .param("ip", ip).param("userAgent", agent).param("now", now).update();
    }

    public boolean isOpen(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(IS_OPEN).param("id", id).query(Boolean.class).single());
    }

    public void touch(UUID id, OffsetDateTime now) {
        jdbc.sql(TOUCH).param("id", id).param("now", now).update();
    }

    public int end(UUID id, OffsetDateTime now) {
        return jdbc.sql(END).param("id", id).param("now", now).update();
    }

    public int deactivateDevices(UUID user, OffsetDateTime now) {
        return jdbc.sql(DEACTIVATE_DEVICES).param("user", user).param("now", now).update();
    }

    public int deactivateSessionDevice(UUID session, UUID user, OffsetDateTime now) {
        return jdbc.sql(DEACTIVATE_SESSION_DEVICE).param("session", session).param("user", user).param("now", now)
                .update();
    }

    public int endAll(UUID user, OffsetDateTime now) {
        return jdbc.sql(END_ALL).param("user", user).param("now", now).update();
    }

    /** Ends every other session of the user (a password change keeps the one it was made from). */
    public int endAllExcept(UUID user, UUID keep, OffsetDateTime now) {
        return jdbc.sql(END_ALL_EXCEPT).param("user", user).param("keep", keep).param("now", now).update();
    }
}
