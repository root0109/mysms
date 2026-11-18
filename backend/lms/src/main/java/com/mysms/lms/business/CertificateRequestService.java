package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.CertificateRequest;
import com.mysms.lms.api.model.CertificateRequestUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.CertificateRequestEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.CertificateRequestMapper;
import com.mysms.lms.repository.CertificateRequestRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.mysms.authclient.AuthContext;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.lms.api.model.CertificateEvaluation;
import com.mysms.lms.domain.CertificateEvaluationEntity;
import com.mysms.lms.mapper.CertificateEvaluationMapper;
import com.mysms.lms.repository.CertificateEvaluationRepository;
import com.mysms.lms.repository.CertificationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;

/**
 * Business service of lms.certificate_request: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class CertificateRequestService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = CertificateRequestEntity.TABLE;
    static final String PERMISSION_LIST = "lms.certificate_request.read";
    static final String PERMISSION_GET = "lms.certificate_request.read";
    static final String PERMISSION_CREATE = "lms.certificate_request.create";
    static final String PERMISSION_UPDATE = "lms.certificate_request.update";
    static final String PERMISSION_DELETE = "lms.certificate_request.delete";
    static final String PERMISSION_STAFF = "lms.evaluator.update";
    /** The contract's CertificateRequestUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("courseId", "cohortId", "evaluatorUserId", "requestDate", "weekday", "startTime",
                    "endTime",
            "timezone", "meetingLink");


    private final CertificateRequestRepository repository;
    private final CertificateRequestMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CertificationRepository certification;
    private final CertificateEvaluationRepository evaluations;
    private final CertificateEvaluationMapper evaluationMapper;
    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> SECRETS = Set.of("meetingLink");
    private static final String SECRET_MEETING_LINK_ENC = "lms.certificate_request.meeting_link_enc";
    private final FieldCipher cipher;

    public CertificateRequestService(CertificateRequestRepository repository, CertificateRequestMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, FieldCipher cipher, CertificationRepository certification,
            CertificateEvaluationRepository evaluations, CertificateEvaluationMapper evaluationMapper) {

        super(database, transactions);
        this.certification = certification;
        this.evaluations = evaluations;
        this.evaluationMapper = evaluationMapper;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.cipher = cipher;
    }

    /** Refuses to start without a master key: this resource stores secret columns. */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public CertificateRequest getCertificateRequest(UUID id) {
        return doInReadTransaction(() -> {
            return dto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<CertificateRequest> listCertificateRequests(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CertificateRequestRepository.SORT_FIELDS,
                        CertificateRequestRepository.DEFAULT_ORDER,
                CertificateRequestRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CertificateRequestRepository.FILTER_FIELDS)
                .and(CertificateRequestRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CertificateRequestEntity> rows = repository.list(page, where);
            return rows.map(this::dto);
        });
    }

    /**
     * Books an evaluation slot for the caller (status upcoming, set by the service); with validateOnly every check runs
     * and nothing is written.
     */
    public CertificateRequest createCertificateRequest(CertificateRequest request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("endTime", request.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        CertificateRequestEntity entity = mapper.toEntity(request);
        entity.setUserId(OwnRows.caller());
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealMeetingLink(entity, request.getMeetingLink());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return dto(entity);
            }
            CertificateRequestEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return dto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public CertificateRequest updateCertificateRequest(UUID id, String ifMatch, CertificateRequestUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        WireValues.checkTime("endTime", changes.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            CertificateRequestEntity entity = loadVisible(scope, id);
            requireReached(entity);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validateExcept(dto(entity), SECRETS);
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return dto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return dto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCertificateRequest(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CertificateRequestEntity entity = loadVisible(scope, id);
            requireReached(entity);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCertificateEvaluationCertificateRequestId(id)) {
                throw new InvalidStateException(
                        "The certificate request is still referenced by lms.certificate_evaluation rows; remove "
                                + "those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** Stores meetingLink encrypted for the row's tenant and column; never the plaintext. */
    private void sealMeetingLink(CertificateRequestEntity entity, String plaintext) {
        entity.setMeetingLinkEnc(cipher.encrypt(plaintext, entity.getTenantId(), SECRET_MEETING_LINK_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private CertificateRequest dto(CertificateRequestEntity entity) {
        CertificateRequest dto = mapper.toDto(entity);
        dto.setMeetingLinkMasked(
                cipher.maskStored(entity.getMeetingLinkEnc(), entity.getTenantId(), SECRET_MEETING_LINK_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CertificateRequestEntity loadVisible(DataScope scope, UUID id) {
        CertificateRequestEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOf(CertificateRequestEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected CertificateRequestUpdate fields; explicit per field so a mask can clear values. */
    private void apply(CertificateRequestEntity entity, CertificateRequestUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "evaluatorUserId" -> entity.setEvaluatorUserId(changes.getEvaluatorUserId());
                case "requestDate" -> entity.setRequestDate(changes.getRequestDate());
                case "weekday" -> entity.setWeekday(Optional.ofNullable(changes.getWeekday()).map(v -> v.getValue())
                        .orElse(null));
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "endTime" -> entity.setEndTime(WireValues.time(changes.getEndTime()));
                case "timezone" -> entity.setTimezone(changes.getTimezone());
                case "meetingLink" -> sealMeetingLink(entity, changes.getMeetingLink());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CertificateRequestEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getRequestDate() == null) {
            errors.add(new FieldError("requestDate", "must not be null", "NotNull"));
        }
        if (entity.getStartTime() == null) {
            errors.add(new FieldError("startTime", "must not be null", "NotNull"));
        }
        if (entity.getEndTime() == null) {
            errors.add(new FieldError("endTime", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CertificateRequestEntity entity) {

    }

    // ---- the booking's workflow: cancelling it, and starting its evaluation ----

    static final String PERMISSION_CANCEL = "lms.certificate_request.cancel_certificate_request";
    static final String PERMISSION_START_EVALUATION = "lms.certificate_request.start_evaluation";
    private static final String EVALUATION = "lms.certificate_evaluation";
    private static final String UPCOMING = "upcoming";

    /**
     * lms.CancelCertificateRequest: cancels an upcoming booking; the reason is kept with the audit record. The student
     * who booked, the evaluator of the slot or a holder of lms.certificate_request.update in the course's institution
     * may cancel (403 for anybody else who sees it); a booking already completed or cancelled is 409.
     */
    public CertificateRequest cancelCertificateRequest(UUID id, String etag, String reason) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_CANCEL);
        OptionalInt expected = Etags.bodyVersion(etag);
        return doInWriteTransaction(() -> {
            CertificateRequestEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            requireParty(caller, entity);
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!UPCOMING.equals(entity.getStatus())) {
                throw new InvalidStateException("Only an upcoming booking can be cancelled.");
            }
            if (certification.setRequestStatus(id, entity.getRowVersion(), "cancelled", now(), caller.userId()) == 0) {
                throw new PreconditionFailedException("The booking changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), reason);
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, LmsEvents.payload(id));
            return dto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * lms.StartEvaluation: the evaluator (or a holder of lms.certificate_request.update) opens the evaluation of an
     * upcoming booking: an in-progress evaluation of the booked slot is created and the booking is completed. A booking
     * that is not upcoming, or already has an evaluation, is 409.
     */
    public CertificateEvaluation startEvaluation(UUID id) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_START_EVALUATION);
        return doInWriteTransaction(() -> {
            CertificateRequestEntity booking = loadVisible(scope, id);
            scope.requireWritable(institutionOf(booking));
            if (!caller.userId().equals(booking.getEvaluatorUserId()) && !staff(caller, booking)) {
                throw new PermissionDeniedException("Only the booking's evaluator may start its evaluation.");
            }
            if (!UPCOMING.equals(booking.getStatus())) {
                throw new InvalidStateException("Only an upcoming booking can be evaluated.");
            }
            if (certification.hasEvaluation(id)) {
                throw new ConflictException("EVALUATION_EXISTS", "The booking already has an evaluation.");
            }
            CertificateEvaluationEntity evaluation = CertificateEvaluationEntity.builder()
                    .courseId(booking.getCourseId()).userId(booking.getUserId()).cohortId(booking.getCohortId())
                    .certificateRequestId(id).evaluatorUserId(booking.getEvaluatorUserId() != null
                            ? booking.getEvaluatorUserId() : caller.userId())
                    .evaluationDate(booking.getRequestDate()).startTime(booking.getStartTime())
                    .endTime(booking.getEndTime()).status("in_progress").build();
            evaluation.onCreatingData(Actor.current(), clock);
            evaluation.setTenantId(booking.getTenantId());
            CertificateEvaluationEntity stored = evaluations.insert(evaluation);
            if (certification.setRequestStatus(id, booking.getRowVersion(), "completed", now(), caller.userId())
                    == 0) {
                throw new PreconditionFailedException("The booking changed since it was read.");
            }
            audit.recordCreate(EVALUATION, stored.getId());
            outbox.publish(EVALUATION + LmsEvents.CREATED, EVALUATION, stored.getId(), LmsEvents.payload(stored.getId()));
            audit.recordTransition(AGGREGATE, id, "start_evaluation", List.of("status"), null);
            outbox.publish(AGGREGATE + ".start_evaluation", AGGREGATE, id, LmsEvents.payload(id));
            return evaluationMapper.toDto(stored);
        });
    }

    /** The booking's student, its evaluator, or staff of its course's institution (403 for anybody else). */
    private void requireParty(AuthContext caller, CertificateRequestEntity entity) {
        if (!caller.userId().equals(entity.getUserId()) && !caller.userId().equals(entity.getEvaluatorUserId())
                && !staff(caller, entity)) {
            throw new PermissionDeniedException("Only the student, the evaluator or staff may change this booking.");
        }
    }

    /** Who manages evaluators in the booking's institution (the update permission is the student's own too). */
    private boolean staff(AuthContext caller, CertificateRequestEntity entity) {
        return caller.hasPermission(PERMISSION_STAFF)
                && DataScope.of(caller, PERMISSION_STAFF).canRead(institutionOf(entity))
                && (institutionOf(entity).isPresent() || caller.hasTenantWidePermission(PERMISSION_STAFF));
    }

    /** The booking's student, its evaluator or staff may change it; to anybody else it is absent (404). */
    private void requireReached(CertificateRequestEntity entity) {
        AuthContext caller = AuthContext.require();
        if (!caller.userId().equals(entity.getUserId()) && !caller.userId().equals(entity.getEvaluatorUserId())
                && !staff(caller, entity)) {
            throw new ResourceNotFoundException(AGGREGATE, entity.getId());
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
