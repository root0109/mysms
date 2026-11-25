package com.mysms.privacy.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.model.StudentRecord;
import com.mysms.privacy.model.StudentRecords;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps privacy.subject.staff_records — the hr.staff records of each person — in step with hr's staff events
 * (event-carried state: an hr.staff event carries the record's account, person, institution and row version). A live
 * staff record makes the person staff for the access summary (design §9.6) and names the login accounts a rights
 * request's subject or requester holds as staff (no staff member acts on such a request).
 *
 * <p>An entry has the shape of a student record ({@link StudentRecord}: account, institution, row version, live) and
 * changes the same way as {@link SubjectSync}'s: only to a newer row version, here or at another person the record
 * belonged to, whose entry becomes a tombstone; a deleted record stays as a tombstone. A staff event may come before its
 * person's: the subject is then created with status unknown. Staff records decide no consent answer. An event without
 * the record's state (a reference cleanup's id-only announcement) changes nothing. Runs inside the event's tenant
 * (messaging's InboxProcessor: exactly once per event).
 */
@Component
public final class StaffSync implements EventSubscriptions {

    static final String STAFF = "hr.staff";

    private final SubjectRepository subjects;
    private final JsonMapper json;
    private final Clock clock;

    public StaffSync(SubjectRepository subjects, JsonMapper json, Clock clock) {
        this.subjects = subjects;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "privacy";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(STAFF + ".created", this::apply, STAFF + ".updated", this::apply, STAFF + ".deleted", this::apply);
    }

    /** Applies one staff event; whether a stored row changed. */
    public boolean apply(EventEnvelope event) {
        if (!STAFF.equals(event.aggregateType())) {
            throw new RejectedEventException("event " + event.id() + " is not a staff record's");
        }
        SourceEvent staff = SourceEvent.read(event, json);
        if (!staff.carriesState()) {
            return false; // a cleanup's announcement of the id alone (see the class comment)
        }
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        int version = staff.rowVersion();
        UUID personId = staff.requiredId("personId");
        StudentRecord record = staff.deleted() ? StudentRecord.gone(version, staff.optionalId("userId"))
                : new StudentRecord(staff.optionalId("userId"), staff.optionalId("institutionId"), version, true);
        boolean changed = false;
        boolean stale = false;
        for (UUID other : subjects.staffHoldersOf(staff.id(), personId)) {
            boolean moved = put(other, staff.id(), StudentRecord.gone(version), now);
            changed |= moved;
            stale |= !moved;
        }
        if (stale) {
            return changed; // another person holds a newer version of the record: this event is late
        }
        subjects.ensure(personId, now);
        return put(personId, staff.id(), record, now) || changed;
    }

    /** Sets one entry of the person's staff records under a row lock; whether it changed (only to a newer version). */
    private boolean put(UUID personId, UUID staffId, StudentRecord record, OffsetDateTime now) {
        Optional<StudentRecords> changed = subjects.lockStaffRecords(personId).with(staffId, record);
        changed.ifPresent(records -> subjects.writeStaffRecords(personId, records, now));
        return changed.isPresent();
    }
}
