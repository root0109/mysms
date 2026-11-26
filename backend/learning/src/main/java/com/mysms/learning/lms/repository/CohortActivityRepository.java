package com.mysms.learning.lms.repository;

import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The cohort-activity view of lms.content_item: a cohort activity is the cohort's own content item of kind quiz,
 * assignment or exercise outside any chapter. Rows are read and written through {@link ContentItemRepository}; this
 * class holds what only the view needs — its filter and order allowlists, the predicate that selects a cohort's
 * activities, and the check that one quiz, assignment or exercise is placed in a cohort once.
 */
@Repository
public class CohortActivityRepository {

    /** The item kinds a cohort activity may have, one per reference of the contract. */
    public static final List<String> KINDS = List.of("quiz", "assignment", "exercise");

    /** Fields clients may filter on (contract names → columns of lms.content_item). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("quizId", "r.quiz_id", ValueType.UUID),
            field("assignmentId", "r.assignment_id", ValueType.UUID),
            field("exerciseId", "r.exercise_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by (those {@link ContentItemRepository} pages by). */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final String OF_COHORT = """
            r.cohort_id = :activityCohortId AND r.chapter_id IS NULL
            AND r.item_kind IN ('quiz', 'assignment', 'exercise')
            """;
    private static final String TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item
                            WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND chapter_id IS NULL
                              AND id <> :id AND deleted_at IS NULL
                              AND (quiz_id = :quizId OR assignment_id = :assignmentId OR exercise_id = :exerciseId))
            """;

    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public CohortActivityRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The activities of one cohort. */
    public static SqlFragment ofCohort(UUID cohortId) {
        return new SqlFragment(OF_COHORT, Map.of("activityCohortId", cohortId));
    }

    /** Activities of the caller's institutions only (the institution of the cohort they belong to). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Whether the quiz, assignment or exercise is already an activity of the cohort (another row than {@code id}). */
    public boolean taken(UUID id, UUID cohortId, UUID quizId, UUID assignmentId, UUID exerciseId) {
        return Boolean.TRUE.equals(jdbc.sql(TAKEN).param("id", id).param("cohortId", cohortId)
                .param("quizId", quizId).param("assignmentId", assignmentId).param("exerciseId", exerciseId)
                .query(Boolean.class).single());
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
