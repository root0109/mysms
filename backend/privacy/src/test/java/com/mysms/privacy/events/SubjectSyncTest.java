package com.mysms.privacy.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.business.ConsentStateWriter;
import com.mysms.privacy.business.MajorityJob;
import com.mysms.privacy.business.RetentionJob;
import com.mysms.privacy.model.Majority;
import com.mysms.privacy.model.StudentRecord;
import com.mysms.privacy.model.StudentRecords;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * The subject sync's reading of person and student events: the majority on the clock's day and the consent answers
 * recomputed after it, a deletion that clears the person, a student record moved between persons, a late student
 * event, and what it refuses.
 */
class SubjectSyncTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID TENANT = UUID.randomUUID();

    private final SubjectRepository subjects = mock(SubjectRepository.class);
    private final ConsentStateWriter stateWriter = mock(ConsentStateWriter.class);
    private final MajorityJob majorityJob = mock(MajorityJob.class);
    private final RetentionJob retentionJob = mock(RetentionJob.class);
    private final SubjectSync sync = new SubjectSync(subjects, stateWriter, majorityJob, retentionJob,
            JsonMapper.builder().build(), CLOCK, 18);

    private static EventEnvelope event(String type, UUID id, String data) {
        String aggregate = type.substring(0, type.lastIndexOf('.'));
        return new EventEnvelope(UUID.randomUUID(), aggregate.substring(0, aggregate.indexOf('.')), type,
                Optional.of(TENANT), aggregate, id, Instant.now(), data, Optional.empty());
    }

    @Test
    void aPersonEventSetsTheMajorityOnTheClocksDay() {
        UUID person = UUID.randomUUID();
        when(subjects.applyPerson(any(), any(), any(), any(), anyInt(), any())).thenReturn(true);
        assertThat(sync.apply(event("people.person.updated", person,
                "{\"id\":\"" + person + "\",\"dateOfBirth\":\"2008-11-22\",\"rowVersion\":4}"))).isTrue();
        verify(subjects).applyPerson(eq(person), eq(Majority.MINOR), eq(LocalDate.of(2008, 11, 22)),
                eq(LocalDate.of(2026, 11, 22)), eq(4), any());
        verify(stateWriter).recomputeSubject(person);
        verify(majorityJob).enrol(any());
        verify(retentionJob).enrol(any());
    }

    @Test
    void anOlderPersonEventRecomputesNothing() {
        UUID person = UUID.randomUUID();
        when(subjects.applyPerson(any(), any(), any(), any(), anyInt(), any())).thenReturn(false);
        assertThat(sync.apply(event("people.person.updated", person,
                "{\"id\":\"" + person + "\",\"dateOfBirth\":\"2008-11-22\",\"rowVersion\":2}"))).isFalse();
        verifyNoInteractions(stateWriter);
    }

    @Test
    void aDeletedPersonLosesTheBirthDate() {
        UUID person = UUID.randomUUID();
        sync.apply(event("people.person.deleted", person,
                "{\"id\":\"" + person + "\",\"dateOfBirth\":\"1990-01-01\",\"rowVersion\":5}"));
        verify(subjects).applyPerson(eq(person), eq(Majority.UNKNOWN), isNull(), isNull(), eq(5), any());
    }

    @Test
    void aStudentRecordMovesFromTheOtherPersonToItsNewOne() {
        UUID student = UUID.randomUUID();
        UUID before = UUID.randomUUID();
        UUID after = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        when(subjects.holdersOf(student, after)).thenReturn(List.of(before));
        when(subjects.lockStudentRecords(before))
                .thenReturn(StudentRecords.of(Map.of(student, new StudentRecord(null, institution, 1, true))));
        when(subjects.lockStudentRecords(after)).thenReturn(StudentRecords.of(Map.of()));
        assertThat(sync.apply(event("sis.student.updated", student, "{\"id\":\"" + student + "\",\"personId\":\""
                + after + "\",\"userId\":null,\"institutionId\":\"" + institution + "\",\"rowVersion\":2}"))).isTrue();

        ArgumentCaptor<StudentRecords> left = ArgumentCaptor.forClass(StudentRecords.class);
        verify(subjects).writeStudentRecords(eq(before), left.capture(), any());
        assertThat(left.getValue().records()).containsEntry(student, StudentRecord.gone(2));
        verify(subjects).ensure(eq(after), any());
        ArgumentCaptor<StudentRecords> joined = ArgumentCaptor.forClass(StudentRecords.class);
        verify(subjects).writeStudentRecords(eq(after), joined.capture(), any());
        assertThat(joined.getValue().studentIds()).containsExactly(student);
        assertThat(joined.getValue().institutionIds()).containsExactly(institution);
        verifyNoInteractions(stateWriter);
    }

    @Test
    void aStudentEventOlderThanAnotherPersonsEntryChangesNothing() {
        UUID student = UUID.randomUUID();
        UUID current = UUID.randomUUID();
        UUID late = UUID.randomUUID();
        when(subjects.holdersOf(student, late)).thenReturn(List.of(current));
        when(subjects.lockStudentRecords(current))
                .thenReturn(StudentRecords.of(Map.of(student, new StudentRecord(null, null, 3, true))));
        assertThat(sync.apply(event("sis.student.updated", student,
                "{\"id\":\"" + student + "\",\"personId\":\"" + late + "\",\"rowVersion\":2}"))).isFalse();
        verify(subjects, never()).ensure(any(), any());
        verify(subjects, never()).writeStudentRecords(any(), any(), any());
    }

    @Test
    void anAnnouncementOfTheIdAloneChangesNothing() {
        UUID person = UUID.randomUUID();
        assertThat(sync.apply(event("people.person.updated", person, "{\"id\":\"" + person + "\"}"))).isFalse();
        verifyNoInteractions(subjects);
    }

    @Test
    void refusesAnEventThatIsNotAboutItsRowOrIsMalformed() {
        UUID person = UUID.randomUUID();
        assertThatThrownBy(() -> sync.apply(event("people.person.updated", person,
                "{\"id\":\"" + UUID.randomUUID() + "\",\"rowVersion\":1}"))).isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(event("people.person.updated", person,
                "{\"id\":\"" + person + "\",\"dateOfBirth\":\"21/11/2008\",\"rowVersion\":1}")))
                .isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(event("people.person.updated", person,
                "{\"id\":\"" + person + "\",\"rowVersion\":0}"))).isInstanceOf(RejectedEventException.class);
        UUID student = UUID.randomUUID();
        assertThatThrownBy(() -> sync.apply(event("sis.student.created", student,
                "{\"id\":\"" + student + "\",\"rowVersion\":1}"))).isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(event("sis.student.created", student, "{\"id\":\"" + student
                + "\",\"personId\":\"" + person + "\",\"userId\":\"not-a-uuid\",\"rowVersion\":1}")))
                .isInstanceOf(RejectedEventException.class);
        verifyNoInteractions(subjects);
    }

    @Test
    void refusesAnAgeOfMajorityBelowOneYear() {
        assertThatThrownBy(() -> new SubjectSync(subjects, stateWriter, majorityJob, retentionJob,
                JsonMapper.builder().build(), CLOCK, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
