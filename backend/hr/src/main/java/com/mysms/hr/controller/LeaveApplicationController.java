package com.mysms.hr.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hr.api.LeaveApplicationApi;
import com.mysms.hr.api.model.ApproveLeaveRequest;
import com.mysms.hr.api.model.CancelLeaveRequest;
import com.mysms.hr.api.model.LeaveApplication;
import com.mysms.hr.api.model.LeaveApplicationUpdate;
import com.mysms.hr.api.model.RejectLeaveRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.hr.business.LeaveApplicationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LeaveApplication operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class LeaveApplicationController implements LeaveApplicationApi {

    private final LeaveApplicationService leaveApplicationService;

    public LeaveApplicationController(LeaveApplicationService leaveApplicationService) {
        this.leaveApplicationService = leaveApplicationService;
    }

    @Override
    public ResponseEntity<LeaveApplication> approveLeave(UUID leaveApplicationId,
            ApproveLeaveRequest approveLeaveRequest, UUID idempotencyKey) {
        LeaveApplication changed = leaveApplicationService.approveLeave(leaveApplicationId, approveLeaveRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LeaveApplication> cancelLeave(UUID leaveApplicationId,
            CancelLeaveRequest cancelLeaveRequest, UUID idempotencyKey) {
        LeaveApplication changed = leaveApplicationService.cancelLeave(leaveApplicationId, cancelLeaveRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LeaveApplication> createLeaveApplication(LeaveApplication leaveApplication,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LeaveApplication created = leaveApplicationService.createLeaveApplication(leaveApplication, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/hr/leave-applications/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLeaveApplication(UUID leaveApplicationId, String ifMatch) {
        leaveApplicationService.deleteLeaveApplication(leaveApplicationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LeaveApplication> getLeaveApplication(UUID leaveApplicationId, String fields) {
        LeaveApplication found = leaveApplicationService.getLeaveApplication(leaveApplicationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LeaveApplication>> listLeaveApplications(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LeaveApplication> list =
                PagingUtil.pagingInfo(leaveApplicationService.listLeaveApplications(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LeaveApplication> rejectLeave(UUID leaveApplicationId,
            RejectLeaveRequest rejectLeaveRequest, UUID idempotencyKey) {
        LeaveApplication changed = leaveApplicationService.rejectLeave(leaveApplicationId, rejectLeaveRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LeaveApplication> updateLeaveApplication(UUID leaveApplicationId,
            String ifMatch, LeaveApplicationUpdate leaveApplicationUpdate, String updateMask, Boolean validateOnly) {
        LeaveApplication updated =
                leaveApplicationService.updateLeaveApplication(leaveApplicationId, ifMatch, leaveApplicationUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
