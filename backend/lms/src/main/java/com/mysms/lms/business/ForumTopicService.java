package com.mysms.lms.business;

import com.mysms.lms.api.model.ForumReadState;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.ForumPost;
import com.mysms.lms.api.model.ForumPostUpdate;
import com.mysms.lms.api.model.ForumTopic;
import com.mysms.lms.api.model.ForumTopicUpdate;
import com.mysms.lms.api.model.HideForumTopicRequest;
import com.mysms.lms.api.model.UnhideForumTopicRequest;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ForumPostEntity;
import com.mysms.lms.domain.ForumReadStateEntity;
import com.mysms.lms.domain.ForumTopicEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ForumPostMapper;
import com.mysms.lms.mapper.ForumReadStateMapper;
import com.mysms.lms.mapper.ForumTopicMapper;
import com.mysms.lms.repository.ForumPostRepository;
import com.mysms.lms.repository.ForumReadStateRepository;
import com.mysms.lms.repository.ForumTopicRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.forum_topic (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a ForumTopic
 * are served here too — ForumPost, ForumReadState.
 */
@Service
public class ForumTopicService extends AppTransactionManager {

    public static final String AGGREGATE = ForumTopicEntity.TABLE;
    static final String PERMISSION_LIST = "lms.forum_topic.read";
    static final String PERMISSION_GET = "lms.forum_topic.read";
    static final String PERMISSION_CREATE = "lms.forum_topic.create";
    static final String PERMISSION_UPDATE = "lms.forum_topic.update";
    static final String PERMISSION_DELETE = "lms.forum_topic.delete";
    static final String PERMISSION_HIDE_FORUM_TOPIC = "lms.forum_topic.hide_forum_topic";
    static final String PERMISSION_UNHIDE_FORUM_TOPIC = "lms.forum_topic.unhide_forum_topic";
    /** The contract's ForumTopicUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("forumSectionId", "title", "description", "topicType", "opensAt", "closesAt", "contentItemId",
            "discussionPromptId", "linkedEntityType", "linkedEntityId", "individualUserId", "isGraded",
            "pointsPossible", "gradeWeightPercent", "gradingDueAt", "isGradingCompleted", "isHidden", "isTeacherTopic",
            "isSystemGenerated", "lastPostId");

    public static final String FORUM_POST_AGGREGATE = ForumPostEntity.TABLE;
    static final String FORUM_POST_PERMISSION_LIST = "lms.forum_post.read";
    static final String FORUM_POST_PERMISSION_GET = "lms.forum_post.read";
    static final String FORUM_POST_PERMISSION_CREATE = "lms.forum_post.create";
    static final String FORUM_POST_PERMISSION_UPDATE = "lms.forum_post.update";
    static final String FORUM_POST_PERMISSION_DELETE = "lms.forum_post.delete";
    /** The contract's ForumPostUpdate fields, in apply order. */
    static final List<String> FORUM_POST_WRITABLE =
            List.of("parentPostId", "title", "body", "authorUserId", "postedAt", "isDraft", "isTeacherPost",
                    "substantiveFlag",
            "isHidden", "isHiddenByParent", "isFlagged", "showSignature", "usesInlineAuthoring", "isAbusive",
            "abuseReported", "abuseReportCount", "abuseConfirmedBy", "abuseConfirmedAt", "likeCount");

    public static final String FORUM_READ_STATE_AGGREGATE = ForumReadStateEntity.TABLE;
    static final String FORUM_READ_STATE_PERMISSION_LIST = "lms.forum_read_state.read";

    private final ForumTopicRepository repository;
    private final ForumTopicMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ForumPostRepository forumPostRepository;
    private final ForumPostMapper forumPostMapper;
    private final ForumReadStateRepository forumReadStateRepository;
    private final ForumReadStateMapper forumReadStateMapper;

    public ForumTopicService(ForumTopicRepository repository,
            ForumTopicMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ForumPostRepository forumPostRepository,
            ForumPostMapper forumPostMapper,
            ForumReadStateRepository forumReadStateRepository,
            ForumReadStateMapper forumReadStateMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.forumPostRepository = forumPostRepository;
        this.forumPostMapper = forumPostMapper;
        this.forumReadStateRepository = forumReadStateRepository;
        this.forumReadStateMapper = forumReadStateMapper;
    }

    public ForumTopic getForumTopic(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ForumTopic> listForumTopics(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ForumTopicRepository.SORT_FIELDS, ForumTopicRepository.DEFAULT_ORDER,
                ForumTopicRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ForumTopicRepository.FILTER_FIELDS)
                .and(ForumTopicRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ForumTopicEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ForumTopic createForumTopic(ForumTopic request, boolean validateOnly) {
        validator.validate(request);
        ForumTopicEntity entity = mapper.toEntity(request);
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
            ForumTopicEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ForumTopic updateForumTopic(UUID id, String ifMatch, ForumTopicUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ForumTopicEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteForumTopic(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ForumTopicEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAnnotationForumTopicId(id)) {
                throw new InvalidStateException(
                        "The forum topic is still referenced by lms.annotation rows; remove those first.");
            }
            if (repository.referencedByAssignmentGroupDiscussionTopicId(id)) {
                throw new InvalidStateException(
                        "The forum topic is still referenced by lms.assignment_group rows; remove those first.");
            }
            if (repository.referencedByForumPostForumTopicId(id)) {
                throw new InvalidStateException(
                        "The forum topic is still referenced by lms.forum_post rows; remove those first.");
            }
            if (repository.referencedByForumReadStateForumTopicId(id)) {
                throw new InvalidStateException(
                        "The forum topic is still referenced by lms.forum_read_state rows; remove those first.");
            }
            if (repository.referencedByGradebookEntryForumTopicId(id)) {
                throw new InvalidStateException(
                        "The forum topic is still referenced by lms.gradebook_entry rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /**
     * lms.HideForumTopic: Hide from students. The row must be visible and writable for the caller, and the step is
     * allowed while is_hidden is false; in any other state
     * the answer is 409 and nothing changes.
     */
    public ForumTopic hideForumTopic(UUID id, HideForumTopicRequest request) {
        DataScope scope = DataScope.current(PERMISSION_HIDE_FORUM_TOPIC);
        return doInWriteTransaction(() -> {
            ForumTopicEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(!Boolean.TRUE.equals(entity.getIsHidden()))) {
                throw new InvalidStateException("The forum topic cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.hideForumTopic(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "hide", List.of("isHidden"), null);
            outbox.publish(AGGREGATE + ".hide", AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * lms.UnhideForumTopic: Show again. The row must be visible and writable for the caller, and the step is allowed
     * while is_hidden is true; in any other state
     * the answer is 409 and nothing changes.
     */
    public ForumTopic unhideForumTopic(UUID id, UnhideForumTopicRequest request) {
        DataScope scope = DataScope.current(PERMISSION_UNHIDE_FORUM_TOPIC);
        return doInWriteTransaction(() -> {
            ForumTopicEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Boolean.TRUE.equals(entity.getIsHidden()))) {
                throw new InvalidStateException("The forum topic cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.unhideForumTopic(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "unhide", List.of("isHidden"), null);
            outbox.publish(AGGREGATE + ".unhide", AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ForumTopicEntity loadVisible(DataScope scope, UUID id) {
        ForumTopicEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.discussion_prompt it belongs to). */
    private Optional<UUID> institutionOf(ForumTopicEntity entity) {
        return repository.institutionOf(entity.getDiscussionPromptId());
    }

    /** Applies the selected ForumTopicUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(ForumTopicEntity entity, ForumTopicUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "forumSectionId" -> entity.setForumSectionId(changes.getForumSectionId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "topicType" -> entity.setTopicType(Optional.ofNullable(changes.getTopicType())
                        .map(v -> v.getValue()).orElse(null));
                case "opensAt" -> entity.setOpensAt(changes.getOpensAt());
                case "closesAt" -> entity.setClosesAt(changes.getClosesAt());
                case "contentItemId" -> entity.setContentItemId(changes.getContentItemId());
                case "discussionPromptId" -> entity.setDiscussionPromptId(changes.getDiscussionPromptId());
                case "linkedEntityType" -> entity.setLinkedEntityType(changes.getLinkedEntityType());
                case "linkedEntityId" -> entity.setLinkedEntityId(changes.getLinkedEntityId());
                case "individualUserId" -> entity.setIndividualUserId(changes.getIndividualUserId());
                case "isGraded" -> entity.setIsGraded(changes.getIsGraded());
                case "pointsPossible" -> entity.setPointsPossible(WireValues.decimal(changes.getPointsPossible()));
                case "gradeWeightPercent" -> entity.setGradeWeightPercent(
                        WireValues.decimal(changes.getGradeWeightPercent()));
                case "gradingDueAt" -> entity.setGradingDueAt(changes.getGradingDueAt());
                case "isGradingCompleted" -> entity.setIsGradingCompleted(changes.getIsGradingCompleted());
                case "isHidden" -> entity.setIsHidden(changes.getIsHidden());
                case "isTeacherTopic" -> entity.setIsTeacherTopic(changes.getIsTeacherTopic());
                case "isSystemGenerated" -> entity.setIsSystemGenerated(changes.getIsSystemGenerated());
                case "lastPostId" -> entity.setLastPostId(changes.getLastPostId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ForumTopicEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getTopicType() == null) {
            errors.add(new FieldError("topicType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsGraded() == null) {
            errors.add(new FieldError("isGraded", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("pointsPossible", WireValues.decimal(entity.getPointsPossible()), 8, 2, errors);
        WireValues.checkDecimal("gradeWeightPercent", WireValues.decimal(entity.getGradeWeightPercent()), 5, 2, errors);
        if (!creating && entity.getIsGradingCompleted() == null) {
            errors.add(new FieldError("isGradingCompleted", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsHidden() == null) {
            errors.add(new FieldError("isHidden", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsTeacherTopic() == null) {
            errors.add(new FieldError("isTeacherTopic", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsSystemGenerated() == null) {
            errors.add(new FieldError("isSystemGenerated", "must not be null", "NotNull"));
        }
        if (entity.getContentItemId() != null && !repository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getDiscussionPromptId() != null
                && !repository.existsDiscussionPromptId(entity.getDiscussionPromptId())) {
            errors.add(new FieldError("discussionPromptId", "discussion prompt does not exist", "NOT_FOUND"));
        }
        if (entity.getForumSectionId() != null && !repository.existsForumSectionId(entity.getForumSectionId())) {
            errors.add(new FieldError("forumSectionId", "forum section does not exist", "NOT_FOUND"));
        }
        if (entity.getLastPostId() != null && !repository.existsLastPostId(entity.getLastPostId())) {
            errors.add(new FieldError("lastPostId", "forum post does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ForumTopicEntity entity) {

    }

    // ---- ForumPost: a record of the ForumTopic aggregate ----

    public ForumPost getForumPost(UUID forumTopicId, UUID id) {
        return doInReadTransaction(() -> {
            return forumPostMapper.toDto(loadVisibleForumPost(DataScope.current(FORUM_POST_PERMISSION_GET), forumTopicId, id));
        });
    }

    public Page<ForumPost> listForumPosts(UUID forumTopicId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ForumPostRepository.SORT_FIELDS, ForumPostRepository.DEFAULT_ORDER,
                ForumPostRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ForumPostRepository.FILTER_FIELDS)
                .and(ForumPostRepository.ofParent(forumTopicId))
                        .and(ForumPostRepository.scope(DataScope.current(FORUM_POST_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + forumTopicId, clock);
        return doInReadTransaction(() -> {
            Page<ForumPostEntity> rows = forumPostRepository.list(page, where);
            return rows.map(forumPostMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ForumPost createForumPost(UUID forumTopicId, ForumPost request, boolean validateOnly) {
        validator.validate(request);
        ForumPostEntity entity = forumPostMapper.toEntity(request);
        entity.setForumTopicId(forumTopicId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(FORUM_POST_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!forumPostRepository.existsForumTopicId(entity.getForumTopicId())) {
                throw new ResourceNotFoundException("lms.forum_topic", entity.getForumTopicId());
            }
            if (!scope.canRead(institutionOfForumPost(entity))) {
                throw new ResourceNotFoundException("lms.forum_topic", entity.getForumTopicId());
            }
            assertValidForumPost(entity, true);
            scope.requireWritable(institutionOfForumPost(entity));
            assertUniqueForumPost(entity);
            if (validateOnly) {
                return forumPostMapper.toDto(entity);
            }
            ForumPostEntity stored = forumPostRepository.insert(entity);
            audit.recordCreate(FORUM_POST_AGGREGATE, stored.getId());
            outbox.publish(FORUM_POST_AGGREGATE + LmsEvents.CREATED, FORUM_POST_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return forumPostMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ForumPost updateForumPost(UUID forumTopicId, UUID id, String ifMatch, ForumPostUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, FORUM_POST_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FORUM_POST_PERMISSION_UPDATE);
            ForumPostEntity entity = loadVisibleForumPost(scope, forumTopicId, id);
            scope.requireWritable(institutionOfForumPost(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyForumPost(entity, changes, fields);
            validator.validate(forumPostMapper.toDto(entity));
            assertValidForumPost(entity, false);
            scope.requireWritable(institutionOfForumPost(entity));
            assertUniqueForumPost(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return forumPostMapper.toDto(entity);
            }
            if (forumPostRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(FORUM_POST_AGGREGATE, id, fields);
            outbox.publish(FORUM_POST_AGGREGATE + LmsEvents.UPDATED, FORUM_POST_AGGREGATE, id, LmsEvents.payload(id));
            return forumPostMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteForumPost(UUID forumTopicId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FORUM_POST_PERMISSION_DELETE);
            ForumPostEntity entity = loadVisibleForumPost(scope, forumTopicId, id);
            scope.requireWritable(institutionOfForumPost(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (forumPostRepository.referencedByForumPostParentPostId(id)) {
                throw new InvalidStateException(
                        "The forum post is still referenced by lms.forum_post rows; remove those first.");
            }
            if (forumPostRepository.referencedByForumPostReportForumPostId(id)) {
                throw new InvalidStateException(
                        "The forum post is still referenced by lms.forum_post_report rows; remove those first.");
            }
            if (forumPostRepository.referencedByForumPostVoteForumPostId(id)) {
                throw new InvalidStateException(
                        "The forum post is still referenced by lms.forum_post_vote rows; remove those first.");
            }
            if (forumPostRepository.referencedByForumReadStateForumPostId(id)) {
                throw new InvalidStateException(
                        "The forum post is still referenced by lms.forum_read_state rows; remove those first.");
            }
            if (forumPostRepository.referencedByForumTopicLastPostId(id)) {
                throw new InvalidStateException(
                        "The forum post is still referenced by lms.forum_topic rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (forumPostRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(FORUM_POST_AGGREGATE, id);
            outbox.publish(FORUM_POST_AGGREGATE + LmsEvents.DELETED, FORUM_POST_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ForumPostEntity loadVisibleForumPost(DataScope scope, UUID forumTopicId, UUID id) {
        ForumPostEntity entity = forumPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FORUM_POST_AGGREGATE, id));
        if (!scope.canRead(institutionOfForumPost(entity)) || !forumTopicId.equals(entity.getForumTopicId())) {
            throw new ResourceNotFoundException(FORUM_POST_AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the lms.forum_topic → lms.discussion_prompt it belongs to).
     */
    private Optional<UUID> institutionOfForumPost(ForumPostEntity entity) {
        return forumPostRepository.institutionOf(entity.getForumTopicId());
    }

    /** Applies the selected ForumPostUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void applyForumPost(ForumPostEntity entity, ForumPostUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "parentPostId" -> entity.setParentPostId(changes.getParentPostId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "body" -> entity.setBody(changes.getBody());
                case "authorUserId" -> entity.setAuthorUserId(changes.getAuthorUserId());
                case "postedAt" -> entity.setPostedAt(changes.getPostedAt());
                case "isDraft" -> entity.setIsDraft(changes.getIsDraft());
                case "isTeacherPost" -> entity.setIsTeacherPost(changes.getIsTeacherPost());
                case "substantiveFlag" -> entity.setSubstantiveFlag(changes.getSubstantiveFlag());
                case "isHidden" -> entity.setIsHidden(changes.getIsHidden());
                case "isHiddenByParent" -> entity.setIsHiddenByParent(changes.getIsHiddenByParent());
                case "isFlagged" -> entity.setIsFlagged(changes.getIsFlagged());
                case "showSignature" -> entity.setShowSignature(changes.getShowSignature());
                case "usesInlineAuthoring" -> entity.setUsesInlineAuthoring(changes.getUsesInlineAuthoring());
                case "isAbusive" -> entity.setIsAbusive(changes.getIsAbusive());
                case "abuseReported" -> entity.setAbuseReported(changes.getAbuseReported());
                case "abuseReportCount" -> entity.setAbuseReportCount(changes.getAbuseReportCount());
                case "abuseConfirmedBy" -> entity.setAbuseConfirmedBy(changes.getAbuseConfirmedBy());
                case "abuseConfirmedAt" -> entity.setAbuseConfirmedAt(changes.getAbuseConfirmedAt());
                case "likeCount" -> entity.setLikeCount(changes.getLikeCount());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidForumPost(ForumPostEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getForumTopicId() == null) {
            errors.add(new FieldError("forumTopicId", "must not be null", "NotNull"));
        }
        if (entity.getAuthorUserId() == null) {
            errors.add(new FieldError("authorUserId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getPostedAt() == null) {
            errors.add(new FieldError("postedAt", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsDraft() == null) {
            errors.add(new FieldError("isDraft", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsTeacherPost() == null) {
            errors.add(new FieldError("isTeacherPost", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsHidden() == null) {
            errors.add(new FieldError("isHidden", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsHiddenByParent() == null) {
            errors.add(new FieldError("isHiddenByParent", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsFlagged() == null) {
            errors.add(new FieldError("isFlagged", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShowSignature() == null) {
            errors.add(new FieldError("showSignature", "must not be null", "NotNull"));
        }
        if (!creating && entity.getUsesInlineAuthoring() == null) {
            errors.add(new FieldError("usesInlineAuthoring", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsAbusive() == null) {
            errors.add(new FieldError("isAbusive", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAbuseReported() == null) {
            errors.add(new FieldError("abuseReported", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAbuseReportCount() == null) {
            errors.add(new FieldError("abuseReportCount", "must not be null", "NotNull"));
        }
        if (!creating && entity.getLikeCount() == null) {
            errors.add(new FieldError("likeCount", "must not be null", "NotNull"));
        }
        if (entity.getForumTopicId() != null && !forumPostRepository.existsForumTopicId(entity.getForumTopicId())) {
            errors.add(new FieldError("forumTopicId", "forum topic does not exist", "NOT_FOUND"));
        }
        if (entity.getParentPostId() != null && !forumPostRepository.existsParentPostId(entity.getParentPostId())) {
            errors.add(new FieldError("parentPostId", "forum post does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueForumPost(ForumPostEntity entity) {

    }

    // ---- ForumReadState: a record of the ForumTopic aggregate ----

    public Page<ForumReadState> listForumReadStates(UUID forumTopicId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ForumReadStateRepository.SORT_FIELDS, ForumReadStateRepository.DEFAULT_ORDER,
                ForumReadStateRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ForumReadStateRepository.FILTER_FIELDS)
                .and(ForumReadStateRepository.ofParent(forumTopicId))
                        .and(ForumReadStateRepository.scope(DataScope.current(FORUM_READ_STATE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + forumTopicId, clock);
        return doInReadTransaction(() -> {
            Page<ForumReadStateEntity> rows = forumReadStateRepository.list(page, where);
            return rows.map(forumReadStateMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ForumReadStateEntity loadVisibleForumReadState(DataScope scope, UUID forumTopicId, UUID id) {
        ForumReadStateEntity entity = forumReadStateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FORUM_READ_STATE_AGGREGATE, id));
        if (!scope.canRead(institutionOfForumReadState(entity)) || !forumTopicId.equals(entity.getForumTopicId())) {
            throw new ResourceNotFoundException(FORUM_READ_STATE_AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the lms.forum_topic → lms.discussion_prompt it belongs to).
     */
    private Optional<UUID> institutionOfForumReadState(ForumReadStateEntity entity) {
        return forumReadStateRepository.institutionOf(entity.getForumTopicId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidForumReadState(ForumReadStateEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getForumTopicId() == null) {
            errors.add(new FieldError("forumTopicId", "must not be null", "NotNull"));
        }
        if (entity.getForumPostId() != null && !forumReadStateRepository.existsForumPostId(entity.getForumPostId())) {
            errors.add(new FieldError("forumPostId", "forum post does not exist", "NOT_FOUND"));
        }
        if (entity.getForumTopicId() != null && !forumReadStateRepository.existsForumTopicId(entity.getForumTopicId())) {
            errors.add(new FieldError("forumTopicId", "forum topic does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueForumReadState(ForumReadStateEntity entity) {

    }
}
