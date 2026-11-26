package com.mysms.students.admission.business;

import com.mysms.students.admission.api.model.Application;
import com.mysms.students.admission.api.model.ApproveApplicationRequest;
import com.mysms.students.admission.api.model.CancelApplicationRequest;
import com.mysms.students.admission.api.model.ConfirmAdmissionRequest;
import com.mysms.students.admission.api.model.ConfirmApplicationRequest;
import com.mysms.students.admission.api.model.MarkApplicationPendingRequest;
import com.mysms.students.admission.api.model.RejectApplicationRequest;
import com.mysms.students.admission.api.model.ResetApplicationRequest;
import com.mysms.students.admission.api.model.SubmitApplicationRequest;
import com.mysms.students.admission.config.AdmissionDatabaseConfig;
import com.mysms.students.admission.config.AdmissionEventsConfig;
import com.mysms.students.admission.domain.AdmissionCycleEntity;
import com.mysms.students.admission.domain.ApplicationEntity;
import com.mysms.students.admission.events.AdmissionEvents;
import com.mysms.students.admission.repository.AdmissionCycleRepository;
import com.mysms.students.admission.repository.ApplicationRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.web.Etags;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The lifecycle of an admission application: the applicant (or the office) hands a draft in; the office confirms it,
 * may put it on hold, approves it and confirms the admission once the place is accepted; it may be rejected or
 * cancelled on the way and then reset to a draft. Enrolment is {@link ApplicationEnrollmentService}. <p>Every step is a
 * row of the {@link Step} state table run by {@link #take}: reachable (404), changeable (403), matching etag (412),
 * allowed state (409), the step's own rules, then one guarded UPDATE with the audit record and the {@code
 * admission.application.<step>} event in the same transaction. The audit record is the only record of who decided and
 * when.
 */
@Service
public class ApplicationWorkflowService extends AppTransactionManager {

    /** The longest reason a step keeps (in its audit record). */
    static final int MAX_REASON = 4000;
    private static final String AGGREGATE = ApplicationEntity.TABLE;
    private static final String PERMISSION_PREFIX = "admission.application.";
    private static final Set<String> FEE_SETTLED = Set.of("paid", "waived");

    /** The steps of an application: its state table. */
    enum Step {
        SUBMIT("submit", "submit_application", Set.of("draft"), "submitted", true),
        CONFIRM("confirm", "confirm_application", Set.of("submitted", "pending"), "confirmed", false),
        MARK_PENDING("markPending", "mark_application_pending", Set.of("submitted", "confirmed"), "pending", false),
        REJECT("reject", "reject_application", Set.of("submitted", "pending", "confirmed", "approved"), "rejected",
                false),
        CANCEL("cancel", "cancel_application",
                Set.of("draft", "submitted", "pending", "confirmed", "approved", "admitted"), "cancelled", true),
        RESET("reset", "reset_application", Set.of("rejected", "cancelled"), "draft", false),
        APPROVE("approve", "approve_application", Set.of("confirmed"), "approved", false),
        CONFIRM_ADMISSION("confirmAdmission", "confirm_admission", Set.of("approved"), "admitted", false);

        private final String event;
        private final String permission;
        private final Set<String> from;
        private final String to;
        /** The applicant may take the step on their own application; every other step is the office's. */
        private final boolean applicantMay;

        Step(String event, String action, Set<String> from, String to, boolean applicantMay) {
            this.event = event;
            this.permission = PERMISSION_PREFIX + action;
            this.from = from;
            this.to = to;
            this.applicantMay = applicantMay;
        }
    }

    private final ApplicationRepository applications;
    private final AdmissionCycleRepository cycles;
    private final ApplicationService applicationService;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public ApplicationWorkflowService(ApplicationRepository applications, AdmissionCycleRepository cycles,
            ApplicationService applicationService,
            @Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AdmissionDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AdmissionEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AdmissionEventsConfig.AUDIT) AuditWriter audit, Clock clock) {

        super(database, transactions);
        this.applications = applications;
        this.cycles = cycles;
        this.applicationService = applicationService;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * admission.SubmitApplication: a draft is handed in. It needs a guardian; an application of an admission cycle
     * is taken only while the cycle is open for applications, has a place left and the applicant is old enough for
     * it. The application date becomes the time of submitting.
     */
    public Application submitApplication(UUID id, SubmitApplicationRequest request) {
        return take(Step.SUBMIT, id, request == null ? null : request.getEtag(), null);
    }

    /** admission.ConfirmApplication: the office confirms a submitted (or held) application is complete. */
    public Application confirmApplication(UUID id, ConfirmApplicationRequest request) {
        return take(Step.CONFIRM, id, request == null ? null : request.getEtag(), null);
    }

    /** admission.MarkApplicationPending: a submitted or confirmed application is put on hold. */
    public Application markApplicationPending(UUID id, MarkApplicationPendingRequest request) {
        return request == null ? take(Step.MARK_PENDING, id, null, null)
                : take(Step.MARK_PENDING, id, request.getEtag(), request.getReason());
    }

    /** admission.RejectApplication: an application under review is refused. */
    public Application rejectApplication(UUID id, RejectApplicationRequest request) {
        return request == null ? take(Step.REJECT, id, null, null)
                : take(Step.REJECT, id, request.getEtag(), request.getReason());
    }

    /** admission.CancelApplication: withdrawn — by the office, or by the applicant — any time before enrolment. */
    public Application cancelApplication(UUID id, CancelApplicationRequest request) {
        return request == null ? take(Step.CANCEL, id, null, null)
                : take(Step.CANCEL, id, request.getEtag(), request.getReason());
    }

    /** admission.ResetApplication: a rejected or cancelled application is a draft again. */
    public Application resetApplication(UUID id, ResetApplicationRequest request) {
        return take(Step.RESET, id, request == null ? null : request.getEtag(), null);
    }

    /** admission.ApproveApplication: a confirmed application is approved after review. */
    public Application approveApplication(UUID id, ApproveApplicationRequest request) {
        return take(Step.APPROVE, id, request == null ? null : request.getEtag(), null);
    }

    /**
     * admission.ConfirmAdmission: the place is offered and accepted. The application fee, when one is asked, must be
     * paid or waived, and a cycle the application belongs to must still be running (open or in its admission
     * stage). The admission date becomes today (UTC) when the office gave none.
     */
    public Application confirmAdmission(UUID id, ConfirmAdmissionRequest request) {
        return take(Step.CONFIRM_ADMISSION, id, request == null ? null : request.getEtag(), null);
    }

    /** The template of every step (see the class comment). */
    private Application take(Step step, UUID id, String etag, String givenReason) {
        String reason = reason(givenReason);
        ApplicationAccess access = ApplicationAccess.current(step.permission);
        OptionalInt expected = Etags.bodyVersion(etag);
        return doInWriteTransaction(() -> {
            ApplicationEntity application = applications.findById(id).filter(access::reads)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!access.selfService(application)) {
                access.scope().requireWritable(Optional.ofNullable(application.getInstitutionId()));
            } else if (!step.applicantMay) {
                throw new PermissionDeniedException("Only the admissions office takes this step.");
            }
            if (expected.isPresent() && expected.getAsInt() != application.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!step.from.contains(application.getStatus())) {
                throw new InvalidStateException("The application cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            OffsetDateTime applicationDate = null;
            LocalDate admissionDate = null;
            if (step == Step.SUBMIT) {
                requireSubmittable(application, now.toLocalDate());
                applicationDate = now;
            } else if (step == Step.CONFIRM_ADMISSION) {
                requireAdmittable(application);
                admissionDate = now.toLocalDate();
            }
            if (applications.transition(id, application.getRowVersion(), step.to, applicationDate, admissionDate, now,
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, step.event, changedFields(step), reason);
            outbox.publish(AGGREGATE + "." + step.event, AGGREGATE, id, AdmissionEvents.payload(id));
            return applicationService.dtoFor(access, applications.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    private static List<String> changedFields(Step step) {
        return switch (step) {
            case SUBMIT -> List.of("status", "applicationDate");
            case CONFIRM_ADMISSION -> List.of("status", "admissionDate");
            default -> List.of("status");
        };
    }

    /**
     * What handing in needs: a guardian on the application (422), and — for an application of a cycle — the cycle
     * open (409) and not past its end date (409), of the application's own institution (422), a place left (409) and the
     * applicant old enough on the day the cycle starts (422). The cycle row is
     * locked for the rest of the transaction, so two submissions cannot both take the last place and the cycle
     * cannot leave the open state under one.
     */
    private void requireSubmittable(ApplicationEntity application, LocalDate today) {
        if (!applications.referencedByApplicationGuardianApplicationId(application.getId())) {
            throw new ValidationFailedException(List.of(
                    new FieldError("guardians", "an application needs a parent or guardian", "REQUIRED")));
        }
        if (application.getAdmissionCycleId() == null) {
            return;
        }
        AdmissionCycleEntity cycle = cycles.lockById(application.getAdmissionCycleId())
                .filter(found -> AdmissionCycleService.OPEN.equals(found.getStatus()))
                .orElseThrow(() -> new InvalidStateException("The admission cycle is not open for applications."));
        if (!cycle.getInstitutionId().equals(application.getInstitutionId())) {
            throw new ValidationFailedException(List.of(new FieldError("admissionCycleId",
                    "the admission cycle belongs to another institution", "INVALID_REFERENCE")));
        }
        if (cycle.getEndDate() != null && today.isAfter(cycle.getEndDate())) {
            throw new InvalidStateException("The admission cycle no longer takes applications: it ended on "
                    + cycle.getEndDate() + ".");
        }
        Integer places = cycle.getMaxApplications();
        if (places != null && applications.countInCycle(cycle.getId()) >= places) {
            throw new InvalidStateException("The admission cycle has no place left.");
        }
        Integer minimumAge = cycle.getMinimumAgeYears();
        if (minimumAge != null
                && Period.between(application.getDateOfBirth(), cycle.getStartDate()).getYears() < minimumAge) {
            throw new ValidationFailedException(List.of(new FieldError("dateOfBirth",
                    "the applicant must be at least " + minimumAge + " years old when the cycle starts", "MIN_AGE")));
        }
    }

    /** What confirming an admission needs: the fee settled when one is asked, and a cycle that still runs. */
    private void requireAdmittable(ApplicationEntity application) {
        BigDecimal fee = application.getApplicationFee();
        if (fee != null && fee.signum() > 0 && !FEE_SETTLED.contains(application.getPaymentStatus())) {
            throw new InvalidStateException("The application fee is neither paid nor waived.");
        }
        if (application.getAdmissionCycleId() == null) {
            return;
        }
        String cycleStatus = cycles.findById(application.getAdmissionCycleId())
                .map(AdmissionCycleEntity::getStatus).orElse(null);
        if (!AdmissionCycleService.OPEN.equals(cycleStatus) && !AdmissionCycleService.ADMISSION.equals(cycleStatus)) {
            throw new InvalidStateException("The admission cycle is not running.");
        }
    }

    /** A reason as it is kept: trimmed, null when blank, refused when longer than the limit (422). */
    private static String reason(String given) {
        if (given == null || given.isBlank()) {
            return null;
        }
        String trimmed = given.trim();
        if (trimmed.length() > MAX_REASON) {
            throw new ValidationFailedException(List.of(
                    new FieldError("reason", "must be at most " + MAX_REASON + " characters", "Size")));
        }
        return trimmed;
    }
}
