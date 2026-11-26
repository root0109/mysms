package com.mysms.platform.org.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TermPlanTest {

    private static final LocalDate START = LocalDate.of(2027, 4, 1);
    private static final LocalDate END = LocalDate.of(2028, 3, 31);

    @Test
    void quartersCoverTheYearWithoutGapsTheRemainderGoingToTheEarlierOnes() {
        List<TermPlan.Term> quarters = TermPlan.of("four_quarters", START, END).orElseThrow();
        assertThat(quarters).extracting(TermPlan.Term::name)
                .containsExactly("Quarter 1", "Quarter 2", "Quarter 3", "Quarter 4");
        // 366 days (2028 is a leap year): 92, 92, 91, 91
        assertThat(quarters).extracting(TermPlan.Term::startDate).containsExactly(LocalDate.of(2027, 4, 1),
                LocalDate.of(2027, 7, 2), LocalDate.of(2027, 10, 2), LocalDate.of(2028, 1, 1));
        assertThat(quarters).extracting(TermPlan.Term::endDate).containsExactly(LocalDate.of(2027, 7, 1),
                LocalDate.of(2027, 10, 1), LocalDate.of(2027, 12, 31), END);
        assertThat(quarters).extracting(TermPlan.Term::sequence).containsExactly(1, 2, 3, 4);
        assertThat(quarters).allSatisfy(term -> assertThat(term.children()).isEmpty());
    }

    @Test
    void quartersNestInsideTheirSemesters() {
        List<TermPlan.Term> semesters = TermPlan.of("two_semesters_final", START, END).orElseThrow();
        assertThat(semesters).extracting(TermPlan.Term::name).containsExactly("Semester 1", "Semester 2");
        TermPlan.Term second = semesters.get(1);
        assertThat(second.children()).extracting(TermPlan.Term::name)
                .containsExactly("Quarter 3", "Final examinations");
        assertThat(second.children().get(0).startDate()).isEqualTo(second.startDate());
        assertThat(second.children().get(1).endDate()).isEqualTo(END);
        assertThat(second.children().get(0).endDate().plusDays(1)).isEqualTo(second.children().get(1).startDate());
        assertThat(TermPlan.of("two_semesters_quarterly", START, END).orElseThrow().get(0).children())
                .extracting(TermPlan.Term::name).containsExactly("Quarter 1", "Quarter 2");
    }

    @Test
    void theOtherStructuresAndTheYearsWithoutOne() {
        assertThat(TermPlan.of("two_semesters", START, END).orElseThrow()).hasSize(2);
        assertThat(TermPlan.of("three_semesters", START, END).orElseThrow()).extracting(TermPlan.Term::name)
                .containsExactly("Semester 1", "Semester 2", "Semester 3");
        List<TermPlan.Term> whole = TermPlan.of("final_year", START, END).orElseThrow();
        assertThat(whole).singleElement().satisfies(term -> {
            assertThat(term.startDate()).isEqualTo(START);
            assertThat(term.endDate()).isEqualTo(END);
        });
        assertThat(TermPlan.of("other", START, END)).isEmpty();
        assertThat(TermPlan.of(null, START, END)).isEmpty();
    }

    @Test
    void aYearMustGiveEveryTermADay() {
        LocalDate threeDays = START.plusDays(2);
        assertThat(TermPlan.fits("four_quarters", START, threeDays)).isFalse();
        assertThat(TermPlan.fits("four_quarters", START, START.plusDays(3))).isTrue();
        assertThat(TermPlan.fits("two_semesters_quarterly", START, threeDays)).isFalse();
        assertThat(TermPlan.fits("final_year", START, START)).isTrue();
    }
}
