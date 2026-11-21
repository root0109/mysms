package com.mysms.iam.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.iam.api.ApiPermissionApi;
import com.mysms.iam.api.model.ApiPermission;
import com.mysms.iam.business.ApiPermissionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of iam.ListApiPermissions; authentication, the platform-staff scheme and the permission check already
 * happened in the auth-client filter.
 */
@RestController
public class ApiPermissionController implements ApiPermissionApi {

    private final ApiPermissionService apiPermissionService;

    public ApiPermissionController(ApiPermissionService apiPermissionService) {
        this.apiPermissionService = apiPermissionService;
    }

    @Override
    public ResponseEntity<PagingInfo<ApiPermission>> listApiPermissions(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ApiPermission> list =
                PagingUtil.pagingInfo(apiPermissionService.list(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
