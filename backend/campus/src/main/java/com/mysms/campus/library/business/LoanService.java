package com.mysms.campus.library.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.campus.library.api.model.Invoice;
import com.mysms.campus.library.api.model.Loan;
import com.mysms.campus.library.api.model.LoanUpdate;
import com.mysms.campus.library.api.model.MarkLoanLostRequest;
import com.mysms.campus.library.api.model.RequestLoanReturnRequest;
import com.mysms.campus.library.api.model.ReturnLoanRequest;
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
import com.mysms.core.web.Patches;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.config.LibraryEventsConfig;
import com.mysms.campus.library.domain.LoanEntity;
import com.mysms.campus.library.events.LibraryEvents;
import com.mysms.campus.library.mapper.LoanMapper;
import com.mysms.campus.library.repository.CardHolder;
import com.mysms.campus.library.repository.LoanRepository;
import com.mysms.campus.library.repository.MediaUnitRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
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
 * Business service of library.loan: audit and domain event commit in the same transaction as every change. Rows outside
 * the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant. A borrower
 * reads only their own loans — lent to their account or on a card of their person (404 otherwise); library staff,
 * who issue or check in loans in the title's institution, every loan of it.
 */
@Service
public class LoanService extends AppTransactionManager {

    public static final String AGGREGATE = LoanEntity.TABLE;
    static final String PERMISSION_LIST = "library.loan.read";
    static final String PERMISSION_GET = "library.loan.read";
    static final String PERMISSION_CREATE = "library.loan.create";
    static final String PERMISSION_UPDATE = "library.loan.update";
    static final String PERMISSION_DELETE = "library.loan.delete";
    static final String PERMISSION_REQUEST_RETURN = "library.loan.request_loan_return";
    static final String PERMISSION_RETURN = "library.loan.return_loan";
    static final String PERMISSION_MARK_LOST = "library.loan.mark_loan_lost";
    static final String PERMISSION_INVOICE_FINE = "library.loan.invoice_loan_fine";
    /** The permissions only library staff hold: whoever issues or checks in loans in an institution reads all of them. */
    private static final String[] STAFF = {PERMISSION_CREATE, PERMISSION_RETURN, PERMISSION_MARK_LOST};
    /** A loan the borrower still holds. */
    private static final Set<String> OUT = Set.of("issued", "return_pending");
    private static final int REASON_MAX = 500;


    private final LoanRepository repository;
    private final MediaUnitRepository mediaUnits;
    private final FineInvoicing invoicing;
    private final LoanMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LoanService(LoanRepository repository, MediaUnitRepository mediaUnits, FineInvoicing invoicing,
            LoanMapper mapper,
            @Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LibraryDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LibraryEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(LibraryEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mediaUnits = mediaUnits;
        this.invoicing = invoicing;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /** The loan, when it is the caller's own or the caller staffs its title's institution. */
    public Loan getLoan(UUID id) {
        return doInReadTransaction(() -> {
            LoanEntity loan = loadVisible(DataScope.current(PERMISSION_GET), id);
            if (!own(loan) && !LibraryStaff.staffs(institutionOf(loan), STAFF)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            return mapper.toDto(loan);
        });
    }

    /** The caller's own loans, and every loan of the institutions the caller staffs. */
    public Page<Loan> listLoans(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LoanRepository.SORT_FIELDS, LoanRepository.DEFAULT_ORDER,
                LoanRepository.ID_COLUMN);
        AuthContext caller = AuthContext.require();
        SqlFragment where = FilterParser.parse(filter, LoanRepository.FILTER_FIELDS)
                .and(LoanRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(LoanRepository.reachableBy(caller.userId(), caller.personId(),
                        LibraryStaff.reach(STAFF)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + caller.userId(), clock);
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Loan updateLoan(UUID id, String ifMatch, LoanUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LoanEntity entity = loadVisible(scope, id);
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

    /**
     * library.RequestLoanReturn: the borrower (or a librarian in reach) drops the copy for return; the loan waits as
     * {@code return_pending} until a librarian checks it in. Only from {@code issued} (409); body etag 412. Someone else's
     * loan outside the caller's reach is 404.
     */
    public Loan requestLoanReturn(UUID id, RequestLoanReturnRequest request) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_REQUEST_RETURN);
        OptionalInt bodyVersion = Etags.bodyVersion(request == null ? null : request.getEtag());
        return doInWriteTransaction(() -> {
            LoanEntity loan = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!caller.userId().equals(loan.getBorrowerUserId())) {
                if (!scope.canRead(institutionOf(loan))) {
                    throw new ResourceNotFoundException(AGGREGATE, id);
                }
                scope.requireWritable(institutionOf(loan));
            }
            requireVersion(loan, bodyVersion);
            if (!"issued".equals(loan.getStatus())) {
                throw new InvalidStateException("Only a loan that is out can be dropped for return.");
            }
            settle(loan, "return_pending", loan.getReturnedOn(), loan.getFineAmount(), "requestReturn",
                    List.of("status"), null);
            return reloaded(id);
        });
    }

    /**
     * library.ReturnLoan: the librarian checks the copy in on the given day (not before it was issued, not in the
     * future: 422). The fine is the card type's {@code fine_per_day} for every day past the due date. The copy is back on
     * the shelf. Idempotent: a loan already returned on that day answers as it is; on another day it is 409.
     */
    public Loan returnLoan(UUID id, ReturnLoanRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_RETURN);
        OptionalInt bodyVersion = Etags.bodyVersion(request.getEtag());
        LocalDate returnedOn = request.getReturnedOn();
        return doInWriteTransaction(() -> {
            LoanEntity loan = librarianLoan(scope, id);
            if ("returned".equals(loan.getStatus()) && returnedOn.equals(loan.getReturnedOn())) {
                return mapper.toDto(loan);
            }
            requireVersion(loan, bodyVersion);
            if (!OUT.contains(loan.getStatus())) {
                throw new InvalidStateException("The loan is not out, so it cannot be returned.");
            }
            List<FieldError> errors = new ArrayList<>();
            if (loan.getIssuedOn() != null && returnedOn.isBefore(loan.getIssuedOn())) {
                errors.add(new FieldError("returnedOn", "must not be before the issue date", "DATE_RANGE"));
            }
            if (returnedOn.isAfter(today())) {
                errors.add(new FieldError("returnedOn", "must not be in the future", "DATE_RANGE"));
            }
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            settle(loan, "returned", returnedOn, fine(loan, returnedOn), "return",
                    List.of("status", "returnedOn", "fineAmount"), null);
            copyStatus(loan, "available");
            return reloaded(id);
        });
    }

    /**
     * library.MarkLoanLost: a copy that is out is lost. The loan keeps the overdue fine up to today (the card type's
     * daily fine); the copy is marked lost. Only from {@code issued} or {@code return_pending} (409); the reason is kept
     * in the audit record.
     */
    public Loan markLoanLost(UUID id, MarkLoanLostRequest request) {
        String reason = reason(request == null ? null : request.getReason());
        DataScope scope = DataScope.current(PERMISSION_MARK_LOST);
        OptionalInt bodyVersion = Etags.bodyVersion(request == null ? null : request.getEtag());
        return doInWriteTransaction(() -> {
            LoanEntity loan = librarianLoan(scope, id);
            requireVersion(loan, bodyVersion);
            if (!OUT.contains(loan.getStatus())) {
                throw new InvalidStateException("Only a loan that is out can be marked lost.");
            }
            settle(loan, "lost", loan.getReturnedOn(), fine(loan, today()), "markLost",
                    List.of("status", "fineAmount"), reason);
            copyStatus(loan, "lost");
            return reloaded(id);
        });
    }

    /**
     * library.InvoiceLoanFine: raises the fine of a returned or lost loan as a draft invoice in finance (through its
     * gRPC, with the caller's token) and records the invoice on the loan. Finance keeps one invoice per loan, so a
     * repeat answers the same invoice. 409 while the copy is out or without a fine; the loan is read again after the
     * call, so a loan changed meanwhile is 412.
     */
    public Invoice invoiceLoanFine(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_INVOICE_FINE);
        record Charge(LoanEntity loan, CardHolder holder) {
        }
        Charge charge = doInPrimaryReadTransaction(() -> {
            LoanEntity loan = librarianLoan(scope, id);
            if (!Set.of("returned", "lost").contains(loan.getStatus())) {
                throw new InvalidStateException("A fine is invoiced once the copy is back or lost.");
            }
            if (loan.getFineAmount() == null || loan.getFineAmount().signum() <= 0) {
                throw new InvalidStateException("The loan has no fine to invoice.");
            }
            CardHolder holder = repository.cardHolder(loan.getLibraryCardId())
                    .orElseThrow(() -> new InvalidStateException("The loan's library card no longer exists."));
            return new Charge(loan, holder);
        });
        Invoice invoice = invoicing.raise(id, charge.loan().getFineAmount(), charge.holder(),
                charge.loan().getBorrowerUserId());
        runInWriteTransaction(() -> {
            LoanEntity loan = librarianLoan(scope, id);
            if (loan.getInvoiceId() != null) {
                if (!loan.getInvoiceId().equals(invoice.getId())) {
                    throw new InvalidStateException("The loan already names another invoice.");
                }
                return;
            }
            if (loan.getRowVersion() != charge.loan().getRowVersion() || repository.linkInvoice(id,
                    loan.getRowVersion(), invoice.getId(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The loan changed while its fine was invoiced; try again.");
            }
            audit.recordTransition(AGGREGATE, id, "invoiceFine", List.of("invoiceId"), null);
            outbox.publish(AGGREGATE + ".invoiceFine", AGGREGATE, id, LibraryEvents.payload(id));
        });
        return invoice;
    }

    /** A loan a librarian may act on: visible for the permission (404) and writable (403). */
    private LoanEntity librarianLoan(DataScope scope, UUID id) {
        LoanEntity loan = loadVisible(scope, id);
        scope.requireWritable(institutionOf(loan));
        return loan;
    }

    private static void requireVersion(LoanEntity loan, OptionalInt bodyVersion) {
        if (bodyVersion.isPresent() && bodyVersion.getAsInt() != loan.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
    }

    /** The overdue fine on a day: the card type's daily fine for every day past the due date (none without one). */
    private BigDecimal fine(LoanEntity loan, LocalDate on) {
        if (loan.getDueOn() == null || !on.isAfter(loan.getDueOn())) {
            return BigDecimal.ZERO.setScale(2);
        }
        long days = ChronoUnit.DAYS.between(loan.getDueOn(), on);
        BigDecimal perDay = repository.finePerDay(loan.getLibraryCardId()).orElse(BigDecimal.ZERO);
        return perDay.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    private void settle(LoanEntity loan, String status, LocalDate returnedOn, BigDecimal fineAmount, String step,
            List<String> changed, String reason) {
        OffsetDateTime now = now();
        if (repository.settle(loan.getId(), loan.getRowVersion(), status, returnedOn, fineAmount, now,
                Actor.current().orElse(null)) == 0) {
            throw new PreconditionFailedException("The row changed since it was read.");
        }
        audit.recordTransition(AGGREGATE, loan.getId(), step, changed, reason);
        outbox.publish(AGGREGATE + "." + step, AGGREGATE, loan.getId(), LibraryEvents.payload(loan.getId()));
    }

    /** The loan's copy takes the status (audited as the copy's update); nothing when it already has it. */
    private void copyStatus(LoanEntity loan, String status) {
        if (mediaUnits.setStatus(loan.getMediaUnitId(), status, now(), Actor.current().orElse(null)) == 1) {
            audit.recordUpdate(MediaService.MEDIA_UNIT_AGGREGATE, loan.getMediaUnitId(), List.of("status"));
            outbox.publish(MediaService.MEDIA_UNIT_AGGREGATE + LibraryEvents.UPDATED, MediaService.MEDIA_UNIT_AGGREGATE,
                    loan.getMediaUnitId(), LibraryEvents.payload(loan.getMediaUnitId()));
        }
    }

    private Loan reloaded(UUID id) {
        return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
    }

    private static String reason(String given) {
        String reason = given == null || given.isBlank() ? null : given.trim();
        if (reason != null && reason.length() > REASON_MAX) {
            throw new ValidationFailedException(List.of(new FieldError("reason", "at most " + REASON_MAX
                    + " characters", "Size")));
        }
        return reason;
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LoanEntity loadVisible(DataScope scope, UUID id) {
        LoanEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** Whether the loan was lent to the caller's account or on a card of the caller's person. */
    private boolean own(LoanEntity loan) {
        AuthContext caller = AuthContext.require();
        if (caller.userId().equals(loan.getBorrowerUserId())) {
            return true;
        }
        Optional<UUID> person = caller.personId();
        return person.isPresent() && repository.cardHolder(loan.getLibraryCardId())
                .map(holder -> person.get().equals(holder.personId())).orElse(false);
    }

    /** The institution of the library.media the row belongs to. */
    private Optional<UUID> institutionOf(LoanEntity entity) {
        return repository.institutionOf(entity.getMediaId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(LoanEntity entity, LoanUpdate changes) {
        Patches patch = new Patches();
        patch.apply("mediaUnitId", changes.getMediaUnitId(), entity::setMediaUnitId);
        patch.apply("mediaId", changes.getMediaId(), entity::setMediaId);
        patch.apply("libraryCardId", changes.getLibraryCardId(), entity::setLibraryCardId);
        patch.apply("borrowerUserId", changes.getBorrowerUserId(), entity::setBorrowerUserId);
        patch.apply("issuedOn", changes.getIssuedOn(), entity::setIssuedOn);
        patch.apply("dueOn", changes.getDueOn(), entity::setDueOn);
        patch.apply("returnedOn", changes.getReturnedOn(), entity::setReturnedOn);
        patch.apply("fineAmount", changes.getFineAmount(), v -> entity.setFineAmount(WireValues.decimal(v)));
        patch.apply("invoiceId", changes.getInvoiceId(), entity::setInvoiceId);
        patch.apply("issuedByUserId", changes.getIssuedByUserId(), entity::setIssuedByUserId);
        return patch.applied();
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
