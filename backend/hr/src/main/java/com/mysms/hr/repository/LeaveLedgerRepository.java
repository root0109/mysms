package com.mysms.hr.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.hr.config.HrDatabaseConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of the leave ledger (hr.ApproveLeave, hr.GetStaffLeaveBalance): how many days an application
 * takes, the balance it is counted against, and a staff member's position per leave type. Every statement names the
 * tenant; row-level security is the backstop.
 */
@Repository
public class LeaveLedgerRepository {

    /**
     * The days the application aliased {@code a} takes: calendar days from start to end, both counted, halved for a
     * half day. Days are UTC days, exact for the clients' convention of sending UTC midnights; weekends and holidays
     * count, as hr has no calendar of them.
     */
    private static final String LEAVE_DAYS = """
            ((((a.to_at AT TIME ZONE 'UTC')::date - (a.from_at AT TIME ZONE 'UTC')::date) + 1)
             * CASE WHEN a.day_part = 'full_day' THEN 1 ELSE 0.5 END)
            """;

    private static final String DAYS_OF = "SELECT " + LEAVE_DAYS + """
              FROM hr.leave_application a
             WHERE a.tenant_id = :tenantId AND a.id = :id AND a.deleted_at IS NULL
            """;

    private static final String APPLICANT = """
            SELECT s.user_id, s.person_id FROM hr.staff s
             WHERE s.tenant_id = :tenantId AND s.id = :staffId AND s.deleted_at IS NULL
            """;

    private static final String LOCK_APPLICATION = """
            SELECT a.id FROM hr.leave_application a
             WHERE a.tenant_id = :tenantId AND a.id = :id AND a.deleted_at IS NULL
               FOR UPDATE
            """;

    /** Whether the institution offers leave types for the year: then an application has to name one. */
    private static final String HAS_LEAVE_TYPES = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_type t
                            WHERE t.tenant_id = :tenantId AND t.institution_id = :institutionId
                              AND t.deleted_at IS NULL AND t.is_active
                              AND (t.academic_year_id IS NULL OR t.academic_year_id = :academicYearId))
            """;

    /** Whether the leave type is one the institution offers for the year. */
    private static final String OFFERS_LEAVE_TYPE = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_type t
                            WHERE t.tenant_id = :tenantId AND t.id = :leaveTypeId
                              AND t.institution_id = :institutionId AND t.deleted_at IS NULL AND t.is_active
                              AND (t.academic_year_id IS NULL OR t.academic_year_id = :academicYearId))
            """;

    private static final String STAFF_OF_INSTITUTION = """
            SELECT EXISTS (SELECT 1 FROM hr.staff s
                            WHERE s.tenant_id = :tenantId AND s.id = :staffId
                              AND s.institution_id = :institutionId AND s.deleted_at IS NULL)
            """;

    /** A balance row for the staff member, leave type and year, opened with the type's yearly allowance. */
    private static final String OPEN_BALANCE = """
            INSERT INTO hr.leave_balance (id, tenant_id, staff_id, leave_type_id, academic_year_id, entitled_days,
                                          created_at, created_by, updated_at, updated_by, row_version)
            SELECT :id, :tenantId, :staffId, t.id, :academicYearId, t.max_days_per_year, :now, :actor, :now, :actor, 1
              FROM hr.leave_type t
             WHERE t.tenant_id = :tenantId AND t.id = :leaveTypeId AND t.deleted_at IS NULL
               AND t.institution_id = :institutionId AND t.is_active
               AND (t.academic_year_id IS NULL OR t.academic_year_id = :academicYearId)
            ON CONFLICT DO NOTHING
            """;

    private static final String LOCK_BALANCE = """
            SELECT b.id, b.row_version, b.entitled_days + b.carried_forward_days - b.used_days AS available
              FROM hr.leave_balance b
             WHERE b.tenant_id = :tenantId AND b.staff_id = :staffId AND b.leave_type_id = :leaveTypeId
               AND b.academic_year_id = :academicYearId AND b.deleted_at IS NULL
               FOR UPDATE
            """;

    private static final String USE_DAYS = """
            UPDATE hr.leave_balance
               SET used_days = used_days + :days,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected
            """;

    /**
     * Gives the days of a cancelled approved leave back. A balance an administrator has since lowered below them is
     * left at zero, never negative.
     */
    private static final String RETURN_DAYS = """
            UPDATE hr.leave_balance
               SET used_days = GREATEST(used_days - :days, 0),
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected
            """;

    /**
     * A staff member's position per active leave type of their institution for one academic year: the allowance (the
     * balance row's, with what was carried forward; the type's yearly allowance while no row exists), what approved
     * leave has used, and what pending applications ask for.
     */
    private static final String POSITIONS = """
            SELECT t.id AS leave_type_id, t.sort_order, t.name,
                   COALESCE(b.entitled_days + b.carried_forward_days, t.max_days_per_year) AS entitled,
                   COALESCE(b.used_days, 0) AS taken,
                   COALESCE((SELECT sum(""" + LEAVE_DAYS + """
                               )
                               FROM hr.leave_application a
                              WHERE a.tenant_id = :tenantId AND a.staff_id = :staffId AND a.leave_type_id = t.id
                                AND a.academic_year_id = :academicYearId AND a.status = 'pending'
                                AND a.deleted_at IS NULL), 0) AS pending
              FROM hr.leave_type t
              LEFT JOIN hr.leave_balance b
                     ON b.tenant_id = :tenantId AND b.staff_id = :staffId AND b.leave_type_id = t.id
                    AND b.academic_year_id = :academicYearId AND b.deleted_at IS NULL
             WHERE t.tenant_id = :tenantId AND t.institution_id = :institutionId AND t.deleted_at IS NULL
               AND t.is_active AND (t.academic_year_id IS NULL OR t.academic_year_id = :academicYearId)
            """;

    /** The order of a leave position: the leave types as they are listed (sort order, then name). */
    public static final SortSpec POSITION_ORDER = SortSpec.parse(null, Map.of(
            "sortOrder", new FieldSpec("sortOrder", "t.sort_order", ValueType.INTEGER),
            "name", new FieldSpec("name", "t.name", ValueType.STRING)), "sortOrder,name", "t.id");

    /** A balance row found and locked for the rest of the transaction. */
    public record LockedBalance(UUID id, int rowVersion, BigDecimal available, boolean opened) {
    }

    /** Who an application is for: the staff member's login account and person, where they have them. */
    public record Applicant(Optional<UUID> userId, Optional<UUID> personId) {
    }

    /** One line of a staff member's leave position, with the leave type's sort keys. */
    public record Position(UUID leaveTypeId, long sortOrder, String name, BigDecimal entitled, BigDecimal taken,
            BigDecimal pending) {
    }

    private final JdbcClient jdbc;

    public LeaveLedgerRepository(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The days the application takes. */
    public BigDecimal daysOf(UUID applicationId) {
        return jdbc.sql(DAYS_OF).param("id", applicationId).query(BigDecimal.class).single();
    }

    /** Who the staff member is; nobody when the staff member does not exist. */
    public Applicant applicant(UUID staffId) {
        return jdbc.sql(APPLICANT).param("staffId", staffId)
                .query((rs, row) -> new Applicant(Optional.ofNullable(rs.getObject("user_id", UUID.class)),
                        Optional.ofNullable(rs.getObject("person_id", UUID.class))))
                .optional().orElse(new Applicant(Optional.empty(), Optional.empty()));
    }

    /** Locks the application for the rest of the transaction, so one decision at a time is taken on it. */
    public boolean lockApplication(UUID applicationId) {
        return jdbc.sql(LOCK_APPLICATION).param("id", applicationId).query(UUID.class).optional().isPresent();
    }

    public boolean hasLeaveTypes(UUID institutionId, UUID academicYearId) {
        return Boolean.TRUE.equals(jdbc.sql(HAS_LEAVE_TYPES).param("institutionId", institutionId)
                .param("academicYearId", academicYearId).query(Boolean.class).single());
    }

    public boolean offersLeaveType(UUID leaveTypeId, UUID institutionId, UUID academicYearId) {
        return Boolean.TRUE.equals(jdbc.sql(OFFERS_LEAVE_TYPE).param("leaveTypeId", leaveTypeId)
                .param("institutionId", institutionId).param("academicYearId", academicYearId)
                .query(Boolean.class).single());
    }

    public boolean staffOfInstitution(UUID staffId, UUID institutionId) {
        return Boolean.TRUE.equals(jdbc.sql(STAFF_OF_INSTITUTION).param("staffId", staffId)
                .param("institutionId", institutionId).query(Boolean.class).single());
    }

    /**
     * The balance the leave is counted against, locked until the transaction ends; opened with the leave type's
     * yearly allowance when the staff member has none yet for that type and year — only for a type the institution
     * offers for the year. Empty when there is neither a balance nor such a type. The answer says whether this call
     * opened the row (for its audit record).
     */
    public Optional<LockedBalance> lockBalance(UUID balanceId, UUID staffId, UUID leaveTypeId, UUID institutionId,
            UUID academicYearId, OffsetDateTime now, UUID actor) {
        boolean opened = jdbc.sql(OPEN_BALANCE).param("id", balanceId).param("staffId", staffId)
                .param("leaveTypeId", leaveTypeId).param("institutionId", institutionId)
                .param("academicYearId", academicYearId).param("now", now).param("actor", actor).update() == 1;
        return jdbc.sql(LOCK_BALANCE).param("staffId", staffId).param("leaveTypeId", leaveTypeId)
                .param("academicYearId", academicYearId)
                .query((rs, row) -> new LockedBalance(rs.getObject("id", UUID.class), rs.getInt("row_version"),
                        rs.getBigDecimal("available"), opened))
                .optional();
    }

    /**
     * The balance an approved leave was counted against, locked until the transaction ends; empty when there is none
     * (a school without leave types keeps no balances).
     */
    public Optional<LockedBalance> lockCountedBalance(UUID staffId, UUID leaveTypeId, UUID academicYearId) {
        return jdbc.sql(LOCK_BALANCE).param("staffId", staffId).param("leaveTypeId", leaveTypeId)
                .param("academicYearId", academicYearId)
                .query((rs, row) -> new LockedBalance(rs.getObject("id", UUID.class), rs.getInt("row_version"),
                        rs.getBigDecimal("available"), false))
                .optional();
    }

    /** Gives the days back to the (locked) balance; 0 rows means it changed since it was locked, which cannot be. */
    public int returnDays(LockedBalance balance, BigDecimal days, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RETURN_DAYS).param("id", balance.id()).param("expected", balance.rowVersion())
                .param("days", days).param("now", now).param("actor", actor).update();
    }

    /** Counts the days against the (locked) balance; 0 rows means it changed since it was locked, which cannot be. */
    public int useDays(LockedBalance balance, BigDecimal days, OffsetDateTime now, UUID actor) {
        return jdbc.sql(USE_DAYS).param("id", balance.id()).param("expected", balance.rowVersion())
                .param("days", days).param("now", now).param("actor", actor).update();
    }

    /**
     * One page of the staff member's position per leave type for the year, in {@link #POSITION_ORDER}.
     *
     * @param page a request sorted by {@link #POSITION_ORDER}
     */
    public Page<Position> positions(UUID staffId, UUID institutionId, UUID academicYearId, PageRequest page) {
        SqlFragment after = page.keysetPredicate();
        String sql = POSITIONS + " AND (" + after.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<Position> rows = jdbc.sql(sql).param("staffId", staffId).param("institutionId", institutionId)
                .param("academicYearId", academicYearId).params(after.params()).params(page.limitParams())
                .query((rs, row) -> new Position(rs.getObject("leave_type_id", UUID.class), rs.getLong("sort_order"),
                        rs.getString("name"), rs.getBigDecimal("entitled"), rs.getBigDecimal("taken"),
                        rs.getBigDecimal("pending")))
                .list();
        return page.toPage(rows, position -> List.of(position.sortOrder(), position.name()), Position::leaveTypeId);
    }
}
