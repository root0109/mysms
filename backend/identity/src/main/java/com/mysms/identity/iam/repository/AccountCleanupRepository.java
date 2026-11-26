package com.mysms.identity.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Retires the rows that belong to a deleted account (its devices and preferences) in the deleting transaction. */
@Repository
public class AccountCleanupRepository {

    private static final String RETIRE_DEVICES = """
            UPDATE iam.user_device
               SET is_active = false, deleted_at = :now, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :user AND deleted_at IS NULL
            """;
    private static final String RETIRE_PREFERENCES = """
            UPDATE iam.user_preference
               SET deleted_at = :now, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :user AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public AccountCleanupRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** @return the number of rows retired */
    public int retireOwnedRows(UUID user, UUID actor, OffsetDateTime now) {
        return jdbc.sql(RETIRE_DEVICES).param("user", user).param("actor", actor).param("now", now).update()
                + jdbc.sql(RETIRE_PREFERENCES).param("user", user).param("actor", actor).param("now", now).update();
    }
}
