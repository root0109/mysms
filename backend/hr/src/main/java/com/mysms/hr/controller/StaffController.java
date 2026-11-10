package com.mysms.hr.controller;

import com.mysms.hr.api.model.LeaveBalanceSummary;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hr.api.StaffApi;
import com.mysms.hr.api.model.ChangeStaffStatusRequest;
import com.mysms.hr.api.model.CreateStaffPortalAccountRequest;
import com.mysms.hr.api.model.Staff;
import com.mysms.hr.api.model.StaffDepartment;
import com.mysms.hr.api.model.StaffDepartmentUpdate;
import com.mysms.hr.api.model.StaffSubject;
import com.mysms.hr.api.model.StaffSubjectUpdate;
import com.mysms.hr.api.model.StaffUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.hr.business.StaffService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Staff operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/hr.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
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
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<StaffSubject> createStaffSubject(UUID staffId, StaffSubject staffSubject,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StaffSubject created = staffService.createStaffSubject(staffId, staffSubject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/hr/staff/" + staffId + "/subjects/" + created.getId()))
                .eTag(created.getEtag()).body(created);
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
    public ResponseEntity<Void> deleteStaffSubject(UUID staffId, UUID staffSubjectId, String ifMatch) {
        staffService.deleteStaffSubject(staffId, staffSubjectId, ifMatch);
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
    public ResponseEntity<StaffSubject> getStaffSubject(UUID staffId, UUID staffSubjectId,
            String fields) {
        StaffSubject found = staffService.getStaffSubject(staffId, staffSubjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
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
    public ResponseEntity<PagingInfo<StaffSubject>> listStaffSubjects(UUID staffId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<StaffSubject> list =
                PagingUtil.pagingInfo(staffService.listStaffSubjects(staffId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Staff> updateStaff(UUID staffId, String ifMatch, StaffUpdate staffUpdate,
            String updateMask, Boolean validateOnly) {
        Staff updated = staffService.updateStaff(staffId, ifMatch, staffUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<StaffDepartment> updateStaffDepartment(UUID staffId, UUID staffDepartmentId,
            String ifMatch, StaffDepartmentUpdate staffDepartmentUpdate, String updateMask, Boolean validateOnly) {
        StaffDepartment updated =
                staffService.updateStaffDepartment(staffId, staffDepartmentId, ifMatch, staffDepartmentUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<StaffSubject> updateStaffSubject(UUID staffId, UUID staffSubjectId,
            String ifMatch, StaffSubjectUpdate staffSubjectUpdate, String updateMask, Boolean validateOnly) {
        StaffSubject updated =
                staffService.updateStaffSubject(staffId, staffSubjectId, ifMatch, staffSubjectUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
