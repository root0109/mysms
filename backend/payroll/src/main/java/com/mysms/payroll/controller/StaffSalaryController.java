package com.mysms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.payroll.api.StaffSalaryApi;
import com.mysms.payroll.api.model.StaffSalary;
import com.mysms.payroll.api.model.StaffSalaryComponent;
import com.mysms.payroll.api.model.StaffSalaryComponentUpdate;
import com.mysms.payroll.api.model.StaffSalaryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.payroll.business.StaffSalaryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StaffSalary operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class StaffSalaryController implements StaffSalaryApi {

    private final StaffSalaryService staffSalaryService;

    public StaffSalaryController(StaffSalaryService staffSalaryService) {
        this.staffSalaryService = staffSalaryService;
    }

    @Override
    public ResponseEntity<StaffSalary> createStaffSalary(StaffSalary staffSalary, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StaffSalary created = staffSalaryService.createStaffSalary(staffSalary, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/payroll/staff-salaries/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<StaffSalaryComponent> createStaffSalaryComponent(UUID staffSalaryId,
            StaffSalaryComponent staffSalaryComponent, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StaffSalaryComponent created =
                staffSalaryService.createStaffSalaryComponent(staffSalaryId, staffSalaryComponent, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/payroll/staff-salaries/" + staffSalaryId + "/components/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStaffSalary(UUID staffSalaryId, String ifMatch) {
        staffSalaryService.deleteStaffSalary(staffSalaryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteStaffSalaryComponent(UUID staffSalaryId,
            UUID staffSalaryComponentId, String ifMatch) {
        staffSalaryService.deleteStaffSalaryComponent(staffSalaryId, staffSalaryComponentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StaffSalary> getStaffSalary(UUID staffSalaryId, String fields) {
        StaffSalary found = staffSalaryService.getStaffSalary(staffSalaryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<StaffSalaryComponent> getStaffSalaryComponent(UUID staffSalaryId,
            UUID staffSalaryComponentId, String fields) {
        StaffSalaryComponent found =
                staffSalaryService.getStaffSalaryComponent(staffSalaryId, staffSalaryComponentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StaffSalary>> listStaffSalaries(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StaffSalary> list =
                PagingUtil.pagingInfo(staffSalaryService.listStaffSalarys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<StaffSalaryComponent>> listStaffSalaryComponents(UUID staffSalaryId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<StaffSalaryComponent> list =
                PagingUtil.pagingInfo(staffSalaryService.listStaffSalaryComponents(staffSalaryId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StaffSalary> updateStaffSalary(UUID staffSalaryId, String ifMatch,
            StaffSalaryUpdate staffSalaryUpdate, String updateMask, Boolean validateOnly) {
        StaffSalary updated =
                staffSalaryService.updateStaffSalary(staffSalaryId, ifMatch, staffSalaryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<StaffSalaryComponent> updateStaffSalaryComponent(UUID staffSalaryId,
            UUID staffSalaryComponentId, String ifMatch, StaffSalaryComponentUpdate staffSalaryComponentUpdate,
            String updateMask, Boolean validateOnly) {
        StaffSalaryComponent updated =
                staffSalaryService.updateStaffSalaryComponent(staffSalaryId, staffSalaryComponentId, ifMatch,
                        staffSalaryComponentUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
