package com.mysms.org.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.org.api.DepartmentApi;
import com.mysms.org.api.model.Department;
import com.mysms.org.api.model.DepartmentUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.org.business.DepartmentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Department operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class DepartmentController implements DepartmentApi {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Override
    public ResponseEntity<Department> createDepartment(Department department, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Department created = departmentService.createDepartment(department, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/org/departments/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteDepartment(UUID departmentId, String ifMatch) {
        departmentService.deleteDepartment(departmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Department> getDepartment(UUID departmentId, String fields) {
        Department found = departmentService.getDepartment(departmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Department>> listDepartments(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Department> list =
                PagingUtil.pagingInfo(departmentService.listDepartments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Department> updateDepartment(UUID departmentId, String ifMatch,
            DepartmentUpdate departmentUpdate, String updateMask, Boolean validateOnly) {
        Department updated = departmentService.updateDepartment(departmentId, ifMatch, departmentUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
