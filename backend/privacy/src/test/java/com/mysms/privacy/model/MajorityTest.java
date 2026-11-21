package com.mysms.privacy.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** A person is an adult from the day their birth date plus the age of majority comes, a minor before. */
class MajorityTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    @Test
    void anAdultFromTheMajorityDateOn() {
        assertThat(Majority.of(LocalDate.of(2008, 11, 21), 18, TODAY))
                .isEqualTo(new Majority(Majority.ADULT, LocalDate.of(2026, 11, 21)));
        assertThat(Majority.of(LocalDate.of(1990, 1, 1), 18, TODAY).status()).isEqualTo(Majority.ADULT);
    }

    @Test
    void aMinorTheDayBefore() {
        assertThat(Majority.of(LocalDate.of(2008, 11, 22), 18, TODAY))
                .isEqualTo(new Majority(Majority.MINOR, LocalDate.of(2026, 11, 22)));
    }

    @Test
    void theAgeOfMajorityDecides() {
        LocalDate birthDate = LocalDate.of(2006, 6, 1);
        assertThat(Majority.of(birthDate, 18, TODAY)).isEqualTo(new Majority(Majority.ADULT, LocalDate.of(2024, 6, 1)));
        assertThat(Majority.of(birthDate, 21, TODAY)).isEqualTo(new Majority(Majority.MINOR, LocalDate.of(2027, 6, 1)));
    }

    @Test
    void aLeapDayBirthComesOfAgeOnTheLastDayOfFebruary() {
        assertThat(Majority.of(LocalDate.of(2008, 2, 29), 18, TODAY).majorityDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void unknownWithoutABirthDate() {
        assertThat(Majority.of(null, 18, TODAY)).isEqualTo(new Majority(Majority.UNKNOWN, null));
    }
}
