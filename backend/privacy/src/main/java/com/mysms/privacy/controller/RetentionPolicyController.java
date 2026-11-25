package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.RetentionPolicyApi;
import com.mysms.privacy.api.model.RetentionPolicy;
import com.mysms.privacy.api.model.RetentionPolicyUpdate;
import com.mysms.privacy.business.RetentionPolicyService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the retention policies with the tenant's setting of each (tenant administrators); authentication and
 * the permission check already ran in the auth-client filter.
 */
@RestController
public class RetentionPolicyController implements RetentionPolicyApi {

    private final RetentionPolicyService retentionPolicyService;

    public RetentionPolicyController(RetentionPolicyService retentionPolicyService) {
        this.retentionPolicyService = retentionPolicyService;
    }

    @Override
    public ResponseEntity<RetentionPolicy> getRetentionPolicy(UUID retentionPolicyId, String fields) {
        RetentionPolicy found = retentionPolicyService.getRetentionPolicy(retentionPolicyId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RetentionPolicy>> listRetentionPolicies(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<RetentionPolicy> list = PagingUtil.pagingInfo(
                retentionPolicyService.listRetentionPolicies(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<RetentionPolicy> updateRetentionPolicy(UUID retentionPolicyId, String ifMatch,
            RetentionPolicyUpdate retentionPolicyUpdate, String updateMask) {
        RetentionPolicy updated = retentionPolicyService.updateRetentionPolicy(retentionPolicyId, ifMatch,
                retentionPolicyUpdate, updateMask);
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
