package com.mysms.campus.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.campus.library.api.LoanApi;
import com.mysms.campus.library.api.model.Invoice;
import com.mysms.campus.library.api.model.Loan;
import com.mysms.campus.library.api.model.LoanUpdate;
import com.mysms.campus.library.api.model.MarkLoanLostRequest;
import com.mysms.campus.library.api.model.RequestLoanReturnRequest;
import com.mysms.campus.library.api.model.ReturnLoanRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.campus.library.business.LoanService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Loan operations; authentication and the permission check already ran in the auth-client filter.
 */
@RestController
public class LoanController implements LoanApi {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Override
    public ResponseEntity<Loan> createLoan(Loan loan, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Loan created = loanService.createLoan(loan, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/loans/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLoan(UUID loanId, String ifMatch) {
        loanService.deleteLoan(loanId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Loan> getLoan(UUID loanId, String fields) {
        Loan found = loanService.getLoan(loanId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Invoice> invoiceLoanFine(UUID loanId, Object body, UUID idempotencyKey) {
        Invoice invoice = loanService.invoiceLoanFine(loanId);
        return ResponseEntity.ok().eTag(invoice.getEtag()).body(invoice);
    }

    @Override
    public ResponseEntity<PagingInfo<Loan>> listLoans(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Loan> list = PagingUtil.pagingInfo(loanService.listLoans(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Loan> markLoanLost(UUID loanId, MarkLoanLostRequest markLoanLostRequest,
            UUID idempotencyKey) {
        Loan changed = loanService.markLoanLost(loanId, markLoanLostRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Loan> requestLoanReturn(UUID loanId,
            RequestLoanReturnRequest requestLoanReturnRequest, UUID idempotencyKey) {
        Loan changed = loanService.requestLoanReturn(loanId, requestLoanReturnRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Loan> returnLoan(UUID loanId, ReturnLoanRequest returnLoanRequest,
            UUID idempotencyKey) {
        Loan changed = loanService.returnLoan(loanId, returnLoanRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Loan> updateLoan(UUID loanId, String ifMatch, LoanUpdate loanUpdate,
            Boolean validateOnly) {
        Loan updated = loanService.updateLoan(loanId, ifMatch, loanUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
