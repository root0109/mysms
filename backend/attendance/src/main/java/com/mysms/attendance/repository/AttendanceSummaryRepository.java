package com.mysms.attendance.repository;

import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * attendance.SummarizeAttendance: counts of the marks on completed sheets of one institution in a date range, per
 * student, per class section or per day. A read model over attendance.attendance_record, its sheet and the sheet's
 * register (all of this module's schema); marks of sheets still being taken, cancelled or deleted do not count. The
 * date range bounds the partitions read.
 */
@Repository
public class AttendanceSummaryRepository {

    /** Tie-breaker of the day grouping: days are unique, so the keyset needs no second key. */
    private static final UUID NO_TIE = new UUID(0L, 0L);

    private static final String COUNTS = """
                   CAST(sum(CASE WHEN r.status = 'present' THEN 1 ELSE 0 END) AS integer) AS present,
                   CAST(sum(CASE WHEN r.status = 'absent' THEN 1 ELSE 0 END) AS integer) AS absent,
                   CAST(sum(CASE WHEN r.status = 'late' THEN 1 ELSE 0 END) AS integer) AS late,
                   CAST(sum(CASE WHEN r.status = 'excused' THEN 1 ELSE 0 END) AS integer) AS excused
              FROM attendance.attendance_record r
              JOIN attendance.attendance_sheet s ON s.tenant_id = :tenantId AND s.id = r.sheet_id
                   AND s.deleted_at IS NULL AND s.status = 'done'
              JOIN attendance.attendance_register g ON g.tenant_id = :tenantId AND g.id = s.register_id
                   AND g.deleted_at IS NULL
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
               AND r.attendance_date >= :fromDate AND r.attendance_date <= :toDate
               AND g.institution_id = :institutionId
            """;
    private static final String BY_STUDENT = "SELECT r.student_id AS group_id, CAST(NULL AS date) AS group_date,"
            + COUNTS;
    private static final String BY_CLASS_SECTION =
            "SELECT g.class_section_id AS group_id, CAST(NULL AS date) AS group_date," + COUNTS
                    + " AND g.class_section_id IS NOT NULL";
    private static final String BY_DATE = "SELECT CAST(NULL AS uuid) AS group_id, r.attendance_date AS group_date,"
            + COUNTS;
    private static final String ONE_STUDENT = " AND r.student_id = :studentId";
    private static final String ONE_CLASS_SECTION = " AND g.class_section_id = :classSectionId";

    /** What the counts are grouped by, with the column that is both the group and the paging key. */
    public enum Grouping {
        STUDENT("student", BY_STUDENT, "r.student_id", ValueType.UUID, "r.student_id"),
        CLASS_SECTION("class_section", BY_CLASS_SECTION, "g.class_section_id", ValueType.UUID, "g.class_section_id"),
        DATE("date", BY_DATE, "r.attendance_date", ValueType.DATE, "CAST('" + NO_TIE + "' AS uuid)");

        private final String wireValue;
        private final String select;
        private final String column;
        private final SortSpec sort;

        Grouping(String wireValue, String select, String column, ValueType type, String tieBreaker) {
            this.wireValue = wireValue;
            this.select = select;
            this.column = column;
            this.sort = SortSpec.parse(null, Map.of("group", new FieldSpec("group", column, type)), "group",
                    tieBreaker);
        }

        /** The contract's groupBy value. */
        public String wireValue() {
            return wireValue;
        }

        /** The order of the groups, for the page request and its token. */
        public SortSpec sort() {
            return sort;
        }
    }

    /**
     * One group's counts (a projection, JAVA-CODING-RULES §5.4).
     *
     * @param groupId   the student or class section, when grouped by one
     * @param groupDate the day, when grouped by date
     */
    public record Counts(UUID groupId, LocalDate groupDate, int present, int absent, int late, int excused) {
    }

    private final JdbcClient jdbc;

    public AttendanceSummaryRepository(@Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /**
     * One page of groups in key order.
     *
     * @param studentId      only this student's marks, or null
     * @param classSectionId only marks in this class section's registers, or null
     */
    public Page<Counts> summarize(Grouping grouping, UUID institutionId, LocalDate fromDate, LocalDate toDate,
            UUID studentId, UUID classSectionId, PageRequest page) {
        SqlFragment after = page.keysetPredicate();
        String sql = grouping.select
                + (studentId == null ? "" : ONE_STUDENT)
                + (classSectionId == null ? "" : ONE_CLASS_SECTION)
                + " AND (" + after.sql() + ") GROUP BY " + grouping.column + " ORDER BY " + grouping.column
                + page.limitClause();
        JdbcClient.StatementSpec statement = jdbc.sql(sql).params(after.params())
                .param("institutionId", institutionId)
                .param("fromDate", fromDate)
                .param("toDate", toDate)
                .params(page.limitParams());
        if (studentId != null) {
            statement = statement.param("studentId", studentId);
        }
        if (classSectionId != null) {
            statement = statement.param("classSectionId", classSectionId);
        }
        List<Counts> rows = statement.query((rs, row) -> new Counts(rs.getObject("group_id", UUID.class),
                rs.getObject("group_date", LocalDate.class), rs.getInt("present"), rs.getInt("absent"),
                rs.getInt("late"), rs.getInt("excused"))).list();
        return page.toPage(rows,
                counts -> List.of(grouping == Grouping.DATE ? counts.groupDate() : counts.groupId()),
                counts -> grouping == Grouping.DATE ? NO_TIE : counts.groupId());
    }
}
