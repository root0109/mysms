package com.mysms.finance.model;

import java.util.UUID;

/**
 * A student dues are generated for, with the placement the dues belong to (the school-year enrolment or the course
 * registration that brought the student into the run, when there is one). Not part of the contract.
 */
public record DueTarget(UUID studentId, UUID studentEnrollmentId, UUID programEnrollmentId) {

    public static DueTarget student(UUID studentId) {
        return new DueTarget(studentId, null, null);
    }
}
