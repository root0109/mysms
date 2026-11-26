package com.mysms.identity.iam.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.identity.iam.api.RoleApi;
import com.mysms.identity.iam.api.model.Role;
import com.mysms.identity.iam.api.model.RolePermission;
import com.mysms.identity.iam.api.model.RolePermissionUpdate;
import com.mysms.identity.iam.api.model.RoleUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.identity.iam.business.RoleService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Role operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class RoleController implements RoleApi {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public ResponseEntity<Role> createRole(Role role, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Role created = roleService.createRole(role, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/iam/roles/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<RolePermission> createRolePermission(UUID roleId, RolePermission rolePermission,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RolePermission created = roleService.createRolePermission(roleId, rolePermission, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/iam/roles/" + roleId + "/permissions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteRole(UUID roleId, String ifMatch) {
        roleService.deleteRole(roleId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRolePermission(UUID roleId, UUID rolePermissionId, String ifMatch) {
        roleService.deleteRolePermission(roleId, rolePermissionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Role> getRole(UUID roleId, String fields) {
        Role found = roleService.getRole(roleId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RolePermission> getRolePermission(UUID roleId, UUID rolePermissionId,
            String fields) {
        RolePermission found = roleService.getRolePermission(roleId, rolePermissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RolePermission>> listRolePermissions(UUID roleId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<RolePermission> list =
                PagingUtil.pagingInfo(roleService.listRolePermissions(roleId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Role>> listRoles(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Role> list = PagingUtil.pagingInfo(roleService.listRoles(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Role> updateRole(UUID roleId, String ifMatch, RoleUpdate roleUpdate,
            Boolean validateOnly) {
        Role updated = roleService.updateRole(roleId, ifMatch, roleUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RolePermission> updateRolePermission(UUID roleId, UUID rolePermissionId,
            String ifMatch, RolePermissionUpdate rolePermissionUpdate, Boolean validateOnly) {
        RolePermission updated =
                roleService.updateRolePermission(roleId, rolePermissionId, ifMatch, rolePermissionUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
