package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
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
import com.mysms.lms.api.model.AssignmentGroup;
import com.mysms.lms.api.model.AssignmentGroupUpdate;
import com.mysms.lms.api.model.ContentItem;
import com.mysms.lms.api.model.ContentItemRating;
import com.mysms.lms.api.model.ContentItemRatingUpdate;
import com.mysms.lms.api.model.ContentItemUpdate;
import com.mysms.lms.api.model.ItemOverride;
import com.mysms.lms.api.model.ItemOverrideUpdate;
import com.mysms.lms.api.model.ReleaseGrant;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.AssignmentGroupEntity;
import com.mysms.lms.domain.ContentItemEntity;
import com.mysms.lms.domain.ContentItemRatingEntity;
import com.mysms.lms.domain.ItemOverrideEntity;
import com.mysms.lms.domain.ReleaseGrantEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.AssignmentGroupMapper;
import com.mysms.lms.mapper.ContentItemMapper;
import com.mysms.lms.mapper.ContentItemRatingMapper;
import com.mysms.lms.mapper.ItemOverrideMapper;
import com.mysms.lms.mapper.ReleaseGrantMapper;
import com.mysms.lms.repository.AssignmentGroupRepository;
import com.mysms.lms.repository.ContentItemRatingRepository;
import com.mysms.lms.repository.ContentItemRepository;
import com.mysms.lms.repository.ItemOverrideRepository;
import com.mysms.lms.repository.ReleaseGrantRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.content_item (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a ContentItem
 * are served here too — AssignmentGroup, ContentItemRating, ItemOverride, ReleaseGrant.
 */
@Service
public class ContentItemService extends AppTransactionManager {

    public static final String AGGREGATE = ContentItemEntity.TABLE;
    static final String PERMISSION_LIST = "lms.content_item.read";
    static final String PERMISSION_GET = "lms.content_item.read";
    static final String PERMISSION_CREATE = "lms.content_item.create";
    static final String PERMISSION_UPDATE = "lms.content_item.update";
    static final String PERMISSION_DELETE = "lms.content_item.delete";
    /** The contract's ContentItemUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("exerciseId", "chapterId", "courseId", "cohortId", "itemKind", "lessonId", "fileId", "assignmentId",
            "quizId", "discussionPromptId", "participationActivityId", "webResourceId", "ebookId",
            "externalToolResourceId", "liveClassId", "linkUrl", "title", "sortOrder", "isActive", "isAlwaysVisible",
            "visibleFrom", "visibleUntil", "visibleUntilCohortEnd", "attemptFrom", "dueAt", "attemptUntilCohortEnd",
            "visibleStartDay", "visibleStartTime", "visibleEndDay", "visibleEndTime", "attemptStartDay",
            "attemptStartTime", "attemptEndDay", "attemptEndTime", "lateSubmissionAllowed", "lateSubmissionDays",
            "isGraded", "points", "weightPercent", "gradingCategoryId", "rubricId", "plagiarismCheckRequired",
            "showInCalendar", "allowDelete", "allowEditDetails", "allowEditDates", "allowEditGrading");

    public static final String ASSIGNMENT_GROUP_AGGREGATE = AssignmentGroupEntity.TABLE;
    static final String ASSIGNMENT_GROUP_PERMISSION_LIST = "lms.assignment_group.read";
    static final String ASSIGNMENT_GROUP_PERMISSION_GET = "lms.assignment_group.read";
    static final String ASSIGNMENT_GROUP_PERMISSION_CREATE = "lms.assignment_group.create";
    static final String ASSIGNMENT_GROUP_PERMISSION_UPDATE = "lms.assignment_group.update";
    static final String ASSIGNMENT_GROUP_PERMISSION_DELETE = "lms.assignment_group.delete";
    /** The contract's AssignmentGroupUpdate fields, in apply order. */
    static final List<String> ASSIGNMENT_GROUP_WRITABLE =
            List.of("cohortId", "name", "points", "isActive", "discussionTopicId", "isReassigned");

    public static final String CONTENT_ITEM_RATING_AGGREGATE = ContentItemRatingEntity.TABLE;
    static final String CONTENT_ITEM_RATING_PERMISSION_LIST = "lms.content_item_rating.read";
    static final String CONTENT_ITEM_RATING_PERMISSION_GET = "lms.content_item_rating.read";
    static final String CONTENT_ITEM_RATING_PERMISSION_CREATE = "lms.content_item_rating.create";
    static final String CONTENT_ITEM_RATING_PERMISSION_UPDATE = "lms.content_item_rating.update";
    static final String CONTENT_ITEM_RATING_PERMISSION_DELETE = "lms.content_item_rating.delete";
    /** The contract's ContentItemRatingUpdate fields, in apply order. */
    static final List<String> CONTENT_ITEM_RATING_WRITABLE = List.of("cohortId", "userId", "isLike", "rating");

    public static final String ITEM_OVERRIDE_AGGREGATE = ItemOverrideEntity.TABLE;
    static final String ITEM_OVERRIDE_PERMISSION_LIST = "lms.item_override.read";
    static final String ITEM_OVERRIDE_PERMISSION_GET = "lms.item_override.read";
    static final String ITEM_OVERRIDE_PERMISSION_CREATE = "lms.item_override.create";
    static final String ITEM_OVERRIDE_PERMISSION_UPDATE = "lms.item_override.update";
    static final String ITEM_OVERRIDE_PERMISSION_DELETE = "lms.item_override.delete";
    /** The contract's ItemOverrideUpdate fields, in apply order. */
    static final List<String> ITEM_OVERRIDE_WRITABLE =
            List.of("userId", "groupId", "availableFrom", "dueAt", "extraAttempts", "extraTimeMinutes", "reason");

    public static final String RELEASE_GRANT_AGGREGATE = ReleaseGrantEntity.TABLE;
    static final String RELEASE_GRANT_PERMISSION_LIST = "lms.release_grant.read";
    static final String RELEASE_GRANT_PERMISSION_GET = "lms.release_grant.read";
    static final String RELEASE_GRANT_PERMISSION_DELETE = "lms.release_grant.delete";

    private final ContentItemRepository repository;
    private final ContentItemMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final AssignmentGroupRepository assignmentGroupRepository;
    private final AssignmentGroupMapper assignmentGroupMapper;
    private final ContentItemRatingRepository contentItemRatingRepository;
    private final ContentItemRatingMapper contentItemRatingMapper;
    private final ItemOverrideRepository itemOverrideRepository;
    private final ItemOverrideMapper itemOverrideMapper;
    private final ReleaseGrantRepository releaseGrantRepository;
    private final ReleaseGrantMapper releaseGrantMapper;

    public ContentItemService(ContentItemRepository repository,
            ContentItemMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            AssignmentGroupRepository assignmentGroupRepository,
            AssignmentGroupMapper assignmentGroupMapper,
            ContentItemRatingRepository contentItemRatingRepository,
            ContentItemRatingMapper contentItemRatingMapper,
            ItemOverrideRepository itemOverrideRepository,
            ItemOverrideMapper itemOverrideMapper,
            ReleaseGrantRepository releaseGrantRepository,
            ReleaseGrantMapper releaseGrantMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.assignmentGroupRepository = assignmentGroupRepository;
        this.assignmentGroupMapper = assignmentGroupMapper;
        this.contentItemRatingRepository = contentItemRatingRepository;
        this.contentItemRatingMapper = contentItemRatingMapper;
        this.itemOverrideRepository = itemOverrideRepository;
        this.itemOverrideMapper = itemOverrideMapper;
        this.releaseGrantRepository = releaseGrantRepository;
        this.releaseGrantMapper = releaseGrantMapper;
    }

    public ContentItem getContentItem(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ContentItem> listContentItems(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ContentItemRepository.SORT_FIELDS, ContentItemRepository.DEFAULT_ORDER,
                ContentItemRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ContentItemRepository.FILTER_FIELDS)
                .and(ContentItemRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ContentItemEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ContentItem createContentItem(ContentItem request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("visibleStartTime", request.getVisibleStartTime(), wire);
        WireValues.checkTime("visibleEndTime", request.getVisibleEndTime(), wire);
        WireValues.checkTime("attemptStartTime", request.getAttemptStartTime(), wire);
        WireValues.checkTime("attemptEndTime", request.getAttemptEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        ContentItemEntity entity = mapper.toEntity(request);
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
            ContentItemEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ContentItem updateContentItem(UUID id, String ifMatch, ContentItemUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("visibleStartTime", changes.getVisibleStartTime(), wire);
        WireValues.checkTime("visibleEndTime", changes.getVisibleEndTime(), wire);
        WireValues.checkTime("attemptStartTime", changes.getAttemptStartTime(), wire);
        WireValues.checkTime("attemptEndTime", changes.getAttemptEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ContentItemEntity entity = loadVisible(scope, id);
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
    public void deleteContentItem(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ContentItemEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAnnotationContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.annotation rows; remove those first.");
            }
            if (repository.referencedByAssignmentGroupContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.assignment_group rows; remove those first.");
            }
            if (repository.referencedByAssignmentGroupMemberContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.assignment_group_member rows; remove those "
                                + "first.");
            }
            if (repository.referencedByAssignmentSubmissionContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.assignment_submission rows; remove those first.");
            }
            if (repository.referencedByContentItemCopiedFromId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByContentItemRatingContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.content_item_rating rows; remove those first.");
            }
            if (repository.referencedByContentViewSessionContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.content_view_session rows; remove those first.");
            }
            if (repository.referencedByForumTopicContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.forum_topic rows; remove those first.");
            }
            if (repository.referencedByGradebookEntryContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.gradebook_entry rows; remove those first.");
            }
            if (repository.referencedByItemOverrideContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.item_override rows; remove those first.");
            }
            if (repository.referencedByQuizSubmissionContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.quiz_submission rows; remove those first.");
            }
            if (repository.referencedByReassignmentLogContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.reassignment_log rows; remove those first.");
            }
            if (repository.referencedByReleaseGrantContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.release_grant rows; remove those first.");
            }
            if (repository.referencedByReleaseRuleSourceContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.release_rule rows; remove those first.");
            }
            if (repository.referencedByReleaseRuleTargetContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.release_rule_target rows; remove those first.");
            }
            if (repository.referencedByRubricAssessmentContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.rubric_assessment rows; remove those first.");
            }
            if (repository.referencedByStudentAlertContentItemId(id)) {
                throw new InvalidStateException(
                        "The content item is still referenced by lms.student_alert rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ContentItemEntity loadVisible(DataScope scope, UUID id) {
        ContentItemEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOf(ContentItemEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected ContentItemUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(ContentItemEntity entity, ContentItemUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "exerciseId" -> entity.setExerciseId(changes.getExerciseId());
                case "chapterId" -> entity.setChapterId(changes.getChapterId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "itemKind" -> entity.setItemKind(Optional.ofNullable(changes.getItemKind()).map(v -> v.getValue())
                        .orElse(null));
                case "lessonId" -> entity.setLessonId(changes.getLessonId());
                case "fileId" -> entity.setFileId(changes.getFileId());
                case "assignmentId" -> entity.setAssignmentId(changes.getAssignmentId());
                case "quizId" -> entity.setQuizId(changes.getQuizId());
                case "discussionPromptId" -> entity.setDiscussionPromptId(changes.getDiscussionPromptId());
                case "participationActivityId" -> entity.setParticipationActivityId(
                        changes.getParticipationActivityId());
                case "webResourceId" -> entity.setWebResourceId(changes.getWebResourceId());
                case "ebookId" -> entity.setEbookId(changes.getEbookId());
                case "externalToolResourceId" -> entity.setExternalToolResourceId(changes.getExternalToolResourceId());
                case "liveClassId" -> entity.setLiveClassId(changes.getLiveClassId());
                case "linkUrl" -> entity.setLinkUrl(changes.getLinkUrl());
                case "title" -> entity.setTitle(changes.getTitle());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                case "isAlwaysVisible" -> entity.setIsAlwaysVisible(changes.getIsAlwaysVisible());
                case "visibleFrom" -> entity.setVisibleFrom(changes.getVisibleFrom());
                case "visibleUntil" -> entity.setVisibleUntil(changes.getVisibleUntil());
                case "visibleUntilCohortEnd" -> entity.setVisibleUntilCohortEnd(changes.getVisibleUntilCohortEnd());
                case "attemptFrom" -> entity.setAttemptFrom(changes.getAttemptFrom());
                case "dueAt" -> entity.setDueAt(changes.getDueAt());
                case "attemptUntilCohortEnd" -> entity.setAttemptUntilCohortEnd(changes.getAttemptUntilCohortEnd());
                case "visibleStartDay" -> entity.setVisibleStartDay(changes.getVisibleStartDay());
                case "visibleStartTime" -> entity.setVisibleStartTime(WireValues.time(changes.getVisibleStartTime()));
                case "visibleEndDay" -> entity.setVisibleEndDay(changes.getVisibleEndDay());
                case "visibleEndTime" -> entity.setVisibleEndTime(WireValues.time(changes.getVisibleEndTime()));
                case "attemptStartDay" -> entity.setAttemptStartDay(changes.getAttemptStartDay());
                case "attemptStartTime" -> entity.setAttemptStartTime(WireValues.time(changes.getAttemptStartTime()));
                case "attemptEndDay" -> entity.setAttemptEndDay(changes.getAttemptEndDay());
                case "attemptEndTime" -> entity.setAttemptEndTime(WireValues.time(changes.getAttemptEndTime()));
                case "lateSubmissionAllowed" -> entity.setLateSubmissionAllowed(changes.getLateSubmissionAllowed());
                case "lateSubmissionDays" -> entity.setLateSubmissionDays(changes.getLateSubmissionDays());
                case "isGraded" -> entity.setIsGraded(changes.getIsGraded());
                case "points" -> entity.setPoints(WireValues.decimal(changes.getPoints()));
                case "weightPercent" -> entity.setWeightPercent(WireValues.decimal(changes.getWeightPercent()));
                case "gradingCategoryId" -> entity.setGradingCategoryId(changes.getGradingCategoryId());
                case "rubricId" -> entity.setRubricId(changes.getRubricId());
                case "plagiarismCheckRequired" -> entity.setPlagiarismCheckRequired(
                        changes.getPlagiarismCheckRequired());
                case "showInCalendar" -> entity.setShowInCalendar(changes.getShowInCalendar());
                case "allowDelete" -> entity.setAllowDelete(changes.getAllowDelete());
                case "allowEditDetails" -> entity.setAllowEditDetails(changes.getAllowEditDetails());
                case "allowEditDates" -> entity.setAllowEditDates(changes.getAllowEditDates());
                case "allowEditGrading" -> entity.setAllowEditGrading(changes.getAllowEditGrading());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ContentItemEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getItemKind() == null) {
            errors.add(new FieldError("itemKind", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsAlwaysVisible() == null) {
            errors.add(new FieldError("isAlwaysVisible", "must not be null", "NotNull"));
        }
        if (!creating && entity.getVisibleUntilCohortEnd() == null) {
            errors.add(new FieldError("visibleUntilCohortEnd", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAttemptUntilCohortEnd() == null) {
            errors.add(new FieldError("attemptUntilCohortEnd", "must not be null", "NotNull"));
        }
        if (!creating && entity.getLateSubmissionAllowed() == null) {
            errors.add(new FieldError("lateSubmissionAllowed", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsGraded() == null) {
            errors.add(new FieldError("isGraded", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("points", WireValues.decimal(entity.getPoints()), 8, 2, errors);
        WireValues.checkDecimal("weightPercent", WireValues.decimal(entity.getWeightPercent()), 5, 2, errors);
        if (!creating && entity.getPlagiarismCheckRequired() == null) {
            errors.add(new FieldError("plagiarismCheckRequired", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShowInCalendar() == null) {
            errors.add(new FieldError("showInCalendar", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowDelete() == null) {
            errors.add(new FieldError("allowDelete", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowEditDetails() == null) {
            errors.add(new FieldError("allowEditDetails", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowEditDates() == null) {
            errors.add(new FieldError("allowEditDates", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowEditGrading() == null) {
            errors.add(new FieldError("allowEditGrading", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentId() != null && !repository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCopiedFromId() != null && !repository.existsCopiedFromId(entity.getCopiedFromId())) {
            errors.add(new FieldError("copiedFromId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getDiscussionPromptId() != null
                && !repository.existsDiscussionPromptId(entity.getDiscussionPromptId())) {
            errors.add(new FieldError("discussionPromptId", "discussion prompt does not exist", "NOT_FOUND"));
        }
        if (entity.getEbookId() != null && !repository.existsEbookId(entity.getEbookId())) {
            errors.add(new FieldError("ebookId", "ebook does not exist", "NOT_FOUND"));
        }
        if (entity.getExerciseId() != null && !repository.existsExerciseId(entity.getExerciseId())) {
            errors.add(new FieldError("exerciseId", "programming exercise does not exist", "NOT_FOUND"));
        }
        if (entity.getExternalToolResourceId() != null
                && !repository.existsExternalToolResourceId(entity.getExternalToolResourceId())) {
            errors.add(new FieldError("externalToolResourceId", "external tool resource does not exist", "NOT_FOUND"));
        }
        if (entity.getGradingCategoryId() != null
                && !repository.existsGradingCategoryId(entity.getGradingCategoryId())) {
            errors.add(new FieldError("gradingCategoryId", "grading category does not exist", "NOT_FOUND"));
        }
        if (entity.getLessonId() != null && !repository.existsLessonId(entity.getLessonId())) {
            errors.add(new FieldError("lessonId", "lesson does not exist", "NOT_FOUND"));
        }
        if (entity.getLiveClassId() != null && !repository.existsLiveClassId(entity.getLiveClassId())) {
            errors.add(new FieldError("liveClassId", "live class does not exist", "NOT_FOUND"));
        }
        if (entity.getParticipationActivityId() != null
                && !repository.existsParticipationActivityId(entity.getParticipationActivityId())) {
            errors.add(new FieldError("participationActivityId", "participation activity does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizId() != null && !repository.existsQuizId(entity.getQuizId())) {
            errors.add(new FieldError("quizId", "quiz does not exist", "NOT_FOUND"));
        }
        if (entity.getRubricId() != null && !repository.existsRubricId(entity.getRubricId())) {
            errors.add(new FieldError("rubricId", "rubric does not exist", "NOT_FOUND"));
        }
        if (entity.getWebResourceId() != null && !repository.existsWebResourceId(entity.getWebResourceId())) {
            errors.add(new FieldError("webResourceId", "web resource does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ContentItemEntity entity) {

    }

    // ---- AssignmentGroup: a record of the ContentItem aggregate ----

    public AssignmentGroup getAssignmentGroup(UUID contentItemId, UUID id) {
        return doInReadTransaction(() -> {
            return assignmentGroupMapper.toDto(loadVisibleAssignmentGroup(DataScope.current(ASSIGNMENT_GROUP_PERMISSION_GET),
                    contentItemId, id));
        });
    }

    public Page<AssignmentGroup> listAssignmentGroups(UUID contentItemId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AssignmentGroupRepository.SORT_FIELDS, AssignmentGroupRepository.DEFAULT_ORDER,
                AssignmentGroupRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AssignmentGroupRepository.FILTER_FIELDS)
                .and(AssignmentGroupRepository.ofParent(contentItemId))
                        .and(AssignmentGroupRepository.scope(DataScope.current(ASSIGNMENT_GROUP_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + contentItemId, clock);
        return doInReadTransaction(() -> {
            Page<AssignmentGroupEntity> rows = assignmentGroupRepository.list(page, where);
            return rows.map(assignmentGroupMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AssignmentGroup createAssignmentGroup(UUID contentItemId, AssignmentGroup request, boolean validateOnly) {
        validator.validate(request);
        AssignmentGroupEntity entity = assignmentGroupMapper.toEntity(request);
        entity.setContentItemId(contentItemId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ASSIGNMENT_GROUP_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!assignmentGroupRepository.existsContentItemId(entity.getContentItemId())) {
                throw new ResourceNotFoundException("lms.content_item", entity.getContentItemId());
            }
            assertValidAssignmentGroup(entity, true);
            scope.requireWritable(institutionOfAssignmentGroup(entity));
            assertUniqueAssignmentGroup(entity);
            if (validateOnly) {
                return assignmentGroupMapper.toDto(entity);
            }
            AssignmentGroupEntity stored = assignmentGroupRepository.insert(entity);
            audit.recordCreate(ASSIGNMENT_GROUP_AGGREGATE, stored.getId());
            outbox.publish(ASSIGNMENT_GROUP_AGGREGATE + LmsEvents.CREATED, ASSIGNMENT_GROUP_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return assignmentGroupMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AssignmentGroup updateAssignmentGroup(UUID contentItemId, UUID id, String ifMatch,
            AssignmentGroupUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ASSIGNMENT_GROUP_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ASSIGNMENT_GROUP_PERMISSION_UPDATE);
            AssignmentGroupEntity entity = loadVisibleAssignmentGroup(scope, contentItemId, id);
            scope.requireWritable(institutionOfAssignmentGroup(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyAssignmentGroup(entity, changes, fields);
            validator.validate(assignmentGroupMapper.toDto(entity));
            assertValidAssignmentGroup(entity, false);
            scope.requireWritable(institutionOfAssignmentGroup(entity));
            assertUniqueAssignmentGroup(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return assignmentGroupMapper.toDto(entity);
            }
            if (assignmentGroupRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ASSIGNMENT_GROUP_AGGREGATE, id, fields);
            outbox.publish(ASSIGNMENT_GROUP_AGGREGATE + LmsEvents.UPDATED, ASSIGNMENT_GROUP_AGGREGATE, id, LmsEvents.payload(id));
            return assignmentGroupMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAssignmentGroup(UUID contentItemId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ASSIGNMENT_GROUP_PERMISSION_DELETE);
            AssignmentGroupEntity entity = loadVisibleAssignmentGroup(scope, contentItemId, id);
            scope.requireWritable(institutionOfAssignmentGroup(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (assignmentGroupRepository.referencedByAssignmentGroupMemberAssignmentGroupId(id)) {
                throw new InvalidStateException(
                        "The assignment group is still referenced by lms.assignment_group_member rows; remove those "
                                + "first.");
            }
            if (assignmentGroupRepository.referencedByAssignmentSubmissionGroupId(id)) {
                throw new InvalidStateException(
                        "The assignment group is still referenced by lms.assignment_submission rows; remove those "
                                + "first.");
            }
            if (assignmentGroupRepository.referencedByGradebookEntryGroupId(id)) {
                throw new InvalidStateException(
                        "The assignment group is still referenced by lms.gradebook_entry rows; remove those first.");
            }
            if (assignmentGroupRepository.referencedByItemOverrideGroupId(id)) {
                throw new InvalidStateException(
                        "The assignment group is still referenced by lms.item_override rows; remove those first.");
            }
            if (assignmentGroupRepository.referencedByPeerReviewAssignmentGroupId(id)) {
                throw new InvalidStateException(
                        "The assignment group is still referenced by lms.peer_review rows; remove those first.");
            }
            if (assignmentGroupRepository.referencedByReassignmentLogGroupId(id)) {
                throw new InvalidStateException(
                        "The assignment group is still referenced by lms.reassignment_log rows; remove those first.");
            }
            if (assignmentGroupRepository.referencedByRubricAssessmentGroupId(id)) {
                throw new InvalidStateException(
                        "The assignment group is still referenced by lms.rubric_assessment rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (assignmentGroupRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ASSIGNMENT_GROUP_AGGREGATE, id);
            outbox.publish(ASSIGNMENT_GROUP_AGGREGATE + LmsEvents.DELETED, ASSIGNMENT_GROUP_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AssignmentGroupEntity loadVisibleAssignmentGroup(DataScope scope, UUID contentItemId, UUID id) {
        AssignmentGroupEntity entity = assignmentGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_GROUP_AGGREGATE, id));
        if (!scope.canRead(institutionOfAssignmentGroup(entity)) || !contentItemId.equals(entity.getContentItemId())) {
            throw new ResourceNotFoundException(ASSIGNMENT_GROUP_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfAssignmentGroup(AssignmentGroupEntity entity) {
        return assignmentGroupRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected AssignmentGroupUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyAssignmentGroup(AssignmentGroupEntity entity, AssignmentGroupUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "name" -> entity.setName(changes.getName());
                case "points" -> entity.setPoints(WireValues.decimal(changes.getPoints()));
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                case "discussionTopicId" -> entity.setDiscussionTopicId(changes.getDiscussionTopicId());
                case "isReassigned" -> entity.setIsReassigned(changes.getIsReassigned());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAssignmentGroup(AssignmentGroupEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getContentItemId() == null) {
            errors.add(new FieldError("contentItemId", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("points", WireValues.decimal(entity.getPoints()), 8, 2, errors);
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsReassigned() == null) {
            errors.add(new FieldError("isReassigned", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !assignmentGroupRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getContentItemId() != null && !assignmentGroupRepository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getDiscussionTopicId() != null
                && !assignmentGroupRepository.existsDiscussionTopicId(entity.getDiscussionTopicId())) {
            errors.add(new FieldError("discussionTopicId", "forum topic does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAssignmentGroup(AssignmentGroupEntity entity) {
        if (assignmentGroupRepository.takenContentItemIdName1(entity)) {
            throw new DuplicateResourceException(
                    "A assignment group with the same content item id and name already exists.");
        }
    }

    // ---- ContentItemRating: a record of the ContentItem aggregate ----

    public ContentItemRating getContentItemRating(UUID contentItemId, UUID id) {
        return doInReadTransaction(() -> {
            return contentItemRatingMapper.toDto(loadVisibleContentItemRating(DataScope.current(
                    CONTENT_ITEM_RATING_PERMISSION_GET), contentItemId, id));
        });
    }

    public Page<ContentItemRating> listContentItemRatings(UUID contentItemId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ContentItemRatingRepository.SORT_FIELDS,
                        ContentItemRatingRepository.DEFAULT_ORDER,
                ContentItemRatingRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ContentItemRatingRepository.FILTER_FIELDS)
                .and(ContentItemRatingRepository.ofParent(contentItemId))
                        .and(ContentItemRatingRepository.scope(DataScope.current(CONTENT_ITEM_RATING_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + contentItemId, clock);
        return doInReadTransaction(() -> {
            Page<ContentItemRatingEntity> rows = contentItemRatingRepository.list(page, where);
            return rows.map(contentItemRatingMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ContentItemRating createContentItemRating(UUID contentItemId, ContentItemRating request,
            boolean validateOnly) {
        validator.validate(request);
        ContentItemRatingEntity entity = contentItemRatingMapper.toEntity(request);
        entity.setContentItemId(contentItemId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CONTENT_ITEM_RATING_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!contentItemRatingRepository.existsContentItemId(entity.getContentItemId())) {
                throw new ResourceNotFoundException("lms.content_item", entity.getContentItemId());
            }
            assertValidContentItemRating(entity, true);
            scope.requireWritable(institutionOfContentItemRating(entity));
            assertUniqueContentItemRating(entity);
            if (validateOnly) {
                return contentItemRatingMapper.toDto(entity);
            }
            ContentItemRatingEntity stored = contentItemRatingRepository.insert(entity);
            audit.recordCreate(CONTENT_ITEM_RATING_AGGREGATE, stored.getId());
            outbox.publish(CONTENT_ITEM_RATING_AGGREGATE + LmsEvents.CREATED, CONTENT_ITEM_RATING_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return contentItemRatingMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ContentItemRating updateContentItemRating(UUID contentItemId, UUID id, String ifMatch,
            ContentItemRatingUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CONTENT_ITEM_RATING_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONTENT_ITEM_RATING_PERMISSION_UPDATE);
            ContentItemRatingEntity entity = loadVisibleContentItemRating(scope, contentItemId, id);
            scope.requireWritable(institutionOfContentItemRating(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyContentItemRating(entity, changes, fields);
            validator.validate(contentItemRatingMapper.toDto(entity));
            assertValidContentItemRating(entity, false);
            scope.requireWritable(institutionOfContentItemRating(entity));
            assertUniqueContentItemRating(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return contentItemRatingMapper.toDto(entity);
            }
            if (contentItemRatingRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CONTENT_ITEM_RATING_AGGREGATE, id, fields);
            outbox.publish(CONTENT_ITEM_RATING_AGGREGATE + LmsEvents.UPDATED, CONTENT_ITEM_RATING_AGGREGATE, id,
                    LmsEvents.payload(id));
            return contentItemRatingMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteContentItemRating(UUID contentItemId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONTENT_ITEM_RATING_PERMISSION_DELETE);
            ContentItemRatingEntity entity = loadVisibleContentItemRating(scope, contentItemId, id);
            scope.requireWritable(institutionOfContentItemRating(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (contentItemRatingRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CONTENT_ITEM_RATING_AGGREGATE, id);
            outbox.publish(CONTENT_ITEM_RATING_AGGREGATE + LmsEvents.DELETED, CONTENT_ITEM_RATING_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ContentItemRatingEntity loadVisibleContentItemRating(DataScope scope, UUID contentItemId, UUID id) {
        ContentItemRatingEntity entity = contentItemRatingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_ITEM_RATING_AGGREGATE, id));
        if (!scope.canRead(institutionOfContentItemRating(entity)) || !contentItemId.equals(entity.getContentItemId())) {
            throw new ResourceNotFoundException(CONTENT_ITEM_RATING_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfContentItemRating(ContentItemRatingEntity entity) {
        return contentItemRatingRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected ContentItemRatingUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyContentItemRating(ContentItemRatingEntity entity, ContentItemRatingUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "userId" -> entity.setUserId(changes.getUserId());
                case "isLike" -> entity.setIsLike(changes.getIsLike());
                case "rating" -> entity.setRating(changes.getRating());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidContentItemRating(ContentItemRatingEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getContentItemId() == null) {
            errors.add(new FieldError("contentItemId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !contentItemRatingRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getContentItemId() != null && !contentItemRatingRepository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueContentItemRating(ContentItemRatingEntity entity) {
        if (contentItemRatingRepository.takenContentItemIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A content item rating with the same content item id and user id already exists.");
        }
    }

    // ---- ItemOverride: a record of the ContentItem aggregate ----

    public ItemOverride getItemOverride(UUID contentItemId, UUID id) {
        return doInReadTransaction(() -> {
            return itemOverrideMapper.toDto(loadVisibleItemOverride(DataScope.current(ITEM_OVERRIDE_PERMISSION_GET),
                    contentItemId, id));
        });
    }

    public Page<ItemOverride> listItemOverrides(UUID contentItemId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ItemOverrideRepository.SORT_FIELDS, ItemOverrideRepository.DEFAULT_ORDER,
                ItemOverrideRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ItemOverrideRepository.FILTER_FIELDS)
                .and(ItemOverrideRepository.ofParent(contentItemId))
                        .and(ItemOverrideRepository.scope(DataScope.current(ITEM_OVERRIDE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + contentItemId, clock);
        return doInReadTransaction(() -> {
            Page<ItemOverrideEntity> rows = itemOverrideRepository.list(page, where);
            return rows.map(itemOverrideMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ItemOverride createItemOverride(UUID contentItemId, ItemOverride request, boolean validateOnly) {
        validator.validate(request);
        ItemOverrideEntity entity = itemOverrideMapper.toEntity(request);
        entity.setContentItemId(contentItemId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ITEM_OVERRIDE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!itemOverrideRepository.existsContentItemId(entity.getContentItemId())) {
                throw new ResourceNotFoundException("lms.content_item", entity.getContentItemId());
            }
            if (!scope.canRead(institutionOfItemOverride(entity))) {
                throw new ResourceNotFoundException("lms.content_item", entity.getContentItemId());
            }
            assertValidItemOverride(entity, true);
            scope.requireWritable(institutionOfItemOverride(entity));
            assertUniqueItemOverride(entity);
            if (validateOnly) {
                return itemOverrideMapper.toDto(entity);
            }
            ItemOverrideEntity stored = itemOverrideRepository.insert(entity);
            audit.recordCreate(ITEM_OVERRIDE_AGGREGATE, stored.getId());
            outbox.publish(ITEM_OVERRIDE_AGGREGATE + LmsEvents.CREATED, ITEM_OVERRIDE_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return itemOverrideMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ItemOverride updateItemOverride(UUID contentItemId, UUID id, String ifMatch, ItemOverrideUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ITEM_OVERRIDE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ITEM_OVERRIDE_PERMISSION_UPDATE);
            ItemOverrideEntity entity = loadVisibleItemOverride(scope, contentItemId, id);
            scope.requireWritable(institutionOfItemOverride(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyItemOverride(entity, changes, fields);
            validator.validate(itemOverrideMapper.toDto(entity));
            assertValidItemOverride(entity, false);
            scope.requireWritable(institutionOfItemOverride(entity));
            assertUniqueItemOverride(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return itemOverrideMapper.toDto(entity);
            }
            if (itemOverrideRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ITEM_OVERRIDE_AGGREGATE, id, fields);
            outbox.publish(ITEM_OVERRIDE_AGGREGATE + LmsEvents.UPDATED, ITEM_OVERRIDE_AGGREGATE, id, LmsEvents.payload(id));
            return itemOverrideMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteItemOverride(UUID contentItemId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ITEM_OVERRIDE_PERMISSION_DELETE);
            ItemOverrideEntity entity = loadVisibleItemOverride(scope, contentItemId, id);
            scope.requireWritable(institutionOfItemOverride(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (itemOverrideRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ITEM_OVERRIDE_AGGREGATE, id);
            outbox.publish(ITEM_OVERRIDE_AGGREGATE + LmsEvents.DELETED, ITEM_OVERRIDE_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ItemOverrideEntity loadVisibleItemOverride(DataScope scope, UUID contentItemId, UUID id) {
        ItemOverrideEntity entity = itemOverrideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ITEM_OVERRIDE_AGGREGATE, id));
        if (!scope.canRead(institutionOfItemOverride(entity)) || !contentItemId.equals(entity.getContentItemId())) {
            throw new ResourceNotFoundException(ITEM_OVERRIDE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.content_item → lms.course it belongs to). */
    private Optional<UUID> institutionOfItemOverride(ItemOverrideEntity entity) {
        return itemOverrideRepository.institutionOf(entity.getContentItemId());
    }

    /**
     * Applies the selected ItemOverrideUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyItemOverride(ItemOverrideEntity entity, ItemOverrideUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "groupId" -> entity.setGroupId(changes.getGroupId());
                case "availableFrom" -> entity.setAvailableFrom(changes.getAvailableFrom());
                case "dueAt" -> entity.setDueAt(changes.getDueAt());
                case "extraAttempts" -> entity.setExtraAttempts(changes.getExtraAttempts());
                case "extraTimeMinutes" -> entity.setExtraTimeMinutes(changes.getExtraTimeMinutes());
                case "reason" -> entity.setReason(changes.getReason());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidItemOverride(ItemOverrideEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getContentItemId() == null) {
            errors.add(new FieldError("contentItemId", "must not be null", "NotNull"));
        }
        if (entity.getContentItemId() != null && !itemOverrideRepository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getGroupId() != null && !itemOverrideRepository.existsGroupId(entity.getGroupId())) {
            errors.add(new FieldError("groupId", "assignment group does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueItemOverride(ItemOverrideEntity entity) {

    }

    // ---- ReleaseGrant: a record of the ContentItem aggregate ----

    public ReleaseGrant getReleaseGrant(UUID contentItemId, UUID id) {
        return doInReadTransaction(() -> {
            return releaseGrantMapper.toDto(loadVisibleReleaseGrant(DataScope.current(RELEASE_GRANT_PERMISSION_GET),
                    contentItemId, id));
        });
    }

    public Page<ReleaseGrant> listReleaseGrants(UUID contentItemId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ReleaseGrantRepository.SORT_FIELDS, ReleaseGrantRepository.DEFAULT_ORDER,
                ReleaseGrantRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ReleaseGrantRepository.FILTER_FIELDS)
                .and(ReleaseGrantRepository.ofParent(contentItemId))
                        .and(ReleaseGrantRepository.scope(DataScope.current(RELEASE_GRANT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + contentItemId, clock);
        return doInReadTransaction(() -> {
            Page<ReleaseGrantEntity> rows = releaseGrantRepository.list(page, where);
            return rows.map(releaseGrantMapper::toDto);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteReleaseGrant(UUID contentItemId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELEASE_GRANT_PERMISSION_DELETE);
            ReleaseGrantEntity entity = loadVisibleReleaseGrant(scope, contentItemId, id);
            scope.requireWritable(institutionOfReleaseGrant(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (releaseGrantRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RELEASE_GRANT_AGGREGATE, id);
            outbox.publish(RELEASE_GRANT_AGGREGATE + LmsEvents.DELETED, RELEASE_GRANT_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ReleaseGrantEntity loadVisibleReleaseGrant(DataScope scope, UUID contentItemId, UUID id) {
        ReleaseGrantEntity entity = releaseGrantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RELEASE_GRANT_AGGREGATE, id));
        if (!scope.canRead(institutionOfReleaseGrant(entity)) || !contentItemId.equals(entity.getContentItemId())) {
            throw new ResourceNotFoundException(RELEASE_GRANT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.content_item → lms.course it belongs to). */
    private Optional<UUID> institutionOfReleaseGrant(ReleaseGrantEntity entity) {
        return releaseGrantRepository.institutionOf(entity.getContentItemId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidReleaseGrant(ReleaseGrantEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getContentItemId() == null) {
            errors.add(new FieldError("contentItemId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getChapterId() != null && !releaseGrantRepository.existsChapterId(entity.getChapterId())) {
            errors.add(new FieldError("chapterId", "chapter does not exist", "NOT_FOUND"));
        }
        if (entity.getContentItemId() != null && !releaseGrantRepository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueReleaseGrant(ReleaseGrantEntity entity) {
        if (releaseGrantRepository.takenContentItemIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A release grant with the same content item id and user id already exists.");
        }
    }
}
