package com.mysms.finance.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.api.InvoiceApi;
import com.mysms.finance.api.model.CancelInvoiceRequest;
import com.mysms.finance.api.model.CreateInvoiceFromDuesRequest;
import com.mysms.finance.api.model.FeeStatement;
import com.mysms.finance.api.model.Invoice;
import com.mysms.finance.api.model.InvoiceLine;
import com.mysms.finance.api.model.InvoiceUpdate;
import com.mysms.finance.api.model.PostInvoiceRequest;
import com.mysms.finance.api.model.RaiseChargeInvoiceRequest;
import com.mysms.finance.api.model.SendFeeRemindersRequest;
import com.mysms.finance.api.model.SendResult;
import com.mysms.finance.business.InvoiceService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Invoice operations. Authentication, the x-permission check and the Idempotency-Key already happened
 * in core's and auth-client's filters.
 */
@RestController
public class InvoiceController implements InvoiceApi {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Override
    public ResponseEntity<Invoice> cancelInvoice(UUID invoiceId, CancelInvoiceRequest cancelInvoiceRequest,
            UUID idempotencyKey) {
        Invoice changed = invoiceService.cancelInvoice(invoiceId, cancelInvoiceRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Invoice> createInvoice(Invoice invoice, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Invoice created = invoiceService.createInvoice(invoice, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/finance/invoices/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Invoice> raiseChargeInvoice(RaiseChargeInvoiceRequest raiseChargeInvoiceRequest,
            UUID idempotencyKey) {
        Invoice raised = invoiceService.raiseChargeInvoice(raiseChargeInvoiceRequest);
        return ResponseEntity.ok().eTag(raised.getEtag()).body(raised);
    }

    @Override
    public ResponseEntity<Invoice> createInvoiceFromDues(UUID idempotencyKey,
            CreateInvoiceFromDuesRequest createInvoiceFromDuesRequest) {
        Invoice created = invoiceService.createInvoiceFromDues(createInvoiceFromDuesRequest);
        return ResponseEntity.ok().eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<FeeStatement> getFeeStatement(UUID studentId, UUID academicYearId) {
        return ResponseEntity.ok(invoiceService.getFeeStatement(studentId, academicYearId));
    }

    @Override
    public ResponseEntity<Invoice> getInvoice(UUID invoiceId, String fields) {
        Invoice found = invoiceService.getInvoice(invoiceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<InvoiceLine> getInvoiceLine(UUID invoiceId, UUID invoiceLineId, String fields) {
        InvoiceLine found = invoiceService.getInvoiceLine(invoiceId, invoiceLineId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<InvoiceLine>> listInvoiceLines(UUID invoiceId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<InvoiceLine> list =
                PagingUtil.pagingInfo(invoiceService.listInvoiceLines(invoiceId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Invoice>> listInvoices(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Invoice> list =
                PagingUtil.pagingInfo(invoiceService.listInvoices(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Invoice> postInvoice(UUID invoiceId, PostInvoiceRequest postInvoiceRequest,
            UUID idempotencyKey) {
        Invoice changed = invoiceService.postInvoice(invoiceId, postInvoiceRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<SendResult> sendFeeReminders(SendFeeRemindersRequest sendFeeRemindersRequest,
            UUID idempotencyKey) {
        return ResponseEntity.ok(invoiceService.sendFeeReminders(sendFeeRemindersRequest));
    }

    @Override
    public ResponseEntity<Invoice> updateInvoice(UUID invoiceId, String ifMatch, InvoiceUpdate invoiceUpdate,
            Boolean validateOnly) {
        Invoice updated = invoiceService.updateInvoice(invoiceId, ifMatch, invoiceUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
