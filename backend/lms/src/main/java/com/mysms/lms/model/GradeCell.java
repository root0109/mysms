package com.mysms.lms.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One student's gradebook entry for one graded item (not a contract shape). {@code pointsPossible} is the entry's own
 * when it has one, otherwise the item's points.
 */
public record GradeCell(UUID contentItemId, UUID userId, UUID entryId, BigDecimal points, BigDecimal pointsPossible,
        String status, boolean isPublished) {

    public static final String GRADED = "graded";
    public static final String SUBMITTED = "submitted";
    public static final String EXCUSED = "excused";
    public static final String NOT_SUBMITTED = "not_submitted";
}
