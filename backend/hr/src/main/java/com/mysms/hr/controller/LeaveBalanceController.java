package com.mysms.hr.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hr.api.LeaveBalanceApi;
import com.mysms.hr.api.model.LeaveBalance;
import com.mysms.hr.api.model.LeaveBalanceUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.hr.business.LeaveBalanceService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LeaveBalance operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class LeaveBalanceController implements LeaveBalanceApi {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @Override
    public ResponseEntity<LeaveBalance> createLeaveBalance(LeaveBalance leaveBalance, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LeaveBalance created = leaveBalanceService.createLeaveBalance(leaveBalance, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/hr/leave-balances/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<LeaveBalance> getLeaveBalance(UUID leaveBalanceId, String fields) {
        LeaveBalance found = leaveBalanceService.getLeaveBalance(leaveBalanceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LeaveBalance>> listLeaveBalances(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LeaveBalance> list =
                PagingUtil.pagingInfo(leaveBalanceService.listLeaveBalances(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LeaveBalance> updateLeaveBalance(UUID leaveBalanceId, String ifMatch,
            LeaveBalanceUpdate leaveBalanceUpdate, String updateMask, Boolean validateOnly) {
        LeaveBalance updated =
                leaveBalanceService.updateLeaveBalance(leaveBalanceId, ifMatch, leaveBalanceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
