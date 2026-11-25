package com.mysms.demoseed.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.demoseed.client.Filter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SeedDataTest {

    @Test
    void rolesSelectPermissionsByPatternWithExclusionsWinning() {
        assertThat(DemoRole.TEACHER.grants("attendance.attendance_sheet.record_attendance")).isTrue();
        assertThat(DemoRole.TEACHER.grants("assessment.homework.create")).isTrue();
        assertThat(DemoRole.TEACHER.grants("assessment.homework.approve_homework")).isFalse();
        assertThat(DemoRole.TEACHER.grants("iam.role.read")).isFalse();
        assertThat(DemoRole.PRINCIPAL.grants("sis.student.read")).isTrue();
        assertThat(DemoRole.PRINCIPAL.grants("iam.role.read")).isFalse();
        assertThat(DemoRole.STUDENT.code()).as("the role sis grants to student portal accounts").isEqualTo("student");
        assertThat(DemoRole.GUARDIAN.code()).isEqualTo("guardian");
    }

    @Test
    void theSchoolYearRunsFromAprilToMarchAroundToday() {
        SchoolCalendar autumn = SchoolCalendar.around(LocalDate.of(2026, 11, 18));
        assertThat(autumn.yearName()).isEqualTo("2026-27");
        assertThat(autumn.yearStart()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(autumn.yearEnd()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(autumn.termOneEnd()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(SchoolCalendar.around(LocalDate.of(2027, 2, 1)).yearName()).isEqualTo("2026-27");
        assertThat(autumn.schoolDaysOfPastWeeks(2)).hasSize(12).allMatch(d -> d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .allMatch(d -> d.isBefore(LocalDate.of(2026, 11, 16)));
        assertThat(SchoolCalendar.around(LocalDate.of(2026, 4, 2)).schoolDaysOfPastWeeks(1)).isEmpty();
    }

    @Test
    void namesAreReproducibleAndAddressesStayOnTheReservedDomain() {
        DemoPeople.Name first = DemoPeople.name(new Random(7));
        assertThat(DemoPeople.name(new Random(7))).isEqualTo(first);
        assertThat(first.email("SV2026-0001")).endsWith("@" + DemoPeople.DOMAIN).matches("[a-z0-9.]+@.*");
        assertThat(DemoPeople.name(new Random(7), "D'Souza", "female").email("x")).startsWith(
                DemoPeople.name(new Random(7), "D'Souza", "female").given().toLowerCase() + ".dsouza.x@");
    }

    @Test
    void filtersQuoteTheirValues() {
        assertThat(Filter.and(Filter.eq("code", "A\"B"), Filter.eq("id", 1)))
                .isEqualTo("code = \"A\\\"B\" AND id = \"1\"");
    }
}
