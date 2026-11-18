package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.DiscussionPromptApi;
import com.mysms.lms.api.model.DiscussionPrompt;
import com.mysms.lms.api.model.DiscussionPromptUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.DiscussionPromptService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the DiscussionPrompt operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class DiscussionPromptController implements DiscussionPromptApi {

    private final DiscussionPromptService discussionPromptService;

    public DiscussionPromptController(DiscussionPromptService discussionPromptService) {
        this.discussionPromptService = discussionPromptService;
    }

    @Override
    public ResponseEntity<DiscussionPrompt> createDiscussionPrompt(DiscussionPrompt discussionPrompt,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        DiscussionPrompt created = discussionPromptService.createDiscussionPrompt(discussionPrompt, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/discussion-prompts/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteDiscussionPrompt(UUID discussionPromptId, String ifMatch) {
        discussionPromptService.deleteDiscussionPrompt(discussionPromptId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DiscussionPrompt> getDiscussionPrompt(UUID discussionPromptId, String fields) {
        DiscussionPrompt found = discussionPromptService.getDiscussionPrompt(discussionPromptId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<DiscussionPrompt>> listDiscussionPrompts(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<DiscussionPrompt> list =
                PagingUtil.pagingInfo(discussionPromptService.listDiscussionPrompts(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<DiscussionPrompt> updateDiscussionPrompt(UUID discussionPromptId,
            String ifMatch, DiscussionPromptUpdate discussionPromptUpdate, String updateMask, Boolean validateOnly) {
        DiscussionPrompt updated =
                discussionPromptService.updateDiscussionPrompt(discussionPromptId, ifMatch, discussionPromptUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
