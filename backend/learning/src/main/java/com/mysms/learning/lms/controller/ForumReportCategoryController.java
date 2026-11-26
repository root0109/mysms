package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ForumReportCategoryApi;
import com.mysms.learning.lms.api.model.ForumReportCategory;
import com.mysms.learning.lms.api.model.ForumReportCategoryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ForumReportCategoryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ForumReportCategory operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class ForumReportCategoryController implements ForumReportCategoryApi {

    private final ForumReportCategoryService forumReportCategoryService;

    public ForumReportCategoryController(ForumReportCategoryService forumReportCategoryService) {
        this.forumReportCategoryService = forumReportCategoryService;
    }

    @Override
    public ResponseEntity<ForumReportCategory> createForumReportCategory(
            ForumReportCategory forumReportCategory, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ForumReportCategory created = forumReportCategoryService.createForumReportCategory(forumReportCategory, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/forum-report-categories/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteForumReportCategory(UUID forumReportCategoryId, String ifMatch) {
        forumReportCategoryService.deleteForumReportCategory(forumReportCategoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ForumReportCategory> getForumReportCategory(UUID forumReportCategoryId,
            String fields) {
        ForumReportCategory found = forumReportCategoryService.getForumReportCategory(forumReportCategoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ForumReportCategory>> listForumReportCategories(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ForumReportCategory> list =
                PagingUtil.pagingInfo(forumReportCategoryService.listForumReportCategorys(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ForumReportCategory> updateForumReportCategory(UUID forumReportCategoryId,
            String ifMatch, ForumReportCategoryUpdate forumReportCategoryUpdate, Boolean validateOnly) {
        ForumReportCategory updated =
                forumReportCategoryService.updateForumReportCategory(forumReportCategoryId, ifMatch,
                        forumReportCategoryUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
