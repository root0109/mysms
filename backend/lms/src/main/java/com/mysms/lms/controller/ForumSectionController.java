package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ForumSectionApi;
import com.mysms.lms.api.model.ForumSection;
import com.mysms.lms.api.model.ForumSectionUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ForumSectionService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ForumSection operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ForumSectionController implements ForumSectionApi {

    private final ForumSectionService forumSectionService;

    public ForumSectionController(ForumSectionService forumSectionService) {
        this.forumSectionService = forumSectionService;
    }

    @Override
    public ResponseEntity<ForumSection> createForumSection(ForumSection forumSection, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ForumSection created = forumSectionService.createForumSection(forumSection, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/forum-sections/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteForumSection(UUID forumSectionId, String ifMatch) {
        forumSectionService.deleteForumSection(forumSectionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ForumSection> getForumSection(UUID forumSectionId, String fields) {
        ForumSection found = forumSectionService.getForumSection(forumSectionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ForumSection>> listForumSections(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ForumSection> list =
                PagingUtil.pagingInfo(forumSectionService.listForumSections(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ForumSection> updateForumSection(UUID forumSectionId, String ifMatch,
            ForumSectionUpdate forumSectionUpdate, String updateMask, Boolean validateOnly) {
        ForumSection updated =
                forumSectionService.updateForumSection(forumSectionId, ifMatch, forumSectionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
