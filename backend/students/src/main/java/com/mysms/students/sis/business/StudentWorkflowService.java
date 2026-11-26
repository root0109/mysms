package com.mysms.students.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.web.Etags;
import com.mysms.students.sis.api.model.ChangeStudentStatusRequest;
import com.mysms.students.sis.api.model.IssueBonafideCertificateRequest;
import com.mysms.students.sis.api.model.Operation;
import com.mysms.students.sis.api.model.Student;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.config.SisEventsConfig;
import com.mysms.students.sis.domain.StudentEnrollmentEntity;
import com.mysms.students.sis.domain.StudentEntity;
import com.mysms.students.sis.events.SisEvents;
import com.mysms.students.sis.mapper.StudentMapper;
import com.mysms.students.sis.repository.NumberSequenceRepository;
import com.mysms.students.sis.repository.StudentEnrollmentRepository;
import com.mysms.students.sis.repository.StudentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The steps of a student record that do more than set a column: sis.ChangeStudentStatus, which also ends the student's
 * open placements when the student leaves, and sis.IssueBonafideCertificate, which takes the next certificate number of
 * the institution. Both follow the transition shape — visible under the step's own permission (404), writable (403),
 * the body etag (412), the guard on the row as read (409), one guarded UPDATE — and audit and announce every row they
 * change in the same transaction.
 */
@Service
public class StudentWorkflowService extends AppTransactionManager {

    static final String AGGREGATE = StudentService.AGGREGATE;
    static final String PERMISSION_CHANGE_STATUS = "sis.student.change_student_status";
    static final String PERMISSION_ISSUE_BONAFIDE = "sis.student.issue_bonafide_certificate";
    /** The statuses of a student who has left the school: they take an exit date and end the open placements. */
    static final Set<String> EXIT_STATUSES = Set.of("withdrawn", "transferred", "graduated", "alumni");
    /** The number series of bonafide certificates in sis.number_sequence (one counter per institution and year). */
    static final String BONAFIDE_SEQUENCE = "bonafide_certificate";

    private final StudentRepository repository;
    private final StudentEnrollmentRepository enrollments;
    private final NumberSequenceRepository numbers;
    private final StudentMapper mapper;
    private final OperationService operations;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public StudentWorkflowService(StudentRepository repository, StudentEnrollmentRepository enrollments,
            NumberSequenceRepository numbers, StudentMapper mapper, OperationService operations,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.enrollments = enrollments;
        this.numbers = numbers;
        this.mapper = mapper;
        this.operations = operations;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * sis.ChangeStudentStatus. Any status may follow any other except itself (409). An exit status (withdrawn,
     * transferred, graduated, alumni) needs the exit date, not before the admission or joining date, and ends every
     * open placement of the student on that day (graduated and alumni as completed, the others as left; a placement
     * that starts after the exit date is a 422 on the date). Going back to active or inactive clears the exit
     * details, so a re-admitted student is placed again with a new enrolment; such a status takes no exit details
     * (422). The exit reason is kept with the student and in the audit record.
     */
    public Student changeStudentStatus(UUID id, ChangeStudentStatusRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CHANGE_STATUS);
        String status = request.getStatus().getValue();
        boolean leaving = EXIT_STATUSES.contains(status);
        LocalDate exitDate = request.getExitDate();
        String exitReason = blankToNull(request.getExitReason());
        requireExitDetails(leaving, exitDate, exitReason);
        return doInWriteTransaction(() -> {
            StudentEntity student = loadWritable(scope, id, request.getEtag());
            if (status.equals(student.getStatus())) {
                throw new InvalidStateException("The student already has this status.");
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            if (leaving) {
                requireAfterJoining(student, exitDate);
                endPlacements(id, exitDate, "graduated".equals(status) || "alumni".equals(status), now, actor);
            }
            if (repository.changeStatus(id, student.getRowVersion(), status, exitDate, exitReason, now, actor) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            StudentEntity changed = reload(id);
            audit.recordTransition(AGGREGATE, id, "changeStatus", List.of("status", "exitDate", "exitReason"),
                    exitReason);
            Map<String, Object> payload = StudentService.memberPayload(changed);
            payload.put("status", status);
            outbox.publish(AGGREGATE + ".changeStatus", AGGREGATE, id, payload);
            return mapper.toDto(changed);
        });
    }

    /**
     * sis.IssueBonafideCertificate. Only a current (active) student is certified (409 otherwise). The certificate
     * takes the next number of the institution's series for the calendar year ({@code BC/<year>/000001}, gap-free
     * under the counter's row lock), which is recorded on the student; the purpose is kept in the audit record and
     * the event. The work is done in the request, so the answer is the finished operation, whose result is the
     * student. Rendering the PDF is not part of it yet: the file store (platform.CreateUploadUrl) is not served.
     */
    public Operation issueBonafideCertificate(UUID id, IssueBonafideCertificateRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ISSUE_BONAFIDE);
        String purpose = request == null ? null : blankToNull(request.getPurpose());
        return doInWriteTransaction(() -> {
            StudentEntity student = loadWritable(scope, id, null);
            if (!"active".equals(student.getStatus())) {
                throw new InvalidStateException("A bonafide certificate is issued to a current (active) student only.");
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            String year = String.valueOf(now.getYear());
            String number = numbers.next(UuidV7.next(clock), student.getInstitutionId(), BONAFIDE_SEQUENCE, year,
                    "BC/" + year + "/", now, actor);
            if (repository.recordBonafideCertificate(id, student.getRowVersion(), number, now, actor) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            StudentEntity changed = reload(id);
            audit.recordTransition(AGGREGATE, id, "issueBonafideCertificate", List.of("bonafideCertificateNumber"),
                    purpose);
            Map<String, Object> payload = StudentService.memberPayload(changed);
            payload.put("certificateNumber", number);
            payload.put("purpose", purpose);
            outbox.publish(AGGREGATE + ".issueBonafideCertificate", AGGREGATE, id, payload);
            return operations.recordFinished("sis.IssueBonafideCertificate", AGGREGATE, id, AGGREGATE, id);
        });
    }

    /** The exit details fit the status: an exit date for an exit status, no exit details for any other (422). */
    private static void requireExitDetails(boolean leaving, LocalDate exitDate, String exitReason) {
        List<FieldError> errors = new ArrayList<>();
        if (leaving && exitDate == null) {
            errors.add(new FieldError("exitDate", "is required when the student leaves", "NotNull"));
        }
        if (!leaving && exitDate != null) {
            errors.add(new FieldError("exitDate", "is only given when the student leaves", "INVALID"));
        }
        if (!leaving && exitReason != null) {
            errors.add(new FieldError("exitReason", "is only given when the student leaves", "INVALID"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    /** A student cannot leave before they were admitted or joined (422 on the exit date). */
    private static void requireAfterJoining(StudentEntity student, LocalDate exitDate) {
        for (LocalDate start : new LocalDate[] {student.getAdmissionDate(), student.getJoiningDate()}) {
            if (start != null && exitDate.isBefore(start)) {
                throw new ValidationFailedException(List.of(new FieldError("exitDate",
                        "is before the student's admission or joining date", "INVALID")));
            }
        }
    }

    /** Ends the student's open placements on the exit date: completed (graduation) or left. */
    private void endPlacements(UUID studentId, LocalDate exitDate, boolean completed, OffsetDateTime now, UUID actor) {
        for (StudentEnrollmentEntity placement : enrollments.openOfStudent(studentId)) {
            if (placement.getEffectiveFrom() != null && exitDate.isBefore(placement.getEffectiveFrom())) {
                throw new ValidationFailedException(List.of(new FieldError("exitDate",
                        "is before the start of the student's current placement", "INVALID")));
            }
            String outcome = completed ? "completed" : "left";
            if (enrollments.close(placement.getId(), placement.getRowVersion(), outcome, exitDate,
                    completed ? null : exitDate, now, actor) == 0) {
                throw new PreconditionFailedException("A placement of the student changed meanwhile; try again.");
            }
            audit.recordUpdate(StudentEnrollmentEntity.TABLE, placement.getId(),
                    List.of("enrollmentStatus", "effectiveTo", "leftOn"));
            outbox.publish(StudentEnrollmentEntity.TABLE + ".updated", StudentEnrollmentEntity.TABLE,
                    placement.getId(), SisEvents.payload(placement.getId()));
        }
    }

    /** The live student under the step's permission: invisible 404, not writable 403, a stale body etag 412. */
    private StudentEntity loadWritable(DataScope scope, UUID id, String etag) {
        StudentEntity student = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        Optional<UUID> institution = Optional.ofNullable(student.getInstitutionId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        scope.requireWritable(institution);
        OptionalInt expected = Etags.bodyVersion(etag);
        if (expected.isPresent() && expected.getAsInt() != student.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
        return student;
    }

    private StudentEntity reload(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
