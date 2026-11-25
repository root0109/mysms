package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.CommentReaction;
import com.mysms.comms.api.model.CommentReactionUpdate;
import com.mysms.comms.api.model.PostReaction;
import com.mysms.comms.api.model.PostReactionUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.PostEntity;
import com.mysms.comms.domain.ReactionEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.CommentReactionMapper;
import com.mysms.comms.mapper.PostReactionMapper;
import com.mysms.comms.repository.PostRepository;
import com.mysms.comms.repository.ReactionRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Reactions to posts and to their comments (comms.reaction), a record with a lifecycle of its own: a reaction is
 * always the caller's — the user comes from the token, and another user's reaction is answered as absent whatever the
 * caller's grants. A reaction is made only to a post the caller may read (the feed's rule) or to a visible comment of
 * one.
 */
@Service
public class ReactionService extends AppTransactionManager {

    public static final String AGGREGATE = ReactionEntity.TABLE;
    static final String POST_PERMISSION_READ = "comms.post_reaction.read";
    static final String POST_PERMISSION_CREATE = "comms.post_reaction.create";
    static final String COMMENT_PERMISSION_READ = "comms.comment_reaction.read";
    static final String COMMENT_PERMISSION_CREATE = "comms.comment_reaction.create";
    /** The contract's PostReactionUpdate fields, in apply order. */
    static final List<String> POST_WRITABLE = List.of("reaction", "isSaved", "isActive");
    /** The contract's CommentReactionUpdate fields, in apply order. */
    static final List<String> COMMENT_WRITABLE = List.of("reaction", "isActive");

    private final ReactionRepository repository;
    private final PostRepository posts;
    private final PostReactionMapper postMapper;
    private final CommentReactionMapper commentMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ReactionService(ReactionRepository repository, PostRepository posts, PostReactionMapper postMapper,
            CommentReactionMapper commentMapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.posts = posts;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    // ---- reactions to a post

    public PostReaction getPostReaction(UUID postId, UUID id) {
        return doInReadTransaction(() -> postMapper.toDto(ownOnPost(postId, id)));
    }

    /** The caller's own reactions to a post they may read. */
    public Page<PostReaction> listPostReactions(UUID postId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ReactionRepository.SORT_FIELDS, ReactionRepository.DEFAULT_ORDER,
                ReactionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ReactionRepository.POST_FILTER_FIELDS)
                .and(ReactionRepository.ofPost(postId));
        UUID caller = OwnRows.caller();
        DataScope scope = DataScope.current(POST_PERMISSION_READ);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + postId + "|" + caller,
                clock);
        return doInReadTransaction(() -> {
            requireReadablePost(postId, caller, scope);
            return repository.listOwn(caller, page, where).map(postMapper::toDto);
        });
    }

    /** The caller reacts to (or bookmarks) a post they may read; one live reaction per post and user. */
    public PostReaction createPostReaction(UUID postId, PostReaction request, boolean validateOnly) {
        validator.validate(request);
        ReactionEntity entity = postMapper.toEntity(request);
        entity.setPostId(postId);
        UUID caller = OwnRows.caller();
        entity.setUserId(caller);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(POST_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            requireReadablePost(postId, caller, scope);
            if (repository.postTaken(postId, caller)) {
                throw new DuplicateResourceException("The caller already reacted to this post; change that reaction.");
            }
            return postMapper.toDto(validateOnly ? entity : created(entity));
        });
    }

    public PostReaction updatePostReaction(UUID postId, UUID id, String ifMatch, PostReactionUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, POST_WRITABLE);
        return doInWriteTransaction(() -> {
            ReactionEntity entity = ownOnPost(postId, id);
            return postMapper.toDto(updated(entity, expected, fields, validateOnly, field -> {
                switch (field) {
                    case "reaction" -> entity.setReaction(Optional.ofNullable(changes.getReaction())
                            .map(reaction -> reaction.getValue()).orElse(null));
                    case "isSaved" -> entity.setIsSaved(changes.getIsSaved());
                    case "isActive" -> entity.setIsActive(changes.getIsActive());
                    default -> throw new IllegalStateException("unhandled writable field " + field);
                }
            }, () -> validator.validate(postMapper.toDto(entity))));
        });
    }

    public void deletePostReaction(UUID postId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> deleted(ownOnPost(postId, id), expected));
    }

    // ---- reactions to a comment

    public CommentReaction getCommentReaction(UUID id) {
        return doInReadTransaction(() -> commentMapper.toDto(ownOnComment(id)));
    }

    /** The caller's own reactions to comments. */
    public Page<CommentReaction> listCommentReactions(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ReactionRepository.SORT_FIELDS, ReactionRepository.DEFAULT_ORDER,
                ReactionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ReactionRepository.COMMENT_FILTER_FIELDS)
                .and(ReactionRepository.onComments());
        UUID caller = OwnRows.caller();
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + caller, clock);
        return doInReadTransaction(() -> repository.listOwn(caller, page, where).map(commentMapper::toDto));
    }

    /** The caller reacts to a visible comment of a post they may read; one live reaction per comment and user. */
    public CommentReaction createCommentReaction(CommentReaction request, boolean validateOnly) {
        validator.validate(request);
        ReactionEntity entity = commentMapper.toEntity(request);
        UUID caller = OwnRows.caller();
        entity.setUserId(caller);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COMMENT_PERMISSION_CREATE);
        UUID commentId = entity.getCommentId();
        return doInWriteTransaction(() -> {
            UUID postId = repository.postOfComment(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("comms.post_comment", commentId));
            if (posts.findReadable(postId, PostRepository.readableBy(caller, scope)).isEmpty()) {
                throw new ResourceNotFoundException("comms.post_comment", commentId);
            }
            if (repository.commentTaken(commentId, caller)) {
                throw new DuplicateResourceException(
                        "The caller already reacted to this comment; change that reaction.");
            }
            return commentMapper.toDto(validateOnly ? entity : created(entity));
        });
    }

    public CommentReaction updateCommentReaction(UUID id, String ifMatch, CommentReactionUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COMMENT_WRITABLE);
        return doInWriteTransaction(() -> {
            ReactionEntity entity = ownOnComment(id);
            return commentMapper.toDto(updated(entity, expected, fields, validateOnly, field -> {
                switch (field) {
                    case "reaction" -> entity.setReaction(Optional.ofNullable(changes.getReaction())
                            .map(reaction -> reaction.getValue()).orElse(null));
                    case "isActive" -> entity.setIsActive(changes.getIsActive());
                    default -> throw new IllegalStateException("unhandled writable field " + field);
                }
            }, () -> validator.validate(commentMapper.toDto(entity))));
        });
    }

    public void deleteCommentReaction(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> deleted(ownOnComment(id), expected));
    }

    // ---- shared steps

    private ReactionEntity created(ReactionEntity entity) {
        ReactionEntity stored = repository.insert(entity);
        audit.recordCreate(AGGREGATE, stored.getId());
        outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(), CommsEvents.payload(stored.getId()));
        return stored;
    }

    /** Applies the fields under If-Match, re-validates the merged row, and writes it unless only validating. */
    private ReactionEntity updated(ReactionEntity entity, int expected, List<String> fields, boolean validateOnly,
            Consumer<String> apply, Runnable revalidate) {
        if (entity.getRowVersion() != expected) {
            throw new PreconditionFailedException("If-Match does not match the current ETag.");
        }
        fields.forEach(apply);
        revalidate.run();
        entity.onUpdatingData(Actor.current(), clock);
        if (validateOnly) {
            return entity;
        }
        if (repository.update(entity, expected) == 0) {
            throw new PreconditionFailedException("If-Match does not match the current ETag.");
        }
        entity.setRowVersion(expected + 1);
        audit.recordUpdate(AGGREGATE, entity.getId(), fields);
        outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, entity.getId(), CommsEvents.payload(entity.getId()));
        return entity;
    }

    private void deleted(ReactionEntity entity, int expected) {
        if (entity.getRowVersion() != expected) {
            throw new PreconditionFailedException("If-Match does not match the current ETag.");
        }
        entity.onDeletingData(Actor.current(), clock);
        if (repository.softDelete(entity, expected) == 0) {
            throw new PreconditionFailedException("If-Match does not match the current ETag.");
        }
        audit.recordDelete(AGGREGATE, entity.getId());
        outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, entity.getId(), CommsEvents.payload(entity.getId()));
    }

    /** The post, if the caller may read it; 404 otherwise, as for a post that does not exist. */
    private void requireReadablePost(UUID postId, UUID caller, DataScope scope) {
        if (posts.findReadable(postId, PostRepository.readableBy(caller, scope)).isEmpty()) {
            throw new ResourceNotFoundException(PostEntity.TABLE, postId);
        }
    }

    /** The caller's own reaction to this post; anything else is 404. */
    private ReactionEntity ownOnPost(UUID postId, UUID id) {
        ReactionEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!postId.equals(entity.getPostId())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        OwnRows.require(entity.getUserId(), AGGREGATE, id);
        return entity;
    }

    /** The caller's own reaction to a comment; anything else is 404. */
    private ReactionEntity ownOnComment(UUID id) {
        ReactionEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (entity.getCommentId() == null) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        OwnRows.require(entity.getUserId(), AGGREGATE, id);
        return entity;
    }
}
