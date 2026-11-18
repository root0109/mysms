package com.mysms.attendance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.attendance.business.StaffAttendanceService;
import com.mysms.attendance.api.StaffAttendanceApi;
import com.mysms.attendance.api.model.StaffAttendance;
import com.mysms.attendance.api.model.StaffAttendanceUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StaffAttendance operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class StaffAttendanceController implements StaffAttendanceApi {

    private final StaffAttendanceService staffAttendanceService;

    public StaffAttendanceController(StaffAttendanceService staffAttendanceService) {
        this.staffAttendanceService = staffAttendanceService;
    }

    @Override
    public ResponseEntity<StaffAttendance> createStaffAttendance(StaffAttendance staffAttendance,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StaffAttendance created = staffAttendanceService.createStaffAttendance(staffAttendance, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/attendance/staff-attendance-days/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<StaffAttendance> getStaffAttendance(UUID staffAttendanceId, String fields) {
        StaffAttendance found = staffAttendanceService.getStaffAttendance(staffAttendanceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StaffAttendance>> listStaffAttendances(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StaffAttendance> list =
                PagingUtil.pagingInfo(staffAttendanceService.listStaffAttendances(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StaffAttendance> updateStaffAttendance(UUID staffAttendanceId, String ifMatch,
            StaffAttendanceUpdate staffAttendanceUpdate, String updateMask, Boolean validateOnly) {
        StaffAttendance updated =
                staffAttendanceService.updateStaffAttendance(staffAttendanceId, ifMatch, staffAttendanceUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
