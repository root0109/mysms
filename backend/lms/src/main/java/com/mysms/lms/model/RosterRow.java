package com.mysms.lms.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One member of a cohort as the roster reads them (not a contract shape): a student's lms.cohort_enrollment or a staff
 * member's lms.cohort_teacher, with the row that places them.
 *
 * @param id        the placing row (enrollment or teacher row)
 * @param roleCode  {@code student} or {@code teacher}
 * @param createdAt when they were placed (the roster's order)
 */
public record RosterRow(UUID id, UUID userId, String roleCode, boolean isActive, OffsetDateTime createdAt) {

    public static final String STUDENT = "student";
    public static final String TEACHER = "teacher";
}
