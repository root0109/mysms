package com.mysms.finance.business;

import com.mysms.authclient.DataScope;
import com.mysms.finance.api.model.FeeConcession;
import com.mysms.finance.api.model.FeeConcessionUpdate;
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
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.FeeConcessionEntity;
import com.mysms.finance.events.FinanceEvents;
import com.mysms.finance.mapper.FeeConcessionMapper;
import com.mysms.finance.repository.FeeConcessionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of finance.fee_concession (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class FeeConcessionService extends AppTransactionManager {

    public static final String AGGREGATE = FeeConcessionEntity.TABLE;
    static final String PERMISSION_LIST = "finance.fee_concession.read";
    static final String PERMISSION_GET = "finance.fee_concession.read";
    static final String PERMISSION_CREATE = "finance.fee_concession.create";
    static final String PERMISSION_UPDATE = "finance.fee_concession.update";
    static final String PERMISSION_DELETE = "finance.fee_concession.delete";
    /** The contract's FeeConcessionUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "feeHeadId", "concessionType", "concessionValue", "reason", "status", "validFrom",
            "validUntil");


    private final FeeConcessionRepository repository;
    private final FeeConcessionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public FeeConcessionService(FeeConcessionRepository repository, FeeConcessionMapper mapper,
            @Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public FeeConcession getFeeConcession(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<FeeConcession> listFeeConcessions(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, FeeConcessionRepository.SORT_FIELDS, FeeConcessionRepository.DEFAULT_ORDER,
                FeeConcessionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FeeConcessionRepository.FILTER_FIELDS)
                .and(FeeConcessionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<FeeConcessionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public FeeConcession createFeeConcession(FeeConcession request, boolean validateOnly) {
        validator.validate(request);
        FeeConcessionEntity entity = mapper.toEntity(request);
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
            FeeConcessionEntity stored = repository.insert(entity);
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
    public FeeConcession updateFeeConcession(UUID id, String ifMatch, FeeConcessionUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            FeeConcessionEntity entity = loadVisible(scope, id);
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
    public void deleteFeeConcession(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            FeeConcessionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByStudentFeeDueConcessionId(id)) {
                throw new InvalidStateException(
                        "The fee concession is still referenced by finance.student_fee_due rows; remove those first.");
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
    private FeeConcessionEntity loadVisible(DataScope scope, UUID id) {
        FeeConcessionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(FeeConcessionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected FeeConcessionUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(FeeConcessionEntity entity, FeeConcessionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "feeHeadId" -> entity.setFeeHeadId(changes.getFeeHeadId());
                case "concessionType" -> entity.setConcessionType(Optional.ofNullable(changes.getConcessionType())
                        .map(v -> v.getValue()).orElse(null));
                case "concessionValue" -> entity.setConcessionValue(WireValues.decimal(changes.getConcessionValue()));
                case "reason" -> entity.setReason(changes.getReason());
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                case "validFrom" -> entity.setValidFrom(changes.getValidFrom());
                case "validUntil" -> entity.setValidUntil(changes.getValidUntil());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(FeeConcessionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getConcessionType() == null) {
            errors.add(new FieldError("concessionType", "must not be null", "NotNull"));
        }
        if (entity.getConcessionValue() == null) {
            errors.add(new FieldError("concessionValue", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("concessionValue", WireValues.decimal(entity.getConcessionValue()), 14, 2, errors);
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getFeeHeadId() != null && !repository.existsFeeHeadId(entity.getFeeHeadId())) {
            errors.add(new FieldError("feeHeadId", "fee head does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(FeeConcessionEntity entity) {

    }
}
