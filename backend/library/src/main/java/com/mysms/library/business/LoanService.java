package com.mysms.library.business;

import com.mysms.authclient.DataScope;
import com.mysms.library.api.model.Loan;
import com.mysms.library.api.model.LoanUpdate;
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
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.config.LibraryEventsConfig;
import com.mysms.library.domain.LoanEntity;
import com.mysms.library.events.LibraryEvents;
import com.mysms.library.mapper.LoanMapper;
import com.mysms.library.repository.LoanRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of library.loan (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class LoanService extends AppTransactionManager {

    public static final String AGGREGATE = LoanEntity.TABLE;
    static final String PERMISSION_LIST = "library.loan.read";
    static final String PERMISSION_GET = "library.loan.read";
    static final String PERMISSION_CREATE = "library.loan.create";
    static final String PERMISSION_UPDATE = "library.loan.update";
    static final String PERMISSION_DELETE = "library.loan.delete";
    /** The contract's LoanUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("mediaUnitId", "mediaId", "libraryCardId", "borrowerUserId", "issuedOn", "dueOn", "returnedOn",
            "fineAmount", "invoiceId", "issuedByUserId");


    private final LoanRepository repository;
    private final LoanMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LoanService(LoanRepository repository, LoanMapper mapper,
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

    public Loan getLoan(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Loan> listLoans(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LoanRepository.SORT_FIELDS, LoanRepository.DEFAULT_ORDER,
                LoanRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LoanRepository.FILTER_FIELDS)
                .and(LoanRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LoanEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Loan createLoan(Loan request, boolean validateOnly) {
        validator.validate(request);
        LoanEntity entity = mapper.toEntity(request);
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
            LoanEntity stored = repository.insert(entity);
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
    public Loan updateLoan(UUID id, String ifMatch, LoanUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LoanEntity entity = loadVisible(scope, id);
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
    public void deleteLoan(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LoanEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
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
    private LoanEntity loadVisible(DataScope scope, UUID id) {
        LoanEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the library.media it belongs to). */
    private Optional<UUID> institutionOf(LoanEntity entity) {
        return repository.institutionOf(entity.getMediaId());
    }

    /** Applies the selected LoanUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(LoanEntity entity, LoanUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "mediaUnitId" -> entity.setMediaUnitId(changes.getMediaUnitId());
                case "mediaId" -> entity.setMediaId(changes.getMediaId());
                case "libraryCardId" -> entity.setLibraryCardId(changes.getLibraryCardId());
                case "borrowerUserId" -> entity.setBorrowerUserId(changes.getBorrowerUserId());
                case "issuedOn" -> entity.setIssuedOn(changes.getIssuedOn());
                case "dueOn" -> entity.setDueOn(changes.getDueOn());
                case "returnedOn" -> entity.setReturnedOn(changes.getReturnedOn());
                case "fineAmount" -> entity.setFineAmount(WireValues.decimal(changes.getFineAmount()));
                case "invoiceId" -> entity.setInvoiceId(changes.getInvoiceId());
                case "issuedByUserId" -> entity.setIssuedByUserId(changes.getIssuedByUserId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LoanEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getMediaUnitId() == null) {
            errors.add(new FieldError("mediaUnitId", "must not be null", "NotNull"));
        }
        if (entity.getMediaId() == null) {
            errors.add(new FieldError("mediaId", "must not be null", "NotNull"));
        }
        if (entity.getLibraryCardId() == null) {
            errors.add(new FieldError("libraryCardId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getFineAmount() == null) {
            errors.add(new FieldError("fineAmount", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("fineAmount", WireValues.decimal(entity.getFineAmount()), 10, 2, errors);
        if (entity.getLibraryCardId() != null && !repository.existsLibraryCardId(entity.getLibraryCardId())) {
            errors.add(new FieldError("libraryCardId", "library card does not exist", "NOT_FOUND"));
        }
        if (entity.getMediaId() != null && !repository.existsMediaId(entity.getMediaId())) {
            errors.add(new FieldError("mediaId", "media does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LoanEntity entity) {

    }
}
