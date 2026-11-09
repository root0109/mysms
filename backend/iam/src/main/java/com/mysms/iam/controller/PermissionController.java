package com.mysms.iam.controller;

import com.mysms.contracts.iam.api.PermissionApi;
import com.mysms.contracts.iam.model.Permission;
import com.mysms.contracts.iam.model.PermissionList;
import com.mysms.core.web.ReadMask;
import com.mysms.iam.business.PermissionCatalogService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the read-only permission catalogue (iam.ListPermissions, iam.GetPermission). */
@RestController
public class PermissionController implements PermissionApi {

    private final PermissionCatalogService service;

    public PermissionController(PermissionCatalogService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<PermissionList> listPermissions(Integer pageSize, String pageToken, String filter,
            String orderBy, String fields) {
        PermissionList list = service.list(pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Permission> getPermission(UUID permissionId, String fields) {
        Permission found = service.get(permissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }
}
