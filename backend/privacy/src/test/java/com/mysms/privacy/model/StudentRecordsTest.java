package com.mysms.privacy.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A person's student records change only to a newer row version, and the arrays list the live records' ids, accounts
 * and institutions once each.
 */
class StudentRecordsTest {

    private static final UUID STUDENT = UUID.fromString("00000000-0000-7000-8000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-7000-8000-000000000002");
    private static final UUID USER = UUID.fromString("00000000-0000-7000-8000-0000000000a1");
    private static final UUID INSTITUTION = UUID.fromString("00000000-0000-7000-8000-0000000000b1");
    private static final UUID OTHER_INSTITUTION = UUID.fromString("00000000-0000-7000-8000-0000000000b2");

    @Test
    void aNewerVersionReplacesTheEntryAndAnOlderOrEqualOneChangesNothing() {
        StudentRecords records = StudentRecords.of(Map.of(STUDENT, new StudentRecord(null, INSTITUTION, 2, true)));
        assertThat(records.with(STUDENT, new StudentRecord(USER, INSTITUTION, 1, true))).isEmpty();
        assertThat(records.with(STUDENT, new StudentRecord(USER, INSTITUTION, 2, true))).isEmpty();
        StudentRecords newer = records.with(STUDENT, new StudentRecord(USER, INSTITUTION, 3, true)).orElseThrow();
        assertThat(newer.records()).containsEntry(STUDENT, new StudentRecord(USER, INSTITUTION, 3, true));
        assertThat(records.records()).as("the original is unchanged")
                .containsEntry(STUDENT, new StudentRecord(null, INSTITUTION, 2, true));
    }

    @Test
    void theArraysNameTheLiveRecordsOnly() {
        StudentRecords records = StudentRecords.of(Map.of(
                STUDENT, new StudentRecord(USER, INSTITUTION, 1, true),
                SECOND, new StudentRecord(USER, INSTITUTION, 4, true)));
        assertThat(records.studentIds()).containsExactly(STUDENT, SECOND);
        assertThat(records.userIds()).containsExactly(USER);
        assertThat(records.institutionIds()).containsExactly(INSTITUTION);

        StudentRecords moved = records.with(SECOND, new StudentRecord(null, OTHER_INSTITUTION, 5, true)).orElseThrow()
                .with(STUDENT, StudentRecord.gone(2)).orElseThrow();
        assertThat(moved.studentIds()).containsExactly(SECOND);
        assertThat(moved.userIds()).isEmpty();
        assertThat(moved.institutionIds()).containsExactly(OTHER_INSTITUTION);
    }

    @Test
    void aTombstoneKeepsItsVersionAgainstALateEvent() {
        StudentRecords deleted = StudentRecords.of(Map.of()).with(STUDENT, StudentRecord.gone(3)).orElseThrow();
        assertThat(deleted.with(STUDENT, new StudentRecord(USER, INSTITUTION, 2, true))).isEmpty();
        assertThat(deleted.studentIds()).isEmpty();
    }
}
