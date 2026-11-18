package com.mysms.lms.model;

import java.math.BigDecimal;
import java.util.UUID;

/** A student's progress in one course of their cohort (not a contract shape); 0 when not enrolled in it. */
public record CourseProgressRow(UUID userId, UUID courseId, BigDecimal progressPercent) {
}
