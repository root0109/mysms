package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.GradingCategoryApi;
import com.mysms.learning.lms.api.model.GradingCategory;
import com.mysms.learning.lms.api.model.GradingCategoryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.GradingCategoryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the GradingCategory operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class GradingCategoryController implements GradingCategoryApi {

    private final GradingCategoryService gradingCategoryService;

    public GradingCategoryController(GradingCategoryService gradingCategoryService) {
        this.gradingCategoryService = gradingCategoryService;
    }

    @Override
    public ResponseEntity<GradingCategory> createGradingCategory(GradingCategory gradingCategory,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        GradingCategory created = gradingCategoryService.createGradingCategory(gradingCategory, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/grading-categories/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteGradingCategory(UUID gradingCategoryId, String ifMatch) {
        gradingCategoryService.deleteGradingCategory(gradingCategoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GradingCategory> getGradingCategory(UUID gradingCategoryId, String fields) {
        GradingCategory found = gradingCategoryService.getGradingCategory(gradingCategoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<GradingCategory>> listGradingCategories(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<GradingCategory> list =
                PagingUtil.pagingInfo(gradingCategoryService.listGradingCategorys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<GradingCategory> updateGradingCategory(UUID gradingCategoryId, String ifMatch,
            GradingCategoryUpdate gradingCategoryUpdate, Boolean validateOnly) {
        GradingCategory updated =
                gradingCategoryService.updateGradingCategory(gradingCategoryId, ifMatch, gradingCategoryUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
