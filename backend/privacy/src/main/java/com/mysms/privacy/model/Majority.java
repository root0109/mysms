package com.mysms.privacy.model;

import java.time.LocalDate;

/**
 * Whether a person is a minor or an adult on a given day, from their date of birth and the age of majority: adult once
 * the birth date plus that many years has come, minor before, unknown (treated as a minor) without a birth date.
 *
 * @param status        minor, adult or unknown (privacy.subject.status)
 * @param majorityDate  the day the person becomes an adult; null when the birth date is unknown
 */
public record Majority(String status, LocalDate majorityDate) {

    public static final String MINOR = "minor";
    public static final String ADULT = "adult";
    public static final String UNKNOWN = "unknown";

    /**
     * @param birthDate      the person's date of birth, or null when unknown
     * @param ageOfMajority  the age, in years, at which a person becomes an adult
     * @param today          the day the status is for
     */
    public static Majority of(LocalDate birthDate, int ageOfMajority, LocalDate today) {
        if (birthDate == null) {
            return new Majority(UNKNOWN, null);
        }
        LocalDate majorityDate = birthDate.plusYears(ageOfMajority);
        return new Majority(majorityDate.isAfter(today) ? MINOR : ADULT, majorityDate);
    }
}
