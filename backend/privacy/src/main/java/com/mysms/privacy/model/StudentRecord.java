package com.mysms.privacy.model;

import java.util.UUID;

/**
 * One sis.student record of a person as privacy last saw it (an entry of privacy.subject.student_records, jsonb).
 *
 * @param userId         the record's login account, or null
 * @param institutionId  the record's institution, or null
 * @param rowVersion     the row version of the sis.student row the entry reflects
 * @param live           false once the record was deleted or moved to another person (a tombstone)
 */
public record StudentRecord(UUID userId, UUID institutionId, int rowVersion, boolean live) {

    /** A tombstone: the record left the person (deleted, or now another person's) at this row version. */
    public static StudentRecord gone(int rowVersion) {
        return new StudentRecord(null, null, rowVersion, false);
    }

    /**
     * A tombstone that keeps the record's last login account: the account stays known for the person (separation of
     * duties) while staying out of the live accounts, since the record is no longer live.
     */
    public static StudentRecord gone(int rowVersion, UUID userId) {
        return new StudentRecord(userId, null, rowVersion, false);
    }
}
