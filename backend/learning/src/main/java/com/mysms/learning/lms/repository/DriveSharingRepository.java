package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain-JDBC writes of ShareDriveItem: the item locked for the change, its shares and its shared flag. */
@Repository
public class DriveSharingRepository {

    /** The owner and version of a drive item. */
    public record Owned(UUID ownerUserId, int rowVersion) {
    }

    private static final String LOCK = """
            SELECT owner_user_id, row_version FROM lms.drive_item
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL FOR UPDATE
            """;
    private static final String SHARED_WITH = """
            SELECT user_id FROM lms.drive_item_share
             WHERE tenant_id = :tenantId AND drive_item_id = :id AND deleted_at IS NULL
            """;
    private static final String SHARE = """
            INSERT INTO lms.drive_item_share (id, tenant_id, drive_item_id, user_id, created_at, created_by,
                   updated_at, updated_by, row_version)
            VALUES (:shareId, :tenantId, :id, :userId, :now, :actor, :now, :actor, 1)
            """;
    private static final String UNSHARE = """
            UPDATE lms.drive_item_share SET deleted_at = :now, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND drive_item_id = :id AND user_id = :userId AND deleted_at IS NULL
            RETURNING id
            """;
    private static final String MARK_SHARED = """
            UPDATE lms.drive_item SET is_shared = :shared, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SHARES = """
            SELECT id FROM lms.drive_item_share
             WHERE tenant_id = :tenantId AND drive_item_id = :id AND deleted_at IS NULL ORDER BY created_at, id
            """;

    private final JdbcClient jdbc;

    public DriveSharingRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<Owned> lock(UUID id) {
        return jdbc.sql(LOCK).param("id", id).query((rs, n) -> new Owned(rs.getObject("owner_user_id", UUID.class),
                rs.getInt("row_version"))).optional();
    }

    public List<UUID> sharedWith(UUID id) {
        return jdbc.sql(SHARED_WITH).param("id", id).query(UUID.class).list();
    }

    public void share(UUID shareId, UUID id, UUID userId, OffsetDateTime now, UUID actor) {
        jdbc.sql(SHARE).param("shareId", shareId).param("id", id).param("userId", userId).param("now", now)
                .param("actor", actor).update();
    }

    /** Ends the user's share of the item; answers the ended share's id. */
    public List<UUID> unshare(UUID id, UUID userId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(UNSHARE).param("id", id).param("userId", userId).param("now", now).param("actor", actor)
                .query(UUID.class).list();
    }

    public int markShared(UUID id, int expected, boolean shared, OffsetDateTime now, UUID actor) {
        return jdbc.sql(MARK_SHARED).param("id", id).param("expected", expected).param("shared", shared)
                .param("now", now).param("actor", actor).update();
    }

    /** The ids of the item's live shares, oldest first. */
    public List<UUID> shares(UUID id) {
        return jdbc.sql(SHARES).param("id", id).query(UUID.class).list();
    }
}
