package com.mysms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.payroll.api.DisbursementAccountApi;
import com.mysms.payroll.api.model.DisbursementAccount;
import com.mysms.payroll.api.model.DisbursementAccountUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.payroll.business.DisbursementAccountService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the DisbursementAccount operations: implements the generated contract interface, so paths, parameters
 * and payloads
 * cannot drift from api/openapi/payroll.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class DisbursementAccountController implements DisbursementAccountApi {

    private final DisbursementAccountService disbursementAccountService;

    public DisbursementAccountController(DisbursementAccountService disbursementAccountService) {
        this.disbursementAccountService = disbursementAccountService;
    }

    @Override
    public ResponseEntity<DisbursementAccount> createDisbursementAccount(
            DisbursementAccount disbursementAccount, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        DisbursementAccount created = disbursementAccountService.createDisbursementAccount(disbursementAccount, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/payroll/disbursement-accounts/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteDisbursementAccount(UUID disbursementAccountId, String ifMatch) {
        disbursementAccountService.deleteDisbursementAccount(disbursementAccountId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DisbursementAccount> getDisbursementAccount(UUID disbursementAccountId,
            String fields) {
        DisbursementAccount found = disbursementAccountService.getDisbursementAccount(disbursementAccountId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<DisbursementAccount>> listDisbursementAccounts(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<DisbursementAccount> list =
                PagingUtil.pagingInfo(disbursementAccountService.listDisbursementAccounts(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<DisbursementAccount> updateDisbursementAccount(UUID disbursementAccountId,
            String ifMatch, DisbursementAccountUpdate disbursementAccountUpdate, String updateMask,
            Boolean validateOnly) {
        DisbursementAccount updated =
                disbursementAccountService.updateDisbursementAccount(disbursementAccountId, ifMatch,
                        disbursementAccountUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
