package com.mysms.payroll.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
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
import com.mysms.payroll.api.model.CancelPayslipRequest;
import com.mysms.payroll.api.model.Payslip;
import com.mysms.payroll.api.model.PayslipLine;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.config.PayrollEventsConfig;
import com.mysms.payroll.domain.PayslipEntity;
import com.mysms.payroll.domain.PayslipLineEntity;
import com.mysms.payroll.events.PayrollEvents;
import com.mysms.payroll.mapper.PayslipLineMapper;
import com.mysms.payroll.mapper.PayslipMapper;
import com.mysms.payroll.repository.PayslipLineRepository;
import com.mysms.payroll.repository.PayslipRepository;
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
 * Business service of payroll.payslip (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Payslip
 * are served here too — PayslipLine.
 */
@Service
public class PayslipService extends AppTransactionManager {

    public static final String AGGREGATE = PayslipEntity.TABLE;
    static final String PERMISSION_LIST = "payroll.payslip.read";
    static final String PERMISSION_GET = "payroll.payslip.read";
    static final String PERMISSION_CANCEL_PAYSLIP = "payroll.payslip.cancel_payslip";

    public static final String PAYSLIP_LINE_AGGREGATE = PayslipLineEntity.TABLE;
    static final String PAYSLIP_LINE_PERMISSION_LIST = "payroll.payslip_line.read";
    static final String PAYSLIP_LINE_PERMISSION_GET = "payroll.payslip_line.read";

    private final PayslipRepository repository;
    private final PayslipMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final PayslipLineRepository payslipLineRepository;
    private final PayslipLineMapper payslipLineMapper;

    public PayslipService(PayslipRepository repository,
            PayslipMapper mapper,
            @Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            PayslipLineRepository payslipLineRepository,
            PayslipLineMapper payslipLineMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.payslipLineRepository = payslipLineRepository;
        this.payslipLineMapper = payslipLineMapper;
    }

    public Payslip getPayslip(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Payslip> listPayslips(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PayslipRepository.SORT_FIELDS, PayslipRepository.DEFAULT_ORDER,
                PayslipRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PayslipRepository.FILTER_FIELDS)
                .and(PayslipRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PayslipEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * payroll.CancelPayslip: Cancel. The row must be visible and writable for the caller, and the step is allowed while
     * status is unpaid; in any other state
     * the answer is 409 and nothing changes.
     */
    public Payslip cancelPayslip(UUID id, CancelPayslipRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_PAYSLIP);
        return doInWriteTransaction(() -> {
            PayslipEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("unpaid").contains(entity.getStatus()))) {
                throw new InvalidStateException("The payslip cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelPayslip(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, PayrollEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PayslipEntity loadVisible(DataScope scope, UUID id) {
        PayslipEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(PayslipEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PayslipEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getPayslipNumber() == null) {
            errors.add(new FieldError("payslipNumber", "must not be null", "NotNull"));
        }
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getStaffSalaryId() == null) {
            errors.add(new FieldError("staffSalaryId", "must not be null", "NotNull"));
        }
        if (entity.getPeriodStart() == null) {
            errors.add(new FieldError("periodStart", "must not be null", "NotNull"));
        }
        if (entity.getPeriodEnd() == null) {
            errors.add(new FieldError("periodEnd", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("payPercentage", WireValues.decimal(entity.getPayPercentage()), 5, 2, errors);
        WireValues.checkDecimal("leaveDays", WireValues.decimal(entity.getLeaveDays()), 5, 1, errors);
        WireValues.checkDecimal("leaveDeduction", WireValues.decimal(entity.getLeaveDeduction()), 14, 2, errors);
        if (entity.getStaffSalaryId() != null && !repository.existsStaffSalaryId(entity.getStaffSalaryId())) {
            errors.add(new FieldError("staffSalaryId", "staff salary does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PayslipEntity entity) {
        if (repository.takenInstitutionIdPayslipNumber1(entity)) {
            throw new DuplicateResourceException(
                    "A payslip with the same institution id and payslip number already exists.");
        }
    }

    // ---- PayslipLine: a record of the Payslip aggregate ----

    public PayslipLine getPayslipLine(UUID payslipId, UUID id) {
        return doInReadTransaction(() -> {
            return payslipLineMapper.toDto(loadVisiblePayslipLine(DataScope.current(PAYSLIP_LINE_PERMISSION_GET), payslipId, id));
        });
    }

    public Page<PayslipLine> listPayslipLines(UUID payslipId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PayslipLineRepository.SORT_FIELDS, PayslipLineRepository.DEFAULT_ORDER,
                PayslipLineRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PayslipLineRepository.FILTER_FIELDS)
                .and(PayslipLineRepository.ofParent(payslipId))
                        .and(PayslipLineRepository.scope(DataScope.current(PAYSLIP_LINE_PERMISSION_LIST)));
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

    /** The institution that owns the row (the institution of the payroll.payslip it belongs to). */
    private Optional<UUID> institutionOfPayslipLine(PayslipLineEntity entity) {
        return payslipLineRepository.institutionOf(entity.getPayslipId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPayslipLine(PayslipLineEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPayslipId() == null) {
            errors.add(new FieldError("payslipId", "must not be null", "NotNull"));
        }
        if (entity.getStaffSalaryComponentId() == null) {
            errors.add(new FieldError("staffSalaryComponentId", "must not be null", "NotNull"));
        }
        if (entity.getAmount() == null) {
            errors.add(new FieldError("amount", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("amount", WireValues.decimal(entity.getAmount()), 14, 2, errors);
        if (entity.getPayslipId() != null && !payslipLineRepository.existsPayslipId(entity.getPayslipId())) {
            errors.add(new FieldError("payslipId", "payslip does not exist", "NOT_FOUND"));
        }
        if (entity.getStaffSalaryComponentId() != null
                && !payslipLineRepository.existsStaffSalaryComponentId(entity.getStaffSalaryComponentId())) {
            errors.add(new FieldError("staffSalaryComponentId", "staff salary component does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePayslipLine(PayslipLineEntity entity) {
        if (payslipLineRepository.takenPayslipIdStaffSalaryComponentId1(entity)) {
            throw new DuplicateResourceException(
                    "A payslip line with the same payslip id and staff salary component id already exists.");
        }
    }
}
