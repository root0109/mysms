package com.mysms.students.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.web.Etags;
import com.mysms.students.sis.api.model.ApproveSubjectRegistrationRequest;
import com.mysms.students.sis.api.model.SubjectRegistration;
import com.mysms.students.sis.business.AcademicsLookup.PlannedSubject;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.config.SisEventsConfig;
import com.mysms.students.sis.domain.ProgramEnrollmentSubjectEntity;
import com.mysms.students.sis.domain.SubjectRegistrationEntity;
import com.mysms.students.sis.domain.SubjectRegistrationLineEntity;
import com.mysms.students.sis.events.SisEvents;
import com.mysms.students.sis.mapper.SubjectRegistrationMapper;
import com.mysms.students.sis.repository.ProgramEnrollmentSubjectRepository;
import com.mysms.students.sis.repository.SubjectRegistrationLineRepository;
import com.mysms.students.sis.repository.SubjectRegistrationRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The steps of a subject registration (the student fills and submits, a teacher approves) that write more than the
 * registration's status: sis.LoadCourseSubjects pre-fills a draft from the course's plan in academics, and
 * sis.ApproveSubjectRegistration approves a submitted registration and adds its subjects to the student's course
 * registration (sis.program_enrollment_subject). Submit, reject and reset are the plain transitions of
 * {@link SubjectRegistrationService}. A registration belongs to its student's institution.
 */
@Service
public class SubjectRegistrationWorkflowService extends AppTransactionManager {

    static final String AGGREGATE = SubjectRegistrationService.AGGREGATE;
    static final String LINE = SubjectRegistrationLineEntity.TABLE;
    static final String COVERED = ProgramEnrollmentSubjectEntity.TABLE;
    static final String PERMISSION_APPROVE = "sis.subject_registration.approve_subject_registration";
    static final String PERMISSION_LOAD = "sis.subject_registration.load_course_subjects";

    private final SubjectRegistrationRepository repository;
    private final SubjectRegistrationLineRepository lines;
    private final ProgramEnrollmentSubjectRepository covered;
    private final SubjectRegistrationMapper mapper;
    private final AcademicsLookup academics;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public SubjectRegistrationWorkflowService(SubjectRegistrationRepository repository,
            SubjectRegistrationLineRepository lines, ProgramEnrollmentSubjectRepository covered,
            SubjectRegistrationMapper mapper, AcademicsLookup academics,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.lines = lines;
        this.covered = covered;
        this.mapper = mapper;
        this.academics = academics;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * sis.ApproveSubjectRegistration: allowed while the registration is submitted and lists at least one subject
     * (409 otherwise). Every subject of its lines that the student's course registration does not cover yet is added
     * to it, in the same transaction as the approval.
     */
    public SubjectRegistration approveSubjectRegistration(UUID id, ApproveSubjectRegistrationRequest request) {
        DataScope scope = DataScope.current(PERMISSION_APPROVE);
        return doInWriteTransaction(() -> {
            SubjectRegistrationEntity registration = loadWritable(scope, id, request == null ? null : request.getEtag());
            if (!"submitted".equals(registration.getStatus())) {
                throw new InvalidStateException("Only a submitted subject registration can be approved.");
            }
            List<SubjectRegistrationLineEntity> chosen = lines.ofRegistration(id);
            if (chosen.isEmpty()) {
                throw new InvalidStateException("The subject registration lists no subjects.");
            }
            OffsetDateTime now = now();
            if (repository.approveSubjectRegistration(id, registration.getRowVersion(), now,
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            Set<UUID> already = new HashSet<>(covered.subjectsOf(registration.getProgramEnrollmentId()));
            for (SubjectRegistrationLineEntity line : chosen) {
                if (already.add(line.getSubjectId())) {
                    ProgramEnrollmentSubjectEntity subject = new ProgramEnrollmentSubjectEntity();
                    subject.setProgramEnrollmentId(registration.getProgramEnrollmentId());
                    subject.setSubjectId(line.getSubjectId());
                    subject.onCreatingData(Actor.current(), clock);
                    subject.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
                    ProgramEnrollmentSubjectEntity stored = covered.insert(subject);
                    audit.recordCreate(COVERED, stored.getId());
                    outbox.publish(COVERED + SisEvents.CREATED, COVERED, stored.getId(),
                            SisEvents.payload(stored.getId()));
                }
            }
            audit.recordTransition(AGGREGATE, id, "approve", List.of("status"), null);
            outbox.publish(AGGREGATE + ".approve", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(reload(id));
        });
    }

    /**
     * sis.LoadCourseSubjects: adds a line for every subject of the course's plan that the draft does not list yet —
     * elective when the plan files the subject as elective, compulsory otherwise — and leaves the lines it has. Only
     * a draft is filled (409); the course's plan is read from academics with the caller's token first (403 naming
     * academics' permission, 503 without the service), and a draft that changed meanwhile is 412. The registration
     * is locked while its lines are added and takes a new version when any is, so a submit made meanwhile waits and
     * an etag read before the lines changed no longer passes.
     */
    public SubjectRegistration loadCourseSubjects(UUID id) {
        return loadCourseSubjects(DataScope.current(PERMISSION_LOAD), id);
    }

    /** sis.LoadCourseSubjects for a caller of the given scope. */
    SubjectRegistration loadCourseSubjects(DataScope scope, UUID id) {
        SubjectRegistrationEntity read = doInPrimaryReadTransaction(() -> requireDraft(loadWritable(scope, id, null)));
        List<PlannedSubject> plan = academics.courseSubjects(read.getCourseId(), "Loading course subjects");
        return doInWriteTransaction(() -> {
            // locked: a submit waits for the lines, and then finds the version this step gave the registration
            repository.lockById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            SubjectRegistrationEntity registration = requireDraft(loadWritable(scope, id, null));
            if (registration.getRowVersion() != read.getRowVersion()) {
                throw new PreconditionFailedException("The subject registration changed meanwhile; try again.");
            }
            Set<UUID> listed = new HashSet<>();
            lines.ofRegistration(id).forEach(line -> listed.add(line.getSubjectId()));
            boolean added = false;
            for (PlannedSubject planned : plan) {
                if (planned.subjectId() != null && listed.add(planned.subjectId())) {
                    SubjectRegistrationLineEntity line = new SubjectRegistrationLineEntity();
                    line.setSubjectRegistrationId(id);
                    line.setSubjectId(planned.subjectId());
                    line.setCourseSubjectId(planned.courseSubjectId());
                    line.setSelectionType(planned.elective() ? "elective" : "compulsory");
                    line.onCreatingData(Actor.current(), clock);
                    line.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
                    SubjectRegistrationLineEntity stored = lines.insert(line);
                    audit.recordCreate(LINE, stored.getId());
                    outbox.publish(LINE + SisEvents.CREATED, LINE, stored.getId(), SisEvents.payload(stored.getId()));
                    added = true;
                }
            }
            if (!added) {
                return mapper.toDto(registration);
            }
            if (repository.linesChanged(id, registration.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The subject registration changed meanwhile; try again.");
            }
            audit.recordTransition(AGGREGATE, id, "loadCourseSubjects", List.of("lines"), null);
            outbox.publish(AGGREGATE + ".loadCourseSubjects", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(reload(id));
        });
    }

    private static SubjectRegistrationEntity requireDraft(SubjectRegistrationEntity registration) {
        if (!"draft".equals(registration.getStatus())) {
            throw new InvalidStateException("Only a draft subject registration is filled from the course.");
        }
        return registration;
    }

    /** The live registration under the step's permission: invisible 404, not writable 403, a stale body etag 412. */
    private SubjectRegistrationEntity loadWritable(DataScope scope, UUID id, String etag) {
        SubjectRegistrationEntity registration = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        Optional<UUID> institution = repository.institutionOf(registration.getStudentId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        scope.requireWritable(institution);
        OptionalInt expected = Etags.bodyVersion(etag);
        if (expected.isPresent() && expected.getAsInt() != registration.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
        return registration;
    }

    private SubjectRegistrationEntity reload(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
