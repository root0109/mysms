package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ResourceCategoryApi;
import com.mysms.learning.lms.api.model.ResourceCategory;
import com.mysms.learning.lms.api.model.ResourceCategoryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ResourceCategoryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ResourceCategory operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ResourceCategoryController implements ResourceCategoryApi {

    private final ResourceCategoryService resourceCategoryService;

    public ResourceCategoryController(ResourceCategoryService resourceCategoryService) {
        this.resourceCategoryService = resourceCategoryService;
    }

    @Override
    public ResponseEntity<ResourceCategory> createResourceCategory(ResourceCategory resourceCategory,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ResourceCategory created = resourceCategoryService.createResourceCategory(resourceCategory, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/resource-categories/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteResourceCategory(UUID resourceCategoryId, String ifMatch) {
        resourceCategoryService.deleteResourceCategory(resourceCategoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ResourceCategory> getResourceCategory(UUID resourceCategoryId, String fields) {
        ResourceCategory found = resourceCategoryService.getResourceCategory(resourceCategoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ResourceCategory>> listResourceCategories(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ResourceCategory> list =
                PagingUtil.pagingInfo(resourceCategoryService.listResourceCategorys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ResourceCategory> updateResourceCategory(UUID resourceCategoryId,
            String ifMatch, ResourceCategoryUpdate resourceCategoryUpdate, Boolean validateOnly) {
        ResourceCategory updated =
                resourceCategoryService.updateResourceCategory(resourceCategoryId, ifMatch, resourceCategoryUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
