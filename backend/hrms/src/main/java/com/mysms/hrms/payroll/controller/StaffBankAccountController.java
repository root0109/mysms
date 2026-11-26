package com.mysms.hrms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hrms.payroll.api.StaffBankAccountApi;
import com.mysms.hrms.payroll.api.model.StaffBankAccount;
import com.mysms.hrms.payroll.api.model.StaffBankAccountUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.hrms.payroll.business.StaffBankAccountService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StaffBankAccount operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class StaffBankAccountController implements StaffBankAccountApi {

    private final StaffBankAccountService staffBankAccountService;

    public StaffBankAccountController(StaffBankAccountService staffBankAccountService) {
        this.staffBankAccountService = staffBankAccountService;
    }

    @Override
    public ResponseEntity<StaffBankAccount> createStaffBankAccount(StaffBankAccount staffBankAccount,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StaffBankAccount created = staffBankAccountService.createStaffBankAccount(staffBankAccount, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/payroll/staff-bank-accounts/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStaffBankAccount(UUID staffBankAccountId, String ifMatch) {
        staffBankAccountService.deleteStaffBankAccount(staffBankAccountId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StaffBankAccount> getStaffBankAccount(UUID staffBankAccountId, String fields) {
        StaffBankAccount found = staffBankAccountService.getStaffBankAccount(staffBankAccountId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StaffBankAccount>> listStaffBankAccounts(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StaffBankAccount> list =
                PagingUtil.pagingInfo(staffBankAccountService.listStaffBankAccounts(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StaffBankAccount> updateStaffBankAccount(UUID staffBankAccountId,
            String ifMatch, StaffBankAccountUpdate staffBankAccountUpdate, Boolean validateOnly) {
        StaffBankAccount updated =
                staffBankAccountService.updateStaffBankAccount(staffBankAccountId, ifMatch, staffBankAccountUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
