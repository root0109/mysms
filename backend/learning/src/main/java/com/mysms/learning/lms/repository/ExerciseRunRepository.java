package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain-JDBC access of SubmitExercise: the exercise's test cases and the submission with its test results. */
@Repository
public class ExerciseRunRepository {

    /** One test case of an exercise. */
    public record TestCase(UUID id, String input, String expectedOutput, boolean hidden) {
    }

    private static final String EXISTS = """
            SELECT EXISTS (SELECT 1 FROM lms.programming_exercise WHERE tenant_id = :tenantId AND id = :id
                              AND deleted_at IS NULL)
            """;
    private static final String TEST_CASES = """
            SELECT id, input, expected_output, is_hidden FROM lms.exercise_test_case
             WHERE tenant_id = :tenantId AND exercise_id = :exerciseId AND deleted_at IS NULL ORDER BY sort_order, id
            """;
    private static final String INSERT_SUBMISSION = """
            INSERT INTO lms.exercise_submission (id, tenant_id, exercise_id, user_id, code, is_full_code, status,
                   created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :exerciseId, :userId, :code, :fullCode, :status, :now, :userId, :now, :userId, 1)
            """;
    private static final String INSERT_RESULT = """
            INSERT INTO lms.exercise_test_result (id, tenant_id, exercise_submission_id, test_case_id, input,
                   expected_output, actual_output, status, created_at, created_by, updated_at, updated_by,
                   row_version)
            VALUES (:id, :tenantId, :submissionId, :testCaseId, :input, :expected, :actual, :status, :now, :actor,
                    :now, :actor, 1)
            """;

    private final JdbcClient jdbc;

    public ExerciseRunRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public boolean exists(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS).param("id", id).query(Boolean.class).single());
    }

    public List<TestCase> testCases(UUID exerciseId) {
        return jdbc.sql(TEST_CASES).param("exerciseId", exerciseId).query((rs, n) -> new TestCase(
                rs.getObject("id", UUID.class), rs.getString("input"), rs.getString("expected_output"),
                rs.getBoolean("is_hidden"))).list();
    }

    public void insertSubmission(UUID id, UUID exerciseId, UUID userId, String code, boolean fullCode, String status,
            OffsetDateTime now) {
        jdbc.sql(INSERT_SUBMISSION).param("id", id).param("exerciseId", exerciseId).param("userId", userId)
                .param("code", code).param("fullCode", fullCode).param("status", status).param("now", now).update();
    }

    /** One result; a hidden test keeps neither its input nor its expected output on the result. */
    public void insertResult(UUID id, UUID submissionId, TestCase test, String actual, String status,
            OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_RESULT).param("id", id).param("submissionId", submissionId).param("testCaseId", test.id())
                .param("input", test.hidden() ? null : test.input())
                .param("expected", test.hidden() ? null : test.expectedOutput()).param("actual", actual)
                .param("status", status).param("now", now).param("actor", actor).update();
    }
}
