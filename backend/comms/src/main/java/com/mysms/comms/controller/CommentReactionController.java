package com.mysms.comms.controller;

import com.mysms.comms.api.CommentReactionApi;
import com.mysms.comms.api.model.CommentReaction;
import com.mysms.comms.api.model.CommentReactionUpdate;
import com.mysms.comms.business.ReactionService;
import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the caller's reactions to comments; authentication and the permission check already ran. */
@RestController
public class CommentReactionController implements CommentReactionApi {

    private final ReactionService reactionService;

    public CommentReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @Override
    public ResponseEntity<CommentReaction> createCommentReaction(CommentReaction commentReaction,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CommentReaction created = reactionService.createCommentReaction(commentReaction, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/comment-reactions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCommentReaction(UUID commentReactionId, String ifMatch) {
        reactionService.deleteCommentReaction(commentReactionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CommentReaction> getCommentReaction(UUID commentReactionId, String fields) {
        CommentReaction found = reactionService.getCommentReaction(commentReactionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CommentReaction>> listCommentReactions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CommentReaction> list =
                PagingUtil.pagingInfo(reactionService.listCommentReactions(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CommentReaction> updateCommentReaction(UUID commentReactionId, String ifMatch,
            CommentReactionUpdate commentReactionUpdate, String updateMask, Boolean validateOnly) {
        CommentReaction updated = reactionService.updateCommentReaction(commentReactionId, ifMatch,
                commentReactionUpdate, updateMask, Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
