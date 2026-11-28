package com.mysms.students.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.students.sis.api.model.Guardian;
import com.mysms.students.sis.api.model.GuardianUpdate;
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
import com.mysms.core.web.Patches;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.config.SisEventsConfig;
import com.mysms.students.sis.domain.GuardianEntity;
import com.mysms.students.sis.events.SisEvents;
import com.mysms.students.sis.mapper.GuardianMapper;
import com.mysms.students.sis.repository.GuardianRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.guardian: audit and domain event in the same transaction as every change; rows outside the
 * caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class GuardianService extends AppTransactionManager {

    public static final String AGGREGATE = GuardianEntity.TABLE;
    static final String PERMISSION_LIST = "sis.guardian.read";
    static final String PERMISSION_GET = "sis.guardian.read";
    public static final String PERMISSION_CREATE = "sis.guardian.create";
    static final String PERMISSION_UPDATE = "sis.guardian.update";
    static final String PERMISSION_DELETE = "sis.guardian.delete";


    private final GuardianRepository repository;
    private final GuardianMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public GuardianService(GuardianRepository repository, GuardianMapper mapper,
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

    public Guardian getGuardian(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Guardian> listGuardians(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, GuardianRepository.SORT_FIELDS, GuardianRepository.DEFAULT_ORDER,
                GuardianRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, GuardianRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<GuardianEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Guardian createGuardian(Guardian request, boolean validateOnly) {
        validator.validate(request);
        GuardianEntity entity = mapper.toEntity(request);
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
            GuardianEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Guardian updateGuardian(UUID id, String ifMatch, GuardianUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            GuardianEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + SisEvents.UPDATED, AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteGuardian(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            GuardianEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByStudentGuardianGuardianId(id)) {
                throw new InvalidStateException(
                        "The guardian is still referenced by sis.student_guardian rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + SisEvents.DELETED, AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private GuardianEntity loadVisible(DataScope scope, UUID id) {
        GuardianEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(GuardianEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(GuardianEntity entity, GuardianUpdate changes) {
        Patches patch = new Patches();
        patch.apply("personId", changes.getPersonId(), entity::setPersonId);
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("profession", changes.getProfession(), v -> entity.setProfession(v == null ? null : v.getValue()));
        patch.apply("subOccupation", changes.getSubOccupation(), entity::setSubOccupation);
        patch.apply("designation", changes.getDesignation(), entity::setDesignation);
        patch.apply("organizationName", changes.getOrganizationName(), entity::setOrganizationName);
        patch.apply("annualIncome", changes.getAnnualIncome(), v -> entity.setAnnualIncome(WireValues.decimal(v)));
        patch.apply("qualificationId", changes.getQualificationId(), entity::setQualificationId);
        patch.apply("mobile", changes.getMobile(), entity::setMobile);
        patch.apply("email", changes.getEmail(), entity::setEmail);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(GuardianEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("annualIncome", WireValues.decimal(entity.getAnnualIncome()), 14, 2, errors);
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(GuardianEntity entity) {
        if (repository.takenPersonIdId(entity)) {
            throw new DuplicateResourceException("A guardian with the same person id already exists.");
        }
        if (entity.getUserId() != null && repository.takenUserIdId(entity)) {
            throw new DuplicateResourceException("A guardian with the same user id already exists.");
        }
    }
}
