package com.mysms.lms.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A graded content item of a cohort as its gradebook reads it (not a contract shape): its column's title and kind, its
 * category, the points it is out of, its weight, and when it was
 * created (the order a report pages items in).
 */
public record GradedItem(UUID contentItemId, String title, String itemKind, UUID gradingCategoryId, BigDecimal points,
        BigDecimal weightPercent, OffsetDateTime dueAt, OffsetDateTime createdAt) {
}
