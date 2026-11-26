package com.mysms.students.admission.business;

import com.mysms.students.admission.api.model.EnrollApplicantRequest;
import com.mysms.students.admission.api.model.EnrollApplicantResult;
import com.mysms.students.admission.api.model.Operation;
import com.mysms.students.admission.config.AdmissionDatabaseConfig;
import com.mysms.students.admission.config.AdmissionEventsConfig;
import com.mysms.students.admission.domain.ApplicationEntity;
import com.mysms.students.admission.domain.ApplicationGuardianEntity;
import com.mysms.students.admission.repository.ApplicationGuardianRepository;
import com.mysms.students.admission.repository.ApplicationRepository;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * admission.EnrollApplicant: an admitted application becomes a student. The person, the student, the placement (class
 * section, or course and batch) and the guardians are created in people and sis through {@link EnrollmentWriter} with
 * the caller's own token; the application keeps the person and student and ends {@code done}. <p>The work crosses two
 * services, so it is not one transaction: each step first checks whether its result exists and sends its create with an
 * Idempotency-Key derived from the application, caller and step (not the row version, which other steps move), so a
 * repeated or concurrent call gets the same record back. Each result is kept in its own short transaction under the
 * row-version guard (412), and {@code done} is set last by a statement that refuses a row without a student. A done
 * application answers its student; a half-finished one is completed. A guardian sis already knows (by e-mail, then
 * mobile) is linked, never created twice. <p>Portal accounts ({@code createPortalAccounts}) are a long-running {@link
 * OperationService} operation; its failure never undoes the enrolment.
 */
@Service
public class ApplicationEnrollmentService extends AppTransactionManager {

    static final String PERMISSION_ENROLL = "admission.application.enroll_applicant";
    static final String OPERATION_PORTAL_ACCOUNTS = "admission.EnrollApplicant.portalAccounts";
    private static final String AGGREGATE = ApplicationEntity.TABLE;
    private static final String ADMITTED = "admitted";
    private static final String DONE = "done";
    private static final String STUDENT = "sis.student";

    private final ApplicationRepository applications;
    private final ApplicationGuardianRepository guardians;
    private final ApplicationService applicationService;
    private final OperationService operations;
    private final EnrollmentWriter writer;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public ApplicationEnrollmentService(ApplicationRepository applications, ApplicationGuardianRepository guardians,
            ApplicationService applicationService, OperationService operations, EnrollmentWriter writer,
            @Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AdmissionDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AdmissionEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AdmissionEventsConfig.AUDIT) AuditWriter audit, Clock clock) {

        super(database, transactions);
        this.applications = applications;
        this.guardians = guardians;
        this.applicationService = applicationService;
        this.operations = operations;
        this.writer = writer;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /** Where the student is placed: a class section of an academic year, or a course (and batch). */
    private record Placement(UUID classSectionId, UUID academicYearId, UUID courseId, UUID batchId,
            String rollNumber) {

        boolean inClass() {
            return classSectionId != null;
        }
    }

    /**
     * admission.EnrollApplicant. Needs the permission in the application's institution (404 when it is not visible,
     * 403 when it is not writable) and an admitted application (409; one already done answers its student). The
     * placement is a class section (the request's, else the application's section) of the application's academic
     * year, or — without a class section — the application's course with the request's (else the application's)
     * batch; anything else is 422.
     */
    public EnrollApplicantResult enrollApplicant(UUID id, EnrollApplicantRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ENROLL);
        UUID caller = AuthContext.require().userId();
        ApplicationEntity application = doInPrimaryReadTransaction(() -> {
            ApplicationEntity found = applications.findById(id)
                    .filter(row -> scope.canRead(Optional.ofNullable(row.getInstitutionId())))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            scope.requireWritable(Optional.ofNullable(found.getInstitutionId()));
            return found;
        });
        boolean accounts = Boolean.TRUE.equals(request == null ? null : request.getCreatePortalAccounts());
        if (DONE.equals(application.getStatus()) && application.getStudentId() != null) {
            Operation portalAccounts = accounts ? createPortalAccounts(id, caller, application.getStudentId()) : null;
            return new EnrollApplicantResult(applicationService.dto(application), application.getStudentId())
                    .operation(portalAccounts);
        }
        if (!ADMITTED.equals(application.getStatus())) {
            throw new InvalidStateException("Only an admitted application is enrolled.");
        }
        Placement placement = placement(application, request);

        int version = application.getRowVersion();
        UUID personId = application.getApplicantPersonId();
        if (personId == null) {
            personId = writer.createApplicantPerson(application, key(id, caller, "person"));
            record(id, version, "applicantPersonId", personId);
            version++;
        }
        UUID studentId = application.getStudentId();
        if (studentId == null) {
            LocalDate admissionDate = Optional.ofNullable(application.getAdmissionDate()).orElse(today());
            UUID person = personId;
            studentId = writer.findStudentOfPerson(person, application.getInstitutionId())
                    .orElseGet(() -> writer.createStudent(person, application, admissionDate,
                            key(id, caller, "student")))
                    .id();
            record(id, version, "studentId", studentId);
            version++;
        }
        place(studentId, placement, application, key(id, caller, "placement"));
        addGuardians(studentId, id, caller);
        ApplicationEntity done = complete(id, version, studentId, placement);
        Operation portalAccounts = accounts ? createPortalAccounts(id, caller, studentId) : null;
        return new EnrollApplicantResult(applicationService.dto(done), studentId).operation(portalAccounts);
    }

    /** The placement the request and the application name (422 for every missing or contradicting part). */
    private static Placement placement(ApplicationEntity application, EnrollApplicantRequest request) {
        EnrollApplicantRequest given = request == null ? new EnrollApplicantRequest() : request;
        UUID classSection = Optional.ofNullable(given.getClassSectionId()).orElse(application.getSectionId());
        UUID batch = Optional.ofNullable(given.getBatchId()).orElse(application.getBatchId());
        String roll = Optional.ofNullable(given.getRollNumber()).map(String::trim).filter(value -> !value.isEmpty())
                .orElse(null);
        List<FieldError> errors = new ArrayList<>();
        if (given.getClassSectionId() != null && given.getBatchId() != null) {
            errors.add(new FieldError("batchId", "give a class section or a batch, not both", "CONFLICT"));
        } else if (classSection != null && given.getBatchId() == null) {
            if (application.getAcademicYearId() == null) {
                errors.add(new FieldError("academicYearId",
                        "the application names no academic year to place the student in", "REQUIRED"));
            }
        } else if (application.getCourseId() == null) {
            errors.add(new FieldError(batch != null ? "batchId" : "classSectionId", batch != null
                    ? "the application names no course for the batch" : "a class section or a batch is required",
                    "REQUIRED"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        if (classSection != null && given.getBatchId() == null) {
            return new Placement(classSection, application.getAcademicYearId(), null, null, roll);
        }
        return new Placement(null, application.getAcademicYearId(), application.getCourseId(), batch, roll);
    }

    /** The class or course placement, unless the student has it already. */
    private void place(UUID studentId, Placement placement, ApplicationEntity application, UUID key) {
        if (placement.inClass()) {
            if (!writer.placedInYear(studentId, placement.academicYearId())) {
                writer.placeInClass(studentId, placement.academicYearId(), placement.classSectionId(),
                        placement.rollNumber(), Optional.ofNullable(application.getAdmissionDate()).orElse(today()),
                        key);
            }
        } else if (!writer.enrolledInCourse(studentId, placement.courseId())) {
            writer.enrollInCourse(studentId, placement.courseId(), placement.batchId(), placement.academicYearId(),
                    placement.rollNumber(), key);
        }
    }

    /**
     * Every guardian of the application becomes a guardian of the student. One sis already knows (by e-mail, then
     * mobile: the parent of a sibling) is reused and only linked, unless linked already; one it does not know is
     * created, unless a guardian of that name is linked already (a repeated enrolment of an application that gives
     * no contact details). The relationship types are read once; the first guardian of a student without any is the
     * first contact.
     */
    private void addGuardians(UUID studentId, UUID id, UUID caller) {
        List<ApplicationGuardianEntity> listed = doInReadTransaction(() -> guardians.findOfApplication(id));
        if (listed.isEmpty()) {
            return;
        }
        Set<UUID> linked = new HashSet<>(writer.guardianIds(studentId));
        Set<String> linkedNames = writer.guardianNames(List.copyOf(linked));
        Map<String, UUID> types = writer.relationshipTypes();
        boolean first = linked.isEmpty();
        for (ApplicationGuardianEntity source : listed) {
            String part = "guardian:" + source.getId();
            Optional<UUID> known = writer.findGuardian(source.getEmail(), source.getMobile());
            if (known.map(linked::contains).orElse(false)
                    || known.isEmpty() && linkedNames.contains(EnrollmentWriter.nameKey(source.getName()))) {
                continue;
            }
            UUID guardianId = known.orElseGet(() -> writer.createGuardian(source, key(id, caller, part + ":person"),
                    key(id, caller, part + ":guardian")));
            writer.linkGuardian(studentId, guardianId, types.get(EnrollmentWriter.nameKey(source.getRelationship())),
                    first, key(id, caller, part + ":link"));
            linked.add(guardianId);
            first = false;
        }
    }

    /** Keeps the person or the student on the admitted application (412 when it moved meanwhile). */
    private void record(UUID id, int version, String field, UUID value) {
        OffsetDateTime now = now();
        UUID actor = Actor.current().orElse(null);
        runInWriteTransaction(() -> {
            int changed = "studentId".equals(field) ? applications.recordStudent(id, version, value, now, actor)
                    : applications.recordApplicantPerson(id, version, value, now, actor);
            if (changed == 0) {
                throw new PreconditionFailedException(
                        "The application changed while it was being enrolled; enrol it again to complete it.");
            }
            audit.recordUpdate(AGGREGATE, id, List.of(field));
        });
    }

    /** The last step: done, with the placement it got; refused for a row without a student. */
    private ApplicationEntity complete(UUID id, int version, UUID studentId, Placement placement) {
        OffsetDateTime now = now();
        UUID actor = Actor.current().orElse(null);
        return doInWriteTransaction(() -> {
            if (applications.completeEnrollment(id, version, placement.classSectionId(), placement.batchId(), now,
                    actor) == 0) {
                throw new PreconditionFailedException(
                        "The application changed while it was being enrolled; enrol it again to complete it.");
            }
            audit.recordTransition(AGGREGATE, id, "enroll", List.of("status", "sectionId", "batchId"), null);
            outbox.publish(AGGREGATE + ".enroll", AGGREGATE, id,
                    Map.of("id", id.toString(), "studentId", studentId.toString()));
            return applications.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        });
    }

    /**
     * The portal accounts of the student and of every guardian linked to them, as an operation of this module that
     * ends succeeded (its result is the student) or failed with the reason; the enrolment stands either way.
     */
    private Operation createPortalAccounts(UUID id, UUID caller, UUID studentId) {
        Operation operation = operations.start(OPERATION_PORTAL_ACCOUNTS, AGGREGATE, id);
        OperationService.Outcome outcome;
        try {
            keepExisting(() -> writer.createStudentPortalAccount(studentId, key(id, caller, "portal:student")));
            for (UUID guardianId : writer.guardianIds(studentId)) {
                keepExisting(() -> writer.createGuardianPortalAccount(guardianId,
                        key(id, caller, "portal:" + guardianId)));
            }
            outcome = OperationService.Outcome.succeeded(STUDENT, studentId);
        } catch (EnrollmentWriter.NotServedException e) {
            outcome = OperationService.Outcome.failed("NOT_IMPLEMENTED",
                    "Portal accounts cannot be created yet: sis does not serve it.");
        } catch (ApiException e) {
            outcome = OperationService.Outcome.failed(e.code(), "Portal accounts could not be created.");
        }
        return operations.finish(operation.getOperationId(), outcome);
    }

    /** One portal account; one that exists already (409: a parent of a sibling, a repeat) is kept as it is. */
    private static void keepExisting(Runnable create) {
        try {
            create.run();
        } catch (DuplicateResourceException e) {
            // the account is there: what the step wanted
        }
    }

    /**
     * The Idempotency-Key of one create: the same step of the same application by the same caller always gives the
     * same key, so a retry gets the record the first call made. The caller is part of it because the other service
     * refuses to share a key between users; the row version is not, because an office edit or an earlier step
     * moves it while the step itself stays the same.
     */
    private static UUID key(UUID application, UUID caller, String step) {
        return UUID.nameUUIDFromBytes(("admission.EnrollApplicant:" + application + ":" + caller + ":" + step)
                .getBytes(StandardCharsets.UTF_8));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }
}
