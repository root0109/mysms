package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ForumPostVoteApi;
import com.mysms.lms.api.model.ForumPostVote;
import com.mysms.lms.api.model.ForumPostVoteUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ForumPostVoteService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ForumPostVote operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ForumPostVoteController implements ForumPostVoteApi {

    private final ForumPostVoteService forumPostVoteService;

    public ForumPostVoteController(ForumPostVoteService forumPostVoteService) {
        this.forumPostVoteService = forumPostVoteService;
    }

    @Override
    public ResponseEntity<ForumPostVote> createForumPostVote(ForumPostVote forumPostVote,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ForumPostVote created = forumPostVoteService.createForumPostVote(forumPostVote, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteForumPostVote(UUID forumPostVoteId, String ifMatch) {
        forumPostVoteService.deleteForumPostVote(forumPostVoteId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagingInfo<ForumPostVote>> listForumPostVotes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ForumPostVote> list =
                PagingUtil.pagingInfo(forumPostVoteService.listForumPostVotes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ForumPostVote> updateForumPostVote(UUID forumPostVoteId, String ifMatch,
            ForumPostVoteUpdate forumPostVoteUpdate, String updateMask, Boolean validateOnly) {
        ForumPostVote updated =
                forumPostVoteService.updateForumPostVote(forumPostVoteId, ifMatch, forumPostVoteUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
