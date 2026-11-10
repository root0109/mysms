package com.mysms.finance.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
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
import com.mysms.finance.api.model.InvoiceLine;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.InvoiceLineEntity;
import com.mysms.finance.mapper.InvoiceLineMapper;
import com.mysms.finance.repository.InvoiceLineRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of the Invoice aggregate of finance (JAVA-CODING-RULES §4): the records that exist only as
 * part of an Invoice are served here, each with explicit methods; audit and domain event are written in the
 * transaction of every change, under the data-level rules of api/README "Authorization".
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of an Invoice
 * are served here too — InvoiceLine.
 */
@Service
public class InvoiceService extends AppTransactionManager {

    public static final String INVOICE_LINE_AGGREGATE = InvoiceLineEntity.TABLE;
    static final String INVOICE_LINE_PERMISSION_LIST = "finance.invoice_line.read";
    static final String INVOICE_LINE_PERMISSION_GET = "finance.invoice_line.read";

    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceLineMapper invoiceLineMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public InvoiceService(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            InvoiceLineRepository invoiceLineRepository,
            InvoiceLineMapper invoiceLineMapper,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock) {
        super(database, transactions);
        this.invoiceLineRepository = invoiceLineRepository;
        this.invoiceLineMapper = invoiceLineMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    // ---- InvoiceLine: a record of the Invoice aggregate ----

    public InvoiceLine getInvoiceLine(UUID invoiceId, UUID id) {
        return doInReadTransaction(() -> {
            return invoiceLineMapper.toDto(loadVisibleInvoiceLine(DataScope.current(INVOICE_LINE_PERMISSION_GET), invoiceId, id));
        });
    }

    public Page<InvoiceLine> listInvoiceLines(UUID invoiceId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, InvoiceLineRepository.SORT_FIELDS, InvoiceLineRepository.DEFAULT_ORDER,
                InvoiceLineRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, InvoiceLineRepository.FILTER_FIELDS)
                .and(InvoiceLineRepository.ofParent(invoiceId))
                        .and(InvoiceLineRepository.scope(DataScope.current(INVOICE_LINE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + invoiceId, clock);
        return doInReadTransaction(() -> {
            Page<InvoiceLineEntity> rows = invoiceLineRepository.list(page, where);
            return rows.map(invoiceLineMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private InvoiceLineEntity loadVisibleInvoiceLine(DataScope scope, UUID invoiceId, UUID id) {
        InvoiceLineEntity entity = invoiceLineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(INVOICE_LINE_AGGREGATE, id));
        if (!scope.canRead(institutionOfInvoiceLine(entity)) || !invoiceId.equals(entity.getInvoiceId())) {
            throw new ResourceNotFoundException(INVOICE_LINE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the finance.invoice it belongs to). */
    private Optional<UUID> institutionOfInvoiceLine(InvoiceLineEntity entity) {
        return invoiceLineRepository.institutionOf(entity.getInvoiceId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidInvoiceLine(InvoiceLineEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInvoiceId() == null) {
            errors.add(new FieldError("invoiceId", "must not be null", "NotNull"));
        }
        if (entity.getDescription() == null) {
            errors.add(new FieldError("description", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("quantity", WireValues.decimal(entity.getQuantity()), 10, 2, errors);
        if (entity.getUnitPrice() == null) {
            errors.add(new FieldError("unitPrice", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("unitPrice", WireValues.decimal(entity.getUnitPrice()), 14, 2, errors);
        WireValues.checkDecimal("discountAmount", WireValues.decimal(entity.getDiscountAmount()), 14, 2, errors);
        WireValues.checkDecimal("taxAmount", WireValues.decimal(entity.getTaxAmount()), 14, 2, errors);
        if (entity.getLineTotal() == null) {
            errors.add(new FieldError("lineTotal", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("lineTotal", WireValues.decimal(entity.getLineTotal()), 14, 2, errors);
        if (entity.getFeeHeadId() != null && !invoiceLineRepository.existsFeeHeadId(entity.getFeeHeadId())) {
            errors.add(new FieldError("feeHeadId", "fee head does not exist", "NOT_FOUND"));
        }
        if (entity.getInvoiceId() != null && !invoiceLineRepository.existsInvoiceId(entity.getInvoiceId())) {
            errors.add(new FieldError("invoiceId", "invoice does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueInvoiceLine(InvoiceLineEntity entity) {

    }
}
