package com.mysms.iam.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.iam.api.PermissionApi;
import com.mysms.iam.api.model.Permission;
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
    public ResponseEntity<PagingInfo<Permission>> listPermissions(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Permission> list = PagingUtil.pagingInfo(service.list(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Permission> getPermission(UUID permissionId, String fields) {
        Permission found = service.get(permissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }
}
