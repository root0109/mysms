package com.mysms.core.db;

import com.mysms.core.id.UuidV7;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * One module's {@code <module>.idempotency_key}: a key is claimed in its own short transaction before the request
 * runs, then completed with the response or released after a server failure so a retry runs again. The tenant is
 * filtered explicitly and the response arrives already encrypted.
 */
public final class IdempotencyStore extends AppTransactionManager {

    private static final Pattern SCHEMA = Pattern.compile("[a-z]+");

    private final ModuleDatabase database;
    private final Clock clock;
    private final String claim;
    private final String find;
    private final String takeOver;
    private final String complete;
    private final String release;
    private final String purge;

    public IdempotencyStore(ModuleDatabase database, Clock clock) {
        super(database, database.transactionManager());
        if (!SCHEMA.matcher(database.module()).matches()) {
            throw new IllegalArgumentException("not a module schema: " + database.module());
        }
        this.database = database;
        this.clock = clock;
        String table = database.module() + ".idempotency_key";
        // the schema name comes from the module's own configuration, never from a request
        this.claim = """
                INSERT INTO {table} (id, tenant_id, idempotency_key, user_id, operation_name, request_hash, locked_at,
                                expires_at, created_at, created_by, updated_at, updated_by, row_version)
                VALUES (:id, :tenantId, :key, :user, :operation, :hash, :now, :expiresAt, :now, :user, :now, :user, 1)
                ON CONFLICT (tenant_id, idempotency_key) DO NOTHING
                """.replace("{table}", table);
        this.find = """
                SELECT id, user_id, operation_name, request_hash, response_status, response_body, locked_at,
                       completed_at, expires_at
                  FROM {table}
                 WHERE tenant_id = :tenantId
                   AND idempotency_key = :key
                   FOR UPDATE
                """.replace("{table}", table);
        this.takeOver = """
                UPDATE {table}
                   SET user_id = :user, operation_name = :operation, request_hash = :hash, response_status = NULL,
                       response_body = NULL, locked_at = :now, completed_at = NULL, expires_at = :expiresAt,
                       deleted_at = NULL, updated_at = :now, updated_by = :user, row_version = row_version + 1
                 WHERE tenant_id = :tenantId
                   AND id = :id
                """.replace("{table}", table);
        this.complete = """
                UPDATE {table}
                   SET response_status = :status, response_body = :body, completed_at = :now, locked_at = NULL,
                       updated_at = :now, row_version = row_version + 1
                 WHERE tenant_id = :tenantId
                   AND id = :id
                """.replace("{table}", table);
        // keys past their 24 hours are removed a few at a time, inside the tenant, whenever a new key is claimed
        this.purge = """
                DELETE FROM {table}
                 WHERE tenant_id = :tenantId
                   AND id IN (SELECT id FROM {table}
                               WHERE tenant_id = :tenantId AND expires_at < :now
                               LIMIT 20)
                """.replace("{table}", table);
        this.release = """
                UPDATE {table}
                   SET locked_at = NULL, updated_at = :now, row_version = row_version + 1
                 WHERE tenant_id = :tenantId
                   AND id = :id AND completed_at IS NULL
                """.replace("{table}", table);
    }

    public String module() {
        return database.module();
    }

    /** What a request with this key may do. */
    public enum Outcome { RUN, REPLAY, MISMATCH, IN_PROGRESS }

    /** The decision and, for REPLAY, the stored response (still encrypted). */
    public record Claim(Outcome outcome, UUID id, Integer status, String storedBody) {
    }

    /**
     * Claims the key for this request, in its own transaction: a new key, an expired one or one whose earlier
     * request failed on the server is run; a completed key of the same request is replayed; a key used for another
     * request (other user, operation or body) is a mismatch; a key whose request is still running is in progress
     * (unless its lock is older than {@code staleAfter}, then it is taken over).
     */
    public Claim claim(UUID key, UUID user, String operation, String hash, OffsetDateTime now,
                       OffsetDateTime expiresAt, OffsetDateTime staleBefore) {
        return doInWriteTransaction(() -> {
            UUID id = UuidV7.next(clock);
            JdbcClient jdbc = database.jdbc();
            jdbc.sql(purge).param("now", now).update();
            int inserted = jdbc.sql(claim).param("id", id).param("key", key).param("user", user)
                    .param("operation", operation).param("hash", hash).param("now", now)
                    .param("expiresAt", expiresAt).update();
            if (inserted == 1) {
                return new Claim(Outcome.RUN, id, null, null);
            }
            Row row = jdbc.sql(find).param("key", key).query((rs, n) -> new Row(rs.getObject("id", UUID.class),
                    rs.getObject("user_id", UUID.class), rs.getString("operation_name"), rs.getString("request_hash"),
                    (Integer) rs.getObject("response_status"), rs.getString("response_body"),
                    rs.getObject("locked_at", OffsetDateTime.class), rs.getObject("completed_at", OffsetDateTime.class),
                    rs.getObject("expires_at", OffsetDateTime.class))).single();
            boolean expired = !row.expiresAt().isAfter(now);
            boolean sameRequest = hash.equals(row.hash()) && operation.equals(row.operation())
                    && Objects.equals(user, row.user());
            if (expired || (sameRequest && row.completedAt() == null
                    && (row.lockedAt() == null || row.lockedAt().isBefore(staleBefore)))) {
                jdbc.sql(takeOver).param("id", row.id()).param("user", user).param("operation", operation)
                        .param("hash", hash).param("now", now).param("expiresAt", expiresAt).update();
                return new Claim(Outcome.RUN, row.id(), null, null);
            }
            if (!sameRequest) {
                return new Claim(Outcome.MISMATCH, row.id(), null, null);
            }
            if (row.completedAt() == null) {
                return new Claim(Outcome.IN_PROGRESS, row.id(), null, null);
            }
            return new Claim(Outcome.REPLAY, row.id(), row.status(), row.body());
        });
    }

    /** Records the response of a claimed key (the body already encrypted). */
    public void complete(UUID id, int status, String storedBody, OffsetDateTime now) {
        runInWriteTransaction(() -> database.jdbc().sql(complete).param("id", id).param("status", status)
                .param("body", storedBody).param("now", now).update());
    }

    /** Frees a claimed key whose request failed on the server, so a retry runs it again. */
    public void release(UUID id, OffsetDateTime now) {
        runInWriteTransaction(() -> database.jdbc().sql(release).param("id", id).param("now", now).update());
    }

    private record Row(UUID id, UUID user, String operation, String hash, Integer status, String body,
                       OffsetDateTime lockedAt, OffsetDateTime completedAt, OffsetDateTime expiresAt) {
    }
}
