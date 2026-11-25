package com.mysms.privacy.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.model.StudentRecord;
import com.mysms.privacy.model.StudentRecords;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * The staff sync's reading of hr.staff events: a new staff record, a deletion that keeps the account in the
 * tombstone, a record moved between persons, a late event, an id-only announcement, and what it refuses.
 */
class StaffSyncTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID TENANT = UUID.randomUUID();

    private final SubjectRepository subjects = mock(SubjectRepository.class);
    private final StaffSync sync = new StaffSync(subjects, JsonMapper.builder().build(), CLOCK);

    private static EventEnvelope event(String type, UUID id, String data) {
        return new EventEnvelope(UUID.randomUUID(), "hr", type, Optional.of(TENANT), StaffSync.STAFF, id,
                Instant.now(), data, Optional.empty());
    }

    private static String payload(UUID staff, UUID person, UUID user, int version) {
        return "{\"id\":\"" + staff + "\",\"personId\":\"" + person + "\",\"userId\":\"" + user
                + "\",\"institutionId\":null,\"rowVersion\":" + version + "}";
    }

    @Test
    void aNewStaffRecordMakesThePersonStaff() {
        UUID staff = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(subjects.staffHoldersOf(staff, person)).thenReturn(List.of());
        when(subjects.lockStaffRecords(person)).thenReturn(StudentRecords.of(Map.of()));
        assertThat(sync.apply(event("hr.staff.created", staff, payload(staff, person, user, 1)))).isTrue();

        verify(subjects).ensure(eq(person), any());
        ArgumentCaptor<StudentRecords> written = ArgumentCaptor.forClass(StudentRecords.class);
        verify(subjects).writeStaffRecords(eq(person), written.capture(), any());
        assertThat(written.getValue().records()).containsEntry(staff, new StudentRecord(user, null, 1, true));
    }

    @Test
    void aDeletedStaffRecordKeepsItsAccountInTheTombstone() {
        UUID staff = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(subjects.staffHoldersOf(staff, person)).thenReturn(List.of());
        when(subjects.lockStaffRecords(person))
                .thenReturn(StudentRecords.of(Map.of(staff, new StudentRecord(user, null, 1, true))));
        assertThat(sync.apply(event("hr.staff.deleted", staff, payload(staff, person, user, 2)))).isTrue();

        ArgumentCaptor<StudentRecords> written = ArgumentCaptor.forClass(StudentRecords.class);
        verify(subjects).writeStaffRecords(eq(person), written.capture(), any());
        assertThat(written.getValue().records()).containsEntry(staff, StudentRecord.gone(2, user));
        assertThat(written.getValue().userIds()).isEmpty();
    }

    @Test
    void aStaffRecordMovesFromTheOtherPersonToItsNewOne() {
        UUID staff = UUID.randomUUID();
        UUID before = UUID.randomUUID();
        UUID after = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        when(subjects.staffHoldersOf(staff, after)).thenReturn(List.of(before));
        when(subjects.lockStaffRecords(before))
                .thenReturn(StudentRecords.of(Map.of(staff, new StudentRecord(user, null, 1, true))));
        when(subjects.lockStaffRecords(after)).thenReturn(StudentRecords.of(Map.of()));
        assertThat(sync.apply(event("hr.staff.updated", staff, payload(staff, after, user, 2)))).isTrue();

        ArgumentCaptor<StudentRecords> left = ArgumentCaptor.forClass(StudentRecords.class);
        verify(subjects).writeStaffRecords(eq(before), left.capture(), any());
        assertThat(left.getValue().records()).containsEntry(staff, StudentRecord.gone(2));
        verify(subjects).ensure(eq(after), any());
    }

    @Test
    void aStaffEventOlderThanAnotherPersonsEntryChangesNothing() {
        UUID staff = UUID.randomUUID();
        UUID current = UUID.randomUUID();
        UUID late = UUID.randomUUID();
        when(subjects.staffHoldersOf(staff, late)).thenReturn(List.of(current));
        when(subjects.lockStaffRecords(current))
                .thenReturn(StudentRecords.of(Map.of(staff, new StudentRecord(null, null, 3, true))));
        assertThat(sync.apply(event("hr.staff.updated", staff, payload(staff, late, UUID.randomUUID(), 2))))
                .isFalse();
        verify(subjects, never()).ensure(any(), any());
        verify(subjects, never()).writeStaffRecords(eq(late), any(), any());
    }

    @Test
    void anAnnouncementOfTheIdAloneChangesNothing() {
        UUID staff = UUID.randomUUID();
        assertThat(sync.apply(event("hr.staff.updated", staff, "{\"id\":\"" + staff + "\"}"))).isFalse();
        verifyNoInteractions(subjects);
    }

    @Test
    void refusesAnEventOfAnotherAggregate() {
        UUID id = UUID.randomUUID();
        EventEnvelope other = new EventEnvelope(UUID.randomUUID(), "sis", "sis.student.updated", Optional.of(TENANT),
                "sis.student", id, Instant.now(), "{\"id\":\"" + id + "\",\"rowVersion\":1}", Optional.empty());
        assertThatThrownBy(() -> sync.apply(other)).isInstanceOf(RejectedEventException.class);
    }
}
