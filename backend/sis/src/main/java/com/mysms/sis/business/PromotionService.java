package com.mysms.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.sis.api.model.ApprovePromotionRequest;
import com.mysms.sis.api.model.Promotion;
import com.mysms.sis.api.model.PromotionUpdate;
import com.mysms.sis.api.model.RejectPromotionRequest;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.PromotionEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.PromotionMapper;
import com.mysms.sis.repository.PromotionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.promotion: audit and domain event in the same transaction as every change; rows outside the
 * caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class PromotionService extends AppTransactionManager {

    public static final String AGGREGATE = PromotionEntity.TABLE;
    static final String PERMISSION_LIST = "sis.promotion.read";
    static final String PERMISSION_GET = "sis.promotion.read";
    public static final String PERMISSION_CREATE = "sis.promotion.create";
    static final String PERMISSION_UPDATE = "sis.promotion.update";
    static final String PERMISSION_DELETE = "sis.promotion.delete";
    static final String PERMISSION_APPROVE_PROMOTION = "sis.promotion.approve_promotion";
    static final String PERMISSION_REJECT_PROMOTION = "sis.promotion.reject_promotion";
    /** The contract's PromotionUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("studentId", "institutionId", "fromAcademicYearId", "fromGradeLevelId", "fromSectionId",
                    "toAcademicYearId",
            "toGradeLevelId", "toSectionId", "basedOnExamSessionId", "remarks");


    private final PromotionRepository repository;
    private final PromotionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public PromotionService(PromotionRepository repository, PromotionMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Promotion getPromotion(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Promotion> listPromotions(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PromotionRepository.SORT_FIELDS, PromotionRepository.DEFAULT_ORDER,
                PromotionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PromotionRepository.FILTER_FIELDS)
                .and(PromotionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PromotionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Promotion createPromotion(Promotion request, boolean validateOnly) {
        validator.validate(request);
        PromotionEntity entity = mapper.toEntity(request);
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
            PromotionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Promotion updatePromotion(UUID id, String ifMatch, PromotionUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PromotionEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + SisEvents.UPDATED, AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePromotion(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PromotionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + SisEvents.DELETED, AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /**
     * sis.ApprovePromotion: Approve. Allowed while status is proposed, for a caller who can see and write the row;
     * otherwise 409 and nothing changes.
     */
    public Promotion approvePromotion(UUID id, ApprovePromotionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_APPROVE_PROMOTION);
        return doInWriteTransaction(() -> {
            PromotionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("proposed").contains(entity.getStatus()))) {
                throw new InvalidStateException("The promotion cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.approvePromotion(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "approve", List.of("status"), null);
            outbox.publish(AGGREGATE + ".approve", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * sis.RejectPromotion: Reject (student detained). Allowed while status is proposed, for a caller who can see and
     * write the row; otherwise 409 and nothing changes.
     */
    public Promotion rejectPromotion(UUID id, RejectPromotionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_PROMOTION);
        return doInWriteTransaction(() -> {
            PromotionEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("proposed").contains(entity.getStatus()))) {
                throw new InvalidStateException("The promotion cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectPromotion(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reject", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".reject", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PromotionEntity loadVisible(DataScope scope, UUID id) {
        PromotionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(PromotionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected PromotionUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(PromotionEntity entity, PromotionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "fromAcademicYearId" -> entity.setFromAcademicYearId(changes.getFromAcademicYearId());
                case "fromGradeLevelId" -> entity.setFromGradeLevelId(changes.getFromGradeLevelId());
                case "fromSectionId" -> entity.setFromSectionId(changes.getFromSectionId());
                case "toAcademicYearId" -> entity.setToAcademicYearId(changes.getToAcademicYearId());
                case "toGradeLevelId" -> entity.setToGradeLevelId(changes.getToGradeLevelId());
                case "toSectionId" -> entity.setToSectionId(changes.getToSectionId());
                case "basedOnExamSessionId" -> entity.setBasedOnExamSessionId(changes.getBasedOnExamSessionId());
                case "remarks" -> entity.setRemarks(changes.getRemarks());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PromotionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getFromAcademicYearId() == null) {
            errors.add(new FieldError("fromAcademicYearId", "must not be null", "NotNull"));
        }
        if (entity.getFromGradeLevelId() == null) {
            errors.add(new FieldError("fromGradeLevelId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() != null && !repository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PromotionEntity entity) {

    }
}
