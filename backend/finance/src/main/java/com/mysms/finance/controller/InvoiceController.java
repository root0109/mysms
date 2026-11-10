package com.mysms.finance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.business.InvoiceService;
import com.mysms.finance.api.InvoiceApi;
import com.mysms.finance.api.model.CancelInvoiceRequest;
import com.mysms.finance.api.model.CreateInvoiceFromDuesRequest;
import com.mysms.finance.api.model.FeeStatement;
import com.mysms.finance.api.model.Invoice;
import com.mysms.finance.api.model.InvoiceLine;
import com.mysms.finance.api.model.InvoiceUpdate;
import com.mysms.finance.api.model.PostInvoiceRequest;
import com.mysms.finance.api.model.SendFeeRemindersRequest;
import com.mysms.finance.api.model.SendResult;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Invoice operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/finance.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class InvoiceController implements InvoiceApi {


    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Override
    public ResponseEntity<Invoice> cancelInvoice(UUID invoiceId,
            CancelInvoiceRequest cancelInvoiceRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Invoice> createInvoice(Invoice invoice, UUID idempotencyKey,
            Boolean validateOnly) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Invoice> createInvoiceFromDues(UUID idempotencyKey,
            CreateInvoiceFromDuesRequest createInvoiceFromDuesRequest) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<FeeStatement> getFeeStatement(UUID studentId, UUID academicYearId) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Invoice> getInvoice(UUID invoiceId, String fields) {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Invoice> postInvoice(UUID invoiceId, PostInvoiceRequest postInvoiceRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<SendResult> sendFeeReminders(SendFeeRemindersRequest sendFeeRemindersRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Invoice> updateInvoice(UUID invoiceId, String ifMatch,
            InvoiceUpdate invoiceUpdate, String updateMask, Boolean validateOnly) {
        throw new NotImplementedException();
    }
}
