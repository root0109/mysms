package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.SqlFragment;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC statements of a cohort's timetable as a whole: which rows belong to a cohort or a
 * template, whether a template exists, and the statements that replace a cohort's timetable when a template is
 * applied. Rows themselves are read and written through {@link TimetableEntryRepository} and
 * {@link TimetableLegendRepository}; every statement filters the caller's tenant itself.
 */
@Repository
public class CohortTimetableRepository {

    private static final String TEMPLATE_EXISTS = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_template
                            WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String CLEAR_ENTRIES = """
            UPDATE lms.timetable_entry
               SET deleted_at = :now, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND deleted_at IS NULL
            """;
    private static final String CLEAR_LEGENDS = """
            UPDATE lms.timetable_legend
               SET deleted_at = :now, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND deleted_at IS NULL
            """;
    private static final String SET_TEMPLATE = """
            UPDATE lms.cohort
               SET timetable_template_id = :templateId, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :cohortId AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public CohortTimetableRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The rows of one cohort's timetable (entries or legends: both carry cohort_id). */
    public static SqlFragment ofCohort(UUID cohortId) {
        return new SqlFragment("r.cohort_id = :timetableCohortId", Map.of("timetableCohortId", cohortId));
    }

    /** The rows of one template. */
    public static SqlFragment ofTemplate(UUID templateId) {
        return new SqlFragment("r.timetable_template_id = :timetableTemplateId",
                Map.of("timetableTemplateId", templateId));
    }

    public boolean templateExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(TEMPLATE_EXISTS).param("id", id).query(Boolean.class).single());
    }

    /** Removes the cohort's current entries and legends (soft delete); answers how many entries went. */
    public int clear(UUID cohortId, OffsetDateTime now, UUID actor) {
        int entries = jdbc.sql(CLEAR_ENTRIES).param("cohortId", cohortId).param("now", now).param("actor", actor)
                .update();
        jdbc.sql(CLEAR_LEGENDS).param("cohortId", cohortId).param("now", now).param("actor", actor).update();
        return entries;
    }

    /** Records the template the cohort's timetable now follows, under the cohort's row_version guard. */
    public int setTemplate(UUID cohortId, int expected, UUID templateId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_TEMPLATE).param("cohortId", cohortId).param("expected", expected)
                .param("templateId", templateId).param("now", now).param("actor", actor).update();
    }
}
