package com.mysms.privacy.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.AccessSummary;
import com.mysms.privacy.api.model.AssignRightsRequestRequest;
import com.mysms.privacy.api.model.CloseRightsRequestRequest;
import com.mysms.privacy.api.model.ExtendRightsRequestRequest;
import com.mysms.privacy.api.model.FulfilAccessRequestRequest;
import com.mysms.privacy.api.model.FulfilRightsRequestRequest;
import com.mysms.privacy.api.model.MyRightsRequest;
import com.mysms.privacy.api.model.RejectRightsRequestRequest;
import com.mysms.privacy.api.model.RetryRightsRequestTaskRequest;
import com.mysms.privacy.api.model.RightsRequest;
import com.mysms.privacy.api.model.RightsRequestCorrection;
import com.mysms.privacy.api.model.SubmitMyRequestRequest;
import com.mysms.privacy.api.model.VerifyRightsRequestIdentityRequest;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.NomineeEntity;
import com.mysms.privacy.domain.RightsRequestEntity;
import com.mysms.privacy.domain.RightsRequestTaskEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.AccessSummaryMapper;
import com.mysms.privacy.mapper.RightsRequestMapper;
import com.mysms.privacy.model.AccessSummarySnapshot;
import com.mysms.privacy.model.Majority;
import com.mysms.privacy.model.RightsRequestDetails;
import com.mysms.privacy.repository.GuardianLinkRepository;
import com.mysms.privacy.repository.NomineeRepository;
import com.mysms.privacy.repository.NumberSequenceRepository;
import com.mysms.privacy.repository.RightsRequestRepository;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The rights request aggregate (privacy.rights_request): a data principal's request to exercise a right, and the
 * staff's steps on it (design §4.3). Every change writes its audit record and its event in the same transaction.
 *
 * <p>Self-service: the requester is always the caller's own person from the token, never a field. The caller asks for
 * themself only as an adult (capacity self), and for a subject only as the active legal guardian of a minor (capacity
 * guardian) — the rule consent decisions follow; anything else is 404 NOT_FOUND, so the answer never confirms that a
 * child exists. A minor's own request is the same 404: their legal guardian exercises their rights, exactly as for
 * consent. A signed-in session verifies the requester, so a submitted request is verified at once — except a
 * nominee's: the active nominee of an adult (privacy.nominee) submits a nomination request (the principal died or is
 * incapacitated) and, once the staff fulfil it, any other request for the principal, always in the submitted state
 * for the staff to verify in person or by document. The requester sees only their own requests, another person's is
 * 404. No request is submitted or decided while impersonating (403). A correction names the record and the fields
 * to correct, a grievance its category; no other type carries either (422).
 *
 * <p>Staff: rights requests are tenant-level data — reading needs the permission, every step a tenant-wide grant.
 * Each step compares the body etag (412), checks the state it leaves (409) and writes under the row_version guard.
 * No staff member takes a step on a request they are the subject or the requester of, nor is assigned one through any
 * login account privacy knows for either person (409 RIGHTS_REQUEST_CONFLICT_OF_INTEREST, the separation-of-duties
 * answer consent's four-eyes rule gives). An extension moves the deadline later and is not a state; overdue is
 * derived at read time. Closing answers the requester: a grievance always, a declined correction with its reason
 * (422 without); closing a nomination request as fulfilled lets the nominee act for the principal.
 *
 * <p>Access: an access request is answered with the subject's access summary ({@link AccessSummaryFactory}). The
 * staff preview it while the request is open; fulfilling the request keeps the summary on it and closes it, and from
 * then on the staff and the requester see that summary, whatever changes later. An access request is fulfilled only
 * that way: closing it may only decline it, so a request closed as fulfilled always holds the summary sent.
 *
 * <p>Erasure and export: the staff start the fulfilment of a request in progress, and {@link RightsRequestSaga} sends
 * one task to each module and closes the request as the modules answer (action_required while a task failed for good,
 * until a retry dispatches it again). Withdraw-all: the staff fulfil it in one step that withdraws every live consent
 * decision the requester gave about the subject (the principal's own too when the requester is their nominee). These
 * requests are fulfilled only so: closing one may only decline it, and only before its fulfilment started.
 */
@Service
public class RightsRequestService extends AppTransactionManager {

    public static final String AGGREGATE = RightsRequestEntity.TABLE;
    static final String PERMISSION_VERIFY_IDENTITY = "privacy.rights_request.verify_identity";
    static final String PERMISSION_ASSIGN = "privacy.rights_request.assign";
    static final String PERMISSION_EXTEND = "privacy.rights_request.extend";
    static final String PERMISSION_REJECT = "privacy.rights_request.reject";
    static final String PERMISSION_CLOSE = "privacy.rights_request.close";
    static final String PERMISSION_FULFIL = "privacy.rights_request.fulfil";
    static final String PERMISSION_RETRY_TASK = "privacy.rights_request.retry_task";
    /** The number series of rights requests in privacy.number_sequence (per tenant and calendar year). */
    static final String NUMBER_SEQUENCE = "rights_request";

    /** No institution owns a row of this aggregate. */
    private static final Optional<UUID> TENANT_LEVEL = Optional.empty();
    private static final String SELF = "self";
    private static final String GUARDIAN = "guardian";
    private static final String NOMINEE = "nominee";
    private static final String SUBMITTED = "submitted";
    private static final String VERIFIED = "verified";
    private static final String IN_PROGRESS = "in_progress";
    private static final String ACTION_REQUIRED = "action_required";
    private static final String REJECTED = "rejected";
    private static final String CLOSED = "closed";
    private static final String ACCESS = "access";
    private static final String CORRECTION = "correction";
    private static final String GRIEVANCE = "grievance";
    private static final String NOMINATION = "nomination";
    private static final String ERASURE = "erasure";
    private static final String EXPORT = "export";
    private static final String WITHDRAW_ALL = "withdraw_all";
    private static final String DECLINED = "declined";
    private static final String CONFLICT_OF_INTEREST = "RIGHTS_REQUEST_CONFLICT_OF_INTEREST";
    private static final String DETAILS_REQUIRED = "RIGHTS_REQUEST_DETAILS_REQUIRED";
    private static final String DETAILS_NOT_ALLOWED = "RIGHTS_REQUEST_DETAILS_NOT_ALLOWED";
    private static final String FULFILLED = "fulfilled";
    private static final String PARTIALLY_FULFILLED = "partially_fulfilled";
    private static final String SIGNED_IN_SESSION = "signed_in_session";
    /** What a 404 names when the requester may not ask about the subject: a person, never confirmed to exist. */
    private static final String SUBJECT = "person";
    private static final String DUE_AT_NOT_LATER = "must be later than the request's current due date";
    private static final String NOT_ACCESS = "Only an access request has an access summary.";
    private static final String ACCESS_NOT_CLOSED_FULFILLED = "An access request is fulfilled with its access summary"
            + " (FulfilAccessRequest); close it here only as declined.";
    private static final String FULFILLED_BY_ITS_STEP = "An erasure, export or withdraw-all request is fulfilled by its"
            + " own steps; close it here only as declined.";

    private static final Step VERIFY_IDENTITY = new Step("verify_identity", PrivacyEvents.VERIFIED, in(SUBMITTED),
            "Only a submitted rights request can have its requester verified.", List.of("status", "identityCheck"));
    private static final Step ASSIGN = new Step("assign", PrivacyEvents.ASSIGNED, in(VERIFIED, IN_PROGRESS),
            "Only a verified rights request, or one in progress, can be assigned.", List.of("status", "assigneeUserId"));
    private static final Step EXTEND = new Step("extend", PrivacyEvents.EXTENDED, RightsRequestService::open,
            "A closed or rejected rights request cannot be extended.", List.of("dueAt", "extendedReason"));
    private static final Step REJECT = new Step("reject", PrivacyEvents.REJECTED, in(SUBMITTED, VERIFIED),
            "Only a submitted or verified rights request can be rejected.",
            List.of("status", "outcomeCode", "closedAt"));
    private static final Step CLOSE = new Step("close", PrivacyEvents.CLOSED, in(IN_PROGRESS),
            "Only a rights request in progress can be closed.",
            List.of("status", "outcomeCode", "outcomeNote", "closedAt"));
    private static final Step FULFIL_ACCESS = new Step("fulfil_access", PrivacyEvents.CLOSED,
            in(IN_PROGRESS).and(RightsRequestService::access),
            "Only an access request in progress can be fulfilled with its access summary.",
            List.of("status", "outcomeCode", "closedAt", "accessSummary"), true);
    private static final Step START_FULFILMENT = new Step("start_fulfilment", PrivacyEvents.FULFILMENT_STARTED,
            in(IN_PROGRESS).and(RightsRequestService::fulfilledByTasks),
            "Only an erasure or export request in progress can start its fulfilment.", List.of("tasks"));
    private static final Step RETRY_TASK = new Step("retry_task", PrivacyEvents.TASK_RETRIED,
            in(IN_PROGRESS, ACTION_REQUIRED).and(RightsRequestService::fulfilledByTasks),
            "Only an erasure or export request being fulfilled can have a task retried.", List.of("status", "tasks"));
    private static final Step FULFIL_WITHDRAW_ALL = new Step("fulfil_withdraw_all", PrivacyEvents.CLOSED,
            in(IN_PROGRESS).and(r -> WITHDRAW_ALL.equals(r.getType())),
            "Only a withdraw-all request in progress can be fulfilled by withdrawing its consents.",
            List.of("status", "outcomeCode", "closedAt"), true);

    /**
     * A staff step: its audit name, its event, the states it leaves (refused with the sentence otherwise), the fields
     * it changes, and whether it reads the subject's data and so first takes the subject's row lock, as every consent
     * change does.
     */
    private record Step(String name, String event, Predicate<RightsRequestEntity> allowed, String refusal,
                        List<String> changedFields, boolean locksSubject) {

        /** A step that changes the request only. */
        Step(String name, String event, Predicate<RightsRequestEntity> allowed, String refusal,
             List<String> changedFields) {
            this(name, event, allowed, refusal, changedFields, false);
        }
    }

    /**
     * The step's own write of the loaded request: sets the changed fields on it and runs the guarded update, answering
     * the rows it changed (0 = the row changed since it was read).
     */
    @FunctionalInterface
    private interface StepWrite {
        int write(RightsRequestEntity request, OffsetDateTime at, UUID actor);
    }

    private final RightsRequestRepository repository;
    private final NumberSequenceRepository numbers;
    private final RightsRequestMapper mapper;
    private final AccessSummaryFactory summaries;
    private final AccessSummaryMapper summaryMapper;
    private final SubjectRepository subjects;
    private final GuardianLinkRepository links;
    private final NomineeRepository nominees;
    private final RightsRequestSla sla;
    private final RightsRequestSaga saga;
    private final ConsentService consents;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public RightsRequestService(RightsRequestRepository repository,
            NumberSequenceRepository numbers,
            RightsRequestMapper mapper,
            AccessSummaryFactory summaries,
            AccessSummaryMapper summaryMapper,
            SubjectRepository subjects,
            GuardianLinkRepository links,
            NomineeRepository nominees,
            RightsRequestSla sla,
            RightsRequestSaga saga,
            ConsentService consents,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.numbers = numbers;
        this.mapper = mapper;
        this.summaries = summaries;
        this.summaryMapper = summaryMapper;
        this.subjects = subjects;
        this.links = links;
        this.nominees = nominees;
        this.sla = sla;
        this.saga = saga;
        this.consents = consents;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    // ---- the requester (self-service) ----

    /**
     * privacy.SubmitMyRequest: the caller's request about themself (an adult), a minor they are the active legal
     * guardian of, or an adult they are the active nominee of — the typed details checked first (422), then
     * entitlement (404) before anything is written. Verified by the signed-in session, except a nominee's request,
     * which waits in the submitted state for the staff to verify; numbered PRV-year-number from the tenant's series of
     * the year, due after the type's response period. Times, the number's year and the deadline are UTC, as every
     * stored instant (a request at 00:30 IST on 1 January is numbered in the year before).
     */
    public MyRightsRequest submitMyRequest(SubmitMyRequestRequest request) {
        validator.validate(request);
        String type = request.getType().getValue();
        RightsRequestDetails details = details(type, request);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        UUID requester = callerPerson(caller);
        UUID subject = request.getSubjectPersonId();
        RightsRequestEntity entity = RightsRequestEntity.builder()
                .type(type)
                .subjectPersonId(subject)
                .requesterPersonId(requester)
                .details(details)
                .build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(tenant());
        OffsetDateTime submittedAt = entity.getCreatedAt();
        entity.setSubmittedAt(submittedAt);
        entity.setDueAt(sla.dueAt(entity.getType(), submittedAt));
        return doInWriteTransaction(() -> {
            // the subject's row first, as every consent change takes it: entitlement is read under that lock
            subjects.lockStatus(subject, submittedAt);
            String capacity = capacity(requester, subject, type);
            entity.setRequesterCapacity(capacity);
            // a signed-in session verifies who the requester is, never that a nominee may act for the principal
            entity.setStatus(NOMINEE.equals(capacity) ? SUBMITTED : VERIFIED);
            entity.setIdentityCheck(NOMINEE.equals(capacity) ? null : SIGNED_IN_SESSION);
            String year = String.valueOf(submittedAt.getYear());
            entity.setNumber(numbers.next(UuidV7.next(clock), NUMBER_SEQUENCE, year, "PRV-" + year + "-",
                    submittedAt, entity.getCreatedBy()));
            repository.insert(entity);
            audit.recordCreate(AGGREGATE, entity.getId());
            outbox.publish(AGGREGATE + PrivacyEvents.SUBMITTED, AGGREGATE, entity.getId(),
                    PrivacyEvents.rightsRequest(entity));
            return mine(entity, submittedAt);
        });
    }

    /**
     * privacy.ListMyRequests: the requests the caller submitted about someone they still decide for, newest first; a
     * guardian whose link ended, or whose ward came of age, no longer sees the ward's requests.
     */
    public Page<MyRightsRequest> listMyRequests(Integer pageNumber, Integer pageSize, String cursor) {
        UUID requester = callerPerson(AuthContext.require());
        SortSpec sort = SortSpec.parse(null, RightsRequestRepository.SORT_FIELDS, RightsRequestRepository.NEWEST_FIRST,
                RightsRequestRepository.ID_COLUMN);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, "requester|" + requester, clock);
        return doInReadTransaction(() -> {
            OffsetDateTime now = now();
            return repository.list(page, RightsRequestRepository.requestedBy(requester)).map(r -> mine(r, now));
        });
    }

    /**
     * privacy.GetMyRequest: one of the caller's own requests, while they still decide for its subject (as
     * {@link #listMyRequests}); another person's is 404.
     */
    public MyRightsRequest getMyRequest(UUID id) {
        UUID requester = callerPerson(AuthContext.require());
        return doInReadTransaction(() -> mine(visibleTo(requester, id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)), now()));
    }

    /**
     * privacy.GetMyAccessSummary: the summary kept on one of the caller's own access requests once it was fulfilled,
     * seen as {@link #getMyRequest} sees the request. Before that there is no summary to read, so the answer is 404 —
     * as for a request of another type, another person's or one about someone the caller no longer decides for —
     * rather than 409: the request's own status already tells the requester where it stands.
     */
    public AccessSummary getMyAccessSummary(UUID id) {
        UUID requester = callerPerson(AuthContext.require());
        return doInReadTransaction(() -> visibleTo(requester, id)
                .flatMap(r -> repository.findAccessSummary(r.getId()))
                .map(summaryMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
    }

    /**
     * One of the requester's own requests, while they still decide for its subject or, for a request submitted as
     * nominee, are still the subject's active nominee; empty for any other.
     */
    private Optional<RightsRequestEntity> visibleTo(UUID requester, UUID id) {
        return repository.findById(id)
                .filter(r -> requester.equals(r.getRequesterPersonId()))
                .filter(r -> NOMINEE.equals(r.getRequesterCapacity())
                        ? nominees.findActive(r.getSubjectPersonId(), requester).isPresent()
                        : decidesFor(requester, r.getSubjectPersonId()));
    }

    /**
     * In which capacity the requester may ask about the subject: self as an adult, guardian as the active legal
     * guardian of a minor (a person of unknown age counts as a minor, as for consent), nominee as the subject's active
     * nominee; anyone else is 404. Only a nominee submits a nomination request, and any other type only once their
     * nomination was fulfilled (422 on the type otherwise).
     */
    private String capacity(UUID requester, UUID subject, String type) {
        if (decidesFor(requester, subject)) {
            if (NOMINATION.equals(type)) {
                throw invalid("type", "only the principal's nominee submits a nomination request",
                        "RIGHTS_REQUEST_TYPE_NOT_ALLOWED");
            }
            return requester.equals(subject) ? SELF : GUARDIAN;
        }
        NomineeEntity nominee = nominees.findActive(subject, requester)
                .orElseThrow(() -> new ResourceNotFoundException(SUBJECT, subject));
        if (!NOMINATION.equals(type) && nominee.getActivatedAt() == null) {
            throw invalid("type", "a nominee acts for the principal only once their nomination request is fulfilled",
                    "RIGHTS_REQUEST_NOMINEE_NOT_ACTIVE");
        }
        return NOMINEE;
    }

    /**
     * The request's details, typed by its type: a correction must name its record and fields, a grievance its
     * category, and no other type may carry either (422).
     */
    private static RightsRequestDetails details(String type, SubmitMyRequestRequest request) {
        RightsRequestCorrection correction = request.getCorrection();
        SubmitMyRequestRequest.GrievanceCategoryEnum category = request.getGrievanceCategory();
        if (CORRECTION.equals(type) != (correction != null)) {
            throw CORRECTION.equals(type)
                    ? invalid("correction", "a correction request names the record and the fields to correct",
                            DETAILS_REQUIRED)
                    : invalid("correction", "only a correction request names a record to correct", DETAILS_NOT_ALLOWED);
        }
        if (GRIEVANCE.equals(type) != (category != null)) {
            throw GRIEVANCE.equals(type)
                    ? invalid("grievanceCategory", "a grievance says what it is about", DETAILS_REQUIRED)
                    : invalid("grievanceCategory", "only a grievance has a category", DETAILS_NOT_ALLOWED);
        }
        RightsRequestDetails.Correction named = correction == null ? null : new RightsRequestDetails.Correction(
                correction.getModule(), correction.getRecordType().strip(), correction.getRecordId(),
                correction.getFields().stream()
                        .map(f -> new RightsRequestDetails.Field(f.getField().strip(), f.getRequestedValue()))
                        .toList());
        return new RightsRequestDetails(request.getDescription(), named, category == null ? null : category.getValue());
    }

    /** Whether the person decides for the subject today: themself as an adult, or a minor as an active guardian. */
    private boolean decidesFor(UUID person, UUID subject) {
        boolean adult = subjects.majority(subject).filter(m -> Majority.ADULT.equals(m.status())).isPresent();
        return person.equals(subject) ? adult : !adult && links.isLegalGuardian(person, subject);
    }

    // ---- the staff ----

    public RightsRequest getRightsRequest(UUID id) {
        return doInReadTransaction(() -> staff(load(id), now()));
    }

    public Page<RightsRequest> listRightsRequests(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RightsRequestRepository.SORT_FIELDS,
                RightsRequestRepository.DEFAULT_ORDER, RightsRequestRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RightsRequestRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            OffsetDateTime now = now();
            Page<RightsRequestEntity> rows = repository.list(page, where);
            Map<UUID, List<RightsRequestTaskEntity>> tasks = saga.tasksOf(rows.items().stream()
                    .filter(RightsRequestService::fulfilledByTasks).map(RightsRequestEntity::getId).toList());
            return rows.map(r -> staff(r, tasks.getOrDefault(r.getId(), List.of()), now));
        });
    }

    /**
     * privacy.GetAccessSummary: the access summary of an access request (409 for another type) — while the request is
     * open, the staff's preview of what privacy holds about the subject now; once it ended, the one kept on it when it
     * was fulfilled, and 404 when it ended without one (rejected, or closed as declined): a request that is over is
     * never answered with data generated after it.
     */
    public AccessSummary getAccessSummary(UUID id) {
        return doInReadTransaction(() -> {
            RightsRequestEntity request = load(id);
            if (!access(request)) {
                throw new InvalidStateException(NOT_ACCESS);
            }
            if (open(request)) {
                return summaryMapper.toDto(summaries.build(request.getSubjectPersonId(), now()));
            }
            return repository.findAccessSummary(id).map(summaryMapper::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        });
    }

    /** privacy.VerifyRightsRequestIdentity: submitted → verified, the requester seen in person or by document. */
    public RightsRequest verifyRightsRequestIdentity(UUID id, VerifyRightsRequestIdentityRequest request) {
        requireTenantWide(PERMISSION_VERIFY_IDENTITY);
        validator.validate(request);
        String identityCheck = request.getIdentityCheck().getValue();
        return step(id, request.getEtag(), VERIFY_IDENTITY, null, (r, at, actor) -> {
            r.setStatus(VERIFIED);
            r.setIdentityCheck(identityCheck);
            return repository.verifyIdentity(r.getId(), r.getRowVersion(), identityCheck, at, actor);
        });
    }

    /**
     * privacy.AssignRightsRequest: verified or in_progress → in_progress with the staff member named — never through
     * a login account privacy knows for the request's subject or requester (409 RIGHTS_REQUEST_CONFLICT_OF_INTEREST).
     */
    public RightsRequest assignRightsRequest(UUID id, AssignRightsRequestRequest request) {
        requireTenantWide(PERMISSION_ASSIGN);
        validator.validate(request);
        UUID assignee = request.getAssigneeUserId();
        return step(id, request.getEtag(), ASSIGN, null, (r, at, actor) -> {
            if (subjects.accountsOf(List.of(r.getSubjectPersonId(), r.getRequesterPersonId())).contains(assignee)) {
                throw new ConflictException(CONFLICT_OF_INTEREST,
                        "A rights request cannot be assigned to its subject or its requester.");
            }
            r.setStatus(IN_PROGRESS);
            r.setAssigneeUserId(assignee);
            return repository.assign(r.getId(), r.getRowVersion(), assignee, at, actor);
        });
    }

    /**
     * privacy.ExtendRightsRequest: an open request's deadline moves later (422 when the new one is not), with the
     * reason kept on the request and in the audit record; the status does not change.
     */
    public RightsRequest extendRightsRequest(UUID id, ExtendRightsRequestRequest request) {
        requireTenantWide(PERMISSION_EXTEND);
        validator.validate(request);
        OffsetDateTime dueAt = utc(request.getDueAt());
        String reason = request.getReason();
        return step(id, request.getEtag(), EXTEND, reason, (r, at, actor) -> {
            if (!dueAt.isAfter(r.getDueAt())) {
                throw new ValidationFailedException(List.of(
                        new FieldError("dueAt", DUE_AT_NOT_LATER, "RIGHTS_REQUEST_DUE_AT_NOT_LATER")));
            }
            r.setDueAt(dueAt);
            r.setExtendedReason(reason);
            return repository.extend(r.getId(), r.getRowVersion(), dueAt, reason, at, actor);
        });
    }

    /** privacy.RejectRightsRequest: submitted or verified → rejected with its reason code, which ends it. */
    public RightsRequest rejectRightsRequest(UUID id, RejectRightsRequestRequest request) {
        requireTenantWide(PERMISSION_REJECT);
        validator.validate(request);
        String reasonCode = request.getReasonCode().getValue();
        return step(id, request.getEtag(), REJECT, reasonCode, (r, at, actor) -> {
            r.setStatus(REJECTED);
            r.setOutcomeCode(reasonCode);
            r.setClosedAt(at);
            return repository.reject(r.getId(), r.getRowVersion(), reasonCode, at, actor);
        });
    }

    /**
     * privacy.CloseRightsRequest: in_progress → closed with its outcome, the staff's note and the answer the requester
     * sees. An access request closes here only as declined (409 for fulfilled or partially fulfilled): it is fulfilled
     * with its access summary ({@link #fulfilAccessRequest}), so no access request ends as fulfilled without the
     * summary sent. A grievance is always answered, a declined correction with its reason (422 without a response). A
     * nomination request is fulfilled or declined, never partly (409); fulfilling it lets the requester act as the
     * principal's nominee from now on — while they still are the active nominee (409 otherwise).
     */
    public RightsRequest closeRightsRequest(UUID id, CloseRightsRequestRequest request) {
        requireTenantWide(PERMISSION_CLOSE);
        validator.validate(request);
        String outcomeCode = request.getOutcomeCode().getValue();
        // a blank note or response is none (the columns keep 1 to 4000 characters, or nothing)
        String note = blankToNull(request.getNote());
        String response = blankToNull(request.getResponse());
        boolean fulfilment = FULFILLED.equals(outcomeCode) || PARTIALLY_FULFILLED.equals(outcomeCode);
        return step(id, request.getEtag(), CLOSE, null, (r, at, actor) -> {
            if (fulfilment && access(r)) {
                throw new InvalidStateException(ACCESS_NOT_CLOSED_FULFILLED);
            }
            if (fulfilment && (fulfilledByTasks(r) || WITHDRAW_ALL.equals(r.getType()))) {
                throw new InvalidStateException(FULFILLED_BY_ITS_STEP);
            }
            if (fulfilledByTasks(r) && !saga.tasksOf(List.of(r.getId())).isEmpty()) {
                throw new InvalidStateException("An erasure or export request is not declined once its fulfilment"
                        + " started.");
            }
            if (NOMINATION.equals(r.getType()) && PARTIALLY_FULFILLED.equals(outcomeCode)) {
                throw new InvalidStateException("A nomination request is fulfilled or declined, never partly.");
            }
            boolean answerOwed = GRIEVANCE.equals(r.getType())
                    || CORRECTION.equals(r.getType()) && DECLINED.equals(outcomeCode);
            if (answerOwed && response == null) {
                throw invalid("response", "the requester is owed an answer: a grievance's, or why a correction is"
                        + " declined", "RIGHTS_REQUEST_RESPONSE_REQUIRED");
            }
            if (NOMINATION.equals(r.getType()) && FULFILLED.equals(outcomeCode)) {
                activateNominee(r, at, actor);
            }
            r.setStatus(CLOSED);
            r.setOutcomeCode(outcomeCode);
            r.setOutcomeNote(note);
            r.setOutcomeResponse(response);
            r.setClosedAt(at);
            return repository.close(r.getId(), r.getRowVersion(), outcomeCode, note, response, at, actor);
        });
    }

    /**
     * Lets the nomination request's requester act as the subject's nominee from now on, with the nominee's own audit
     * record and event; 409 when they are no longer the subject's active nominee (revoked or replaced meanwhile).
     */
    private void activateNominee(RightsRequestEntity request, OffsetDateTime at, UUID actor) {
        NomineeEntity nominee = nominees.findActive(request.getSubjectPersonId(), request.getRequesterPersonId())
                .filter(n -> n.getActivatedAt() == null)
                .orElseThrow(() -> new InvalidStateException(
                        "The requester is no longer the principal's active nominee, or already acts for them."));
        if (nominees.activate(request.getSubjectPersonId(), request.getRequesterPersonId(), request.getId(), at,
                actor) == 0) {
            throw new PreconditionFailedException("The nominee changed since it was read.");
        }
        nominee.setActivatedAt(at);
        nominee.setActivatedByRequestId(request.getId());
        audit.recordTransition(NomineeService.AGGREGATE, nominee.getId(), "activate",
                List.of("activatedAt", "activatedByRequestId"), null);
        outbox.publish(NomineeService.AGGREGATE + PrivacyEvents.ACTIVATED, NomineeService.AGGREGATE, nominee.getId(),
                PrivacyEvents.nominee(nominee));
    }

    /**
     * privacy.FulfilAccessRequest: an access request in progress → closed as fulfilled, with the subject's access
     * summary generated now, under the subject's row lock, and kept on the request. The etag is required: the staff
     * fulfil the request they reviewed. The summary goes to the requester, never into the event.
     */
    public RightsRequest fulfilAccessRequest(UUID id, FulfilAccessRequestRequest request) {
        requireTenantWide(PERMISSION_FULFIL);
        validator.validate(request);
        return step(id, request.getEtag(), FULFIL_ACCESS, null, (r, at, actor) -> {
            AccessSummarySnapshot summary = summaries.build(r.getSubjectPersonId(), at);
            r.setStatus(CLOSED);
            r.setOutcomeCode(FULFILLED);
            r.setClosedAt(at);
            return repository.fulfilAccess(r.getId(), r.getRowVersion(), summary, at, actor);
        });
    }

    /**
     * privacy.StartRightsRequestFulfilment: an erasure or export request in progress, not started yet (409 otherwise),
     * gets one task per module, the first wave dispatched now ({@link RightsRequestSaga#start}); it stays in progress
     * until the modules answer. The etag is required: the staff start the request they reviewed.
     */
    public RightsRequest startRightsRequestFulfilment(UUID id, FulfilRightsRequestRequest request) {
        requireTenantWide(PERMISSION_FULFIL);
        validator.validate(request);
        return step(id, request.getEtag(), START_FULFILMENT, null, (r, at, actor) -> {
            if (!saga.tasksOf(List.of(r.getId())).isEmpty()) {
                throw new InvalidStateException("The request's fulfilment has already started.");
            }
            saga.start(r, at, actor);
            return fulfilment(r, IN_PROGRESS, at, actor);
        });
    }

    /**
     * privacy.RetryRightsRequestTask: a failed or unanswered task of an erasure or export request being fulfilled is
     * dispatched again under the same id (404 for a task of another request, 409 for one pending or answered); a
     * request waiting for action goes back in progress.
     */
    public RightsRequest retryRightsRequestTask(UUID id, UUID taskId, RetryRightsRequestTaskRequest request) {
        requireTenantWide(PERMISSION_RETRY_TASK);
        validator.validate(request);
        return step(id, request.getEtag(), RETRY_TASK, taskId.toString(), (r, at, actor) -> {
            saga.retry(r, taskId, at, actor);
            return fulfilment(r, IN_PROGRESS, at, actor);
        });
    }

    /**
     * privacy.FulfilWithdrawAllRequest: a withdraw-all request in progress → closed as fulfilled, every live consent
     * decision the requester gave about the subject withdrawn under the subject's row lock (the principal's own too
     * when the requester is their nominee), each with its evidence, audit record and event. The etag is required.
     */
    public RightsRequest fulfilWithdrawAllRequest(UUID id, FulfilRightsRequestRequest request) {
        requireTenantWide(PERMISSION_FULFIL);
        validator.validate(request);
        return step(id, request.getEtag(), FULFIL_WITHDRAW_ALL, null, (r, at, actor) -> {
            Set<UUID> givers = NOMINEE.equals(r.getRequesterCapacity())
                    ? Set.of(r.getRequesterPersonId(), r.getSubjectPersonId()) : Set.of(r.getRequesterPersonId());
            consents.withdrawForRightsRequest(r.getSubjectPersonId(), givers, actor);
            r.setStatus(CLOSED);
            r.setOutcomeCode(FULFILLED);
            r.setClosedAt(at);
            return repository.fulfilWithdrawAll(r.getId(), r.getRowVersion(), at, actor);
        });
    }

    /** The request's guarded move within its fulfilment by tasks (start, retry): status and stamps as given. */
    private int fulfilment(RightsRequestEntity request, String status, OffsetDateTime at, UUID actor) {
        request.setStatus(status);
        request.setUpdatedAt(at);
        request.setUpdatedBy(actor);
        return repository.fulfilment(request, request.getRowVersion());
    }

    /**
     * Runs one staff step: the request loaded (404), refused to a staff member who is its subject or requester (409),
     * the body etag compared (412), the state checked (409), for a step
     * that reads the subject's data the subject's row locked, then the step's write under the row_version guard (0 rows
     * → 412), its audit record and its event, in one transaction. The step's time is taken after the lock, so nothing
     * it records is dated before what it read could last have changed.
     */
    private RightsRequest step(UUID id, String etag, Step step, String reason, StepWrite write) {
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        Optional<UUID> staffPerson = caller.personId();
        OptionalInt expected = Etags.bodyVersion(etag);
        return doInWriteTransaction(() -> {
            RightsRequestEntity request = load(id);
            if (staffPerson.filter(p -> p.equals(request.getSubjectPersonId())
                    || p.equals(request.getRequesterPersonId())).isPresent()
                    || subjects.accountsOf(List.of(request.getSubjectPersonId(), request.getRequesterPersonId()))
                            .contains(caller.userId())) {
                throw new ConflictException(CONFLICT_OF_INTEREST,
                        "A staff member cannot act on a rights request they are the subject or the requester of.");
            }
            if (expected.isPresent() && expected.getAsInt() != request.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!step.allowed().test(request)) {
                throw new InvalidStateException(step.refusal());
            }
            if (step.locksSubject()) {
                subjects.lockStatus(request.getSubjectPersonId(), now());
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            if (write.write(request, now, actor) == 0) {
                throw new PreconditionFailedException("The rights request changed since it was read.");
            }
            request.setUpdatedAt(now);
            request.setUpdatedBy(actor);
            request.setRowVersion(request.getRowVersion() + 1);
            audit.recordTransition(AGGREGATE, id, step.name(), step.changedFields(), reason);
            outbox.publish(AGGREGATE + step.event(), AGGREGATE, id, PrivacyEvents.rightsRequest(request));
            return staff(request, now);
        });
    }

    // ---- shared ----

    private RightsRequestEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** The request as the staff see it, with its tasks when it is fulfilled by them. */
    private RightsRequest staff(RightsRequestEntity request, OffsetDateTime now) {
        return staff(request, fulfilledByTasks(request)
                ? saga.tasksOf(List.of(request.getId())).getOrDefault(request.getId(), List.of()) : List.of(), now);
    }

    private RightsRequest staff(RightsRequestEntity request, List<RightsRequestTaskEntity> tasks,
            OffsetDateTime now) {
        return mapper.toDto(request).overdue(overdue(request, now)).tasks(tasks.stream().map(mapper::toDto).toList());
    }

    private MyRightsRequest mine(RightsRequestEntity request, OffsetDateTime now) {
        return mapper.toMine(request).overdue(overdue(request, now));
    }

    /** Still open past its deadline. */
    private static boolean overdue(RightsRequestEntity request, OffsetDateTime now) {
        return open(request) && request.getDueAt().isBefore(now);
    }

    private static boolean access(RightsRequestEntity request) {
        return ACCESS.equals(request.getType());
    }

    /** An erasure or export request: fulfilled by the modules' tasks. */
    private static boolean fulfilledByTasks(RightsRequestEntity request) {
        return ERASURE.equals(request.getType()) || EXPORT.equals(request.getType());
    }

    /** Neither closed nor rejected. */
    private static boolean open(RightsRequestEntity request) {
        return request.getClosedAt() == null;
    }

    /** A one-field 422 with its code (the sentence comes from the message catalogue in the caller's language). */
    private static ValidationFailedException invalid(String field, String message, String code) {
        return new ValidationFailedException(List.of(new FieldError(field, message, code)));
    }

    private static String blankToNull(String given) {
        return given == null || given.isBlank() ? null : given;
    }

    private static Predicate<RightsRequestEntity> in(String... states) {
        Set<String> allowed = Set.of(states);
        return request -> allowed.contains(request.getStatus());
    }

    /** The caller's person (the token's pid): a rights request is always a person's (403 without one). */
    private static UUID callerPerson(AuthContext caller) {
        return caller.personId()
                .orElseThrow(() -> new PermissionDeniedException("The caller's account is linked to no person."));
    }

    /** Support staff acting as a user may look at requests, never submit or decide one. */
    private static void refuseImpersonation(AuthContext caller) {
        if (caller.impersonatorId().isPresent()) {
            throw new PermissionDeniedException(
                    "Rights requests cannot be submitted or decided while acting as another user.");
        }
    }

    /** Refuses a change without a tenant-wide grant of the permission (403): rights requests are tenant-level. */
    private static void requireTenantWide(String permission) {
        DataScope.current(permission).requireWritable(TENANT_LEVEL);
    }

    private OffsetDateTime now() {
        return utc(OffsetDateTime.now(clock));
    }

    private static OffsetDateTime utc(OffsetDateTime time) {
        return time.withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static UUID tenant() {
        return CallContextHolder.require().tenantId().orElseThrow();
    }
}
