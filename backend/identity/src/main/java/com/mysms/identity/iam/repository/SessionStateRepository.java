package com.mysms.identity.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * What a sign-in session says about the tokens issued for it (iam.user_session): the role it works in after
 * iam.SwitchRole, and the administrator acting in it when it is an impersonation session.
 */
@Repository
public class SessionStateRepository {

    private static final int USER_AGENT_MAX = 500;
    private static final String INSERT_IMPERSONATION = """
            INSERT INTO iam.user_session
                   (id, tenant_id, user_id, session_ref, ip_address, user_agent, started_at, last_seen_at,
                    impersonator_user_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :user, :ref, CAST(:ip AS inet), :userAgent, :now, :now, :actor, :now, :actor,
                    :now, :actor, 1)
            """;
    private static final String FIND = """
            SELECT user_id, impersonator_user_id, active_role_id, active_scope_id, ended_at
              FROM iam.user_session
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;
    private static final String SET_ACTIVE_ROLE = """
            UPDATE iam.user_session SET active_role_id = :role, active_scope_id = :scope, last_seen_at = :now
             WHERE tenant_id = :tenantId
               AND id = :id AND user_id = :user AND ended_at IS NULL AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public SessionStateRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** A session of {@code user} opened by {@code actor} acting as them (no refresh token belongs to it). */
    public void insertImpersonation(UUID id, UUID tenant, UUID user, UUID actor, String ip, String userAgent,
                                    OffsetDateTime now) {
        String agent = userAgent == null || userAgent.length() <= USER_AGENT_MAX ? userAgent
                : userAgent.substring(0, USER_AGENT_MAX);
        jdbc.sql(INSERT_IMPERSONATION).param("id", id).param("tenantId", tenant).param("user", user)
                .param("ref", id.toString()).param("ip", ip).param("userAgent", agent).param("actor", actor)
                .param("now", now).update();
    }

    public Optional<State> find(UUID session) {
        return jdbc.sql(FIND).param("id", session)
                .query((rs, row) -> new State(rs.getObject("user_id", UUID.class),
                        Optional.ofNullable(rs.getObject("impersonator_user_id", UUID.class)),
                        Optional.ofNullable(rs.getObject("active_role_id", UUID.class)),
                        Optional.ofNullable(rs.getObject("active_scope_id", UUID.class)),
                        rs.getObject("ended_at", OffsetDateTime.class) != null))
                .optional();
    }

    /** 0 when the session is not the user's open session. */
    public int setActiveRole(UUID session, UUID user, UUID role, UUID scope, OffsetDateTime now) {
        return jdbc.sql(SET_ACTIVE_ROLE).param("id", session).param("user", user).param("role", role)
                .param("scope", scope).param("now", now).update();
    }

    /**
     * @param userId         whose session it is
     * @param impersonatorId the administrator acting in it, for an impersonation session
     * @param activeRoleId   the role it works in, after a switch
     * @param activeScopeId  the unit of that role's grant it works in, when narrowed to one
     * @param ended          signed out (or ended by StopImpersonation)
     */
    public record State(UUID userId, Optional<UUID> impersonatorId, Optional<UUID> activeRoleId,
                        Optional<UUID> activeScopeId, boolean ended) {
    }
}
