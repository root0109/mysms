package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC writes of the learning-assignment submission workflows (FinalizeSubmission, GradeLearningSubmission)
 * and the group membership they check; each write is guarded by the row version the step read.
 */
@Repository
public class AssignmentSubmissionWorkflowRepository {

    private static final String ACTIVE_MEMBER = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group_member
                            WHERE tenant_id = :tenantId AND assignment_group_id = :groupId AND user_id = :userId
                              AND status = 'active' AND deleted_at IS NULL)
            """;
    private static final String FINALIZE = """
            UPDATE lms.assignment_submission
               SET is_final = true, finalized_at = :now, submitted_at = COALESCE(submitted_at, :now),
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String GRADE = """
            UPDATE lms.assignment_submission
               SET status = :status, evaluator_user_id = :actor, evaluator_comments = :comments, updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public AssignmentSubmissionWorkflowRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Whether the user is an active member of the assignment group. */
    public boolean activeMember(UUID groupId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(ACTIVE_MEMBER).param("groupId", groupId).param("userId", userId)
                .query(Boolean.class).single());
    }

    public int finalizeSubmission(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(FINALIZE).param("id", id).param("expected", expected).param("now", now).param("actor", actor)
                .update();
    }

    public int grade(UUID id, int expected, String status, String comments, OffsetDateTime now, UUID actor) {
        return jdbc.sql(GRADE).param("id", id).param("expected", expected).param("status", status)
                .param("comments", comments).param("now", now).param("actor", actor).update();
    }
}
