package com.mysms.privacy.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.business.ConsentStateWriter;
import com.mysms.privacy.business.MajorityJob;
import com.mysms.privacy.business.RetentionJob;
import com.mysms.privacy.model.Majority;
import com.mysms.privacy.model.StudentRecord;
import com.mysms.privacy.model.StudentRecords;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps privacy.subject — each person as a data principal: minor or adult, their student records, login accounts and
 * institutions — in step with people.person and sis.student from their events (event-carried state; privacy never
 * reads another schema). One handler for both sources because both write the same row, keyed by the person.
 *
 * <p>A person event sets the birth date, the majority date (birth date plus {@code mysms.privacy.age-of-majority}
 * years) and the status on today's date; it applies only over an older person row version, and a deletion clears them;
 * a changed person row has the subject's consent answers recomputed ({@link ConsentStateWriter}) in the same
 * transaction, since the status decides whose decisions count; a minor enrols the tenant in {@link MajorityJob}, which
 * makes them an adult on their majority date; any changed person enrols the tenant in {@link RetentionJob}, which sends
 * the retention policies' cutoffs to the modules every day. A student event sets the record's entry in its person's
 * student records, and the arrays derived from them, only over an older version of that record — here or at any other
 * person the record belonged to, whose entry becomes a tombstone — so out-of-order and repeated deliveries are
 * harmless; student records decide no consent answer. A student event may come before its person's: the subject is then
 * created with status unknown. Runs inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 *
 * <p>An event without the row's state — the reference cleanup of a module announces only the id of a row it changed
 * — changes nothing: the cleanups that touch these rows clear a person's photo, which is not kept here, or a student
 * record's deleted login account, which signs nobody in, so the id it leaves in user_ids until the record's next event
 * is harmless.
 */
@Component
public final class SubjectSync implements EventSubscriptions {

    static final String PERSON = "people.person";
    static final String STUDENT = "sis.student";

    private final SubjectRepository subjects;
    private final ConsentStateWriter stateWriter;
    private final MajorityJob majorityJob;
    private final RetentionJob retentionJob;
    private final JsonMapper json;
    private final Clock clock;
    private final int ageOfMajority;

    public SubjectSync(SubjectRepository subjects, ConsentStateWriter stateWriter, MajorityJob majorityJob,
            RetentionJob retentionJob, JsonMapper json, Clock clock,
            @Value("${mysms.privacy.age-of-majority}") int ageOfMajority) {
        if (ageOfMajority < 1) {
            throw new IllegalArgumentException("mysms.privacy.age-of-majority must be a positive number of years");
        }
        this.subjects = subjects;
        this.stateWriter = stateWriter;
        this.majorityJob = majorityJob;
        this.retentionJob = retentionJob;
        this.json = json;
        this.clock = clock;
        this.ageOfMajority = ageOfMajority;
    }

    @Override
    public String module() {
        return "privacy";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        Map<String, EventHandler> handlers = new HashMap<>();
        for (String aggregate : new String[] {PERSON, STUDENT}) {
            for (String verb : new String[] {".created", ".updated", ".deleted"}) {
                handlers.put(aggregate + verb, this::apply);
            }
        }
        return Map.copyOf(handlers);
    }

    /** Applies one person or student event; whether a stored row changed. */
    public boolean apply(EventEnvelope event) {
        if (!PERSON.equals(event.aggregateType()) && !STUDENT.equals(event.aggregateType())) {
            throw new RejectedEventException("event " + event.id() + " is neither a person's nor a student's");
        }
        SourceEvent source = SourceEvent.read(event, json);
        if (!source.carriesState()) {
            return false; // a cleanup's announcement of the id alone (see the class comment)
        }
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        return PERSON.equals(event.aggregateType()) ? applyPerson(source, now) : applyStudent(source, now);
    }

    private boolean applyPerson(SourceEvent person, OffsetDateTime now) {
        int version = person.rowVersion();
        LocalDate birthDate = person.deleted() ? null : person.optionalDate("dateOfBirth");
        Majority majority = Majority.of(birthDate, ageOfMajority, now.toLocalDate());
        if (!subjects.applyPerson(person.id(), majority.status(), birthDate, majority.majorityDate(), version, now)) {
            return false;
        }
        stateWriter.recomputeSubject(person.id());
        if (Majority.MINOR.equals(majority.status())) {
            majorityJob.enrol(now);
        }
        retentionJob.enrol(now);
        return true;
    }

    private boolean applyStudent(SourceEvent student, OffsetDateTime now) {
        int version = student.rowVersion();
        UUID personId = student.requiredId("personId");
        StudentRecord record = student.deleted() ? StudentRecord.gone(version)
                : new StudentRecord(student.optionalId("userId"), student.optionalId("institutionId"), version, true);
        boolean changed = false;
        boolean stale = false;
        for (UUID other : subjects.holdersOf(student.id(), personId)) {
            boolean moved = put(other, student.id(), StudentRecord.gone(version), now);
            changed |= moved;
            stale |= !moved;
        }
        if (stale) {
            return changed; // another person holds a newer version of the record: this event is late
        }
        subjects.ensure(personId, now);
        return put(personId, student.id(), record, now) || changed;
    }

    /** Sets one entry of the person's student records under a row lock; whether it changed (only to a newer version). */
    private boolean put(UUID personId, UUID studentId, StudentRecord record, OffsetDateTime now) {
        Optional<StudentRecords> changed = subjects.lockStudentRecords(personId).with(studentId, record);
        changed.ifPresent(records -> subjects.writeStudentRecords(personId, records, now));
        return changed.isPresent();
    }
}
