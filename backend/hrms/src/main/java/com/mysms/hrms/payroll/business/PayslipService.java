package com.mysms.hrms.payroll.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
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
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.hrms.payroll.api.model.CancelPayslipRequest;
import com.mysms.hrms.payroll.api.model.GeneratePayslipsRequest;
import com.mysms.hrms.payroll.api.model.MarkPayslipPaidRequest;
import com.mysms.hrms.payroll.api.model.Operation;
import com.mysms.hrms.payroll.api.model.Payslip;
import com.mysms.hrms.payroll.api.model.PayslipLine;
import com.mysms.hrms.payroll.business.PayslipCalculator.Component;
import com.mysms.hrms.payroll.business.PayslipCalculator.Line;
import com.mysms.hrms.payroll.business.PayslipCalculator.Result;
import com.mysms.hrms.payroll.business.PayslipCalculator.Salary;
import com.mysms.hrms.payroll.business.PayslipCalculator.Segment;
import com.mysms.hrms.payroll.config.PayrollDatabaseConfig;
import com.mysms.hrms.payroll.config.PayrollEventsConfig;
import com.mysms.hrms.payroll.domain.PayrollPaymentEntity;
import com.mysms.hrms.payroll.domain.PayslipEntity;
import com.mysms.hrms.payroll.domain.PayslipLineEntity;
import com.mysms.hrms.payroll.events.PayrollEvents;
import com.mysms.hrms.payroll.mapper.PayslipLineMapper;
import com.mysms.hrms.payroll.mapper.PayslipMapper;
import com.mysms.hrms.payroll.repository.PayrollPaymentRepository;
import com.mysms.hrms.payroll.repository.PayslipLineRepository;
import com.mysms.hrms.payroll.repository.PayslipRepository;
import com.mysms.hrms.payroll.repository.PayslipRunRepository;
import com.mysms.hrms.payroll.repository.PayslipRunRepository.Period;
import com.mysms.hrms.payroll.repository.PayslipRunRepository.SalaryRow;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of payroll.payslip and its PayslipLine records: audit and domain event commit in the same
 * transaction as every change; rows outside the caller's institutions are invisible. Payslips and their lines are
 * written only by the workflow steps {@code GeneratePayslips} (amounts from {@link PayslipCalculator}), {@code
 * MarkPayslipPaid} and {@code CancelPayslip}; a paid payslip no longer changes.
 */
@Service
public class PayslipService extends AppTransactionManager {

    public static final String AGGREGATE = PayslipEntity.TABLE;
    static final String PERMISSION_LIST = "payroll.payslip.read";
    static final String PERMISSION_GET = "payroll.payslip.read";
    static final String PERMISSION_CANCEL_PAYSLIP = "payroll.payslip.cancel_payslip";
    static final String PERMISSION_GENERATE_PAYSLIPS = "payroll.payslip.generate_payslips";
    static final String PERMISSION_MARK_PAYSLIP_PAID = "payroll.payslip.mark_payslip_paid";
    /** The operation kind of a payslip run. */
    static final String KIND_GENERATE = "payroll.GeneratePayslips";
    /** The most staff members one run may name (422 beyond); a run that names none pays everyone. */
    static final int MAX_STAFF_SELECTION = 1000;
    /** The staff members named with a reason in a run's outcome; the count beside the list is always complete. */
    static final int SKIPPED_LISTED = 200;
    /** The longest payment reference the payment row keeps (422 beyond). */
    static final int MAX_REFERENCE = 80;
    private static final String PAYSLIP_PREFIX = "PS-";
    private static final String PAYMENT_PREFIX = "PP-";
    private static final BigDecimal FULL_PAY = new BigDecimal("100.00");
    private static final String UNPAID = "unpaid";
    /** The reach of a caller who is no payroll officer anywhere. */
    private static final DataScope NO_REACH = new DataScope(false, Set.of(), false);
    private static final Logger LOG = LoggerFactory.getLogger(PayslipService.class);

    public static final String PAYSLIP_LINE_AGGREGATE = PayslipLineEntity.TABLE;
    static final String PAYSLIP_LINE_PERMISSION_LIST = "payroll.payslip_line.read";
    static final String PAYSLIP_LINE_PERMISSION_GET = "payroll.payslip_line.read";

    /** What a run did for one staff member. */
    enum Outcome {
        GENERATED, KEPT, NO_SALARY, NO_COMPONENTS, NEGATIVE_NET, OVERLAPPING_PERIOD
    }

    private final PayslipRepository repository;
    private final PayslipMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final PayslipLineRepository payslipLineRepository;
    private final PayslipLineMapper payslipLineMapper;
    private final PayslipRunRepository runRepository;
    private final PayrollPaymentRepository paymentRepository;
    private final OperationService operations;

    public PayslipService(PayslipRepository repository,
            PayslipMapper mapper,
            @Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            PayslipLineRepository payslipLineRepository,
            PayslipLineMapper payslipLineMapper,
            PayslipRunRepository runRepository,
            PayrollPaymentRepository paymentRepository,
            OperationService operations) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.payslipLineRepository = payslipLineRepository;
        this.payslipLineMapper = payslipLineMapper;
        this.runRepository = runRepository;
        this.paymentRepository = paymentRepository;
        this.operations = operations;
    }

    /** payroll.GetPayslip: a payslip in the caller's scope that the owner-only rule lets them read (404 otherwise). */
    public Payslip getPayslip(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_GET);
        DataScope officer = officerScope();
        UUID reader = AuthContext.require().userId();
        return doInPrimaryReadTransaction(() -> {
            PayslipEntity entity = loadVisible(scope, id);
            if (!readable(entity, officer, reader)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            return mapper.toDto(entity);
        });
    }

    public Page<Payslip> listPayslips(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PayslipRepository.SORT_FIELDS, PayslipRepository.DEFAULT_ORDER,
                PayslipRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PayslipRepository.FILTER_FIELDS)
                .and(PayslipRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(PayslipRepository.readableBy(officerScope(), AuthContext.require().userId()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PayslipEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * payroll.CancelPayslip: the row must be visible and writable for the caller, and the step is allowed while
     * status is unpaid; in any other state the answer is 409 and nothing changes.
     */
    public Payslip cancelPayslip(UUID id, CancelPayslipRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_PAYSLIP);
        return doInWriteTransaction(() -> {
            PayslipEntity entity = loadForStep(scope, id, request == null ? null : request.getEtag());
            if (repository.cancelPayslip(id, entity.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, PayrollEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * payroll.MarkPayslipPaid: records the payment of an unpaid payslip and marks it paid, in one transaction. The
     * row must be visible and writable for the caller and unpaid (409 otherwise); the disbursement account must be
     * one of the payslip's institution (422). The payment is a payroll payment of the payslip's net amount to its
     * staff member, numbered from the institution's series; a payslip with nothing to pay (net 0) is marked paid
     * without one. A paid payslip no longer changes.
     */
    public Payslip markPayslipPaid(UUID id, MarkPayslipPaidRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_MARK_PAYSLIP_PAID);
        String given = request.getReferenceNumber();
        String reference = given == null || given.isBlank() ? null : given.strip();
        if (reference != null && reference.length() > MAX_REFERENCE) {
            throw new ValidationFailedException(List.of(new FieldError("referenceNumber",
                    "at most " + MAX_REFERENCE + " characters", "Size")));
        }
        return doInWriteTransaction(() -> {
            PayslipEntity entity = loadForStep(scope, id, request.getEtag());
            if (runRepository.institutionOfDisbursementAccount(request.getDisbursementAccountId())
                    .filter(entity.getInstitutionId()::equals).isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("disbursementAccountId",
                        "disbursement account does not exist", "NOT_FOUND")));
            }
            if (runRepository.markPayslipPaid(id, entity.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            if (entity.getNetAmount() != null && entity.getNetAmount().signum() > 0) {
                recordPayment(entity, request, reference);
            }
            audit.recordTransition(AGGREGATE, id, "markPaid", List.of("status"), null);
            outbox.publish(AGGREGATE + ".markPaid", AGGREGATE, id, PayrollEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * The row of a step (cancel, mark paid): visible to the caller (404), writable (403), at the version the body
     * names (412) and unpaid (409).
     */
    private PayslipEntity loadForStep(DataScope scope, UUID id, String etag) {
        PayslipEntity entity = loadVisible(scope, id);
        scope.requireWritable(institutionOf(entity));
        OptionalInt expected = Etags.bodyVersion(etag);
        if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
        if (!UNPAID.equals(entity.getStatus())) {
            throw new InvalidStateException("The payslip cannot be changed this way in its current state.");
        }
        return entity;
    }

    /** The payroll payment that settles the payslip: its net amount, from the account and by the method given. */
    private void recordPayment(PayslipEntity payslip, MarkPayslipPaidRequest request, String reference) {
        PayrollPaymentEntity payment = PayrollPaymentEntity.builder()
                .institutionId(payslip.getInstitutionId())
                .disbursementAccountId(request.getDisbursementAccountId())
                .staffId(payslip.getStaffId())
                .payslipId(payslip.getId())
                .transactionDate(request.getTransactionDate())
                .amount(payslip.getNetAmount())
                .paymentMethod(request.getPaymentMethod().getValue())
                .referenceNumber(reference)
                .build();
        payment.onCreatingData(Actor.current(), clock);
        payment.setTenantId(payslip.getTenantId());
        payment.setTransactionNumber(runRepository.nextNumber(payslip.getInstitutionId(),
                PayslipRunRepository.PAYMENT_NUMBERS, PAYMENT_PREFIX, payment.getCreatedAt(), payment.getCreatedBy()));
        PayrollPaymentEntity stored = paymentRepository.insert(payment);
        audit.recordCreate(PayrollPaymentService.AGGREGATE, stored.getId());
        outbox.publish(PayrollPaymentService.AGGREGATE + PayrollEvents.CREATED, PayrollPaymentService.AGGREGATE,
                stored.getId(), PayrollEvents.payload(stored.getId()));
    }

    // ---- GeneratePayslips: the payslip run ----

    /**
     * payroll.GeneratePayslips: one payslip per staff member (or per named one) whose salary in force on the period's
     * last day belongs to the institution through its template, answered as the operation that records the run. The
     * caller must be able to write the institution (403); the period must lie within one calendar month (422). <p>Each
     * payslip and its lines are written in a transaction of their own, so one staff member who cannot be paid never
     * holds back the others. A run is safe to repeat and to race: an existing payslip that counts for the period is
     * kept (cancel it to recalculate), a payslip of an overlapping period stops a second payment, and the salary rows
     * are locked while a payslip is written, backed by {@code uq_payslip_period}. <p>The operation is recorded first,
     * so it can be watched and cancelled from another session; the answer is the finished operation with its counts and
     * the skipped staff.
     */
    public Operation generatePayslips(GeneratePayslipsRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_GENERATE_PAYSLIPS);
        List<FieldError> errors = new ArrayList<>();
        if (request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            errors.add(new FieldError("periodEnd", "must not be before periodStart", "Range"));
        } else if (!YearMonth.from(request.getPeriodStart()).equals(YearMonth.from(request.getPeriodEnd()))) {
            errors.add(new FieldError("periodEnd", "a pay period lies within one calendar month", "Range"));
        }
        List<UUID> named = request.getStaffIds() == null ? List.of() : request.getStaffIds();
        if (named.size() > MAX_STAFF_SELECTION) {
            errors.add(new FieldError("staffIds", "at most " + MAX_STAFF_SELECTION + " staff members", "Size"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        scope.requireWritable(Optional.of(request.getInstitutionId()));
        return run(request.getInstitutionId(), request.getPeriodStart(), request.getPeriodEnd(),
                named.isEmpty() ? null : new LinkedHashSet<>(named));
    }

    private Operation run(UUID institutionId, LocalDate periodStart, LocalDate periodEnd, Set<UUID> staffIds) {
        UUID operationId = operations.start(KIND_GENERATE, "org.institution", institutionId);
        try {
            // on the primary: a salary assigned a moment ago is paid by this run
            List<UUID> staff = doInPrimaryReadTransaction(
                    () -> runRepository.staffPaidBy(institutionId, periodEnd, staffIds));
            Map<UUID, Outcome> outcomes = new LinkedHashMap<>();
            if (staffIds != null) {
                Set<UUID> paid = Set.copyOf(staff);
                staffIds.stream().filter(id -> !paid.contains(id)).sorted()
                        .forEach(id -> outcomes.put(id, Outcome.NO_SALARY));
            }
            int reported = 0;
            for (int done = 0; done < staff.size(); done++) {
                UUID staffId = staff.get(done);
                outcomes.put(staffId, generate(institutionId, periodStart, periodEnd, staffId));
                int percent = (done + 1) * 100 / staff.size();
                if (percent >= reported + 10 && percent < 100) {
                    reported = percent;
                    if (!operations.progress(operationId, percent)) {
                        break;
                    }
                }
            }
            return operations.succeed(operationId, summary(institutionId, periodStart, periodEnd, outcomes));
        } catch (RuntimeException e) {
            LOG.error("payslip generation stopped ({})", e.getClass().getSimpleName());
            operations.fail(operationId, "INTERNAL", "Payslip generation stopped before it finished. The payslips "
                    + "already generated are kept; run it again for the rest.");
            throw e;
        }
    }

    /** One staff member's payslip, whole or not at all; a payslip another run wrote at the same moment is kept. */
    private Outcome generate(UUID institutionId, LocalDate periodStart, LocalDate periodEnd, UUID staffId) {
        try {
            return doInWriteTransaction(() -> writePayslip(institutionId, periodStart, periodEnd, staffId));
        } catch (DuplicateKeyException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_payslip_period")) {
                return Outcome.KEPT;
            }
            throw e;
        }
    }

    private Outcome writePayslip(UUID institutionId, LocalDate periodStart, LocalDate periodEnd, UUID staffId) {
        List<SalaryRow> salaries = runRepository.lockSalaries(staffId, periodEnd);
        if (salaries.isEmpty() || !institutionId.equals(salaries.get(salaries.size() - 1).institutionId())) {
            // the salary was removed or moved to another institution since the run listed its staff
            return Outcome.NO_SALARY;
        }
        List<Period> inTheWay = runRepository.periodsInTheWay(staffId, periodStart, periodEnd);
        if (inTheWay.contains(new Period(periodStart, periodEnd))) {
            return Outcome.KEPT;
        }
        if (!inTheWay.isEmpty()) {
            return Outcome.OVERLAPPING_PERIOD;
        }
        // the days under a salary of another institution (a transfer within the month) are that institution's to pay
        Set<UUID> ours = salaries.stream().filter(salary -> institutionId.equals(salary.institutionId()))
                .map(SalaryRow::id).collect(Collectors.toSet());
        List<Segment> segments = PayslipCalculator.segments(periodStart, periodEnd,
                salaries.stream().map(salary -> new Salary(salary.id(), salary.effectiveDate())).toList())
                .stream().filter(segment -> ours.contains(segment.staffSalaryId())).toList();
        List<Component> components = runRepository.components(segments.stream().map(Segment::staffSalaryId).toList())
                .stream().map(row -> new Component(row.id(), row.staffSalaryId(), row.code(), row.name(), row.type(),
                        row.amount()))
                .toList();
        Result result = PayslipCalculator.calculate(periodStart, segments, components);
        if (result.lines().isEmpty()) {
            return Outcome.NO_COMPONENTS;
        }
        if (result.net().signum() < 0) {
            return Outcome.NEGATIVE_NET;
        }
        PayslipEntity payslip = PayslipEntity.builder()
                .institutionId(institutionId)
                .staffId(staffId)
                .staffSalaryId(salaries.get(salaries.size() - 1).id())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .payPercentage(FULL_PAY)
                .leaveDays(BigDecimal.ZERO)
                .leaveDeduction(BigDecimal.ZERO)
                .grossAmount(result.gross())
                .deductionTotal(result.deductions())
                .netAmount(result.net())
                .build();
        payslip.onCreatingData(Actor.current(), clock);
        payslip.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        payslip.setPayslipNumber(runRepository.nextNumber(institutionId, PayslipRunRepository.PAYSLIP_NUMBERS,
                PAYSLIP_PREFIX, payslip.getCreatedAt(), payslip.getCreatedBy()));
        runRepository.insertPayslip(payslip);
        runRepository.insertLines(payslip, lineRows(payslip, result.lines()));
        audit.recordCreate(AGGREGATE, payslip.getId());
        outbox.publish(AGGREGATE + PayrollEvents.CREATED, AGGREGATE, payslip.getId(),
                PayrollEvents.payload(payslip.getId()));
        return Outcome.GENERATED;
    }

    /**
     * The line rows in the calculator's order. Their ids are handed out in ascending order: every line carries the
     * payslip's creation time, so the lines' default order (creation time, then id) is the order they were computed.
     */
    private List<PayslipLineEntity> lineRows(PayslipEntity payslip, List<Line> lines) {
        List<UUID> ids = lines.stream().map(line -> UuidV7.next(clock)).sorted().toList();
        List<PayslipLineEntity> entities = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            PayslipLineEntity entity = PayslipLineEntity.builder()
                    .payslipId(payslip.getId())
                    .staffSalaryComponentId(line.staffSalaryComponentId())
                    .componentCode(line.code())
                    .componentName(line.name())
                    .componentType(line.type())
                    .amount(line.amount())
                    .build();
            entity.onCreatingData(Actor.current(), clock);
            entity.setId(ids.get(i));
            entities.add(entity);
        }
        return entities;
    }

    /** The outcome the operation keeps: what the run was for, the counts, and who was skipped and why. */
    private static Map<String, Object> summary(UUID institutionId, LocalDate periodStart, LocalDate periodEnd,
            Map<UUID, Outcome> outcomes) {
        List<Map<String, String>> skipped = new ArrayList<>();
        int generated = 0;
        int kept = 0;
        int skippedCount = 0;
        for (Map.Entry<UUID, Outcome> entry : outcomes.entrySet()) {
            switch (entry.getValue()) {
                case GENERATED -> generated++;
                case KEPT -> kept++;
                default -> {
                    skippedCount++;
                    if (skipped.size() < SKIPPED_LISTED) {
                        skipped.add(Map.of("staffId", entry.getKey().toString(), "reason", entry.getValue().name()));
                    }
                }
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("institutionId", institutionId.toString());
        summary.put("periodStart", periodStart.toString());
        summary.put("periodEnd", periodEnd.toString());
        summary.put("generated", generated);
        summary.put("kept", kept);
        summary.put("skippedCount", skippedCount);
        summary.put("skipped", skipped);
        return summary;
    }

    /**
     * Where the caller is a payroll officer: the institutions where they hold payroll.payslip.generate_payslips (the
     * permission of the payroll office), none without it.
     */
    private static DataScope officerScope() {
        AuthContext caller = AuthContext.require();
        return caller.hasPermission(PERMISSION_GENERATE_PAYSLIPS)
                ? DataScope.of(caller, PERMISSION_GENERATE_PAYSLIPS) : NO_REACH;
    }

    /**
     * The owner-only rule: outside the institutions where the reader is a payroll officer, only the payslips of the
     * reader's own staff records are readable; the reads answer 404 for any other.
     */
    private boolean readable(PayslipEntity payslip, DataScope officer, UUID reader) {
        return officer.canRead(institutionOf(payslip)) || repository.isOwnStaffRecord(payslip.getStaffId(), reader);
    }

    /** The live row, if the caller may see it: a row outside the caller's scope is 404. */
    private PayslipEntity loadVisible(DataScope scope, UUID id) {
        PayslipEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private static Optional<UUID> institutionOf(PayslipEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    // ---- PayslipLine ----

    public PayslipLine getPayslipLine(UUID payslipId, UUID id) {
        DataScope scope = DataScope.current(PAYSLIP_LINE_PERMISSION_GET);
        DataScope officer = officerScope();
        UUID reader = AuthContext.require().userId();
        return doInPrimaryReadTransaction(() -> {
            PayslipLineEntity line = loadVisiblePayslipLine(scope, payslipId, id);
            if (!repository.findById(payslipId).filter(payslip -> readable(payslip, officer, reader)).isPresent()) {
                throw new ResourceNotFoundException(PAYSLIP_LINE_AGGREGATE, id);
            }
            return payslipLineMapper.toDto(line);
        });
    }

    public Page<PayslipLine> listPayslipLines(UUID payslipId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PayslipLineRepository.SORT_FIELDS, PayslipLineRepository.DEFAULT_ORDER,
                PayslipLineRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PayslipLineRepository.FILTER_FIELDS)
                .and(PayslipLineRepository.ofParent(payslipId))
                .and(PayslipLineRepository.scope(DataScope.current(PAYSLIP_LINE_PERMISSION_LIST)))
                .and(PayslipLineRepository.readableBy(officerScope(), AuthContext.require().userId()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + payslipId, clock);
        return doInReadTransaction(() -> {
            Page<PayslipLineEntity> rows = payslipLineRepository.list(page, where);
            return rows.map(payslipLineMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PayslipLineEntity loadVisiblePayslipLine(DataScope scope, UUID payslipId, UUID id) {
        PayslipLineEntity entity = payslipLineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PAYSLIP_LINE_AGGREGATE, id));
        if (!scope.canRead(institutionOfPayslipLine(entity)) || !payslipId.equals(entity.getPayslipId())) {
            throw new ResourceNotFoundException(PAYSLIP_LINE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the payroll.payslip the row belongs to. */
    private Optional<UUID> institutionOfPayslipLine(PayslipLineEntity entity) {
        return payslipLineRepository.institutionOf(entity.getPayslipId());
    }
}
