package com.mysms.comms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.ApprovePostRequest;
import com.mysms.comms.api.model.CancelPostRequest;
import com.mysms.comms.api.model.Post;
import com.mysms.comms.api.model.PostAudienceClass;
import com.mysms.comms.api.model.PostAudienceClassUpdate;
import com.mysms.comms.api.model.PostComment;
import com.mysms.comms.api.model.PostCommentUpdate;
import com.mysms.comms.api.model.PostUpdate;
import com.mysms.comms.api.model.SubmitPostRequest;
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
import com.mysms.core.error.PermissionDeniedException;
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
import com.mysms.core.web.Patches;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.post and of the records that exist only as part of a Post (PostAudienceClass, PostComment):
 * audit and domain event commit in the same transaction as every change. Rows outside the caller's institutions are
 * invisible; tenant-level rows change only through a tenant-wide grant. A post or comment is always the caller's own:
 * the author comes from the token, only the author edits it (anyone else gets 404), and a class page's members-only
 * rule holds for writes as for reads.
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
    static final String PERMISSION_SUBMIT_POST = "comms.post.submit_post";
    static final String PERMISSION_GET_FEED = "comms.post.get_feed";

    public static final String POST_AUDIENCE_CLASS_AGGREGATE = PostAudienceClassEntity.TABLE;
    static final String POST_AUDIENCE_CLASS_PERMISSION_LIST = "comms.post_audience_class.read";
    static final String POST_AUDIENCE_CLASS_PERMISSION_GET = "comms.post_audience_class.read";
    static final String POST_AUDIENCE_CLASS_PERMISSION_CREATE = "comms.post_audience_class.create";
    static final String POST_AUDIENCE_CLASS_PERMISSION_UPDATE = "comms.post_audience_class.update";
    static final String POST_AUDIENCE_CLASS_PERMISSION_DELETE = "comms.post_audience_class.delete";

    public static final String POST_COMMENT_AGGREGATE = PostCommentEntity.TABLE;
    static final String POST_COMMENT_PERMISSION_LIST = "comms.post_comment.read";
    static final String POST_COMMENT_PERMISSION_GET = "comms.post_comment.read";
    static final String POST_COMMENT_PERMISSION_CREATE = "comms.post_comment.create";
    static final String POST_COMMENT_PERMISSION_UPDATE = "comms.post_comment.update";
    static final String POST_COMMENT_PERMISSION_DELETE = "comms.post_comment.delete";

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

    /** The post, when it is in the caller's scope and, on a class page, the caller is its author or a member. */
    public Post getPost(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_GET);
        UUID caller = OwnRows.caller();
        return doInReadTransaction(() -> mapper.toDto(loadReadable(scope, id, caller)));
    }

    /** The posts in the caller's scope; a class page's posts only to their author and the page's members. */
    public Page<Post> listPosts(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PostRepository.SORT_FIELDS, PostRepository.DEFAULT_ORDER,
                PostRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PostRepository.FILTER_FIELDS)
                .and(PostRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(PostRepository.pageAccess(OwnRows.caller()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PostEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * Creates the caller's post (a class page's only while the caller is a member of it, 403 otherwise); with
     * validateOnly every check runs and nothing is written.
     */
    public Post createPost(Post request, boolean validateOnly) {
        validator.validate(request);
        PostEntity entity = mapper.toEntity(request);
        UUID caller = OwnRows.caller();
        entity.setAuthorUserId(caller);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            requirePageMember(entity.getClassPageId(), caller);
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
     * Partial update of the caller's own post under If-Match (anyone else's is 404) (§7.2: a field left out is kept, a
     * null one cleared, a value set); the merged row must validate, stay in the caller's scope, and move only to a
     * class page the caller is a member of.
     */
    public Post updatePost(UUID id, String ifMatch, PostUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        UUID caller = OwnRows.caller();
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PostEntity entity = loadVisible(scope, id);
            OwnRows.require(entity.getAuthorUserId(), AGGREGATE, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            UUID page = entity.getClassPageId();
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            if (!Objects.equals(page, entity.getClassPageId())) {
                requirePageMember(entity.getClassPageId(), caller);
            }
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

    /**
     * Soft delete under If-Match by the author or a moderator of the post's institution (404 to anyone else); refused
     * while live rows of this module still reference the row.
     */
    public void deletePost(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PostEntity entity = loadVisible(scope, id);
            requireAuthorOrModerator(entity, entity.getAuthorUserId(), AGGREGATE, id);
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
     * comms.ApprovePost: Approve and publish. Allowed while status is pending, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
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
     * comms.CancelPost: Withdraw. Allowed while status is draft or pending or posted, on a row the caller may see and
     * write; otherwise 409 and nothing changes.
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

    /**
     * comms.SubmitPost: the author hands a draft in for moderation (draft → pending; ApprovePost publishes it). Only
     * the author submits, and a post on a class page only while the author is a member of that page.
     */
    public Post submitPost(UUID id, SubmitPostRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SUBMIT_POST);
        UUID caller = OwnRows.caller();
        return doInWriteTransaction(() -> {
            PostEntity entity = loadVisible(scope, id);
            OwnRows.require(entity.getAuthorUserId(), AGGREGATE, id);
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!"draft".equals(entity.getStatus())) {
                throw new InvalidStateException("Only a draft post can be submitted.");
            }
            if (entity.getClassPageId() != null && !repository.isPageMember(entity.getClassPageId(), caller)) {
                throw new PermissionDeniedException("Only members of the class page post to it.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.submitPost(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "submit", List.of("status"), null);
            outbox.publish(AGGREGATE + ".submit", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.GetFeed: the published posts the caller may read, newest first — their own, those of the active class
     * pages they are a member of, and school-wide posts of their institutions — narrowed to a page, a class or their
     * bookmarks when asked.
     */
    public Page<Post> getFeed(UUID classSectionId, UUID classPageId, Boolean savedOnly, Integer pageNumber,
            Integer pageSize, String cursor) {
        UUID caller = OwnRows.caller();
        SqlFragment where = PostRepository.readableBy(caller, DataScope.current(PERMISSION_GET_FEED));
        if (classPageId != null) {
            where = where.and(PostRepository.onClassPage(classPageId));
        }
        if (classSectionId != null) {
            where = where.and(PostRepository.forClassSection(classSectionId));
        }
        if (Boolean.TRUE.equals(savedOnly)) {
            where = where.and(PostRepository.savedBy(caller));
        }
        SortSpec sort = SortSpec.parse(null, PostRepository.FEED_SORT_FIELDS, PostRepository.FEED_ORDER,
                PostRepository.ID_COLUMN);
        String view = "feed|" + caller + "|" + classPageId + "|" + classSectionId + "|" + Boolean.TRUE.equals(savedOnly);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, view, clock);
        SqlFragment admitted = where;
        return doInReadTransaction(() -> repository.list(page, admitted).map(mapper::toDto));
    }

    /** {@link #loadVisible}, and on a class page only for its author and the page's active members (404 otherwise). */
    private PostEntity loadReadable(DataScope scope, UUID id, UUID reader) {
        PostEntity entity = loadVisible(scope, id);
        if (!repository.pageAdmits(entity, reader)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PostEntity loadVisible(DataScope scope, UUID id) {
        PostEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /**
     * A class page's post — with its comments and audience classes — is reached only by its author and the page's
     * members (404 otherwise).
     */
    private void requirePostReadable(UUID postId, UUID reader) {
        if (repository.findById(postId).filter(post -> !repository.pageAdmits(post, reader)).isPresent()) {
            throw new ResourceNotFoundException(AGGREGATE, postId);
        }
    }

    /** 404 (as {@code aggregate} {@code id}) unless the post exists and the caller wrote it. */
    private void requirePostAuthor(UUID postId, String aggregate, UUID id) {
        if (repository.findById(postId).filter(post -> OwnRows.caller().equals(post.getAuthorUserId())).isEmpty()) {
            throw new ResourceNotFoundException(aggregate, id);
        }
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(PostEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** 403 unless the caller is an active member of the class page (none: no page, nothing to check). */
    private void requirePageMember(UUID classPageId, UUID caller) {
        if (classPageId != null && !repository.isPageMember(classPageId, caller)) {
            throw new PermissionDeniedException("Only members of the class page post to it.");
        }
    }

    /**
     * 404 unless the caller wrote the content or moderates the post's institution (holds the post approval there):
     * moderators remove content, they never edit it.
     */
    private void requireAuthorOrModerator(PostEntity post, UUID author, String aggregate, UUID id) {
        if (OwnRows.caller().equals(author)) {
            return;
        }
        if (!AuthContext.require().hasPermission(PERMISSION_APPROVE_POST)
                || !DataScope.current(PERMISSION_APPROVE_POST).canRead(institutionOf(post))) {
            throw new ResourceNotFoundException(aggregate, id);
        }
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(PostEntity entity, PostUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("classPageId", changes.getClassPageId(), entity::setClassPageId);
        patch.apply("body", changes.getBody(), entity::setBody);
        patch.apply("visibility", changes.getVisibility(), v -> entity.setVisibility(v == null ? null : v.getValue()));
        patch.apply("scheduledFor", changes.getScheduledFor(), entity::setScheduledFor);
        return patch.applied();
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

    // ---- PostAudienceClass ----

    /** The audience class, when its post is readable to the caller ({@link #getPost}'s class-page rule). */
    public PostAudienceClass getPostAudienceClass(UUID postId, UUID id) {
        DataScope scope = DataScope.current(POST_AUDIENCE_CLASS_PERMISSION_GET);
        UUID caller = OwnRows.caller();
        return doInReadTransaction(() -> {
            requirePostReadable(postId, caller);
            return postAudienceClassMapper.toDto(loadVisiblePostAudienceClass(scope, postId, id));
        });
    }

    /** The audience classes of a post readable to the caller ({@link #getPost}'s class-page rule; 404 otherwise). */
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
        UUID caller = OwnRows.caller();
        return doInReadTransaction(() -> {
            requirePostReadable(postId, caller);
            Page<PostAudienceClassEntity> rows = postAudienceClassRepository.list(page, where);
            return rows.map(postAudienceClassMapper::toDto);
        });
    }

    /**
     * Addresses the caller's own post to one more class (another author's post is 404); with validateOnly every check
     * runs and nothing is written.
     */
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
            requirePostAuthor(postId, AGGREGATE, postId);
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
     * Partial update of an audience class of the caller's own post under If-Match (404 on another author's post) (§7.2:
     * a field left out is kept, a null one cleared, a value set); the merged row must validate and stay in the caller's
     * scope.
     */
    public PostAudienceClass updatePostAudienceClass(UUID postId, UUID id, String ifMatch,
            PostAudienceClassUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(POST_AUDIENCE_CLASS_PERMISSION_UPDATE);
            PostAudienceClassEntity entity = loadVisiblePostAudienceClass(scope, postId, id);
            requirePostAuthor(postId, POST_AUDIENCE_CLASS_AGGREGATE, id);
            scope.requireWritable(institutionOfPostAudienceClass(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyPostAudienceClass(entity, changes);
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

    /** Soft delete under If-Match of an audience class of the caller's own post (404 on another author's post). */
    public void deletePostAudienceClass(UUID postId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(POST_AUDIENCE_CLASS_PERMISSION_DELETE);
            PostAudienceClassEntity entity = loadVisiblePostAudienceClass(scope, postId, id);
            requirePostAuthor(postId, POST_AUDIENCE_CLASS_AGGREGATE, id);
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

    /** The institution of the comms.post the row belongs to. */
    private Optional<UUID> institutionOfPostAudienceClass(PostAudienceClassEntity entity) {
        return postAudienceClassRepository.institutionOf(entity.getPostId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyPostAudienceClass(PostAudienceClassEntity entity, PostAudienceClassUpdate changes) {
        Patches patch = new Patches();
        patch.apply("classSectionId", changes.getClassSectionId(), entity::setClassSectionId);
        return patch.applied();
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

    // ---- PostComment ----

    /** The comment, when its post is readable to the caller ({@link #getPost}'s class-page rule). */
    public PostComment getPostComment(UUID postId, UUID id) {
        DataScope scope = DataScope.current(POST_COMMENT_PERMISSION_GET);
        UUID caller = OwnRows.caller();
        return doInReadTransaction(() -> {
            requirePostReadable(postId, caller);
            return postCommentMapper.toDto(loadVisiblePostComment(scope, postId, id));
        });
    }

    /** The comments of a post readable to the caller ({@link #getPost}'s class-page rule; 404 otherwise). */
    public Page<PostComment> listPostComments(UUID postId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PostCommentRepository.SORT_FIELDS, PostCommentRepository.DEFAULT_ORDER,
                PostCommentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PostCommentRepository.FILTER_FIELDS)
                .and(PostCommentRepository.ofParent(postId))
                        .and(PostCommentRepository.scope(DataScope.current(POST_COMMENT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + postId, clock);
        UUID caller = OwnRows.caller();
        return doInReadTransaction(() -> {
            requirePostReadable(postId, caller);
            Page<PostCommentEntity> rows = postCommentRepository.list(page, where);
            return rows.map(postCommentMapper::toDto);
        });
    }

    /**
     * Creates the caller's comment on a post readable to them ({@link #getPost}'s class-page rule; 404 otherwise); with
     * validateOnly every check runs and nothing is written.
     */
    public PostComment createPostComment(UUID postId, PostComment request, boolean validateOnly) {
        validator.validate(request);
        PostCommentEntity entity = postCommentMapper.toEntity(request);
        entity.setPostId(postId);
        UUID caller = OwnRows.caller();
        entity.setAuthorUserId(caller);
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
            requirePostReadable(postId, caller);
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
     * Partial update of the caller's own comment under If-Match (anyone else's is 404) (§7.2: a field left out is kept,
     * a null one cleared, a value set); the merged row must validate and stay in the caller's scope.
     */
    public PostComment updatePostComment(UUID postId, UUID id, String ifMatch, PostCommentUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(POST_COMMENT_PERMISSION_UPDATE);
            PostCommentEntity entity = loadVisiblePostComment(scope, postId, id);
            OwnRows.require(entity.getAuthorUserId(), POST_COMMENT_AGGREGATE, id);
            scope.requireWritable(institutionOfPostComment(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyPostComment(entity, changes);
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

    /**
     * Soft delete under If-Match by the comment's author or a moderator of the post's institution (404 to anyone
     * else); refused while live rows of this module still reference the row.
     */
    public void deletePostComment(UUID postId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(POST_COMMENT_PERMISSION_DELETE);
            PostCommentEntity entity = loadVisiblePostComment(scope, postId, id);
            requireAuthorOrModerator(repository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException(POST_COMMENT_AGGREGATE, id)),
                    entity.getAuthorUserId(), POST_COMMENT_AGGREGATE, id);
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


    /** The institution of the comms.post the row belongs to. */
    private Optional<UUID> institutionOfPostComment(PostCommentEntity entity) {
        return postCommentRepository.institutionOf(entity.getPostId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyPostComment(PostCommentEntity entity, PostCommentUpdate changes) {
        Patches patch = new Patches();
        patch.apply("parentCommentId", changes.getParentCommentId(), entity::setParentCommentId);
        patch.apply("body", changes.getBody(), entity::setBody);
        patch.apply("attachmentFileId", changes.getAttachmentFileId(), entity::setAttachmentFileId);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPostComment(PostCommentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPostId() == null) {
            errors.add(new FieldError("postId", "must not be null", "NotNull"));
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
