package com.mysms.admission.business;

import com.mysms.admission.api.model.AdmissionCycle;
import com.mysms.admission.api.model.AdmissionCycleFee;
import com.mysms.admission.api.model.AdmissionCycleFeeUpdate;
import com.mysms.admission.api.model.AdmissionCycleUpdate;
import com.mysms.admission.api.model.CancelAdmissionCycleRequest;
import com.mysms.admission.api.model.CloseAdmissionCycleRequest;
import com.mysms.admission.api.model.ConfirmAdmissionCycleRequest;
import com.mysms.admission.api.model.ResetAdmissionCycleRequest;
import com.mysms.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.config.AdmissionEventsConfig;
import com.mysms.admission.domain.AdmissionCycleEntity;
import com.mysms.admission.domain.AdmissionCycleFeeEntity;
import com.mysms.admission.events.AdmissionEvents;
import com.mysms.admission.mapper.AdmissionCycleFeeMapper;
import com.mysms.admission.mapper.AdmissionCycleMapper;
import com.mysms.admission.repository.AdmissionCycleFeeRepository;
import com.mysms.admission.repository.AdmissionCycleRepository;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
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
 * Business service of admission.admission_cycle (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a AdmissionCycle
 * are served here too — AdmissionCycleFee.
 */
@Service
public class AdmissionCycleService extends AppTransactionManager {

    public static final String AGGREGATE = AdmissionCycleEntity.TABLE;
    static final String PERMISSION_LIST = "admission.admission_cycle.read";
    static final String PERMISSION_GET = "admission.admission_cycle.read";
    static final String PERMISSION_CREATE = "admission.admission_cycle.create";
    static final String PERMISSION_UPDATE = "admission.admission_cycle.update";
    static final String PERMISSION_DELETE = "admission.admission_cycle.delete";
    static final String PERMISSION_CONFIRM_ADMISSION_CYCLE = "admission.admission_cycle.confirm_admission_cycle";
    static final String PERMISSION_CLOSE_ADMISSION_CYCLE = "admission.admission_cycle.close_admission_cycle";
    static final String PERMISSION_CANCEL_ADMISSION_CYCLE = "admission.admission_cycle.cancel_admission_cycle";
    static final String PERMISSION_RESET_ADMISSION_CYCLE = "admission.admission_cycle.reset_admission_cycle";
    /** The contract's AdmissionCycleUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "name", "admissionBasis", "programId", "courseId", "gradeLevelId",
                    "academicYearId",
            "academicTermId", "startDate", "endDate", "minApplications", "maxApplications", "minimumAgeYears",
            "applicationFeeHeadId");

    public static final String ADMISSION_CYCLE_FEE_AGGREGATE = AdmissionCycleFeeEntity.TABLE;
    static final String ADMISSION_CYCLE_FEE_PERMISSION_LIST = "admission.admission_cycle_fee.read";
    static final String ADMISSION_CYCLE_FEE_PERMISSION_GET = "admission.admission_cycle_fee.read";
    static final String ADMISSION_CYCLE_FEE_PERMISSION_CREATE = "admission.admission_cycle_fee.create";
    static final String ADMISSION_CYCLE_FEE_PERMISSION_UPDATE = "admission.admission_cycle_fee.update";
    static final String ADMISSION_CYCLE_FEE_PERMISSION_DELETE = "admission.admission_cycle_fee.delete";
    /** The contract's AdmissionCycleFeeUpdate fields, in apply order. */
    static final List<String> ADMISSION_CYCLE_FEE_WRITABLE = List.of("courseId", "feeHeadId");

    private final AdmissionCycleRepository repository;
    private final AdmissionCycleMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final AdmissionCycleFeeRepository admissionCycleFeeRepository;
    private final AdmissionCycleFeeMapper admissionCycleFeeMapper;

    public AdmissionCycleService(AdmissionCycleRepository repository,
            AdmissionCycleMapper mapper,
            @Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AdmissionDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AdmissionEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AdmissionEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            AdmissionCycleFeeRepository admissionCycleFeeRepository,
            AdmissionCycleFeeMapper admissionCycleFeeMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.admissionCycleFeeRepository = admissionCycleFeeRepository;
        this.admissionCycleFeeMapper = admissionCycleFeeMapper;
    }

    public AdmissionCycle getAdmissionCycle(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AdmissionCycle> listAdmissionCycles(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AdmissionCycleRepository.SORT_FIELDS, AdmissionCycleRepository.DEFAULT_ORDER,
                AdmissionCycleRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AdmissionCycleRepository.FILTER_FIELDS)
                .and(AdmissionCycleRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AdmissionCycleEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AdmissionCycle createAdmissionCycle(AdmissionCycle request, boolean validateOnly) {
        validator.validate(request);
        AdmissionCycleEntity entity = mapper.toEntity(request);
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
            AdmissionCycleEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AdmissionEvents.CREATED, AGGREGATE, stored.getId(),
                    AdmissionEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AdmissionCycle updateAdmissionCycle(UUID id, String ifMatch, AdmissionCycleUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AdmissionCycleEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AdmissionEvents.UPDATED, AGGREGATE, id, AdmissionEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAdmissionCycle(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AdmissionCycleEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAdmissionCycleFeeAdmissionCycleId(id)) {
                throw new InvalidStateException(
                        "The admission cycle is still referenced by admission.admission_cycle_fee rows; remove those "
                                + "first.");
            }
            if (repository.referencedByApplicationAdmissionCycleId(id)) {
                throw new InvalidStateException(
                        "The admission cycle is still referenced by admission.application rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AdmissionEvents.DELETED, AGGREGATE, id, AdmissionEvents.payload(id));
        });
    }

    /**
     * admission.ConfirmAdmissionCycle: Confirm the cycle. The row must be visible and writable for the caller, and the
     * step is allowed while status is draft; in any other state
     * the answer is 409 and nothing changes.
     */
    public AdmissionCycle confirmAdmissionCycle(UUID id, ConfirmAdmissionCycleRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CONFIRM_ADMISSION_CYCLE);
        return doInWriteTransaction(() -> {
            AdmissionCycleEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft").contains(entity.getStatus()))) {
                throw new InvalidStateException("The admission cycle cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.confirmAdmissionCycle(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "confirm", List.of("status"), null);
            outbox.publish(AGGREGATE + ".confirm", AGGREGATE, id, AdmissionEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * admission.CloseAdmissionCycle: Close. The row must be visible and writable for the caller, and the step is
     * allowed while status is open or admission; in any other state
     * the answer is 409 and nothing changes.
     */
    public AdmissionCycle closeAdmissionCycle(UUID id, CloseAdmissionCycleRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CLOSE_ADMISSION_CYCLE);
        return doInWriteTransaction(() -> {
            AdmissionCycleEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("open", "admission").contains(entity.getStatus()))) {
                throw new InvalidStateException("The admission cycle cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.closeAdmissionCycle(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "close", List.of("status"), null);
            outbox.publish(AGGREGATE + ".close", AGGREGATE, id, AdmissionEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * admission.CancelAdmissionCycle: Cancel. The row must be visible and writable for the caller, and the step is
     * allowed while status is draft or confirmed or open; in any other state
     * the answer is 409 and nothing changes.
     */
    public AdmissionCycle cancelAdmissionCycle(UUID id, CancelAdmissionCycleRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_ADMISSION_CYCLE);
        return doInWriteTransaction(() -> {
            AdmissionCycleEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "confirmed", "open").contains(entity.getStatus()))) {
                throw new InvalidStateException("The admission cycle cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelAdmissionCycle(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, AdmissionEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * admission.ResetAdmissionCycle: Back to draft. The row must be visible and writable for the caller, and the step
     * is allowed while status is cancelled; in any other state
     * the answer is 409 and nothing changes.
     */
    public AdmissionCycle resetAdmissionCycle(UUID id, ResetAdmissionCycleRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESET_ADMISSION_CYCLE);
        return doInWriteTransaction(() -> {
            AdmissionCycleEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("cancelled").contains(entity.getStatus()))) {
                throw new InvalidStateException("The admission cycle cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.resetAdmissionCycle(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reset", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reset", AGGREGATE, id, AdmissionEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AdmissionCycleEntity loadVisible(DataScope scope, UUID id) {
        AdmissionCycleEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(AdmissionCycleEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected AdmissionCycleUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(AdmissionCycleEntity entity, AdmissionCycleUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "name" -> entity.setName(changes.getName());
                case "admissionBasis" -> entity.setAdmissionBasis(Optional.ofNullable(changes.getAdmissionBasis())
                        .map(v -> v.getValue()).orElse(null));
                case "programId" -> entity.setProgramId(changes.getProgramId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "gradeLevelId" -> entity.setGradeLevelId(changes.getGradeLevelId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "academicTermId" -> entity.setAcademicTermId(changes.getAcademicTermId());
                case "startDate" -> entity.setStartDate(changes.getStartDate());
                case "endDate" -> entity.setEndDate(changes.getEndDate());
                case "minApplications" -> entity.setMinApplications(changes.getMinApplications());
                case "maxApplications" -> entity.setMaxApplications(changes.getMaxApplications());
                case "minimumAgeYears" -> entity.setMinimumAgeYears(changes.getMinimumAgeYears());
                case "applicationFeeHeadId" -> entity.setApplicationFeeHeadId(changes.getApplicationFeeHeadId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AdmissionCycleEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAdmissionBasis() == null) {
            errors.add(new FieldError("admissionBasis", "must not be null", "NotNull"));
        }
        if (entity.getStartDate() == null) {
            errors.add(new FieldError("startDate", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AdmissionCycleEntity entity) {

    }

    // ---- AdmissionCycleFee: a record of the AdmissionCycle aggregate ----

    public AdmissionCycleFee getAdmissionCycleFee(UUID admissionCycleId, UUID id) {
        return doInReadTransaction(() -> {
            return admissionCycleFeeMapper.toDto(loadVisibleAdmissionCycleFee(DataScope.current(
                    ADMISSION_CYCLE_FEE_PERMISSION_GET), admissionCycleId, id));
        });
    }

    public Page<AdmissionCycleFee> listAdmissionCycleFees(UUID admissionCycleId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AdmissionCycleFeeRepository.SORT_FIELDS,
                        AdmissionCycleFeeRepository.DEFAULT_ORDER,
                AdmissionCycleFeeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AdmissionCycleFeeRepository.FILTER_FIELDS)
                .and(AdmissionCycleFeeRepository.ofParent(admissionCycleId))
                        .and(AdmissionCycleFeeRepository.scope(DataScope.current(ADMISSION_CYCLE_FEE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + admissionCycleId, clock);
        return doInReadTransaction(() -> {
            Page<AdmissionCycleFeeEntity> rows = admissionCycleFeeRepository.list(page, where);
            return rows.map(admissionCycleFeeMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AdmissionCycleFee createAdmissionCycleFee(UUID admissionCycleId, AdmissionCycleFee request,
            boolean validateOnly) {
        validator.validate(request);
        AdmissionCycleFeeEntity entity = admissionCycleFeeMapper.toEntity(request);
        entity.setAdmissionCycleId(admissionCycleId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ADMISSION_CYCLE_FEE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!admissionCycleFeeRepository.existsAdmissionCycleId(entity.getAdmissionCycleId())) {
                throw new ResourceNotFoundException("admission.admission_cycle", entity.getAdmissionCycleId());
            }
            if (!scope.canRead(institutionOfAdmissionCycleFee(entity))) {
                throw new ResourceNotFoundException("admission.admission_cycle", entity.getAdmissionCycleId());
            }
            assertValidAdmissionCycleFee(entity, true);
            scope.requireWritable(institutionOfAdmissionCycleFee(entity));
            assertUniqueAdmissionCycleFee(entity);
            if (validateOnly) {
                return admissionCycleFeeMapper.toDto(entity);
            }
            AdmissionCycleFeeEntity stored = admissionCycleFeeRepository.insert(entity);
            audit.recordCreate(ADMISSION_CYCLE_FEE_AGGREGATE, stored.getId());
            outbox.publish(ADMISSION_CYCLE_FEE_AGGREGATE + AdmissionEvents.CREATED, ADMISSION_CYCLE_FEE_AGGREGATE, stored.getId(),
                    AdmissionEvents.payload(stored.getId()));
            return admissionCycleFeeMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AdmissionCycleFee updateAdmissionCycleFee(UUID admissionCycleId, UUID id, String ifMatch,
            AdmissionCycleFeeUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ADMISSION_CYCLE_FEE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ADMISSION_CYCLE_FEE_PERMISSION_UPDATE);
            AdmissionCycleFeeEntity entity = loadVisibleAdmissionCycleFee(scope, admissionCycleId, id);
            scope.requireWritable(institutionOfAdmissionCycleFee(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyAdmissionCycleFee(entity, changes, fields);
            validator.validate(admissionCycleFeeMapper.toDto(entity));
            assertValidAdmissionCycleFee(entity, false);
            scope.requireWritable(institutionOfAdmissionCycleFee(entity));
            assertUniqueAdmissionCycleFee(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return admissionCycleFeeMapper.toDto(entity);
            }
            if (admissionCycleFeeRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ADMISSION_CYCLE_FEE_AGGREGATE, id, fields);
            outbox.publish(ADMISSION_CYCLE_FEE_AGGREGATE + AdmissionEvents.UPDATED, ADMISSION_CYCLE_FEE_AGGREGATE, id,
                    AdmissionEvents.payload(id));
            return admissionCycleFeeMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAdmissionCycleFee(UUID admissionCycleId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ADMISSION_CYCLE_FEE_PERMISSION_DELETE);
            AdmissionCycleFeeEntity entity = loadVisibleAdmissionCycleFee(scope, admissionCycleId, id);
            scope.requireWritable(institutionOfAdmissionCycleFee(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (admissionCycleFeeRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ADMISSION_CYCLE_FEE_AGGREGATE, id);
            outbox.publish(ADMISSION_CYCLE_FEE_AGGREGATE + AdmissionEvents.DELETED, ADMISSION_CYCLE_FEE_AGGREGATE, id,
                    AdmissionEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AdmissionCycleFeeEntity loadVisibleAdmissionCycleFee(DataScope scope, UUID admissionCycleId, UUID id) {
        AdmissionCycleFeeEntity entity = admissionCycleFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ADMISSION_CYCLE_FEE_AGGREGATE, id));
        if (!scope.canRead(institutionOfAdmissionCycleFee(entity)) || !admissionCycleId.equals(entity.getAdmissionCycleId())) {
            throw new ResourceNotFoundException(ADMISSION_CYCLE_FEE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the admission.admission_cycle it belongs to). */
    private Optional<UUID> institutionOfAdmissionCycleFee(AdmissionCycleFeeEntity entity) {
        return admissionCycleFeeRepository.institutionOf(entity.getAdmissionCycleId());
    }

    /**
     * Applies the selected AdmissionCycleFeeUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyAdmissionCycleFee(AdmissionCycleFeeEntity entity, AdmissionCycleFeeUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "feeHeadId" -> entity.setFeeHeadId(changes.getFeeHeadId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAdmissionCycleFee(AdmissionCycleFeeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAdmissionCycleId() == null) {
            errors.add(new FieldError("admissionCycleId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getAdmissionCycleId() != null
                && !admissionCycleFeeRepository.existsAdmissionCycleId(entity.getAdmissionCycleId())) {
            errors.add(new FieldError("admissionCycleId", "admission cycle does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAdmissionCycleFee(AdmissionCycleFeeEntity entity) {
        if (admissionCycleFeeRepository.takenAdmissionCycleIdCourseId1(entity)) {
            throw new DuplicateResourceException(
                    "A admission cycle fee with the same admission cycle id and course id already exists.");
        }
    }
}
