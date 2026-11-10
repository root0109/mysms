package com.mysms.attendance.business;

import com.mysms.attendance.api.model.AttendanceSummary;
import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.repository.AttendanceSummaryRepository;
import com.mysms.attendance.repository.AttendanceSummaryRepository.Counts;
import com.mysms.attendance.repository.AttendanceSummaryRepository.Grouping;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * attendance.SummarizeAttendance: how often students were present, absent, late and excused in one institution over
 * a date range of at most 366 days — per student, per class section or per day — from the marks of completed sheets.
 * The attendance percentage is (present + late) of all marks, with two decimals. An institution outside the
 * caller's scope has no groups; nothing here is written.
 */
@Service
public class AttendanceSummaryService extends AppTransactionManager {

    static final String PERMISSION_SUMMARIZE = "attendance.attendance_record.summarize_attendance";
    /** Longest range one call may cover: a leap school year (also the bound of the partitioned lists). */
    static final int MAX_DAYS = 366;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AttendanceSummaryRepository repository;
    private final Clock clock;

    public AttendanceSummaryService(AttendanceSummaryRepository repository,
            @Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AttendanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.clock = clock;
    }

    public Page<AttendanceSummary> summarizeAttendance(UUID institutionId, LocalDate fromDate, LocalDate toDate,
            String groupBy, UUID studentId, UUID classSectionId, Integer pageNumber, Integer pageSize, String cursor) {
        Grouping grouping = Arrays.stream(Grouping.values()).filter(g -> g.wireValue().equals(groupBy)).findFirst()
                .orElseThrow(() -> new InvalidArgumentException(
                        "groupBy must be one of student, class_section, date."));
        if (toDate.isBefore(fromDate)) {
            throw new InvalidArgumentException("toDate must not be before fromDate.");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) >= MAX_DAYS) {
            throw new InvalidArgumentException("The date range must not exceed " + MAX_DAYS + " days.");
        }
        // the cursor is bound to every parameter that shapes the result, so it cannot continue a different summary
        String query = String.join("|", grouping.wireValue(), institutionId.toString(), fromDate.toString(),
                toDate.toString(), String.valueOf(studentId), String.valueOf(classSectionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, grouping.sort(), query, clock);
        if (!DataScope.current(PERMISSION_SUMMARIZE).canRead(Optional.of(institutionId))) {
            return new Page<>(List.of(), page.page(), page.size(), "");
        }
        return doInReadTransaction(() -> {
            Page<Counts> groups = repository.summarize(grouping, institutionId, fromDate, toDate, studentId,
                    classSectionId, page);
            return groups.map(counts -> toDto(grouping, counts));
        });
    }

    private static AttendanceSummary toDto(Grouping grouping, Counts counts) {
        int total = counts.present() + counts.absent() + counts.late() + counts.excused();
        // a group exists only through its marks, so total is never zero
        String percent = BigDecimal.valueOf(counts.present() + (long) counts.late()).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).toPlainString();
        AttendanceSummary summary = new AttendanceSummary(counts.present(), counts.absent(), counts.late(),
                counts.excused(), percent);
        return switch (grouping) {
            case STUDENT -> summary.studentId(counts.groupId());
            case CLASS_SECTION -> summary.classSectionId(counts.groupId());
            case DATE -> summary.date(counts.groupDate());
        };
    }
}
