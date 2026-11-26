package com.mysms.learning.lms.model;

import java.util.UUID;

/**
 * Where a user stands in a cohort before a membership change (not a contract shape): the live placing row, whether it
 * is active, and its row version for the guarded update.
 */
public record Placement(UUID id, boolean isActive, int rowVersion) {
}
