package com.mysms.hrms.hr.controller;

import com.mysms.hrms.hr.api.model.LeaveBalanceSummary;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hrms.hr.api.StaffApi;
import com.mysms.hrms.hr.api.model.ChangeStaffStatusRequest;
import com.mysms.hrms.hr.api.model.CreateStaffPortalAccountRequest;
import com.mysms.hrms.hr.api.model.Staff;
import com.mysms.hrms.hr.api.model.StaffDepartment;
import com.mysms.hrms.hr.api.model.StaffDepartmentUpdate;
import com.mysms.hrms.hr.api.model.StaffUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.hrms.hr.business.StaffService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Staff operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from the module's contract. Thin: delegate, then shape the HTTP response. Authentication and the
 * x-permission check already happened in the auth-client filter.
 */
@RestController
public class StaffController implements StaffApi {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @Override
    public ResponseEntity<Staff> changeStaffStatus(UUID staffId,
            ChangeStaffStatusRequest changeStaffStatusRequest, UUID idempotencyKey) {
        Staff changed = staffService.changeStaffStatus(staffId, changeStaffStatusRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Staff> createStaff(Staff staff, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Staff created = staffService.createStaff(staff, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/hr/staff/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<StaffDepartment> createStaffDepartment(UUID staffId,
            StaffDepartment staffDepartment, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StaffDepartment created = staffService.createStaffDepartment(staffId, staffDepartment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/hr/staff/" + staffId + "/departments/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Staff> createStaffPortalAccount(UUID staffId,
            CreateStaffPortalAccountRequest createStaffPortalAccountRequest, UUID idempotencyKey) {
        Staff changed = staffService.createStaffPortalAccount(staffId, createStaffPortalAccountRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> deleteStaff(UUID staffId, String ifMatch) {
        staffService.deleteStaff(staffId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteStaffDepartment(UUID staffId, UUID staffDepartmentId,
            String ifMatch) {
        staffService.deleteStaffDepartment(staffId, staffDepartmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Staff> getStaff(UUID staffId, String fields) {
        Staff found = staffService.getStaff(staffId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<StaffDepartment> getStaffDepartment(UUID staffId, UUID staffDepartmentId,
            String fields) {
        StaffDepartment found = staffService.getStaffDepartment(staffId, staffDepartmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LeaveBalanceSummary>> getStaffLeaveBalance(UUID staffId, UUID academicYearId,
            Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(
                staffService.getStaffLeaveBalance(staffId, academicYearId, page, size, cursor)));
    }

    @Override
    public ResponseEntity<PagingInfo<Staff>> listStaff(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Staff> list = PagingUtil.pagingInfo(staffService.listStaffs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<StaffDepartment>> listStaffDepartments(UUID staffId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<StaffDepartment> list =
                PagingUtil.pagingInfo(staffService.listStaffDepartments(staffId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Staff> updateStaff(UUID staffId, String ifMatch, StaffUpdate staffUpdate,
            Boolean validateOnly) {
        Staff updated = staffService.updateStaff(staffId, ifMatch, staffUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<StaffDepartment> updateStaffDepartment(UUID staffId, UUID staffDepartmentId,
            String ifMatch, StaffDepartmentUpdate staffDepartmentUpdate, Boolean validateOnly) {
        StaffDepartment updated =
                staffService.updateStaffDepartment(staffId, staffDepartmentId, ifMatch, staffDepartmentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
