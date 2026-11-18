package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.CertificateEvaluation;
import com.mysms.lms.api.model.CertificateEvaluationUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
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
import com.mysms.lms.domain.CertificateEvaluationEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.CertificateEvaluationMapper;
import com.mysms.lms.repository.CertificateEvaluationRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.mysms.authclient.AuthContext;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.lms.api.model.Certificate;
import com.mysms.lms.repository.CertificationRepository;
import java.time.LocalDate;

/**
 * Business service of lms.certificate_evaluation: audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class CertificateEvaluationService extends AppTransactionManager {

    public static final String AGGREGATE = CertificateEvaluationEntity.TABLE;
    static final String PERMISSION_LIST = "lms.certificate_evaluation.read";
    static final String PERMISSION_GET = "lms.certificate_evaluation.read";
    static final String PERMISSION_CREATE = "lms.certificate_evaluation.create";
    static final String PERMISSION_UPDATE = "lms.certificate_evaluation.update";
    static final String PERMISSION_DELETE = "lms.certificate_evaluation.delete";
    /** The contract's CertificateEvaluationUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("courseId", "userId", "cohortId", "certificateRequestId", "evaluatorUserId", "evaluationDate",
                    "startTime",
            "endTime", "rating", "summary", "status");


    private final CertificateEvaluationRepository repository;
    private final CertificateEvaluationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CertificateService certificates;
    private final CertificationRepository certification;

    public CertificateEvaluationService(CertificateEvaluationRepository repository, CertificateEvaluationMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, CertificateService certificates, CertificationRepository certification) {

        super(database, transactions);
        this.certificates = certificates;
        this.certification = certification;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public CertificateEvaluation getCertificateEvaluation(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<CertificateEvaluation> listCertificateEvaluations(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CertificateEvaluationRepository.SORT_FIELDS,
                        CertificateEvaluationRepository.DEFAULT_ORDER,
                CertificateEvaluationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CertificateEvaluationRepository.FILTER_FIELDS)
                .and(CertificateEvaluationRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CertificateEvaluationEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CertificateEvaluation createCertificateEvaluation(CertificateEvaluation request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("endTime", request.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        CertificateEvaluationEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            CertificateEvaluationEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public CertificateEvaluation updateCertificateEvaluation(UUID id, String ifMatch,
            CertificateEvaluationUpdate changes, String updateMask, boolean validateOnly) {
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
            CertificateEvaluationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCertificateEvaluation(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CertificateEvaluationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CertificateEvaluationEntity loadVisible(DataScope scope, UUID id) {
        CertificateEvaluationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOf(CertificateEvaluationEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected CertificateEvaluationUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(CertificateEvaluationEntity entity, CertificateEvaluationUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "userId" -> entity.setUserId(changes.getUserId());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "certificateRequestId" -> entity.setCertificateRequestId(changes.getCertificateRequestId());
                case "evaluatorUserId" -> entity.setEvaluatorUserId(changes.getEvaluatorUserId());
                case "evaluationDate" -> entity.setEvaluationDate(changes.getEvaluationDate());
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "endTime" -> entity.setEndTime(WireValues.time(changes.getEndTime()));
                case "rating" -> entity.setRating(WireValues.decimal(changes.getRating()));
                case "summary" -> entity.setSummary(changes.getSummary());
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CertificateEvaluationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getEvaluationDate() == null) {
            errors.add(new FieldError("evaluationDate", "must not be null", "NotNull"));
        }
        if (entity.getStartTime() == null) {
            errors.add(new FieldError("startTime", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("rating", WireValues.decimal(entity.getRating()), 3, 2, errors);
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getCertificateRequestId() != null
                && !repository.existsCertificateRequestId(entity.getCertificateRequestId())) {
            errors.add(new FieldError("certificateRequestId", "certificate request does not exist", "NOT_FOUND"));
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

    private void assertUnique(CertificateEvaluationEntity entity) {

    }

    // ---- issuing the certificate a passed evaluation earns ----

    static final String PERMISSION_ISSUE = "lms.certificate_evaluation.issue_certificate_from_evaluation";

    /**
     * lms.IssueCertificateFromEvaluation: the evaluator (or a holder of lms.certificate_evaluation.update in the course's
     * institution) issues the course certificate a passed evaluation earns. The evaluation must have passed (409), the
     * student must not already hold the certificate (409 CERTIFICATE_EXISTS), and an expiry may not precede the issue
     * date (422).
     */
    public Certificate issueCertificateFromEvaluation(UUID id, String templateCode, LocalDate issueDate,
            LocalDate expiryDate, boolean publish) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_ISSUE);
        return doInWriteTransaction(() -> {
            CertificateEvaluationEntity evaluation = loadVisible(scope, id);
            scope.requireWritable(institutionOf(evaluation));
            boolean staff = caller.hasPermission(PERMISSION_UPDATE)
                    && DataScope.of(caller, PERMISSION_UPDATE).canRead(institutionOf(evaluation))
                    && (institutionOf(evaluation).isPresent() || caller.hasTenantWidePermission(PERMISSION_UPDATE));
            if (!caller.userId().equals(evaluation.getEvaluatorUserId()) && !staff) {
                throw new PermissionDeniedException("Only the evaluator may issue this certificate.");
            }
            if (!"pass".equals(evaluation.getStatus())) {
                throw new InvalidStateException("Only a passed evaluation earns a certificate.");
            }
            if (certification.holdsForCourse(evaluation.getCourseId(), evaluation.getCohortId(),
                    evaluation.getUserId())) {
                throw new ConflictException("CERTIFICATE_EXISTS", "The student already holds this certificate.");
            }
            UUID evaluator = evaluation.getEvaluatorUserId() == null ? null
                    : certification.evaluatorOf(evaluation.getEvaluatorUserId()).orElse(null);
            Certificate issued = certificates.issue(new CertificateService.Issue(evaluation.getUserId(),
                    evaluation.getCourseId(), evaluation.getCohortId(), evaluator, templateCode, issueDate, expiryDate,
                    publish));
            audit.recordTransition(AGGREGATE, id, "issue_certificate", List.of(), null);
            outbox.publish(AGGREGATE + ".issue_certificate", AGGREGATE, id, LmsEvents.payload(id));
            return issued;
        });
    }
}
