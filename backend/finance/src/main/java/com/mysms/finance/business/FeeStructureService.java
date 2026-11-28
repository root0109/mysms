package com.mysms.finance.business;

import com.mysms.authclient.DataScope;
import com.mysms.finance.api.model.FeeStructure;
import com.mysms.finance.api.model.FeeStructureUpdate;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.FeeStructureEntity;
import com.mysms.finance.events.FinanceEvents;
import com.mysms.finance.mapper.FeeStructureMapper;
import com.mysms.finance.model.DueTarget;
import com.mysms.finance.api.model.Operation;
import com.mysms.finance.repository.FeeStructureRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of finance.fee_structure: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class FeeStructureService extends AppTransactionManager {

    public static final String AGGREGATE = FeeStructureEntity.TABLE;
    static final String PERMISSION_LIST = "finance.fee_structure.read";
    static final String PERMISSION_GET = "finance.fee_structure.read";
    static final String PERMISSION_CREATE = "finance.fee_structure.create";
    static final String PERMISSION_UPDATE = "finance.fee_structure.update";
    static final String PERMISSION_DELETE = "finance.fee_structure.delete";
    static final String PERMISSION_ARCHIVE_FEE_STRUCTURE = "finance.fee_structure.archive";
    static final String PERMISSION_PUBLISH_FEE_STRUCTURE = "finance.fee_structure.publish";
    /** The operation kind of the fee run a published structure starts. */
    public static final String KIND_PUBLISH = "finance.PublishFeeStructure";


    private final FeeStructureRepository repository;
    private final FeeStructureMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final StudentFeeDueService dues;
    private final RosterLookup rosters;

    public FeeStructureService(FeeStructureRepository repository, FeeStructureMapper mapper,
            @Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, StudentFeeDueService dues, RosterLookup rosters) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.dues = dues;
        this.rosters = rosters;
    }

    public FeeStructure getFeeStructure(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<FeeStructure> listFeeStructures(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, FeeStructureRepository.SORT_FIELDS, FeeStructureRepository.DEFAULT_ORDER,
                FeeStructureRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FeeStructureRepository.FILTER_FIELDS)
                .and(FeeStructureRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<FeeStructureEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public FeeStructure createFeeStructure(FeeStructure request, boolean validateOnly) {
        validator.validate(request);
        FeeStructureEntity entity = mapper.toEntity(request);
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
            FeeStructureEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, stored.getId(),
                    FinanceEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public FeeStructure updateFeeStructure(UUID id, String ifMatch, FeeStructureUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            FeeStructureEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
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
    public void deleteFeeStructure(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            FeeStructureEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByStudentFeeDueFeeStructureId(id)) {
                throw new InvalidStateException(
                        "The fee structure is still referenced by finance.student_fee_due rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + FinanceEvents.DELETED, AGGREGATE, id, FinanceEvents.payload(id));
        });
    }

    /**
     * finance.ArchiveFeeStructure: Archives a structure so it no longer generates dues; existing dues are unaffected.
     * Allowed while status is published, for a caller who can see and write the row; otherwise 409 and nothing changes.
     */
    public FeeStructure archiveFeeStructure(UUID id, String ifMatch) {
        DataScope scope = DataScope.current(PERMISSION_ARCHIVE_FEE_STRUCTURE);
        return doInWriteTransaction(() -> {
            FeeStructureEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != Etags.expectedVersion(ifMatch)) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (!(Set.of("published").contains(entity.getStatus()))) {
                throw new InvalidStateException("The fee structure cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.archiveFeeStructure(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "archive", List.of("status"), null);
            outbox.publish(AGGREGATE + ".archive", AGGREGATE, id, FinanceEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * finance.PublishFeeStructure: charges the structure's plan to every student it covers — placed in a class of its
     * grade level (any grade level when it names neither a grade level nor a program) of the plan's institution in its
     * academic year, or registered on a course of its program there (sis and academics, read with the caller's token)
     * — scheduled from the year's first day (org), then marks it published; answered as the fee run's operation (202).
     * The structure must be visible (404) and writable (403), a draft (409), and its plan of its own institution (422).
     * Nothing is written when a roster cannot be read (503). The dues are written before the step: a run that stops
     * leaves the structure a draft, and publishing again keeps the dues already generated and adds the rest.
     */
    public Operation publishFeeStructure(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_PUBLISH_FEE_STRUCTURE);
        FeeStructureEntity entity = doInReadTransaction(() -> loadVisible(scope, id));
        scope.requireWritable(institutionOf(entity));
        if (!"draft".equals(entity.getStatus())) {
            throw new InvalidStateException("Only a draft fee structure is published.");
        }
        UUID institution = dues.requirePlanWritable(entity.getFeePlanId(), scope);
        if (entity.getInstitutionId() != null && !entity.getInstitutionId().equals(institution)) {
            throw new ValidationFailedException(List.of(new FieldError("feePlanId",
                    "the fee plan belongs to another institution", "OTHER_INSTITUTION")));
        }
        List<DueTarget> targets = rosters.structureRoster(institution, entity.getAcademicYearId(),
                entity.getGradeLevelId(), entity.getProgramId());
        LocalDate start = rosters.yearStart(entity.getAcademicYearId());
        Operation run = dues.generate(new StudentFeeDueService.FeeRun(entity.getFeePlanId(),
                entity.getAcademicYearId(), id, start, null, targets), KIND_PUBLISH, AGGREGATE, id);
        runInWriteTransaction(() -> {
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.publishFeeStructure(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The fee structure changed while its dues were generated; "
                        + "the dues are kept, publish it again.");
            }
            audit.recordTransition(AGGREGATE, id, "publish", List.of("status"), null);
            outbox.publish(AGGREGATE + ".publish", AGGREGATE, id, FinanceEvents.payload(id));
        });
        return run;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private FeeStructureEntity loadVisible(DataScope scope, UUID id) {
        FeeStructureEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(FeeStructureEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(FeeStructureEntity entity, FeeStructureUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("gradeLevelId", changes.getGradeLevelId(), entity::setGradeLevelId);
        patch.apply("programId", changes.getProgramId(), entity::setProgramId);
        patch.apply("feePlanId", changes.getFeePlanId(), entity::setFeePlanId);
        patch.apply("name", changes.getName(), entity::setName);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(FeeStructureEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getFeePlanId() == null) {
            errors.add(new FieldError("feePlanId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getFeePlanId() != null && !repository.existsFeePlanId(entity.getFeePlanId())) {
            errors.add(new FieldError("feePlanId", "fee plan does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(FeeStructureEntity entity) {
        if (repository.takenAcademicYearIdGradeLevelIdProgramIdFeePlanIdScope(entity)) {
            throw new DuplicateResourceException(
                    "A fee structure with the same academic year id and grade level id and program id and fee plan "
                            + "id already exists.");
        }
    }
}
