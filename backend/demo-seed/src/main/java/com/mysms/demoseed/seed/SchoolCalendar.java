package com.mysms.demoseed.seed;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * The Indian school year the demo runs in (1 April to 31 March) around the given day, with its two terms; the same
 * day always gives the same calendar.
 */
public record SchoolCalendar(LocalDate today, LocalDate yearStart, LocalDate yearEnd) {

    public static SchoolCalendar around(LocalDate today) {
        int startYear = today.getMonthValue() >= Month.APRIL.getValue() ? today.getYear() : today.getYear() - 1;
        LocalDate start = LocalDate.of(startYear, Month.APRIL, 1);
        return new SchoolCalendar(today, start, start.plusYears(1).minusDays(1));
    }

    /** "2026-27". */
    public String yearName() {
        return yearStart.getYear() + "-" + String.format("%02d", (yearStart.getYear() + 1) % 100);
    }

    public LocalDate termOneEnd() {
        return LocalDate.of(yearStart.getYear(), Month.SEPTEMBER, 30);
    }

    /** School days (Monday to Saturday) of the given number of whole weeks before this week, oldest first. */
    public List<LocalDate> schoolDaysOfPastWeeks(int weeks) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weeks);
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate day = monday; day.isBefore(monday.plusWeeks(weeks)); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != DayOfWeek.SUNDAY && !day.isBefore(yearStart)) {
                days.add(day);
            }
        }
        return days;
    }
}
