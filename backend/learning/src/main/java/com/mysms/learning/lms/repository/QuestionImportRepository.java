package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain-JDBC access of ImportQuestions: the target pool and its new items. */
@Repository
public class QuestionImportRepository {

    private static final String POOL_EXISTS = """
            SELECT EXISTS (SELECT 1 FROM lms.question_pool WHERE tenant_id = :tenantId AND id = :id
                              AND deleted_at IS NULL)
            """;
    private static final String ADD_TO_POOL = """
            INSERT INTO lms.question_pool_item (id, tenant_id, question_pool_id, question_id, created_at, created_by,
                   updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :poolId, :questionId, :now, :actor, :now, :actor, 1)
            """;

    private final JdbcClient jdbc;

    public QuestionImportRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public boolean poolExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(POOL_EXISTS).param("id", id).query(Boolean.class).single());
    }

    public void addToPool(UUID id, UUID poolId, UUID questionId, OffsetDateTime now, UUID actor) {
        jdbc.sql(ADD_TO_POOL).param("id", id).param("poolId", poolId).param("questionId", questionId)
                .param("now", now).param("actor", actor).update();
    }
}
