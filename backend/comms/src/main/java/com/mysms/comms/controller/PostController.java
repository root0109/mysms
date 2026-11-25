package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.PostService;
import com.mysms.comms.business.ReactionService;
import com.mysms.comms.api.PostApi;
import com.mysms.comms.api.model.ApprovePostRequest;
import com.mysms.comms.api.model.CancelPostRequest;
import com.mysms.comms.api.model.Post;
import com.mysms.comms.api.model.PostAudienceClass;
import com.mysms.comms.api.model.PostAudienceClassUpdate;
import com.mysms.comms.api.model.PostComment;
import com.mysms.comms.api.model.PostCommentUpdate;
import com.mysms.comms.api.model.PostReaction;
import com.mysms.comms.api.model.PostReactionUpdate;
import com.mysms.comms.api.model.PostUpdate;
import com.mysms.comms.api.model.SubmitPostRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Post operations (posts, their audiences, comments and reactions, the feed); authentication and
 * the permission check already ran in the auth-client filter.
 */
@RestController
public class PostController implements PostApi {

    private final PostService postService;
    private final ReactionService reactionService;

    public PostController(PostService postService, ReactionService reactionService) {
        this.postService = postService;
        this.reactionService = reactionService;
    }

    @Override
    public ResponseEntity<Post> approvePost(UUID postId, ApprovePostRequest approvePostRequest,
            UUID idempotencyKey) {
        Post changed = postService.approvePost(postId, approvePostRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Post> cancelPost(UUID postId, CancelPostRequest cancelPostRequest,
            UUID idempotencyKey) {
        Post changed = postService.cancelPost(postId, cancelPostRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Post> createPost(Post post, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Post created = postService.createPost(post, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/posts/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<PostAudienceClass> createPostAudienceClass(UUID postId,
            PostAudienceClass postAudienceClass, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PostAudienceClass created = postService.createPostAudienceClass(postId, postAudienceClass, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/posts/" + postId + "/audience-classes/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PostComment> createPostComment(UUID postId, PostComment postComment,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PostComment created = postService.createPostComment(postId, postComment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/posts/" + postId + "/comments/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PostReaction> createPostReaction(UUID postId, PostReaction postReaction,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PostReaction created = reactionService.createPostReaction(postId, postReaction, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/posts/" + postId + "/reactions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deletePost(UUID postId, String ifMatch) {
        postService.deletePost(postId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePostAudienceClass(UUID postId, UUID postAudienceClassId,
            String ifMatch) {
        postService.deletePostAudienceClass(postId, postAudienceClassId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePostComment(UUID postId, UUID postCommentId, String ifMatch) {
        postService.deletePostComment(postId, postCommentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePostReaction(UUID postId, UUID postReactionId, String ifMatch) {
        reactionService.deletePostReaction(postId, postReactionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagingInfo<Post>> getFeed(UUID classSectionId, UUID classPageId, Boolean savedOnly,
            Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(
                postService.getFeed(classSectionId, classPageId, savedOnly, page, size, cursor)));
    }

    @Override
    public ResponseEntity<Post> getPost(UUID postId, String fields) {
        Post found = postService.getPost(postId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PostAudienceClass> getPostAudienceClass(UUID postId, UUID postAudienceClassId,
            String fields) {
        PostAudienceClass found = postService.getPostAudienceClass(postId, postAudienceClassId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PostComment> getPostComment(UUID postId, UUID postCommentId, String fields) {
        PostComment found = postService.getPostComment(postId, postCommentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PostReaction> getPostReaction(UUID postId, UUID postReactionId, String fields) {
        PostReaction found = reactionService.getPostReaction(postId, postReactionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PostAudienceClass>> listPostAudienceClasses(UUID postId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PostAudienceClass> list =
                PagingUtil.pagingInfo(postService.listPostAudienceClasss(postId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PostComment>> listPostComments(UUID postId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PostComment> list =
                PagingUtil.pagingInfo(postService.listPostComments(postId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PostReaction>> listPostReactions(UUID postId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PostReaction> list =
                PagingUtil.pagingInfo(reactionService.listPostReactions(postId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Post>> listPosts(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Post> list = PagingUtil.pagingInfo(postService.listPosts(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Post> submitPost(UUID postId, SubmitPostRequest submitPostRequest,
            UUID idempotencyKey) {
        Post changed = postService.submitPost(postId, submitPostRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Post> updatePost(UUID postId, String ifMatch, PostUpdate postUpdate,
            String updateMask, Boolean validateOnly) {
        Post updated = postService.updatePost(postId, ifMatch, postUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PostAudienceClass> updatePostAudienceClass(UUID postId,
            UUID postAudienceClassId, String ifMatch, PostAudienceClassUpdate postAudienceClassUpdate,
            String updateMask, Boolean validateOnly) {
        PostAudienceClass updated =
                postService.updatePostAudienceClass(postId, postAudienceClassId, ifMatch,
                        postAudienceClassUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PostComment> updatePostComment(UUID postId, UUID postCommentId, String ifMatch,
            PostCommentUpdate postCommentUpdate, String updateMask, Boolean validateOnly) {
        PostComment updated =
                postService.updatePostComment(postId, postCommentId, ifMatch, postCommentUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PostReaction> updatePostReaction(UUID postId, UUID postReactionId,
            String ifMatch, PostReactionUpdate postReactionUpdate, String updateMask, Boolean validateOnly) {
        PostReaction updated = reactionService.updatePostReaction(postId, postReactionId, ifMatch, postReactionUpdate,
                updateMask, Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
