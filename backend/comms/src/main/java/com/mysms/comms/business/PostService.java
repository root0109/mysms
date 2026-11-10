package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.ApprovePostRequest;
import com.mysms.comms.api.model.CancelPostRequest;
import com.mysms.comms.api.model.Post;
import com.mysms.comms.api.model.PostAudienceClass;
import com.mysms.comms.api.model.PostAudienceClassUpdate;
import com.mysms.comms.api.model.PostComment;
import com.mysms.comms.api.model.PostCommentUpdate;
import com.mysms.comms.api.model.PostUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.PostAudienceClassEntity;
import com.mysms.comms.domain.PostCommentEntity;
import com.mysms.comms.domain.PostEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.PostAudienceClassMapper;
import com.mysms.comms.mapper.PostCommentMapper;
import com.mysms.comms.mapper.PostMapper;
import com.mysms.comms.repository.PostAudienceClassRepository;
import com.mysms.comms.repository.PostCommentRepository;
import com.mysms.comms.repository.PostRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.post (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Post
 * are served here too — PostAudienceClass, PostComment.
 */
@Service
public class PostService extends AppTransactionManager {

    public static final String AGGREGATE = PostEntity.TABLE;
    static final String PERMISSION_LIST = "comms.post.read";
    static final String PERMISSION_GET = "comms.post.read";
    static final String PERMISSION_CREATE = "comms.post.create";
    static final String PERMISSION_UPDATE = "comms.post.update";
    static final String PERMISSION_DELETE = "comms.post.delete";
    static final String PERMISSION_APPROVE_POST = "comms.post.approve_post";
    static final String PERMISSION_CANCEL_POST = "comms.post.cancel_post";
    /** The contract's PostUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "classPageId", "authorUserId", "body", "visibility",
                    "scheduledFor");

    public static final String POST_AUDIENCE_CLASS_AGGREGATE = PostAudienceClassEntity.TABLE;
    static final String POST_AUDIENCE_CLASS_PERMISSION_LIST = "comms.post_audience_class.read";
    static final String POST_AUDIENCE_CLASS_PERMISSION_GET = "comms.post_audience_class.read";
    static final String POST_AUDIENCE_CLASS_PERMISSION_CREATE = "comms.post_audience_class.create";
    static final String POST_AUDIENCE_CLASS_PERMISSION_UPDATE = "comms.post_audience_class.update";
    static final String POST_AUDIENCE_CLASS_PERMISSION_DELETE = "comms.post_audience_class.delete";
    /** The contract's PostAudienceClassUpdate fields, in apply order. */
    static final List<String> POST_AUDIENCE_CLASS_WRITABLE = List.of("classSectionId");

    public static final String POST_COMMENT_AGGREGATE = PostCommentEntity.TABLE;
    static final String POST_COMMENT_PERMISSION_LIST = "comms.post_comment.read";
    static final String POST_COMMENT_PERMISSION_GET = "comms.post_comment.read";
    static final String POST_COMMENT_PERMISSION_CREATE = "comms.post_comment.create";
    static final String POST_COMMENT_PERMISSION_UPDATE = "comms.post_comment.update";
    static final String POST_COMMENT_PERMISSION_DELETE = "comms.post_comment.delete";
    /** The contract's PostCommentUpdate fields, in apply order. */
    static final List<String> POST_COMMENT_WRITABLE =
            List.of("parentCommentId", "authorUserId", "body", "attachmentFileId", "isActive");

    private final PostRepository repository;
    private final PostMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final PostAudienceClassRepository postAudienceClassRepository;
    private final PostAudienceClassMapper postAudienceClassMapper;
    private final PostCommentRepository postCommentRepository;
    private final PostCommentMapper postCommentMapper;

    public PostService(PostRepository repository,
            PostMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            PostAudienceClassRepository postAudienceClassRepository,
            PostAudienceClassMapper postAudienceClassMapper,
            PostCommentRepository postCommentRepository,
            PostCommentMapper postCommentMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.postAudienceClassRepository = postAudienceClassRepository;
        this.postAudienceClassMapper = postAudienceClassMapper;
        this.postCommentRepository = postCommentRepository;
        this.postCommentMapper = postCommentMapper;
    }

    public Post getPost(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Post> listPosts(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PostRepository.SORT_FIELDS, PostRepository.DEFAULT_ORDER,
                PostRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PostRepository.FILTER_FIELDS)
                .and(PostRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PostEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Post createPost(Post request, boolean validateOnly) {
        validator.validate(request);
        PostEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            PostEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Post updatePost(UUID id, String ifMatch, PostUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PostEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePost(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PostEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByPostAudienceClassPostId(id)) {
                throw new InvalidStateException(
                        "The post is still referenced by comms.post_audience_class rows; remove those first.");
            }
            if (repository.referencedByPostCommentPostId(id)) {
                throw new InvalidStateException(
                        "The post is still referenced by comms.post_comment rows; remove those first.");
            }
            if (repository.referencedByReactionPostId(id)) {
                throw new InvalidStateException(
                        "The post is still referenced by comms.reaction rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /**
     * comms.ApprovePost: Approve and publish. The row must be visible and writable for the caller, and the step is
     * allowed while status is pending; in any other state
     * the answer is 409 and nothing changes.
     */
    public Post approvePost(UUID id, ApprovePostRequest request) {
        DataScope scope = DataScope.current(PERMISSION_APPROVE_POST);
        return doInWriteTransaction(() -> {
            PostEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("pending").contains(entity.getStatus()))) {
                throw new InvalidStateException("The post cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.approvePost(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "approve", List.of("status", "postedAt"), null);
            outbox.publish(AGGREGATE + ".approve", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.CancelPost: Withdraw. The row must be visible and writable for the caller, and the step is allowed while
     * status is draft or pending or posted; in any other state
     * the answer is 409 and nothing changes.
     */
    public Post cancelPost(UUID id, CancelPostRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_POST);
        return doInWriteTransaction(() -> {
            PostEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "pending", "posted").contains(entity.getStatus()))) {
                throw new InvalidStateException("The post cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.cancelPost(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PostEntity loadVisible(DataScope scope, UUID id) {
        PostEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(PostEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected PostUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(PostEntity entity, PostUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "classPageId" -> entity.setClassPageId(changes.getClassPageId());
                case "authorUserId" -> entity.setAuthorUserId(changes.getAuthorUserId());
                case "body" -> entity.setBody(changes.getBody());
                case "visibility" -> entity.setVisibility(Optional.ofNullable(changes.getVisibility())
                        .map(v -> v.getValue()).orElse(null));
                case "scheduledFor" -> entity.setScheduledFor(changes.getScheduledFor());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PostEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getAuthorUserId() == null) {
            errors.add(new FieldError("authorUserId", "must not be null", "NotNull"));
        }
        if (entity.getBody() == null) {
            errors.add(new FieldError("body", "must not be null", "NotNull"));
        }
        if (!creating && entity.getVisibility() == null) {
            errors.add(new FieldError("visibility", "must not be null", "NotNull"));
        }
        if (entity.getClassPageId() != null && !repository.existsClassPageId(entity.getClassPageId())) {
            errors.add(new FieldError("classPageId", "class page does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PostEntity entity) {

    }

    // ---- PostAudienceClass: a record of the Post aggregate ----

    public PostAudienceClass getPostAudienceClass(UUID postId, UUID id) {
        return doInReadTransaction(() -> {
            return postAudienceClassMapper.toDto(loadVisiblePostAudienceClass(DataScope.current(
                    POST_AUDIENCE_CLASS_PERMISSION_GET), postId, id));
        });
    }

    public Page<PostAudienceClass> listPostAudienceClasss(UUID postId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PostAudienceClassRepository.SORT_FIELDS,
                        PostAudienceClassRepository.DEFAULT_ORDER,
                PostAudienceClassRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PostAudienceClassRepository.FILTER_FIELDS)
                .and(PostAudienceClassRepository.ofParent(postId))
                        .and(PostAudienceClassRepository.scope(DataScope.current(POST_AUDIENCE_CLASS_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + postId, clock);
        return doInReadTransaction(() -> {
            Page<PostAudienceClassEntity> rows = postAudienceClassRepository.list(page, where);
            return rows.map(postAudienceClassMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PostAudienceClass createPostAudienceClass(UUID postId, PostAudienceClass request, boolean validateOnly) {
        validator.validate(request);
        PostAudienceClassEntity entity = postAudienceClassMapper.toEntity(request);
        entity.setPostId(postId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(POST_AUDIENCE_CLASS_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!postAudienceClassRepository.existsPostId(entity.getPostId())) {
                throw new ResourceNotFoundException("comms.post", entity.getPostId());
            }
            if (!scope.canRead(institutionOfPostAudienceClass(entity))) {
                throw new ResourceNotFoundException("comms.post", entity.getPostId());
            }
            assertValidPostAudienceClass(entity, true);
            scope.requireWritable(institutionOfPostAudienceClass(entity));
            assertUniquePostAudienceClass(entity);
            if (validateOnly) {
                return postAudienceClassMapper.toDto(entity);
            }
            PostAudienceClassEntity stored = postAudienceClassRepository.insert(entity);
            audit.recordCreate(POST_AUDIENCE_CLASS_AGGREGATE, stored.getId());
            outbox.publish(POST_AUDIENCE_CLASS_AGGREGATE + CommsEvents.CREATED, POST_AUDIENCE_CLASS_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return postAudienceClassMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PostAudienceClass updatePostAudienceClass(UUID postId, UUID id, String ifMatch,
            PostAudienceClassUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, POST_AUDIENCE_CLASS_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(POST_AUDIENCE_CLASS_PERMISSION_UPDATE);
            PostAudienceClassEntity entity = loadVisiblePostAudienceClass(scope, postId, id);
            scope.requireWritable(institutionOfPostAudienceClass(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPostAudienceClass(entity, changes, fields);
            validator.validate(postAudienceClassMapper.toDto(entity));
            assertValidPostAudienceClass(entity, false);
            scope.requireWritable(institutionOfPostAudienceClass(entity));
            assertUniquePostAudienceClass(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return postAudienceClassMapper.toDto(entity);
            }
            if (postAudienceClassRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(POST_AUDIENCE_CLASS_AGGREGATE, id, fields);
            outbox.publish(POST_AUDIENCE_CLASS_AGGREGATE + CommsEvents.UPDATED, POST_AUDIENCE_CLASS_AGGREGATE, id,
                    CommsEvents.payload(id));
            return postAudienceClassMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePostAudienceClass(UUID postId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(POST_AUDIENCE_CLASS_PERMISSION_DELETE);
            PostAudienceClassEntity entity = loadVisiblePostAudienceClass(scope, postId, id);
            scope.requireWritable(institutionOfPostAudienceClass(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (postAudienceClassRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(POST_AUDIENCE_CLASS_AGGREGATE, id);
            outbox.publish(POST_AUDIENCE_CLASS_AGGREGATE + CommsEvents.DELETED, POST_AUDIENCE_CLASS_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PostAudienceClassEntity loadVisiblePostAudienceClass(DataScope scope, UUID postId, UUID id) {
        PostAudienceClassEntity entity = postAudienceClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(POST_AUDIENCE_CLASS_AGGREGATE, id));
        if (!scope.canRead(institutionOfPostAudienceClass(entity)) || !postId.equals(entity.getPostId())) {
            throw new ResourceNotFoundException(POST_AUDIENCE_CLASS_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the comms.post it belongs to). */
    private Optional<UUID> institutionOfPostAudienceClass(PostAudienceClassEntity entity) {
        return postAudienceClassRepository.institutionOf(entity.getPostId());
    }

    /**
     * Applies the selected PostAudienceClassUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyPostAudienceClass(PostAudienceClassEntity entity, PostAudienceClassUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPostAudienceClass(PostAudienceClassEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPostId() == null) {
            errors.add(new FieldError("postId", "must not be null", "NotNull"));
        }
        if (entity.getClassSectionId() == null) {
            errors.add(new FieldError("classSectionId", "must not be null", "NotNull"));
        }
        if (entity.getPostId() != null && !postAudienceClassRepository.existsPostId(entity.getPostId())) {
            errors.add(new FieldError("postId", "post does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePostAudienceClass(PostAudienceClassEntity entity) {
        if (postAudienceClassRepository.takenPostIdClassSectionId1(entity)) {
            throw new DuplicateResourceException(
                    "A post audience class with the same post id and class section id already exists.");
        }
    }

    // ---- PostComment: a record of the Post aggregate ----

    public PostComment getPostComment(UUID postId, UUID id) {
        return doInReadTransaction(() -> {
            return postCommentMapper.toDto(loadVisiblePostComment(DataScope.current(POST_COMMENT_PERMISSION_GET), postId, id));
        });
    }

    public Page<PostComment> listPostComments(UUID postId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PostCommentRepository.SORT_FIELDS, PostCommentRepository.DEFAULT_ORDER,
                PostCommentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PostCommentRepository.FILTER_FIELDS)
                .and(PostCommentRepository.ofParent(postId))
                        .and(PostCommentRepository.scope(DataScope.current(POST_COMMENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + postId, clock);
        return doInReadTransaction(() -> {
            Page<PostCommentEntity> rows = postCommentRepository.list(page, where);
            return rows.map(postCommentMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PostComment createPostComment(UUID postId, PostComment request, boolean validateOnly) {
        validator.validate(request);
        PostCommentEntity entity = postCommentMapper.toEntity(request);
        entity.setPostId(postId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(POST_COMMENT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!postCommentRepository.existsPostId(entity.getPostId())) {
                throw new ResourceNotFoundException("comms.post", entity.getPostId());
            }
            if (!scope.canRead(institutionOfPostComment(entity))) {
                throw new ResourceNotFoundException("comms.post", entity.getPostId());
            }
            assertValidPostComment(entity, true);
            scope.requireWritable(institutionOfPostComment(entity));
            assertUniquePostComment(entity);
            if (validateOnly) {
                return postCommentMapper.toDto(entity);
            }
            PostCommentEntity stored = postCommentRepository.insert(entity);
            audit.recordCreate(POST_COMMENT_AGGREGATE, stored.getId());
            outbox.publish(POST_COMMENT_AGGREGATE + CommsEvents.CREATED, POST_COMMENT_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return postCommentMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PostComment updatePostComment(UUID postId, UUID id, String ifMatch, PostCommentUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, POST_COMMENT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(POST_COMMENT_PERMISSION_UPDATE);
            PostCommentEntity entity = loadVisiblePostComment(scope, postId, id);
            scope.requireWritable(institutionOfPostComment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPostComment(entity, changes, fields);
            validator.validate(postCommentMapper.toDto(entity));
            assertValidPostComment(entity, false);
            scope.requireWritable(institutionOfPostComment(entity));
            assertUniquePostComment(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return postCommentMapper.toDto(entity);
            }
            if (postCommentRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(POST_COMMENT_AGGREGATE, id, fields);
            outbox.publish(POST_COMMENT_AGGREGATE + CommsEvents.UPDATED, POST_COMMENT_AGGREGATE, id, CommsEvents.payload(id));
            return postCommentMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePostComment(UUID postId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(POST_COMMENT_PERMISSION_DELETE);
            PostCommentEntity entity = loadVisiblePostComment(scope, postId, id);
            scope.requireWritable(institutionOfPostComment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (postCommentRepository.referencedByPostCommentParentCommentId(id)) {
                throw new InvalidStateException(
                        "The post comment is still referenced by comms.post_comment rows; remove those first.");
            }
            if (postCommentRepository.referencedByReactionCommentId(id)) {
                throw new InvalidStateException(
                        "The post comment is still referenced by comms.reaction rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (postCommentRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(POST_COMMENT_AGGREGATE, id);
            outbox.publish(POST_COMMENT_AGGREGATE + CommsEvents.DELETED, POST_COMMENT_AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PostCommentEntity loadVisiblePostComment(DataScope scope, UUID postId, UUID id) {
        PostCommentEntity entity = postCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(POST_COMMENT_AGGREGATE, id));
        if (!scope.canRead(institutionOfPostComment(entity)) || !postId.equals(entity.getPostId())) {
            throw new ResourceNotFoundException(POST_COMMENT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the comms.post it belongs to). */
    private Optional<UUID> institutionOfPostComment(PostCommentEntity entity) {
        return postCommentRepository.institutionOf(entity.getPostId());
    }

    /** Applies the selected PostCommentUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void applyPostComment(PostCommentEntity entity, PostCommentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "parentCommentId" -> entity.setParentCommentId(changes.getParentCommentId());
                case "authorUserId" -> entity.setAuthorUserId(changes.getAuthorUserId());
                case "body" -> entity.setBody(changes.getBody());
                case "attachmentFileId" -> entity.setAttachmentFileId(changes.getAttachmentFileId());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPostComment(PostCommentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPostId() == null) {
            errors.add(new FieldError("postId", "must not be null", "NotNull"));
        }
        if (entity.getAuthorUserId() == null) {
            errors.add(new FieldError("authorUserId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getParentCommentId() != null && !postCommentRepository.existsParentCommentId(entity.getParentCommentId())) {
            errors.add(new FieldError("parentCommentId", "post comment does not exist", "NOT_FOUND"));
        }
        if (entity.getPostId() != null && !postCommentRepository.existsPostId(entity.getPostId())) {
            errors.add(new FieldError("postId", "post does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePostComment(PostCommentEntity entity) {

    }
}
