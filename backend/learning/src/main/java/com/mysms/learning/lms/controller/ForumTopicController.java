package com.mysms.learning.lms.controller;

import com.mysms.learning.lms.api.model.ForumReadState;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ForumTopicApi;
import com.mysms.learning.lms.api.model.ConfirmPostAbuseRequest;
import com.mysms.learning.lms.api.model.FlagForumPostRequest;
import com.mysms.learning.lms.api.model.ForumPost;
import com.mysms.learning.lms.api.model.ForumPostUpdate;
import com.mysms.learning.lms.api.model.ForumTopic;
import com.mysms.learning.lms.api.model.ForumTopicUpdate;
import com.mysms.learning.lms.api.model.HideForumPostRequest;
import com.mysms.learning.lms.api.model.HideForumTopicRequest;
import com.mysms.learning.lms.api.model.MarkPostSubstantiveRequest;
import com.mysms.learning.lms.api.model.MarkTopicReadRequest;
import com.mysms.learning.lms.api.model.UnhideForumPostRequest;
import com.mysms.learning.lms.api.model.UnhideForumTopicRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ForumTopicService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ForumTopic operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ForumTopicController implements ForumTopicApi {

    private final ForumTopicService forumTopicService;

    public ForumTopicController(ForumTopicService forumTopicService) {
        this.forumTopicService = forumTopicService;
    }

    @Override
    public ResponseEntity<ForumPost> confirmPostAbuse(UUID forumTopicId, UUID forumPostId,
            ConfirmPostAbuseRequest confirmPostAbuseRequest, UUID idempotencyKey) {
        ForumPost changed = forumTopicService.confirmPostAbuse(forumTopicId, forumPostId,
                confirmPostAbuseRequest == null ? null : confirmPostAbuseRequest.getEtag(),
                confirmPostAbuseRequest == null ? null : confirmPostAbuseRequest.getReason());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ForumPost> createForumPost(UUID forumTopicId, ForumPost forumPost,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ForumPost created = forumTopicService.createForumPost(forumTopicId, forumPost, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/forum-topics/" + forumTopicId + "/posts/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ForumTopic> createForumTopic(ForumTopic forumTopic, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ForumTopic created = forumTopicService.createForumTopic(forumTopic, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/forum-topics/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteForumPost(UUID forumTopicId, UUID forumPostId, String ifMatch) {
        forumTopicService.deleteForumPost(forumTopicId, forumPostId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteForumTopic(UUID forumTopicId, String ifMatch) {
        forumTopicService.deleteForumTopic(forumTopicId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> flagForumPost(UUID forumTopicId, UUID forumPostId,
            FlagForumPostRequest flagForumPostRequest, UUID idempotencyKey) {
        forumTopicService.flagForumPost(forumTopicId, forumPostId, Boolean.TRUE.equals(flagForumPostRequest.getFlagged()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ForumPost> getForumPost(UUID forumTopicId, UUID forumPostId, String fields) {
        ForumPost found = forumTopicService.getForumPost(forumTopicId, forumPostId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ForumTopic> getForumTopic(UUID forumTopicId, String fields) {
        ForumTopic found = forumTopicService.getForumTopic(forumTopicId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ForumPost> hideForumPost(UUID forumTopicId, UUID forumPostId,
            HideForumPostRequest hideForumPostRequest, UUID idempotencyKey) {
        ForumPost changed = forumTopicService.hideForumPost(forumTopicId, forumPostId,
                hideForumPostRequest == null ? null : hideForumPostRequest.getEtag(),
                hideForumPostRequest == null ? null : hideForumPostRequest.getReason());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ForumTopic> hideForumTopic(UUID forumTopicId,
            HideForumTopicRequest hideForumTopicRequest, UUID idempotencyKey) {
        ForumTopic changed = forumTopicService.hideForumTopic(forumTopicId, hideForumTopicRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<PagingInfo<ForumPost>> listForumPosts(UUID forumTopicId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ForumPost> list =
                PagingUtil.pagingInfo(forumTopicService.listForumPosts(forumTopicId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ForumReadState>> listForumReadStates(UUID forumTopicId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ForumReadState> list =
                PagingUtil.pagingInfo(forumTopicService.listForumReadStates(forumTopicId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ForumTopic>> listForumTopics(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ForumTopic> list =
                PagingUtil.pagingInfo(forumTopicService.listForumTopics(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ForumPost> markPostSubstantive(UUID forumTopicId, UUID forumPostId,
            MarkPostSubstantiveRequest markPostSubstantiveRequest, UUID idempotencyKey) {
        ForumPost changed = forumTopicService.markPostSubstantive(forumTopicId, forumPostId,
                Boolean.TRUE.equals(markPostSubstantiveRequest.getSubstantive()));
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> markTopicRead(UUID forumTopicId,
            MarkTopicReadRequest markTopicReadRequest, UUID idempotencyKey) {
        forumTopicService.markTopicRead(forumTopicId,
                markTopicReadRequest == null ? null : markTopicReadRequest.getForumPostIds());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ForumPost> unhideForumPost(UUID forumTopicId, UUID forumPostId,
            UnhideForumPostRequest unhideForumPostRequest, UUID idempotencyKey) {
        ForumPost changed = forumTopicService.unhideForumPost(forumTopicId, forumPostId,
                unhideForumPostRequest == null ? null : unhideForumPostRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ForumTopic> unhideForumTopic(UUID forumTopicId,
            UnhideForumTopicRequest unhideForumTopicRequest, UUID idempotencyKey) {
        ForumTopic changed = forumTopicService.unhideForumTopic(forumTopicId, unhideForumTopicRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ForumPost> updateForumPost(UUID forumTopicId, UUID forumPostId, String ifMatch,
            ForumPostUpdate forumPostUpdate, Boolean validateOnly) {
        ForumPost updated =
                forumTopicService.updateForumPost(forumTopicId, forumPostId, ifMatch, forumPostUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ForumTopic> updateForumTopic(UUID forumTopicId, String ifMatch,
            ForumTopicUpdate forumTopicUpdate, Boolean validateOnly) {
        ForumTopic updated = forumTopicService.updateForumTopic(forumTopicId, ifMatch, forumTopicUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
