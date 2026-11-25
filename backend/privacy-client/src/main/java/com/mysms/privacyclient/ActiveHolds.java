package com.mysms.privacyclient;

import java.util.List;
import java.util.UUID;

/**
 * The active legal holds that cover one subject's data in one module: what an erasure or a retention sweep must keep.
 * A subject hold keeps everything of the subject; a category hold keeps the rows of its data category; a record-set
 * hold keeps its named rows of one table.
 */
public final class ActiveHolds {

    private final List<ActiveHold> holds;

    public ActiveHolds(List<ActiveHold> holds) {
        this.holds = List.copyOf(holds);
    }

    /** No hold applies. */
    public static ActiveHolds none() {
        return new ActiveHolds(List.of());
    }

    public List<ActiveHold> holds() {
        return holds;
    }

    /** Whether one row must be kept: the subject is held, or its category, or the row itself. */
    public boolean keeps(String table, String categoryCode, UUID rowId) {
        return holds.stream().anyMatch(hold -> switch (hold.scope()) {
            case ActiveHold.SUBJECT -> true;
            case ActiveHold.CATEGORY -> categoryCode.equals(hold.categoryCode());
            case ActiveHold.RECORDS -> table.equals(hold.entityType()) && hold.entityIds().contains(rowId);
            default -> true;
        });
    }
}
