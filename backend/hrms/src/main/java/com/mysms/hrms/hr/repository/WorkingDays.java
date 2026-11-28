package com.mysms.hrms.hr.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The working-day rule of hrms, one implementation for leave (LeaveLedgerRepository), pay (PayslipRunRepository) and
 * staff attendance (LeaveAttendanceRepository): a day is a working day of an institution unless it falls on a weekend
 * day ({@code mysms.hr.leave.weekend-days}) or within a live holiday of hr.holiday_record that names the institution or
 * the whole tenant (kept from org's holiday events). <p>{@link #IS_WORKING_DAY} is a predicate over a relation the
 * caller builds and aliases {@code wd}, with the columns {@code day} (date) and {@code institution_id}; every statement
 * that uses it binds {@link #PARAMETER} to {@link #weekendDays()}.
 */
@Component
public class WorkingDays {

    /** The named parameter that carries the weekend days into {@link #IS_WORKING_DAY}. */
    public static final String PARAMETER = "weekendDays";

    /** Whether {@code wd.day} is a working day of institution {@code wd.institution_id}. */
    public static final String IS_WORKING_DAY = """
            EXTRACT(ISODOW FROM wd.day)::int <> ALL (string_to_array(:weekendDays, ',')::int[])
            AND NOT EXISTS (SELECT 1 FROM hr.holiday_record h
                             WHERE h.tenant_id = :tenantId AND NOT h.is_deleted
                               AND (h.institution_id IS NULL OR h.institution_id = wd.institution_id)
                               AND wd.day BETWEEN h.start_date AND h.end_date)
            """;

    private final String weekendDays;

    public WorkingDays(@Value("${mysms.hr.leave.weekend-days}") List<DayOfWeek> weekendDays) {
        this.weekendDays = weekendDays.stream().map(day -> String.valueOf(day.getValue())).distinct().sorted()
                .collect(Collectors.joining(","));
    }

    /** The weekend days as {@link #IS_WORKING_DAY} takes them: ISO day numbers (Monday = 1), comma-separated. */
    public String weekendDays() {
        return weekendDays;
    }
}
