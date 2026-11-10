package com.mysms.library.business;

import com.mysms.authclient.DataScope;
import com.mysms.library.api.model.LibraryCard;
import com.mysms.library.api.model.LibraryCardUpdate;
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
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.config.LibraryEventsConfig;
import com.mysms.library.domain.LibraryCardEntity;
import com.mysms.library.events.LibraryEvents;
import com.mysms.library.mapper.LibraryCardMapper;
import com.mysms.library.repository.LibraryCardRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of library.library_card (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class LibraryCardService extends AppTransactionManager {

    public static final String AGGREGATE = LibraryCardEntity.TABLE;
    static final String PERMISSION_LIST = "library.library_card.read";
    static final String PERMISSION_GET = "library.library_card.read";
    static final String PERMISSION_CREATE = "library.library_card.create";
    static final String PERMISSION_UPDATE = "library.library_card.update";
    static final String PERMISSION_DELETE = "library.library_card.delete";
    /** The contract's LibraryCardUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "cardNumber", "cardTypeId", "holderType", "personId", "studentId", "staffId",
            "maxItemsOverride", "issueDate", "expiryDate", "status");


    private final LibraryCardRepository repository;
    private final LibraryCardMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LibraryCardService(LibraryCardRepository repository, LibraryCardMapper mapper,
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

    public LibraryCard getLibraryCard(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LibraryCard> listLibraryCards(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LibraryCardRepository.SORT_FIELDS, LibraryCardRepository.DEFAULT_ORDER,
                LibraryCardRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LibraryCardRepository.FILTER_FIELDS)
                .and(LibraryCardRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LibraryCardEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LibraryCard createLibraryCard(LibraryCard request, boolean validateOnly) {
        validator.validate(request);
        LibraryCardEntity entity = mapper.toEntity(request);
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
            LibraryCardEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LibraryEvents.CREATED, AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public LibraryCard updateLibraryCard(UUID id, String ifMatch, LibraryCardUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LibraryCardEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + LibraryEvents.UPDATED, AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLibraryCard(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LibraryCardEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByLoanLibraryCardId(id)) {
                throw new InvalidStateException(
                        "The library card is still referenced by library.loan rows; remove those first.");
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
    private LibraryCardEntity loadVisible(DataScope scope, UUID id) {
        LibraryCardEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(LibraryCardEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected LibraryCardUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(LibraryCardEntity entity, LibraryCardUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "cardNumber" -> entity.setCardNumber(changes.getCardNumber());
                case "cardTypeId" -> entity.setCardTypeId(changes.getCardTypeId());
                case "holderType" -> entity.setHolderType(Optional.ofNullable(changes.getHolderType())
                        .map(v -> v.getValue()).orElse(null));
                case "personId" -> entity.setPersonId(changes.getPersonId());
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "staffId" -> entity.setStaffId(changes.getStaffId());
                case "maxItemsOverride" -> entity.setMaxItemsOverride(changes.getMaxItemsOverride());
                case "issueDate" -> entity.setIssueDate(changes.getIssueDate());
                case "expiryDate" -> entity.setExpiryDate(changes.getExpiryDate());
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LibraryCardEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getCardNumber() == null) {
            errors.add(new FieldError("cardNumber", "must not be null", "NotNull"));
        }
        if (entity.getCardTypeId() == null) {
            errors.add(new FieldError("cardTypeId", "must not be null", "NotNull"));
        }
        if (entity.getHolderType() == null) {
            errors.add(new FieldError("holderType", "must not be null", "NotNull"));
        }
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getIssueDate() == null) {
            errors.add(new FieldError("issueDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getCardTypeId() != null && !repository.existsCardTypeId(entity.getCardTypeId())) {
            errors.add(new FieldError("cardTypeId", "library card type does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LibraryCardEntity entity) {
        if (repository.takenInstitutionIdCardNumber1(entity)) {
            throw new DuplicateResourceException(
                    "A library card with the same institution id and card number already exists.");
        }
    }
}
