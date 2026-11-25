package com.mysms.finance.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
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
import com.mysms.finance.api.model.CancelInvoiceRequest;
import com.mysms.finance.api.model.CreateInvoiceFromDuesRequest;
import com.mysms.finance.api.model.RaiseChargeInvoiceRequest;
import com.mysms.finance.api.model.FeeStatement;
import com.mysms.finance.api.model.Invoice;
import com.mysms.finance.api.model.InvoiceLine;
import com.mysms.finance.api.model.InvoiceUpdate;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.api.model.PostInvoiceRequest;
import com.mysms.finance.api.model.SendFeeRemindersRequest;
import com.mysms.finance.api.model.SendResult;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.InvoiceEntity;
import com.mysms.finance.domain.InvoiceLineEntity;
import com.mysms.finance.domain.PaymentEntity;
import com.mysms.finance.domain.StudentFeeDueEntity;
import com.mysms.finance.events.FinanceEvents;
import com.mysms.finance.mapper.InvoiceLineMapper;
import com.mysms.finance.mapper.InvoiceMapper;
import com.mysms.finance.mapper.StudentFeeDueMapper;
import com.mysms.finance.repository.InvoiceLineRepository;
import com.mysms.finance.repository.InvoiceRepository;
import com.mysms.finance.repository.InvoiceRepository.DueLine;
import com.mysms.finance.repository.LedgerRepository;
import com.mysms.finance.repository.NumberSequenceRepository;
import com.mysms.finance.repository.PaymentRepository;
import com.mysms.finance.repository.StudentFeeDueRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of the Invoice aggregate: invoices and their lines, raising an invoice for
 * dues, posting it (it is numbered then and never changes again), cancelling a draft, reminding families of open
 * invoices, and a student's fee statement.
 *
 * <p>Invoices are personal data, owner-only below the fee office: a caller reads the invoices of the institutions
 * where they work on fees ({@link FinanceStaff}); outside that reach only their own students' invoices, which sis
 * confirms with the caller's token ({@link StudentOwnershipLookup}; 503 when it cannot answer). Lines are read through
 * their invoice.
 *
 * <p>Reminders are not delivered here: comms owns delivery and finance may not call it, so each
 * reminder is audited on its invoice and published as a {@code finance.fee_reminder.requested} event (ids and the
 * channel only) for comms to deliver.
 */
@Service
public class InvoiceService extends AppTransactionManager {

    public static final String AGGREGATE = InvoiceEntity.TABLE;
    public static final String INVOICE_LINE_AGGREGATE = InvoiceLineEntity.TABLE;
    static final String PERMISSION_READ = "finance.invoice.read";
    static final String PERMISSION_CREATE = "finance.invoice.create";
    static final String PERMISSION_UPDATE = "finance.invoice.update";
    static final String PERMISSION_FROM_DUES = "finance.invoice.create_invoice_from_dues";
    static final String PERMISSION_POST = "finance.invoice.post_invoice";
    static final String PERMISSION_CANCEL = "finance.invoice.cancel_invoice";
    static final String PERMISSION_REMIND = "finance.invoice.send_fee_reminders";
    static final String PERMISSION_STATEMENT = "finance.invoice.get_fee_statement";
    static final String INVOICE_LINE_PERMISSION_READ = "finance.invoice_line.read";
    /** The event comms delivers a reminder from. */
    public static final String REMINDER_REQUESTED = "finance.fee_reminder.requested";
    /** The contract's InvoiceUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("institutionId", "billToName", "invoiceDate", "dueDate");
    /** The most dues one invoice bills. */
    static final int MAX_DUES = 200;
    /** The most invoices one reminder run names. */
    static final int MAX_REMINDED = 500;
    private static final String DRAFT_PREFIX = "DRAFT-";
    private static final String NUMBER_PREFIX = "INV-";
    private static final int REASON_MAX = 300;
    private static final Set<String> CHARGED = Set.of("posted", "partially_paid", "paid");

    private final InvoiceRepository repository;
    private final InvoiceMapper mapper;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceLineMapper invoiceLineMapper;
    private final StudentFeeDueRepository dues;
    private final StudentFeeDueMapper dueMapper;
    private final PaymentRepository payments;
    private final PaymentViewFactory paymentViews;
    private final LedgerRepository ledger;
    private final NumberSequenceRepository numbers;
    private final StudentOwnershipLookup ownership;
    private final CurrencyLookup currencies;
    private final InstitutionCurrencyLookup institutionCurrencies;
    private final ChargeTargets chargeTargets;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public InvoiceService(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            InvoiceRepository repository, InvoiceMapper mapper, InvoiceLineRepository invoiceLineRepository,
            InvoiceLineMapper invoiceLineMapper, StudentFeeDueRepository dues, StudentFeeDueMapper dueMapper,
            PaymentRepository payments, PaymentViewFactory paymentViews, LedgerRepository ledger,
            NumberSequenceRepository numbers, StudentOwnershipLookup ownership, CurrencyLookup currencies,
            InstitutionCurrencyLookup institutionCurrencies, ChargeTargets chargeTargets,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.invoiceLineRepository = invoiceLineRepository;
        this.invoiceLineMapper = invoiceLineMapper;
        this.dues = dues;
        this.dueMapper = dueMapper;
        this.payments = payments;
        this.paymentViews = paymentViews;
        this.ledger = ledger;
        this.numbers = numbers;
        this.ownership = ownership;
        this.currencies = currencies;
        this.institutionCurrencies = institutionCurrencies;
        this.chargeTargets = chargeTargets;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /** One invoice: in the fee office's reach, billed to the caller, or of the caller's own student (404 otherwise). */
    public Invoice getInvoice(UUID id) {
        return mapper.toDto(readable(id, DataScope.current(PERMISSION_READ)));
    }

    /**
     * The invoices the caller may read: the fee office's reach, plus their own students' invoices and the invoices
     * billed to them elsewhere.
     */
    public Page<Invoice> listInvoices(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(sortBy, InvoiceRepository.SORT_FIELDS, InvoiceRepository.DEFAULT_ORDER,
                InvoiceRepository.ID_COLUMN);
        DataScope scope = DataScope.current(PERMISSION_READ);
        DataScope reach = FinanceStaff.reach(caller);
        SqlFragment where = FilterParser.parse(filter, InvoiceRepository.FILTER_FIELDS)
                .and(InvoiceRepository.scope(scope));
        if (!FinanceStaff.covers(reach, scope)) {
            where = where.and(InvoiceRepository.reachableOrOwn(reach, ownership.ownStudents(caller), caller.userId()));
        }
        SqlFragment filtered = where;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, filtered).map(mapper::toDto));
    }

    /**
     * A draft invoice for a student or a user (at least one, 422), numbered provisionally until it is posted; its
     * totals stay zero until lines are added (lines are written only from dues: finance.CreateInvoiceFromDues).
     */
    public Invoice createInvoice(Invoice request, boolean validateOnly) {
        validator.validate(request);
        InvoiceEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        entity.setInvoiceNumber(draftNumber(entity.getId()));
        entity.setSubtotal(BigDecimal.ZERO);
        entity.setDiscountTotal(BigDecimal.ZERO);
        entity.setTaxTotal(BigDecimal.ZERO);
        entity.setTotal(BigDecimal.ZERO);
        entity.setAmountPaid(BigDecimal.ZERO);
        entity.setStatus("draft");
        entity.setInvoiceType("invoice");
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        assertBilled(entity);
        return doInWriteTransaction(() -> {
            scope.requireWritable(Optional.ofNullable(entity.getInstitutionId()));
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            repository.insert(entity);
            audit.recordCreate(AGGREGATE, entity.getId());
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, entity.getId(),
                    FinanceEvents.payload(entity.getId()));
            return mapper.toDto(repository.findById(entity.getId()).orElseThrow());
        });
    }

    /**
     * Changes a draft's bill-to and dates under If-Match (a posted invoice never changes: 409). The institution of an
     * invoice with lines stays (409): its lines bill that institution's dues.
     */
    public Invoice updateInvoice(UUID id, String ifMatch, InvoiceUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            InvoiceEntity entity = loadVisible(scope, id);
            scope.requireWritable(Optional.ofNullable(entity.getInstitutionId()));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            requireDraft(entity);
            UUID institutionBefore = entity.getInstitutionId();
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            scope.requireWritable(Optional.ofNullable(entity.getInstitutionId()));
            if (!Objects.equals(institutionBefore, entity.getInstitutionId()) && repository.lineCount(id) > 0) {
                throw new InvalidStateException("The invoice bills dues of its institution; its institution stays.");
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

    /**
     * finance.CreateInvoiceFromDues: a draft invoice billing the student's dues, one line per due (the due's amount,
     * its discount, its net as the line total). Every due must be the student's (422), unbilled and not cancelled
     * (409), settled by no payment (409: the payment already counts against the due), of one institution and one
     * currency (422) the caller may write (403). The dues are locked while they are billed, so two requests never
     * bill one due twice. The name on the invoice is the request's, or the student's name from sis.
     */
    public Invoice createInvoiceFromDues(CreateInvoiceFromDuesRequest request) {
        validator.validate(request);
        List<UUID> dueIds = List.copyOf(new LinkedHashSet<>(request.getDueIds()));
        if (dueIds.isEmpty() || dueIds.size() > MAX_DUES) {
            throw new ValidationFailedException(List.of(new FieldError("dueIds",
                    "name between 1 and " + MAX_DUES + " dues", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_FROM_DUES);
        String named = request.getBillToName();
        String billTo = named != null ? named.trim()
                : ownership.studentName(request.getStudentId()).orElseThrow(() -> new ValidationFailedException(
                        List.of(new FieldError("billToName", "the student's name is not known; give the name",
                                "REQUIRED"))));
        return doInWriteTransaction(() -> {
            List<StudentFeeDueEntity> billed = lockDues(dueIds, scope, request.getStudentId());
            StudentFeeDueEntity first = billed.get(0);
            scope.requireWritable(Optional.of(first.getInstitutionId()));
            for (StudentFeeDueEntity due : billed) {
                if (!"draft".equals(due.getStatus()) || due.getInvoiceId() != null) {
                    throw new InvalidStateException("A due is already billed on an invoice, or cancelled.");
                }
            }
            if (!dues.withAllocations(dueIds).isEmpty()) {
                throw new InvalidStateException("A payment already settles one of the dues; record the rest of it "
                        + "against the due.");
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            InvoiceEntity invoice = invoiceFor(billed, billTo);
            invoice.onCreatingData(Actor.current(), clock);
            invoice.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
            invoice.setInvoiceNumber(draftNumber(invoice.getId()));
            repository.insert(invoice);
            repository.insertLines(invoice.getId(), lines(billed), now, actor);
            if (dues.markInvoiced(dueIds, invoice.getId(), now, actor) != dueIds.size()) {
                throw new InvalidStateException("A due changed while it was billed; read the dues again.");
            }
            audit.recordCreate(AGGREGATE, invoice.getId());
            billed.forEach(due -> audit.recordTransition(StudentFeeDueService.AGGREGATE, due.getId(), "invoice",
                    List.of("status", "invoiceId"), null));
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, invoice.getId(),
                    FinanceEvents.payload(invoice.getId()));
            return mapper.toDto(repository.findById(invoice.getId()).orElseThrow());
        });
    }

    /**
     * finance.RaiseChargeInvoice: a draft invoice with one line for a charge another module computed (a library fine),
     * in the institution's currency unless one is named. The billed student must be the institution's, and a billed
     * account that of one of its students or staff (422). One invoice per source: its id is derived from the tenant and
     * the source, so raising the same source again answers the invoice already made while it is live and bills the
     * same institution, party and amount — anything else is 409, never a silently kept or cancelled invoice — and two
     * raises at once end with one invoice, answered to both. Created under the invoice's create permission, inside the
     * caller's institutions.
     */
    public Invoice raiseChargeInvoice(RaiseChargeInvoiceRequest request) {
        validator.validate(request);
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkDecimal("amount", request.getAmount(), 14, 2, errors);
        BigDecimal amount = WireValues.decimal(request.getAmount());
        if (amount.signum() <= 0) {
            errors.add(new FieldError("amount", "must be more than zero", "Min"));
        }
        if (request.getStudentId() == null && request.getBillToUserId() == null) {
            errors.add(new FieldError("studentId", "name the student or the user account billed", "REQUIRED"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        UUID institution = request.getInstitutionId();
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        scope.requireWritable(Optional.of(institution));
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        UUID id = chargeInvoiceId(tenant, request.getSourceType(), request.getSourceId());
        Optional<Invoice> raised = doInPrimaryReadTransaction(() -> {
            chargeTargets.require(institution, request.getStudentId(), request.getBillToUserId());
            return repository.findAnyById(id).map(found -> sameCharge(found, request, amount));
        });
        if (raised.isPresent()) {
            return raised.get();
        }
        UUID currency = request.getCurrencyId() != null ? request.getCurrencyId()
                : institutionCurrencies.currencyOf(institution);
        return doInWriteTransaction(() -> {
            chargeTargets.require(institution, request.getStudentId(), request.getBillToUserId());
            InvoiceEntity invoice = InvoiceEntity.builder()
                    .tenantId(tenant)
                    .institutionId(institution)
                    .studentId(request.getStudentId())
                    .billToUserId(request.getBillToUserId())
                    .billToName(request.getBillToName().trim())
                    .invoiceDate(LocalDate.now(clock))
                    .dueDate(request.getDueDate())
                    .currencyId(currency)
                    .subtotal(amount)
                    .discountTotal(BigDecimal.ZERO)
                    .taxTotal(BigDecimal.ZERO)
                    .total(amount)
                    .amountPaid(BigDecimal.ZERO)
                    .status("draft")
                    .invoiceType("invoice")
                    .build();
            invoice.onCreatingData(Actor.current(), clock);
            invoice.setId(id);
            invoice.setInvoiceNumber(draftNumber(id));
            if (!repository.insertIfAbsent(invoice)) {
                return sameCharge(repository.findAnyById(id).orElseThrow(), request, amount);
            }
            repository.insertLines(id, List.of(new DueLine(UuidV7.next(clock), null, request.getDescription().trim(),
                    amount, BigDecimal.ZERO, amount)), now(), Actor.current().orElse(null));
            audit.recordCreate(AGGREGATE, id);
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, id, FinanceEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * The invoice a source raised before, while it is live and bills the same institution, student, account and amount;
     * 409 otherwise (a deleted or cancelled one included: the source cannot be billed under it again).
     */
    private Invoice sameCharge(InvoiceEntity raised, RaiseChargeInvoiceRequest request, BigDecimal amount) {
        if (raised.getDeletedAt() != null || "cancelled".equals(raised.getStatus())) {
            throw new ConflictException("DUPLICATE", "The source's invoice was cancelled or deleted; it is not raised "
                    + "again.");
        }
        if (!request.getInstitutionId().equals(raised.getInstitutionId())
                || !Objects.equals(request.getStudentId(), raised.getStudentId())
                || !Objects.equals(request.getBillToUserId(), raised.getBillToUserId())
                || raised.getTotal().compareTo(amount) != 0) {
            throw new ConflictException("DUPLICATE", "The source was already invoiced with another institution, party "
                    + "or amount.");
        }
        return mapper.toDto(raised);
    }

    /** The id of the one invoice a source may raise in a tenant (name-based, so every raise of it finds the same). */
    static UUID chargeInvoiceId(UUID tenant, String sourceType, UUID sourceId) {
        return UUID.nameUUIDFromBytes(("finance.charge:" + tenant + ":" + sourceType + ":" + sourceId)
                .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * finance.PostInvoice: a draft with lines and something to pay (422) becomes a posted document numbered from the
     * institution's {@code INV-} series (the tenant's for a tenant-level invoice); a posted invoice never changes
     * again. Body etag 412, any other state 409.
     */
    public Invoice postInvoice(UUID id, PostInvoiceRequest request) {
        DataScope scope = DataScope.current(PERMISSION_POST);
        return doInWriteTransaction(() -> {
            InvoiceEntity entity = loadVisible(scope, id);
            scope.requireWritable(Optional.ofNullable(entity.getInstitutionId()));
            requireBodyEtag(entity, request == null ? null : request.getEtag());
            requireDraft(entity);
            List<FieldError> errors = new ArrayList<>();
            if (repository.lineCount(id) == 0) {
                errors.add(new FieldError("lines", "an invoice needs at least one line", "REQUIRED"));
            } else if (entity.getTotal().signum() <= 0) {
                errors.add(new FieldError("total", "there is nothing to pay on the invoice", "Min"));
            }
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            String number = numbers.next(entity.getInstitutionId(), InvoiceRepository.INVOICE_NUMBERS, NUMBER_PREFIX,
                    UuidV7.next(clock), now, actor);
            if (repository.post(id, entity.getRowVersion(), number, now, actor) == 0) {
                throw new PreconditionFailedException("The invoice changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "post", List.of("status", "invoiceNumber", "postedAt"), null);
            outbox.publish(AGGREGATE + ".post", AGGREGATE, id, FinanceEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * finance.CancelInvoice: a draft is cancelled and its dues are released to be billed again (a posted invoice is a
     * document and is corrected by a credit note, not cancelled: 409). The reason is kept in the audit record.
     */
    public Invoice cancelInvoice(UUID id, CancelInvoiceRequest request) {
        String given = request == null ? null : request.getReason();
        String reason = given == null ? null : given.trim();
        if (reason != null && reason.length() > REASON_MAX) {
            throw new ValidationFailedException(List.of(new FieldError("reason",
                    "at most " + REASON_MAX + " characters", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_CANCEL);
        return doInWriteTransaction(() -> {
            InvoiceEntity entity = loadVisible(scope, id);
            scope.requireWritable(Optional.ofNullable(entity.getInstitutionId()));
            requireBodyEtag(entity, request == null ? null : request.getEtag());
            requireDraft(entity);
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            List<StudentFeeDueEntity> released = dues.ofInvoice(id);
            if (repository.cancel(id, entity.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The invoice changed since it was read.");
            }
            dues.release(id, now, actor);
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status", "cancelledAt"),
                    reason == null || reason.isEmpty() ? null : reason);
            released.forEach(due -> audit.recordTransition(StudentFeeDueService.AGGREGATE, due.getId(), "release",
                    List.of("status", "invoiceId"), null));
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, FinanceEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * finance.SendFeeReminders: one reminder per open invoice (posted, not fully paid) of the institution — only the
     * named ones when the request names any; a named invoice that is not open there is skipped. Each reminder is
     * audited on its invoice and published for comms to deliver; delivery itself is comms' work.
     */
    public SendResult sendFeeReminders(SendFeeRemindersRequest request) {
        validator.validate(request);
        List<UUID> named = request.getInvoiceIds() == null ? List.of()
                : List.copyOf(new LinkedHashSet<>(request.getInvoiceIds()));
        if (named.size() > MAX_REMINDED) {
            throw new ValidationFailedException(List.of(new FieldError("invoiceIds",
                    "at most " + MAX_REMINDED + " invoices", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_REMIND);
        scope.requireWritable(Optional.of(request.getInstitutionId()));
        String channel = request.getChannel().getValue();
        return doInWriteTransaction(() -> {
            List<InvoiceEntity> open = repository.openOfInstitution(request.getInstitutionId(), named);
            for (InvoiceEntity invoice : open) {
                audit.recordTransition(AGGREGATE, invoice.getId(), "remind", List.of(), null);
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("invoiceId", invoice.getId().toString());
                payload.put("institutionId", invoice.getInstitutionId().toString());
                if (invoice.getStudentId() != null) {
                    payload.put("studentId", invoice.getStudentId().toString());
                }
                if (invoice.getBillToUserId() != null) {
                    payload.put("billToUserId", invoice.getBillToUserId().toString());
                }
                payload.put("channel", channel);
                outbox.publish(REMINDER_REQUESTED, AGGREGATE, invoice.getId(), payload);
            }
            int skipped = named.isEmpty() ? 0 : named.size() - open.size();
            return new SendResult(open.size(), skipped);
        });
    }

    /**
     * finance.GetFeeStatement: a student's dues, invoices and payments with the totals (exact, never rounded). The fee
     * office reads the records in its reach; the student, their guardians and a clerk whose own child has records
     * elsewhere read all of it once sis confirms the relation (503 when it cannot). Another payer's payment outside
     * the caller's reach shows only amount, date, status and receipt. 404 when nothing is readable.
     */
    public FeeStatement getFeeStatement(UUID studentId, UUID academicYearId) {
        FeeStatement statement = feeStatement(AuthContext.require(), studentId, academicYearId);
        if (statement.getCurrencyId() != null) {
            currencies.code(statement.getCurrencyId()).ifPresent(statement::setCurrencyCode);
        }
        return statement;
    }

    /** The statement as {@code caller} may read it; sis is asked only when records lie outside the caller's reach. */
    FeeStatement feeStatement(AuthContext caller, UUID studentId, UUID academicYearId) {
        DataScope reach = FinanceStaff.reach(caller);
        boolean office = reach.all() || !reach.institutions().isEmpty();
        Records records = doInReadTransaction(() -> records(studentId, academicYearId));
        Records reached = records.within(reach);
        if (office && reached.covers(records)) {
            // every record of the student lies in the reach: whether the student is also the caller's adds nothing
            if (reached.isEmpty()) {
                throw new ResourceNotFoundException("sis.student", studentId);
            }
            return doInReadTransaction(() -> statement(reached, caller, reach));
        }
        if (ownership.relationTo(caller, studentId).isPresent()) {
            return doInReadTransaction(() -> statement(records, caller, reach));
        }
        if (reached.isEmpty()) {
            throw new ResourceNotFoundException("sis.student", studentId);
        }
        return doInReadTransaction(() -> statement(reached, caller, reach));
    }

    // ---- InvoiceLine: a record of the Invoice aggregate, read through its invoice ----

    public InvoiceLine getInvoiceLine(UUID invoiceId, UUID id) {
        readable(invoiceId, DataScope.current(INVOICE_LINE_PERMISSION_READ));
        return doInReadTransaction(() -> invoiceLineRepository.findById(id)
                .filter(line -> invoiceId.equals(line.getInvoiceId()))
                .map(invoiceLineMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(INVOICE_LINE_AGGREGATE, id)));
    }

    public Page<InvoiceLine> listInvoiceLines(UUID invoiceId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        readable(invoiceId, DataScope.current(INVOICE_LINE_PERMISSION_READ));
        SortSpec sort = SortSpec.parse(sortBy, InvoiceLineRepository.SORT_FIELDS, InvoiceLineRepository.DEFAULT_ORDER,
                InvoiceLineRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, InvoiceLineRepository.FILTER_FIELDS)
                .and(InvoiceLineRepository.ofParent(invoiceId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + invoiceId, clock);
        return doInReadTransaction(() -> invoiceLineRepository.list(page, where).map(invoiceLineMapper::toDto));
    }

    // ---- helpers ----

    /**
     * The invoice if the caller may read it: the fee office's reach, billed to the caller, or their own student's —
     * which only sis can confirm, so it is asked last (404 otherwise).
     */
    InvoiceEntity readable(UUID id, DataScope scope) {
        AuthContext caller = AuthContext.require();
        InvoiceEntity entity = doInReadTransaction(() -> loadVisible(scope, id));
        if (!FinanceStaff.reaches(FinanceStaff.reach(caller), scope, Optional.ofNullable(entity.getInstitutionId()))
                && !caller.userId().equals(entity.getBillToUserId())
                && (entity.getStudentId() == null || ownership.relationTo(caller, entity.getStudentId()).isEmpty())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** A student's dues (of the year, when one is named) and the invoices that bill them. */
    private record Records(List<StudentFeeDueEntity> dues, List<InvoiceEntity> invoices) {

        /** The records of the institutions the fee office reaches. */
        Records within(DataScope reach) {
            return new Records(dues.stream().filter(due -> reach.canRead(Optional.of(due.getInstitutionId()))).toList(),
                    invoices.stream().filter(invoice -> FinanceStaff.reaches(reach, reach,
                            Optional.ofNullable(invoice.getInstitutionId()))).toList());
        }

        boolean covers(Records all) {
            return dues.size() == all.dues().size() && invoices.size() == all.invoices().size();
        }

        boolean isEmpty() {
            return dues.isEmpty() && invoices.isEmpty();
        }
    }

    private Records records(UUID studentId, UUID academicYearId) {
        List<StudentFeeDueEntity> owed = dues.ofStudent(studentId, academicYearId);
        Set<UUID> invoicesOfYear = new HashSet<>();
        owed.forEach(due -> {
            if (due.getInvoiceId() != null) {
                invoicesOfYear.add(due.getInvoiceId());
            }
        });
        List<InvoiceEntity> invoices = repository.ofStudent(studentId).stream()
                .filter(invoice -> academicYearId == null || invoicesOfYear.contains(invoice.getId()))
                .toList();
        return new Records(owed, invoices);
    }

    private FeeStatement statement(Records records, AuthContext caller, DataScope reach) {
        List<StudentFeeDueEntity> owed = records.dues();
        List<InvoiceEntity> invoices = records.invoices();
        List<UUID> dueIds = owed.stream().map(StudentFeeDueEntity::getId).toList();
        List<UUID> invoiceIds = invoices.stream().map(InvoiceEntity::getId).toList();
        List<PaymentEntity> paid = payments.ofStudent(dueIds, invoiceIds);
        Ledger money = Ledger.of(ledger.allocationsTouching(concat(dueIds, invoiceIds)),
                ledger.refunded(paid.stream().map(PaymentEntity::getId).toList()));
        Map<UUID, String> invoiceStatus = new LinkedHashMap<>();
        invoices.forEach(invoice -> invoiceStatus.put(invoice.getId(), invoice.getStatus()));
        BigDecimal totalDue = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        Set<UUID> currencyIds = new LinkedHashSet<>();
        for (StudentFeeDueEntity due : owed) {
            if (!"cancelled".equals(due.getStatus())) {
                currencyIds.add(due.getCurrencyId());
                if (due.getInvoiceId() == null || !CHARGED.contains(invoiceStatus.get(due.getInvoiceId()))) {
                    totalDue = totalDue.add(due.getNetAmount());
                }
                totalPaid = totalPaid.add(money.paidTo(due.getId()));
            }
        }
        for (InvoiceEntity invoice : invoices) {
            if (CHARGED.contains(invoice.getStatus())) {
                currencyIds.add(invoice.getCurrencyId());
                totalDue = totalDue.add(invoice.getTotal());
                totalPaid = totalPaid.add(money.paidTo(invoice.getId()));
            }
        }
        if (currencyIds.size() > 1) {
            throw new ConflictException("MIXED_CURRENCIES",
                    "The student's fees are in more than one currency; read them per academic year.");
        }
        FeeStatement statement = new FeeStatement(WireValues.decimal(totalDue.setScale(2)),
                WireValues.decimal(totalPaid.setScale(2)), WireValues.decimal(totalDue.subtract(totalPaid)
                        .setScale(2)));
        statement.setCurrencyId(currencyIds.isEmpty() ? null : currencyIds.iterator().next());
        statement.setDues(owed.stream().map(dueMapper::toDto).toList());
        statement.setInvoices(invoices.stream().map(mapper::toDto).toList());
        List<Payment> shown = new ArrayList<>(paymentViews.views(paid));
        for (int i = 0; i < paid.size(); i++) {
            PaymentEntity payment = paid.get(i);
            boolean office = FinanceStaff.reaches(reach, reach, Optional.ofNullable(payment.getInstitutionId()));
            if (!office && !caller.userId().equals(payment.getPayerUserId())) {
                shown.set(i, PaymentViewFactory.ownerOnly(shown.get(i)));
            }
        }
        statement.setPayments(shown);
        return statement;
    }

    /** The dues to bill, locked, each the student's and visible to the caller (422 naming each one that is not). */
    private List<StudentFeeDueEntity> lockDues(List<UUID> dueIds, DataScope scope, UUID studentId) {
        Map<UUID, StudentFeeDueEntity> found = new LinkedHashMap<>();
        dues.lock(dueIds).forEach(due -> found.put(due.getId(), due));
        List<FieldError> errors = new ArrayList<>();
        Set<UUID> institutions = new HashSet<>();
        Set<UUID> currencyIds = new HashSet<>();
        for (int i = 0; i < dueIds.size(); i++) {
            StudentFeeDueEntity due = found.get(dueIds.get(i));
            if (due == null || !scope.canRead(Optional.of(due.getInstitutionId()))) {
                errors.add(new FieldError("dueIds[" + i + "]", "due does not exist", "NOT_FOUND"));
            } else if (!studentId.equals(due.getStudentId())) {
                errors.add(new FieldError("dueIds[" + i + "]", "due is another student's", "OTHER_STUDENT"));
            } else {
                institutions.add(due.getInstitutionId());
                currencyIds.add(due.getCurrencyId());
            }
        }
        if (institutions.size() > 1) {
            errors.add(new FieldError("dueIds", "dues of one institution only", "OTHER_INSTITUTION"));
        }
        if (currencyIds.size() > 1) {
            errors.add(new FieldError("dueIds", "dues of one currency only", "OTHER_CURRENCY"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return dueIds.stream().map(found::get).toList();
    }

    private InvoiceEntity invoiceFor(List<StudentFeeDueEntity> billed, String billTo) {
        StudentFeeDueEntity first = billed.get(0);
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (StudentFeeDueEntity due : billed) {
            subtotal = subtotal.add(due.getAmount());
            discount = discount.add(due.getDiscountAmount());
            total = total.add(due.getNetAmount());
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate dueDate = billed.stream().map(StudentFeeDueEntity::getDueDate).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
        return InvoiceEntity.builder()
                .institutionId(first.getInstitutionId())
                .studentId(first.getStudentId())
                .billToName(billTo)
                .invoiceDate(today)
                .dueDate(dueDate)
                .currencyId(first.getCurrencyId())
                .subtotal(subtotal)
                .discountTotal(discount)
                .taxTotal(BigDecimal.ZERO)
                .total(total)
                .amountPaid(BigDecimal.ZERO)
                .status("draft")
                .invoiceType("invoice")
                .build();
    }

    /** One line per due: "<fee head> · installment <n>", its amount, discount and net. */
    private List<DueLine> lines(List<StudentFeeDueEntity> billed) {
        Map<UUID, String> heads = repository.feeHeadNames(billed.stream().map(StudentFeeDueEntity::getFeeHeadId)
                .filter(Objects::nonNull).toList());
        Map<UUID, Integer> installments = repository.installmentNumbers(billed.stream()
                .map(StudentFeeDueEntity::getInstallmentId).filter(Objects::nonNull).toList());
        return billed.stream().map(due -> {
            String head = due.getFeeHeadId() == null ? "Fee" : heads.getOrDefault(due.getFeeHeadId(), "Fee");
            String description = due.getInstallmentId() == null || !installments.containsKey(due.getInstallmentId())
                    ? head : head + " · installment " + installments.get(due.getInstallmentId());
            return new DueLine(UuidV7.next(clock), due.getFeeHeadId(), description, due.getAmount(),
                    due.getDiscountAmount(), due.getNetAmount());
        }).toList();
    }

    /** The live row, if the caller's read scope shows its institution (404 otherwise). */
    private InvoiceEntity loadVisible(DataScope scope, UUID id) {
        InvoiceEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(Optional.ofNullable(entity.getInstitutionId()))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    private static void requireDraft(InvoiceEntity entity) {
        if (!"draft".equals(entity.getStatus())) {
            throw new InvalidStateException("Only a draft invoice changes this way; a posted invoice is corrected by "
                    + "a credit note.");
        }
    }

    private static void requireBodyEtag(InvoiceEntity entity, String etag) {
        if (etag != null && Etags.bodyVersion(etag).orElse(-1) != entity.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the invoice's current version.");
        }
    }

    private static void assertBilled(InvoiceEntity entity) {
        if (entity.getStudentId() == null && entity.getBillToUserId() == null) {
            throw new ValidationFailedException(List.of(new FieldError("studentId",
                    "name the student or the user billed", "REQUIRED")));
        }
    }

    private static String draftNumber(UUID id) {
        return DRAFT_PREFIX + id.toString().replace("-", "");
    }

    private static List<UUID> concat(List<UUID> first, List<UUID> second) {
        List<UUID> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    /** Applies the selected InvoiceUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(InvoiceEntity entity, InvoiceUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "billToName" -> entity.setBillToName(changes.getBillToName());
                case "invoiceDate" -> entity.setInvoiceDate(changes.getInvoiceDate());
                case "dueDate" -> entity.setDueDate(changes.getDueDate());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
