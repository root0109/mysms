package com.mysms.hrms.payroll.business;

import com.mysms.authclient.DataScope;
import com.mysms.hrms.payroll.api.model.PayrollPayment;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
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
import com.mysms.hrms.payroll.config.PayrollDatabaseConfig;
import com.mysms.hrms.payroll.config.PayrollEventsConfig;
import com.mysms.hrms.payroll.domain.PayrollPaymentEntity;
import com.mysms.hrms.payroll.events.PayrollEvents;
import com.mysms.hrms.payroll.mapper.PayrollPaymentMapper;
import com.mysms.hrms.payroll.repository.PayrollPaymentRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of payroll.payroll_payment: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class PayrollPaymentService extends AppTransactionManager {

    public static final String AGGREGATE = PayrollPaymentEntity.TABLE;
    static final String PERMISSION_LIST = "payroll.payroll_payment.read";
    static final String PERMISSION_GET = "payroll.payroll_payment.read";
    static final String PERMISSION_CREATE = "payroll.payroll_payment.create";


    private final PayrollPaymentRepository repository;
    private final PayrollPaymentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public PayrollPaymentService(PayrollPaymentRepository repository, PayrollPaymentMapper mapper,
            @Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public PayrollPayment getPayrollPayment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<PayrollPayment> listPayrollPayments(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PayrollPaymentRepository.SORT_FIELDS, PayrollPaymentRepository.DEFAULT_ORDER,
                PayrollPaymentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PayrollPaymentRepository.FILTER_FIELDS)
                .and(PayrollPaymentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PayrollPaymentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PayrollPayment createPayrollPayment(PayrollPayment request, boolean validateOnly) {
        validator.validate(request);
        PayrollPaymentEntity entity = mapper.toEntity(request);
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
            PayrollPaymentEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PayrollEvents.CREATED, AGGREGATE, stored.getId(),
                    PayrollEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PayrollPaymentEntity loadVisible(DataScope scope, UUID id) {
        PayrollPaymentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(PayrollPaymentEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PayrollPaymentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getTransactionNumber() == null) {
            errors.add(new FieldError("transactionNumber", "must not be null", "NotNull"));
        }
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getTransactionDate() == null) {
            errors.add(new FieldError("transactionDate", "must not be null", "NotNull"));
        }
        if (entity.getAmount() == null) {
            errors.add(new FieldError("amount", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("amount", WireValues.decimal(entity.getAmount()), 14, 2, errors);
        if (entity.getPaymentMethod() == null) {
            errors.add(new FieldError("paymentMethod", "must not be null", "NotNull"));
        }
        if (entity.getDisbursementAccountId() != null
                && !repository.existsDisbursementAccountId(entity.getDisbursementAccountId())) {
            errors.add(new FieldError("disbursementAccountId", "disbursement account does not exist", "NOT_FOUND"));
        }
        if (entity.getPaymentTypeId() != null && !repository.existsPaymentTypeId(entity.getPaymentTypeId())) {
            errors.add(new FieldError("paymentTypeId", "payment type does not exist", "NOT_FOUND"));
        }
        if (entity.getPayslipId() != null && !repository.existsPayslipId(entity.getPayslipId())) {
            errors.add(new FieldError("payslipId", "payslip does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PayrollPaymentEntity entity) {
        if (repository.takenInstitutionIdTransactionNumber1(entity)) {
            throw new DuplicateResourceException(
                    "A payroll payment with the same institution id and transaction number already exists.");
        }
    }
}
