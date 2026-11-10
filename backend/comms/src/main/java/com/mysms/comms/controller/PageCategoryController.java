package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.PageCategoryService;
import com.mysms.comms.api.PageCategoryApi;
import com.mysms.comms.api.model.PageCategory;
import com.mysms.comms.api.model.PageCategoryUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PageCategory operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/comms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class PageCategoryController implements PageCategoryApi {

    private final PageCategoryService pageCategoryService;

    public PageCategoryController(PageCategoryService pageCategoryService) {
        this.pageCategoryService = pageCategoryService;
    }

    @Override
    public ResponseEntity<PageCategory> createPageCategory(PageCategory pageCategory, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PageCategory created = pageCategoryService.createPageCategory(pageCategory, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/page-categories/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deletePageCategory(UUID pageCategoryId, String ifMatch) {
        pageCategoryService.deletePageCategory(pageCategoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PageCategory> getPageCategory(UUID pageCategoryId, String fields) {
        PageCategory found = pageCategoryService.getPageCategory(pageCategoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PageCategory>> listPageCategories(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PageCategory> list =
                PagingUtil.pagingInfo(pageCategoryService.listPageCategorys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PageCategory> updatePageCategory(UUID pageCategoryId, String ifMatch,
            PageCategoryUpdate pageCategoryUpdate, String updateMask, Boolean validateOnly) {
        PageCategory updated =
                pageCategoryService.updatePageCategory(pageCategoryId, ifMatch, pageCategoryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
