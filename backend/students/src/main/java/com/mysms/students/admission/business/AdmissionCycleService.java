package com.mysms.students.admission.business;

import com.mysms.students.admission.api.model.AdmissionCycle;
import com.mysms.students.admission.api.model.AdmissionCycleFee;
import com.mysms.students.admission.api.model.AdmissionCycleFeeUpdate;
import com.mysms.students.admission.api.model.AdmissionCycleUpdate;
import com.mysms.students.admission.api.model.CancelAdmissionCycleRequest;
import com.mysms.students.admission.api.model.CloseAdmissionCycleRequest;
import com.mysms.students.admission.api.model.ConfirmAdmissionCycleRequest;
import com.mysms.students.admission.api.model.OpenApplicationsRequest;
import com.mysms.students.admission.api.model.ResetAdmissionCycleRequest;
import com.mysms.students.admission.api.model.StartAdmissionRequest;
import com.mysms.students.admission.config.AdmissionDatabaseConfig;
import com.mysms.students.admission.config.AdmissionEventsConfig;
import com.mysms.students.admission.domain.AdmissionCycleEntity;
import com.mysms.students.admission.domain.AdmissionCycleFeeEntity;
import com.mysms.students.admission.events.AdmissionEvents;
import com.mysms.students.admission.mapper.AdmissionCycleFeeMapper;
import com.mysms.students.admission.mapper.AdmissionCycleMapper;
import com.mysms.students.admission.repository.AdmissionCycleFeeRepository;
import com.mysms.students.admission.repository.AdmissionCycleRepository;
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
import com.mysms.core.web.Patches;
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
 * Business service of admission.admission_cycle and of the records that exist only as part of a AdmissionCycle
 * (AdmissionCycleFee): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
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
    static final String PERMISSION_OPEN_APPLICATIONS = "admission.admission_cycle.open_applications";
    static final String PERMISSION_START_ADMISSION = "admission.admission_cycle.start_admission";
    /** The cycle takes applications. */
    static final String OPEN = "open";
    /** The cycle takes no more applications; the ones it has are decided and admitted. */
    static final String ADMISSION = "admission";

    public static final String ADMISSION_CYCLE_FEE_AGGREGATE = AdmissionCycleFeeEntity.TABLE;
    static final String ADMISSION_CYCLE_FEE_PERMISSION_LIST = "admission.admission_cycle_fee.read";
    static final String ADMISSION_CYCLE_FEE_PERMISSION_GET = "admission.admission_cycle_fee.read";
    static final String ADMISSION_CYCLE_FEE_PERMISSION_CREATE = "admission.admission_cycle_fee.create";
    static final String ADMISSION_CYCLE_FEE_PERMISSION_UPDATE = "admission.admission_cycle_fee.update";
    static final String ADMISSION_CYCLE_FEE_PERMISSION_DELETE = "admission.admission_cycle_fee.delete";

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

    /**
     * The cycle, for the office of its institution — and, while it is open for applications, for every holder of
     * the permission in the tenant: an applicant is not a member of the school they apply to, and what a cycle says
     * (its name, its dates, what it admits to) is what the school publishes to invite applications.
     */
    public AdmissionCycle getAdmissionCycle(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_GET);
        return doInReadTransaction(() -> {
            AdmissionCycleEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity)) && !OPEN.equals(entity.getStatus())) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            return mapper.toDto(entity);
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public AdmissionCycle updateAdmissionCycle(UUID id, String ifMatch, AdmissionCycleUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AdmissionCycleEntity entity = loadVisible(scope, id);
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
     * admission.ConfirmAdmissionCycle: Confirm the cycle. Allowed while status is draft, on a row the caller may see
     * and write; otherwise 409 and nothing changes.
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
     * admission.CloseAdmissionCycle: Close. Allowed while status is open or admission, on a row the caller may see and
     * write; otherwise 409 and nothing changes.
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
     * admission.CancelAdmissionCycle: Cancel. Allowed while status is draft or confirmed or open, on a row the caller
     * may see and write; otherwise 409 and nothing changes.
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
     * admission.ResetAdmissionCycle: Back to draft. Allowed while status is cancelled, on a row the caller may see and
     * write; otherwise 409 and nothing changes.
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

    /**
     * admission.OpenApplications: Start accepting applications. Allowed while status is confirmed, on a row the caller
     * may see and write; otherwise 409 and nothing changes.
     */
    public AdmissionCycle openApplications(UUID id, OpenApplicationsRequest request) {
        DataScope scope = DataScope.current(PERMISSION_OPEN_APPLICATIONS);
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
            if (!(Set.of("confirmed").contains(entity.getStatus()))) {
                throw new InvalidStateException("The admission cycle cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.openApplications(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "openApplications", List.of("status"), null);
            outbox.publish(AGGREGATE + ".openApplications", AGGREGATE, id, AdmissionEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * admission.StartAdmission: Move to the admission stage. Allowed while status is open, on a row the caller may see
     * and write; otherwise 409 and nothing changes.
     */
    public AdmissionCycle startAdmission(UUID id, StartAdmissionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_START_ADMISSION);
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
            if (!(Set.of(OPEN).contains(entity.getStatus()))) {
                throw new InvalidStateException("The admission cycle cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.startAdmission(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "startAdmission", List.of("status"), null);
            outbox.publish(AGGREGATE + ".startAdmission", AGGREGATE, id, AdmissionEvents.payload(id));
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

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(AdmissionCycleEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(AdmissionCycleEntity entity, AdmissionCycleUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("admissionBasis", changes.getAdmissionBasis(),
                v -> entity.setAdmissionBasis(v == null ? null : v.getValue()));
        patch.apply("programId", changes.getProgramId(), entity::setProgramId);
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        patch.apply("gradeLevelId", changes.getGradeLevelId(), entity::setGradeLevelId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("academicTermId", changes.getAcademicTermId(), entity::setAcademicTermId);
        patch.apply("startDate", changes.getStartDate(), entity::setStartDate);
        patch.apply("endDate", changes.getEndDate(), entity::setEndDate);
        patch.apply("minApplications", changes.getMinApplications(), entity::setMinApplications);
        patch.apply("maxApplications", changes.getMaxApplications(), entity::setMaxApplications);
        patch.apply("minimumAgeYears", changes.getMinimumAgeYears(), entity::setMinimumAgeYears);
        patch.apply("applicationFeeHeadId", changes.getApplicationFeeHeadId(), entity::setApplicationFeeHeadId);
        return patch.applied();
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

    // ---- AdmissionCycleFee ----

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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public AdmissionCycleFee updateAdmissionCycleFee(UUID admissionCycleId, UUID id, String ifMatch,
            AdmissionCycleFeeUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ADMISSION_CYCLE_FEE_PERMISSION_UPDATE);
            AdmissionCycleFeeEntity entity = loadVisibleAdmissionCycleFee(scope, admissionCycleId, id);
            scope.requireWritable(institutionOfAdmissionCycleFee(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyAdmissionCycleFee(entity, changes);
            validator.validate(admissionCycleFeeMapper.toDto(entity));
            assertValidAdmissionCycleFee(entity, false);
            scope.requireWritable(institutionOfAdmissionCycleFee(entity));
            assertUniqueAdmissionCycleFee(entity);
            if (fields.isEmpty()) {
                return admissionCycleFeeMapper.toDto(entity);
            }
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

    /** The institution of the admission.admission_cycle the row belongs to. */
    private Optional<UUID> institutionOfAdmissionCycleFee(AdmissionCycleFeeEntity entity) {
        return admissionCycleFeeRepository.institutionOf(entity.getAdmissionCycleId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyAdmissionCycleFee(AdmissionCycleFeeEntity entity, AdmissionCycleFeeUpdate changes) {
        Patches patch = new Patches();
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        patch.apply("feeHeadId", changes.getFeeHeadId(), entity::setFeeHeadId);
        return patch.applied();
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
