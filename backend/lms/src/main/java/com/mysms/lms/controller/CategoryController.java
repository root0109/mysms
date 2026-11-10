package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.CategoryApi;
import com.mysms.lms.api.model.Category;
import com.mysms.lms.api.model.CategoryUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.lms.business.CategoryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Category operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
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
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCategory(UUID categoryId, String ifMatch) {
        categoryService.deleteCategory(categoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Category> getCategory(UUID categoryId, String fields) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<Category>> listCategories(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Category> updateCategory(UUID categoryId, String ifMatch,
            CategoryUpdate categoryUpdate, String updateMask, Boolean validateOnly) {
        Category updated = categoryService.updateCategory(categoryId, ifMatch, categoryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
