package com.mysms.finance.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
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
import com.mysms.finance.api.model.FeePlan;
import com.mysms.finance.api.model.FeePlanInstallment;
import com.mysms.finance.api.model.FeePlanInstallmentUpdate;
import com.mysms.finance.api.model.FeePlanUpdate;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.FeePlanEntity;
import com.mysms.finance.domain.FeePlanInstallmentEntity;
import com.mysms.finance.events.FinanceEvents;
import com.mysms.finance.mapper.FeePlanInstallmentMapper;
import com.mysms.finance.mapper.FeePlanMapper;
import com.mysms.finance.repository.FeePlanInstallmentRepository;
import com.mysms.finance.repository.FeePlanRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of finance.fee_plan: audit and domain event in the same transaction as every change; rows outside
 * the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 *
 * <p>Also serves the records that exist only as part of the FeePlan aggregate: FeePlanInstallment.
 */
@Service
public class FeePlanService extends AppTransactionManager {

    public static final String AGGREGATE = FeePlanEntity.TABLE;
    static final String PERMISSION_LIST = "finance.fee_plan.read";
    static final String PERMISSION_GET = "finance.fee_plan.read";
    static final String PERMISSION_CREATE = "finance.fee_plan.create";
    static final String PERMISSION_UPDATE = "finance.fee_plan.update";
    static final String PERMISSION_DELETE = "finance.fee_plan.delete";
    /** The contract's FeePlanUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "code", "name", "scheduleBasis", "offsetDays", "offsetDirection",
                    "discountPercent",
            "notes", "isActive");

    public static final String FEE_PLAN_INSTALLMENT_AGGREGATE = FeePlanInstallmentEntity.TABLE;
    static final String FEE_PLAN_INSTALLMENT_PERMISSION_LIST = "finance.fee_plan_installment.read";
    static final String FEE_PLAN_INSTALLMENT_PERMISSION_GET = "finance.fee_plan_installment.read";
    static final String FEE_PLAN_INSTALLMENT_PERMISSION_CREATE = "finance.fee_plan_installment.create";
    static final String FEE_PLAN_INSTALLMENT_PERMISSION_UPDATE = "finance.fee_plan_installment.update";
    static final String FEE_PLAN_INSTALLMENT_PERMISSION_DELETE = "finance.fee_plan_installment.delete";
    /** The contract's FeePlanInstallmentUpdate fields, in apply order. */
    static final List<String> FEE_PLAN_INSTALLMENT_WRITABLE = List.of("sequence", "dueDays", "dueDate", "sharePercent");

    private final FeePlanRepository repository;
    private final FeePlanMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final FeePlanInstallmentRepository feePlanInstallmentRepository;
    private final FeePlanInstallmentMapper feePlanInstallmentMapper;

    public FeePlanService(FeePlanRepository repository,
            FeePlanMapper mapper,
            @Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            FeePlanInstallmentRepository feePlanInstallmentRepository,
            FeePlanInstallmentMapper feePlanInstallmentMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.feePlanInstallmentRepository = feePlanInstallmentRepository;
        this.feePlanInstallmentMapper = feePlanInstallmentMapper;
    }

    public FeePlan getFeePlan(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<FeePlan> listFeePlans(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, FeePlanRepository.SORT_FIELDS, FeePlanRepository.DEFAULT_ORDER,
                FeePlanRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FeePlanRepository.FILTER_FIELDS)
                .and(FeePlanRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<FeePlanEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public FeePlan createFeePlan(FeePlan request, boolean validateOnly) {
        validator.validate(request);
        FeePlanEntity entity = mapper.toEntity(request);
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
            FeePlanEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, stored.getId(),
                    FinanceEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public FeePlan updateFeePlan(UUID id, String ifMatch, FeePlanUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            FeePlanEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + FinanceEvents.UPDATED, AGGREGATE, id, FinanceEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteFeePlan(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            FeePlanEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCourseFeeDefaultFeePlanId(id)) {
                throw new InvalidStateException(
                        "The fee plan is still referenced by finance.course_fee_default rows; remove those first.");
            }
            if (repository.referencedByFeePlanInstallmentFeePlanId(id)) {
                throw new InvalidStateException(
                        "The fee plan is still referenced by finance.fee_plan_installment rows; remove those first.");
            }
            if (repository.referencedByFeeStructureFeePlanId(id)) {
                throw new InvalidStateException(
                        "The fee plan is still referenced by finance.fee_structure rows; remove those first.");
            }
            if (repository.referencedByProgramEnrollmentFeeFeePlanId(id)) {
                throw new InvalidStateException(
                        "The fee plan is still referenced by finance.program_enrollment_fee rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + FinanceEvents.DELETED, AGGREGATE, id, FinanceEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private FeePlanEntity loadVisible(DataScope scope, UUID id) {
        FeePlanEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(FeePlanEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected FeePlanUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(FeePlanEntity entity, FeePlanUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "code" -> entity.setCode(changes.getCode());
                case "name" -> entity.setName(changes.getName());
                case "scheduleBasis" -> entity.setScheduleBasis(Optional.ofNullable(changes.getScheduleBasis())
                        .map(v -> v.getValue()).orElse(null));
                case "offsetDays" -> entity.setOffsetDays(changes.getOffsetDays());
                case "offsetDirection" -> entity.setOffsetDirection(Optional.ofNullable(changes.getOffsetDirection())
                        .map(v -> v.getValue()).orElse(null));
                case "discountPercent" -> entity.setDiscountPercent(WireValues.decimal(changes.getDiscountPercent()));
                case "notes" -> entity.setNotes(changes.getNotes());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(FeePlanEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getScheduleBasis() == null) {
            errors.add(new FieldError("scheduleBasis", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("discountPercent", WireValues.decimal(entity.getDiscountPercent()), 5, 2, errors);
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(FeePlanEntity entity) {
        if (repository.takenInstitutionIdCode1(entity)) {
            throw new DuplicateResourceException("A fee plan with the same institution id and code already exists.");
        }
    }

    // ---- FeePlanInstallment: a record of the FeePlan aggregate ----

    public FeePlanInstallment getFeePlanInstallment(UUID feePlanId, UUID id) {
        return doInReadTransaction(() -> {
            return feePlanInstallmentMapper.toDto(loadVisibleFeePlanInstallment(DataScope.current(
                    FEE_PLAN_INSTALLMENT_PERMISSION_GET), feePlanId, id));
        });
    }

    public Page<FeePlanInstallment> listFeePlanInstallments(UUID feePlanId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, FeePlanInstallmentRepository.SORT_FIELDS,
                        FeePlanInstallmentRepository.DEFAULT_ORDER,
                FeePlanInstallmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FeePlanInstallmentRepository.FILTER_FIELDS)
                .and(FeePlanInstallmentRepository.ofParent(feePlanId))
                        .and(FeePlanInstallmentRepository.scope(DataScope.current(FEE_PLAN_INSTALLMENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + feePlanId, clock);
        return doInReadTransaction(() -> {
            Page<FeePlanInstallmentEntity> rows = feePlanInstallmentRepository.list(page, where);
            return rows.map(feePlanInstallmentMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public FeePlanInstallment createFeePlanInstallment(UUID feePlanId, FeePlanInstallment request,
            boolean validateOnly) {
        validator.validate(request);
        FeePlanInstallmentEntity entity = feePlanInstallmentMapper.toEntity(request);
        entity.setFeePlanId(feePlanId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(FEE_PLAN_INSTALLMENT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!feePlanInstallmentRepository.existsFeePlanId(entity.getFeePlanId())) {
                throw new ResourceNotFoundException("finance.fee_plan", entity.getFeePlanId());
            }
            if (!scope.canRead(institutionOfFeePlanInstallment(entity))) {
                throw new ResourceNotFoundException("finance.fee_plan", entity.getFeePlanId());
            }
            assertValidFeePlanInstallment(entity, true);
            scope.requireWritable(institutionOfFeePlanInstallment(entity));
            assertUniqueFeePlanInstallment(entity);
            if (validateOnly) {
                return feePlanInstallmentMapper.toDto(entity);
            }
            FeePlanInstallmentEntity stored = feePlanInstallmentRepository.insert(entity);
            audit.recordCreate(FEE_PLAN_INSTALLMENT_AGGREGATE, stored.getId());
            outbox.publish(FEE_PLAN_INSTALLMENT_AGGREGATE + FinanceEvents.CREATED, FEE_PLAN_INSTALLMENT_AGGREGATE, stored.getId(),
                    FinanceEvents.payload(stored.getId()));
            return feePlanInstallmentMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public FeePlanInstallment updateFeePlanInstallment(UUID feePlanId, UUID id, String ifMatch,
            FeePlanInstallmentUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, FEE_PLAN_INSTALLMENT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FEE_PLAN_INSTALLMENT_PERMISSION_UPDATE);
            FeePlanInstallmentEntity entity = loadVisibleFeePlanInstallment(scope, feePlanId, id);
            scope.requireWritable(institutionOfFeePlanInstallment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyFeePlanInstallment(entity, changes, fields);
            validator.validate(feePlanInstallmentMapper.toDto(entity));
            assertValidFeePlanInstallment(entity, false);
            scope.requireWritable(institutionOfFeePlanInstallment(entity));
            assertUniqueFeePlanInstallment(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return feePlanInstallmentMapper.toDto(entity);
            }
            if (feePlanInstallmentRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(FEE_PLAN_INSTALLMENT_AGGREGATE, id, fields);
            outbox.publish(FEE_PLAN_INSTALLMENT_AGGREGATE + FinanceEvents.UPDATED, FEE_PLAN_INSTALLMENT_AGGREGATE, id,
                    FinanceEvents.payload(id));
            return feePlanInstallmentMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteFeePlanInstallment(UUID feePlanId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FEE_PLAN_INSTALLMENT_PERMISSION_DELETE);
            FeePlanInstallmentEntity entity = loadVisibleFeePlanInstallment(scope, feePlanId, id);
            scope.requireWritable(institutionOfFeePlanInstallment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (feePlanInstallmentRepository.referencedByFeePlanInstallmentComponentInstallmentId(id)) {
                throw new InvalidStateException(
                        "The fee plan installment is still referenced by finance.fee_plan_installment_component "
                                + "rows; remove those first.");
            }
            if (feePlanInstallmentRepository.referencedByStudentFeeDueInstallmentId(id)) {
                throw new InvalidStateException(
                        "The fee plan installment is still referenced by finance.student_fee_due rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (feePlanInstallmentRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(FEE_PLAN_INSTALLMENT_AGGREGATE, id);
            outbox.publish(FEE_PLAN_INSTALLMENT_AGGREGATE + FinanceEvents.DELETED, FEE_PLAN_INSTALLMENT_AGGREGATE, id,
                    FinanceEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private FeePlanInstallmentEntity loadVisibleFeePlanInstallment(DataScope scope, UUID feePlanId, UUID id) {
        FeePlanInstallmentEntity entity = feePlanInstallmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FEE_PLAN_INSTALLMENT_AGGREGATE, id));
        if (!scope.canRead(institutionOfFeePlanInstallment(entity)) || !feePlanId.equals(entity.getFeePlanId())) {
            throw new ResourceNotFoundException(FEE_PLAN_INSTALLMENT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the finance.fee_plan it belongs to). */
    private Optional<UUID> institutionOfFeePlanInstallment(FeePlanInstallmentEntity entity) {
        return feePlanInstallmentRepository.institutionOf(entity.getFeePlanId());
    }

    /**
     * Applies the selected FeePlanInstallmentUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyFeePlanInstallment(FeePlanInstallmentEntity entity, FeePlanInstallmentUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "sequence" -> entity.setSequence(changes.getSequence());
                case "dueDays" -> entity.setDueDays(changes.getDueDays());
                case "dueDate" -> entity.setDueDate(changes.getDueDate());
                case "sharePercent" -> entity.setSharePercent(WireValues.decimal(changes.getSharePercent()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidFeePlanInstallment(FeePlanInstallmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getFeePlanId() == null) {
            errors.add(new FieldError("feePlanId", "must not be null", "NotNull"));
        }
        if (entity.getSequence() == null) {
            errors.add(new FieldError("sequence", "must not be null", "NotNull"));
        }
        if (entity.getSharePercent() == null) {
            errors.add(new FieldError("sharePercent", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("sharePercent", WireValues.decimal(entity.getSharePercent()), 6, 3, errors);
        if (entity.getFeePlanId() != null && !feePlanInstallmentRepository.existsFeePlanId(entity.getFeePlanId())) {
            errors.add(new FieldError("feePlanId", "fee plan does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueFeePlanInstallment(FeePlanInstallmentEntity entity) {
        if (feePlanInstallmentRepository.takenFeePlanIdSequence1(entity)) {
            throw new DuplicateResourceException(
                    "A fee plan installment with the same fee plan id and sequence already exists.");
        }
    }
}
