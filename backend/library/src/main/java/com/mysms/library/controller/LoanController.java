package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.LoanApi;
import com.mysms.library.api.model.Invoice;
import com.mysms.library.api.model.Loan;
import com.mysms.library.api.model.LoanUpdate;
import com.mysms.library.api.model.MarkLoanLostRequest;
import com.mysms.library.api.model.RequestLoanReturnRequest;
import com.mysms.library.api.model.ReturnLoanRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.LoanService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Loan operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/library.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
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
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Loan> requestLoanReturn(UUID loanId,
            RequestLoanReturnRequest requestLoanReturnRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Loan> returnLoan(UUID loanId, ReturnLoanRequest returnLoanRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Loan> updateLoan(UUID loanId, String ifMatch, LoanUpdate loanUpdate,
            String updateMask, Boolean validateOnly) {
        Loan updated = loanService.updateLoan(loanId, ifMatch, loanUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
