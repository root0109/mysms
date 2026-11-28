package com.mysms.campus.library.business;

import com.mysms.authclient.DataScope;
import com.mysms.campus.library.api.model.LibraryCardType;
import com.mysms.campus.library.api.model.LibraryCardTypeUpdate;
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
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.config.LibraryEventsConfig;
import com.mysms.campus.library.domain.LibraryCardTypeEntity;
import com.mysms.campus.library.events.LibraryEvents;
import com.mysms.campus.library.mapper.LibraryCardTypeMapper;
import com.mysms.campus.library.repository.LibraryCardTypeRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of library.library_card_type: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class LibraryCardTypeService extends AppTransactionManager {

    public static final String AGGREGATE = LibraryCardTypeEntity.TABLE;
    static final String PERMISSION_LIST = "library.library_card_type.read";
    static final String PERMISSION_GET = "library.library_card_type.read";
    static final String PERMISSION_CREATE = "library.library_card_type.create";
    static final String PERMISSION_UPDATE = "library.library_card_type.update";
    static final String PERMISSION_DELETE = "library.library_card_type.delete";


    private final LibraryCardTypeRepository repository;
    private final LibraryCardTypeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LibraryCardTypeService(LibraryCardTypeRepository repository, LibraryCardTypeMapper mapper,
            @Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LibraryDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LibraryEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(LibraryEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public LibraryCardType getLibraryCardType(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LibraryCardType> listLibraryCardTypes(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LibraryCardTypeRepository.SORT_FIELDS, LibraryCardTypeRepository.DEFAULT_ORDER,
                LibraryCardTypeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LibraryCardTypeRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LibraryCardTypeEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LibraryCardType createLibraryCardType(LibraryCardType request, boolean validateOnly) {
        validator.validate(request);
        LibraryCardTypeEntity entity = mapper.toEntity(request);
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
            LibraryCardTypeEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LibraryEvents.CREATED, AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LibraryCardType updateLibraryCardType(UUID id, String ifMatch, LibraryCardTypeUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LibraryCardTypeEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + LibraryEvents.UPDATED, AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLibraryCardType(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LibraryCardTypeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByLibraryCardCardTypeId(id)) {
                throw new InvalidStateException(
                        "The library card type is still referenced by library.library_card rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LibraryEvents.DELETED, AGGREGATE, id, LibraryEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LibraryCardTypeEntity loadVisible(DataScope scope, UUID id) {
        LibraryCardTypeEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(LibraryCardTypeEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(LibraryCardTypeEntity entity, LibraryCardTypeUpdate changes) {
        Patches patch = new Patches();
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("maxItems", changes.getMaxItems(), entity::setMaxItems);
        patch.apply("loanDays", changes.getLoanDays(), entity::setLoanDays);
        patch.apply("finePerDay", changes.getFinePerDay(), v -> entity.setFinePerDay(WireValues.decimal(v)));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LibraryCardTypeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getMaxItems() == null) {
            errors.add(new FieldError("maxItems", "must not be null", "NotNull"));
        }
        if (entity.getLoanDays() == null) {
            errors.add(new FieldError("loanDays", "must not be null", "NotNull"));
        }
        if (!creating && entity.getFinePerDay() == null) {
            errors.add(new FieldError("finePerDay", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("finePerDay", WireValues.decimal(entity.getFinePerDay()), 10, 2, errors);
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LibraryCardTypeEntity entity) {
        if (repository.takenName1(entity)) {
            throw new DuplicateResourceException("A library card type with the same name already exists.");
        }
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A library card type with the same code already exists.");
        }
    }
}
