package com.mysms.hr.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hr.api.LeaveTypeApi;
import com.mysms.hr.api.model.LeaveType;
import com.mysms.hr.api.model.LeaveTypeUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.hr.business.LeaveTypeService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LeaveType operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class LeaveTypeController implements LeaveTypeApi {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @Override
    public ResponseEntity<LeaveType> createLeaveType(LeaveType leaveType, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LeaveType created = leaveTypeService.createLeaveType(leaveType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/hr/leave-types/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLeaveType(UUID leaveTypeId, String ifMatch) {
        leaveTypeService.deleteLeaveType(leaveTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LeaveType> getLeaveType(UUID leaveTypeId, String fields) {
        LeaveType found = leaveTypeService.getLeaveType(leaveTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LeaveType>> listLeaveTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LeaveType> list =
                PagingUtil.pagingInfo(leaveTypeService.listLeaveTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LeaveType> updateLeaveType(UUID leaveTypeId, String ifMatch,
            LeaveTypeUpdate leaveTypeUpdate, String updateMask, Boolean validateOnly) {
        LeaveType updated = leaveTypeService.updateLeaveType(leaveTypeId, ifMatch, leaveTypeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
