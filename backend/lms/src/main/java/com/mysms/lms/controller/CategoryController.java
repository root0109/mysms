package com.mysms.lms.controller;

import com.mysms.core.web.ReadMask;
import com.mysms.core.paging.PagingUtil;
import java.net.URI;
import java.util.List;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.CategoryApi;
import com.mysms.lms.api.model.Category;
import com.mysms.lms.api.model.CategoryUpdate;
import com.mysms.lms.business.CategoryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Category operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class CategoryController implements CategoryApi {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public ResponseEntity<Category> createCategory(Category category, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Category created = categoryService.createCategory(category, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/categories/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCategory(UUID categoryId, String ifMatch) {
        categoryService.deleteCategory(categoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Category> getCategory(UUID categoryId, String fields) {
        Category found = categoryService.getCategory(categoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Category>> listCategories(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Category> list =
                PagingUtil.pagingInfo(categoryService.listCategories(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Category> updateCategory(UUID categoryId, String ifMatch,
            CategoryUpdate categoryUpdate, String updateMask, Boolean validateOnly) {
        Category updated = categoryService.updateCategory(categoryId, ifMatch, categoryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
