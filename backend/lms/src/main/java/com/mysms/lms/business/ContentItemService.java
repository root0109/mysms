package com.mysms.lms.business;

import com.mysms.lms.api.model.ExternalLink;
import com.mysms.lms.api.model.DownloadLink;
import com.mysms.lms.api.model.Operation;
import com.mysms.lms.domain.LmsAssignmentSubmissionEntity;
import com.mysms.lms.errors.NotConfiguredException;
import com.mysms.lms.repository.LmsAssignmentSubmissionRepository;
import org.springframework.beans.factory.annotation.Value;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Objects;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.lms.repository.ReleaseRuleRepository;
import com.mysms.lms.repository.ReleaseRuleConditionRepository;
import com.mysms.lms.repository.ChapterRepository;
import com.mysms.lms.domain.ReleaseRuleEntity;
import com.mysms.lms.domain.ReleaseRuleConditionEntity;
import com.mysms.lms.domain.ChapterEntity;
import com.mysms.lms.api.model.SetContentItemVisibilityRequest;
import com.mysms.lms.api.model.ReleaseStatus;
import com.mysms.lms.api.model.ReleaseGrantList;
import com.mysms.lms.api.model.RecordItemViewRequest;
import com.mysms.lms.api.model.MoveContentItemRequest;
import com.mysms.lms.api.model.GrantItemAccessRequest;
import com.mysms.core.id.UuidV7;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ConflictException;
import com.mysms.authclient.AuthContext;
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
import com.mysms.lms.api.model.AddFromRepositoryRequest;
import com.mysms.lms.api.model.AssignmentGroup;
import com.mysms.lms.api.model.AssignmentGroupMember;
import com.mysms.lms.api.model.AssignmentGroupMemberList;
import com.mysms.lms.api.model.ContentItemList;
import com.mysms.lms.api.model.CopyContentItemRequest;
import com.mysms.lms.api.model.FinalizePeerReviewRequest;
import com.mysms.lms.api.model.GradebookEntry;
import com.mysms.lms.api.model.GradebookEntryList;
import com.mysms.lms.api.model.PeerPoints;
import com.mysms.lms.api.model.PeerReviewList;
import com.mysms.lms.api.model.PublishItemGradesRequest;
import com.mysms.lms.api.model.ReassignItemRequest;
import com.mysms.lms.api.model.ReassignmentLog;
import com.mysms.lms.api.model.ReassignmentLogList;
import com.mysms.lms.api.model.SetGroupMembersRequest;
import com.mysms.lms.api.model.SubmitPeerReviewRequest;
import com.mysms.lms.domain.AssignmentGroupMemberEntity;
import com.mysms.lms.domain.GradebookEntryEntity;
import com.mysms.lms.domain.PeerReviewEntity;
import com.mysms.lms.domain.ReassignmentLogEntity;
import com.mysms.lms.mapper.AssignmentGroupMemberMapper;
import com.mysms.lms.mapper.GradebookEntryMapper;
import com.mysms.lms.mapper.PeerReviewMapper;
import com.mysms.lms.mapper.ReassignmentLogMapper;
import com.mysms.lms.repository.AssignmentGroupMemberRepository;
import com.mysms.lms.repository.DiscussionPromptRepository;
import com.mysms.lms.repository.EbookRepository;
import com.mysms.lms.repository.GradebookEntryRepository;
import com.mysms.lms.repository.LessonRepository;
import com.mysms.lms.repository.LmsAssignmentRepository;
import com.mysms.lms.repository.ParticipationActivityRepository;
import com.mysms.lms.repository.PeerReviewRepository;
import com.mysms.lms.repository.QuizRepository;
import com.mysms.lms.repository.ReassignmentLogRepository;
import com.mysms.lms.repository.WebResourceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
 * Business service of lms.content_item and of the records that exist only as part of a ContentItem (AssignmentGroup,
 * ContentItemRating, ItemOverride, ReleaseGrant): audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class ContentItemService extends AppTransactionManager {

    public static final String AGGREGATE = ContentItemEntity.TABLE;
    /** The largest submitted file DownloadSubmissions packs (the archive is built in memory). */
    static final long MAX_ARCHIVED_FILE_BYTES = 100L * 1024 * 1024;
    static final String PERMISSION_LIST = "lms.content_item.read";
    static final String PERMISSION_GET = "lms.content_item.read";
    static final String PERMISSION_CREATE = "lms.content_item.create";
    static final String PERMISSION_UPDATE = "lms.content_item.update";
    static final String PERMISSION_DELETE = "lms.content_item.delete";
    static final String PERMISSION_SET_VISIBILITY = "lms.content_item.set_content_item_visibility";
    static final String PERMISSION_MOVE = "lms.content_item.move_content_item";
    static final String PERMISSION_GRANT_ACCESS = "lms.content_item.grant_item_access";
    static final String PERMISSION_COPY = "lms.content_item.copy_content_item";
    static final String PERMISSION_DOWNLOAD_SUBMISSIONS = "lms.content_item.download_submissions";
    static final String PERMISSION_ADD_FROM_REPOSITORY = "lms.content_item.add_from_repository";
    static final String PERMISSION_PUBLISH_GRADES = "lms.content_item.publish_item_grades";
    static final String PERMISSION_REASSIGN = "lms.content_item.reassign_item";
    static final String PERMISSION_SET_MEMBERS = "lms.assignment_group.set_group_members";
    static final String PERMISSION_FINALIZE_PEER_REVIEW = "lms.assignment_group.finalize_peer_review";
    /** At most this many students per manual grant (one request, one transaction). */
    static final int MAX_GRANT = 500;
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
    static final List<String> CONTENT_ITEM_RATING_WRITABLE = List.of("cohortId", "isLike", "rating");

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
    private final ChapterRepository chapters;
    private final ReleaseRuleRepository releaseRules;
    private final ReleaseRuleConditionRepository conditions;
    private final AssignmentGroupMemberRepository groupMembers;
    private final AssignmentGroupMemberMapper memberMapper;
    private final PeerReviewRepository peerReviews;
    private final PeerReviewMapper peerReviewMapper;
    private final GradebookEntryRepository gradebook;
    private final GradebookEntryMapper gradebookMapper;
    private final ReassignmentLogRepository reassignments;
    private final ReassignmentLogMapper reassignmentMapper;
    private final LessonRepository lessons;
    private final LmsAssignmentRepository assignments;
    private final DiscussionPromptRepository discussionPrompts;
    private final ParticipationActivityRepository participationActivities;
    private final WebResourceRepository webResources;
    private final QuizRepository quizzes;
    private final EbookRepository ebooks;
    private final LmsAssignmentSubmissionRepository submissions;
    private final OperationService operations;
    private final PlatformFiles files;
    private final String contentBaseUrl;

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
            ReleaseGrantMapper releaseGrantMapper,
            ChapterRepository chapters,
            ReleaseRuleRepository releaseRules,
            ReleaseRuleConditionRepository conditions,
            AssignmentGroupMemberRepository groupMembers,
            AssignmentGroupMemberMapper memberMapper,
            PeerReviewRepository peerReviews,
            PeerReviewMapper peerReviewMapper,
            GradebookEntryRepository gradebook,
            GradebookEntryMapper gradebookMapper,
            ReassignmentLogRepository reassignments,
            ReassignmentLogMapper reassignmentMapper,
            LessonRepository lessons,
            LmsAssignmentRepository assignments,
            DiscussionPromptRepository discussionPrompts,
            ParticipationActivityRepository participationActivities,
            WebResourceRepository webResources,
            QuizRepository quizzes,
            EbookRepository ebooks,
            LmsAssignmentSubmissionRepository submissions,
            OperationService operations,
            PlatformFiles files,
            @Value("${mysms.lms.scorm.content-base-url}") String contentBaseUrl) {
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
        this.chapters = chapters;
        this.releaseRules = releaseRules;
        this.conditions = conditions;
        this.groupMembers = groupMembers;
        this.memberMapper = memberMapper;
        this.peerReviews = peerReviews;
        this.peerReviewMapper = peerReviewMapper;
        this.gradebook = gradebook;
        this.gradebookMapper = gradebookMapper;
        this.reassignments = reassignments;
        this.reassignmentMapper = reassignmentMapper;
        this.lessons = lessons;
        this.assignments = assignments;
        this.discussionPrompts = discussionPrompts;
        this.participationActivities = participationActivities;
        this.webResources = webResources;
        this.quizzes = quizzes;
        this.ebooks = ebooks;
        this.submissions = submissions;
        this.operations = operations;
        this.files = files;
        this.contentBaseUrl = contentBaseUrl == null ? "" : contentBaseUrl.trim();
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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
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
    /**
     * lms.SetContentItemVisibility: shows or hides an item of a cohort (a course template item is shown by its
     * course: 409 NOT_COHORT_ITEM). A hidden item stays in place for teachers; students no longer see it.
     */
    public ContentItem setContentItemVisibility(UUID id, SetContentItemVisibilityRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_SET_VISIBILITY);
        return doInWriteTransaction(() -> {
            ContentItemEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getCohortId() == null) {
                throw new ConflictException("NOT_COHORT_ITEM", "Only an item of a cohort is shown or hidden.");
            }
            if (repository.setActive(id, request.getIsActive(), entity.getRowVersion(), now(),
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            String step = request.getIsActive() ? "show" : "hide";
            audit.recordTransition(AGGREGATE, id, step, List.of("isActive"), null);
            outbox.publish(AGGREGATE + "." + step, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * lms.MoveContentItem: puts the item at a place (0-based) in a chapter of the same course and outline (cohort);
     * the items from that place on move one down. Another course's or outline's chapter is 422.
     */
    public ContentItem moveContentItem(UUID id, MoveContentItemRequest request) {
        validator.validate(request);
        if (request.getPosition() < 0) {
            throw new ValidationFailedException(List.of(new FieldError("position", "must be 0 or more", "Min")));
        }
        DataScope scope = DataScope.current(PERMISSION_MOVE);
        return doInWriteTransaction(() -> {
            ContentItemEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            ChapterEntity target = chapters.findById(request.getTargetChapterId())
                    .filter(c -> c.getCourseId().equals(entity.getCourseId())
                            && Objects.equals(c.getCohortId(), entity.getCohortId()))
                    .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("targetChapterId",
                            "must be a chapter of the item's course and outline", "NOT_FOUND"))));
            if (repository.move(id, entity.getRowVersion(), target.getId(), request.getPosition(), now(),
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "move", List.of("chapterId", "sortOrder"), null);
            outbox.publish(AGGREGATE + ".move", AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * lms.RecordItemView: a student who follows the item's course or cohort (404 otherwise, as for an item they cannot
     * see) views it, and may like it — once: the like is their rating row, and the item's like count is recounted
     * from those rows, so repeated likes never inflate it.
     */
    public ContentItem recordItemView(UUID id, RecordItemViewRequest request) {
        UUID user = AuthContext.require().userId();
        boolean liked = request != null && Boolean.TRUE.equals(request.getLiked());
        UUID ratingId = UuidV7.next(clock);
        return doInWriteTransaction(() -> {
            ContentItemEntity entity = repository.findById(id)
                    .filter(e -> Boolean.TRUE.equals(e.getIsActive()) && repository.isLearnerOf(e, user))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            repository.countView(id, entity.getCohortId(), user, liked, ratingId, now());
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * lms.GrantItemAccess: releases the item to the listed students by hand, whatever its release rules say (or, with
     * {@code grant} false, takes the manual release back; what the rules released stays). One row per item and
     * student; at most 500 students per call.
     */
    public ReleaseGrantList grantItemAccess(UUID id, GrantItemAccessRequest request) {
        validator.validate(request);
        List<UUID> users = request.getUserIds().stream().distinct().toList();
        if (users.isEmpty() || users.size() > MAX_GRANT) {
            throw new ValidationFailedException(List.of(new FieldError("userIds", "name 1 to 500 students", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_GRANT_ACCESS);
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            ContentItemEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            for (UUID user : users) {
                releaseGrantRepository.grant(UuidV7.next(clock), tenant, id, entity.getChapterId(), user,
                        request.getGrant(), now, actor);
            }
            audit.recordTransition(AGGREGATE, id, request.getGrant() ? "grant_access" : "revoke_access",
                    List.of("releaseGrants"), null);
            outbox.publish(AGGREGATE + ".grant_access", AGGREGATE, id, LmsEvents.payload(id));
            return new ReleaseGrantList(users.stream().map(u -> releaseGrantRepository.findOwn(id, u))
                    .flatMap(Optional::stream).map(releaseGrantMapper::toDto).toList());
        });
    }

    /**
     * lms.GetReleaseStatus: whether the item is released to the caller, or to a named student for a reader of the
     * item. An item no enabled rule holds back is released; otherwise the student's release grant decides, and the
     * holding rules' conditions are listed.
     */
    public ReleaseStatus getReleaseStatus(UUID id, UUID userId) {
        UUID caller = AuthContext.require().userId();
        UUID student = userId == null ? caller : userId;
        return doInReadTransaction(() -> {
            ContentItemEntity entity;
            if (student.equals(caller)) {
                entity = repository.findById(id).filter(e -> repository.isLearnerOf(e, caller)
                        || DataScope.current(PERMISSION_GET).canRead(institutionOf(e)) && PublicReads.member(
                                PERMISSION_GET))
                        .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            } else {
                if (!PublicReads.member(PERMISSION_GET)) {
                    throw new PermissionDeniedException("Another student's release status needs " + PERMISSION_GET
                            + ".");
                }
                entity = loadVisible(DataScope.current(PERMISSION_GET), id);
            }
            List<ReleaseRuleEntity> holding = releaseRules.releasing(id, entity.getChapterId());
            Optional<ReleaseGrantEntity> record = releaseGrantRepository.findOwn(id, student);
            boolean granted = record.map(r -> Boolean.TRUE.equals(r.getAccessGranted())).orElse(false);
            boolean released = holding.isEmpty() || granted
                    || record.map(r -> Boolean.TRUE.equals(r.getIsReleased())).orElse(false);
            ReleaseStatus status = new ReleaseStatus(id, released);
            status.setReason(holding.isEmpty() ? "no release rule holds the item back"
                    : granted ? "access granted by a teacher" : released ? "released by its rules"
                    : "waiting for its release rules");
            status.setPendingConditions(released ? List.of() : conditions.ofRules(holding.stream()
                    .map(ReleaseRuleEntity::getId).toList()).stream().map(ContentItemService::describe).toList());
            return status;
        });
    }


    /**
     * lms.CopyContentItem: duplicates the item into a chapter of a course or outline the caller may write, at the
     * end of that chapter. A private (non-repository) lesson, assignment, web resource, discussion prompt or
     * participation activity is duplicated with it; every other kind keeps its underlying reference.
     */
    public ContentItem copyContentItem(UUID id, CopyContentItemRequest request) {
        DataScope scope = DataScope.current(PERMISSION_COPY);
        return doInWriteTransaction(() -> {
            ContentItemEntity source = loadVisible(scope, id);
            scope.requireWritable(institutionOf(source));
            validator.validate(request);
            ChapterEntity target = chapters.findById(request.getTargetChapterId())
                    .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("targetChapterId",
                            "must be a chapter the caller may write", "NOT_FOUND"))));
            if (!scope.canRead(repository.institutionOf(target.getCourseId()))) {
                throw new ValidationFailedException(List.of(new FieldError("targetChapterId",
                        "must be a chapter the caller may write", "NOT_FOUND")));
            }
            scope.requireWritable(repository.institutionOf(target.getCourseId()));
            ContentItemEntity copy = copyOf(source, target);
            copy.onCreatingData(Actor.current(), clock);
            copy.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
            duplicatePrivateAggregate(source, copy, target);
            copy.setSortOrder(repository.nextSortOrder(target.getId()));
            ContentItemEntity stored = repository.insert(copy);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + ".copy", AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /** The placement fields of the copy; counts, ratings and audit fields start fresh. */
    private static ContentItemEntity copyOf(ContentItemEntity s, ChapterEntity target) {
        return ContentItemEntity.builder()
                .chapterId(target.getId()).courseId(target.getCourseId()).cohortId(target.getCohortId())
                .itemKind(s.getItemKind()).lessonId(s.getLessonId()).fileId(s.getFileId())
                .assignmentId(s.getAssignmentId()).quizId(s.getQuizId())
                .discussionPromptId(s.getDiscussionPromptId())
                .participationActivityId(s.getParticipationActivityId()).webResourceId(s.getWebResourceId())
                .ebookId(s.getEbookId()).externalToolResourceId(s.getExternalToolResourceId())
                .liveClassId(s.getLiveClassId()).linkUrl(s.getLinkUrl()).title(s.getTitle())
                .isActive(s.getIsActive()).isAlwaysVisible(s.getIsAlwaysVisible()).visibleFrom(s.getVisibleFrom())
                .visibleUntil(s.getVisibleUntil()).visibleUntilCohortEnd(s.getVisibleUntilCohortEnd())
                .attemptFrom(s.getAttemptFrom()).dueAt(s.getDueAt())
                .attemptUntilCohortEnd(s.getAttemptUntilCohortEnd()).visibleStartDay(s.getVisibleStartDay())
                .visibleStartTime(s.getVisibleStartTime()).visibleEndDay(s.getVisibleEndDay())
                .visibleEndTime(s.getVisibleEndTime()).attemptStartDay(s.getAttemptStartDay())
                .attemptStartTime(s.getAttemptStartTime()).attemptEndDay(s.getAttemptEndDay())
                .attemptEndTime(s.getAttemptEndTime()).lateSubmissionAllowed(s.getLateSubmissionAllowed())
                .lateSubmissionDays(s.getLateSubmissionDays()).isGraded(s.getIsGraded()).points(s.getPoints())
                .weightPercent(s.getWeightPercent()).gradingCategoryId(s.getGradingCategoryId())
                .rubricId(s.getRubricId()).plagiarismCheckRequired(s.getPlagiarismCheckRequired())
                .showInCalendar(s.getShowInCalendar()).allowDelete(s.getAllowDelete())
                .allowEditDetails(s.getAllowEditDetails()).allowEditDates(s.getAllowEditDates())
                .allowEditGrading(s.getAllowEditGrading()).exerciseId(s.getExerciseId())
                .copiedFromId(s.getId()).isOriginal(false)
                .viewCount(0L).likeCount(0L).ratingTotal(java.math.BigDecimal.ZERO).ratingCount(0)
                .build();
    }

    /** Duplicates the copy's underlying aggregate when it is private to its course (not in the repository). */
    private void duplicatePrivateAggregate(ContentItemEntity source, ContentItemEntity copy, ChapterEntity target) {
        switch (source.getItemKind()) {
            case "lesson" -> lessons.findById(source.getLessonId()).ifPresent(lesson -> {
                lesson.setChapterId(target.getId());
                lesson.onCreatingData(Actor.current(), clock);
                lessons.insert(lesson);
                copy.setLessonId(lesson.getId());
            });
            case "assignment" -> assignments.findById(source.getAssignmentId())
                    .filter(a -> !Boolean.TRUE.equals(a.getIsInRepository())).ifPresent(a -> {
                        a.onCreatingData(Actor.current(), clock);
                        copy.setAssignmentId(assignments.insert(a).getId());
                    });
            case "web_resource" -> webResources.findById(source.getWebResourceId())
                    .filter(w -> !Boolean.TRUE.equals(w.getIsInRepository())).ifPresent(w -> {
                        w.onCreatingData(Actor.current(), clock);
                        copy.setWebResourceId(webResources.insert(w).getId());
                    });
            case "discussion" -> discussionPrompts.findById(source.getDiscussionPromptId())
                    .filter(d -> !Boolean.TRUE.equals(d.getIsInRepository())).ifPresent(d -> {
                        d.onCreatingData(Actor.current(), clock);
                        copy.setDiscussionPromptId(discussionPrompts.insert(d).getId());
                    });
            case "participation" -> participationActivities.findById(source.getParticipationActivityId())
                    .filter(d -> !Boolean.TRUE.equals(d.getIsInRepository())).ifPresent(d -> {
                        d.onCreatingData(Actor.current(), clock);
                        copy.setParticipationActivityId(participationActivities.insert(d).getId());
                    });
            default -> { }
        }
    }

    /**
     * lms.AddFromRepository: places shared repository items (quiz, assignment, discussion, participation,
     * web_resource or ebook) in a chapter; the placements reference the shared aggregates.
     */
    public ContentItemList addFromRepository(AddFromRepositoryRequest request) {
        validator.validate(request);
        List<UUID> sources = request.getSourceIds().stream().distinct().toList();
        if (sources.isEmpty()) {
            throw new ValidationFailedException(List.of(new FieldError("sourceIds", "name at least one repository "
                    + "item", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_ADD_FROM_REPOSITORY);
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            ChapterEntity chapter = chapters.findById(request.getChapterId())
                    .filter(c -> scope.canRead(repository.institutionOf(c.getCourseId())))
                    .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("chapterId",
                            "must be a chapter the caller may write", "NOT_FOUND"))));
            scope.requireWritable(repository.institutionOf(chapter.getCourseId()));
            int place = repository.nextSortOrder(chapter.getId());
            List<ContentItem> placed = new ArrayList<>();
            for (UUID sourceId : sources) {
                ContentItemEntity item = fromRepository(request.getItemKind(), sourceId);
                item.setChapterId(chapter.getId());
                item.setCourseId(chapter.getCourseId());
                item.setCohortId(chapter.getCohortId());
                item.setSortOrder(place++);
                item.setIsOriginal(false);
                item.onCreatingData(Actor.current(), clock);
                item.setTenantId(tenant);
                ContentItemEntity stored = repository.insert(item);
                audit.recordCreate(AGGREGATE, stored.getId());
                outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(),
                        LmsEvents.payload(stored.getId()));
                placed.add(mapper.toDto(stored));
            }
            return new ContentItemList(placed);
        });
    }

    /** A placement of one shared repository aggregate; 422 when it does not exist or is not shared. */
    private ContentItemEntity fromRepository(String kind, UUID sourceId) {
        FieldError wrongSource = new FieldError("sourceIds", sourceId + " is not a shared " + kind
                + " of the repository", "NOT_FOUND");
        ContentItemEntity.ContentItemEntityBuilder<?, ?> item = ContentItemEntity.builder().itemKind(kind);
        switch (kind) {
            case "quiz" -> {
                var source = quizzes.findById(sourceId).filter(q -> Boolean.TRUE.equals(q.getIsInRepository()))
                        .orElseThrow(() -> new ValidationFailedException(List.of(wrongSource)));
                item.quizId(sourceId).title(source.getTitle());
            }
            case "assignment" -> {
                var source = assignments.findById(sourceId).filter(a -> Boolean.TRUE.equals(a.getIsInRepository()))
                        .orElseThrow(() -> new ValidationFailedException(List.of(wrongSource)));
                item.assignmentId(sourceId).title(source.getTitle());
            }
            case "discussion" -> {
                var source = discussionPrompts.findById(sourceId)
                        .filter(d -> Boolean.TRUE.equals(d.getIsInRepository()))
                        .orElseThrow(() -> new ValidationFailedException(List.of(wrongSource)));
                item.discussionPromptId(sourceId).title(source.getTitle());
            }
            case "participation" -> {
                var source = participationActivities.findById(sourceId)
                        .filter(d -> Boolean.TRUE.equals(d.getIsInRepository()))
                        .orElseThrow(() -> new ValidationFailedException(List.of(wrongSource)));
                item.participationActivityId(sourceId).title(source.getTitle());
            }
            case "web_resource" -> {
                var source = webResources.findById(sourceId)
                        .filter(w -> Boolean.TRUE.equals(w.getIsInRepository()))
                        .orElseThrow(() -> new ValidationFailedException(List.of(wrongSource)));
                item.webResourceId(sourceId).title(source.getTitle());
            }
            case "ebook" -> {
                var source = ebooks.findById(sourceId).filter(e -> Boolean.TRUE.equals(e.getIsInRepository()))
                        .orElseThrow(() -> new ValidationFailedException(List.of(wrongSource)));
                item.ebookId(sourceId).title(source.getTitle());
            }
            default -> throw new ValidationFailedException(List.of(new FieldError("itemKind",
                    "must be quiz, assignment, discussion, participation, web_resource or ebook", "Pattern")));
        }
        return item.build();
    }

    /**
     * lms.RequestToJoinGroup: a student of the item's cohort asks to join a group (status requested). A student
     * already active in a group of the item, or already asking here, is refused.
     */
    public AssignmentGroupMember requestToJoinGroup(UUID itemId, UUID groupId) {
        UUID user = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            ContentItemEntity item = repository.findById(itemId).filter(e -> repository.isLearnerOf(e, user))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, itemId));
            AssignmentGroupEntity group = groupOf(item, groupId);
            if (groupMembers.activeForItem(itemId, user).isPresent()) {
                throw new ConflictException("ALREADY_IN_GROUP", "Leave the current group first.");
            }
            Optional<AssignmentGroupMemberEntity> existing = groupMembers.findInGroup(groupId, user);
            AssignmentGroupMemberEntity row;
            if (existing.isPresent()) {
                row = existing.get();
                if ("requested".equals(row.getStatus())) {
                    throw new ConflictException("ALREADY_REQUESTED", "The join request is already waiting.");
                }
                if (groupMembers.setStatus(row.getId(), "requested", row.getRowVersion(), now(),
                        Actor.current().orElse(null)) == 0) {
                    throw new PreconditionFailedException("The row changed since it was read.");
                }
            } else {
                row = AssignmentGroupMemberEntity.builder().assignmentGroupId(groupId).userId(user)
                        .contentItemId(itemId).status("requested").build();
                row.onCreatingData(Actor.current(), clock);
                row.setTenantId(tenant);
                row = groupMembers.insertWithStatus(row);
            }
            UUID memberId = row.getId();
            audit.recordTransition(AssignmentGroupMemberEntity.TABLE, memberId, "request_to_join", List.of("status"),
                    null);
            outbox.publish(AssignmentGroupMemberEntity.TABLE + ".request_to_join", AssignmentGroupMemberEntity.TABLE,
                    memberId, LmsEvents.payload(memberId));
            return memberMapper.toDto(groupMembers.findById(memberId).orElseThrow(
                    () -> new ResourceNotFoundException(AssignmentGroupMemberEntity.TABLE, memberId)));
        });
    }

    /**
     * lms.SetGroupMembers: the teacher adds and removes members. An added student joins as active (a waiting
     * request is accepted); one active in another group of the item is refused; a removed row is marked removed.
     */
    public AssignmentGroupMemberList setGroupMembers(UUID itemId, UUID groupId, SetGroupMembersRequest request) {
        validator.validate(request);
        List<UUID> add = request.getAddUserIds() == null ? List.of()
                : request.getAddUserIds().stream().distinct().toList();
        List<UUID> remove = request.getRemoveUserIds() == null ? List.of()
                : request.getRemoveUserIds().stream().distinct().toList();
        DataScope scope = DataScope.current(PERMISSION_SET_MEMBERS);
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            ContentItemEntity item = loadVisible(scope, itemId);
            scope.requireWritable(institutionOf(item));
            groupOf(item, groupId);
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            for (UUID user : add) {
                AssignmentGroupMemberEntity other = groupMembers.activeForItem(itemId, user).orElse(null);
                if (other != null && !groupId.equals(other.getAssignmentGroupId())) {
                    throw new ConflictException("ALREADY_IN_GROUP",
                            "Student " + user + " is active in another group of the item.");
                }
                Optional<AssignmentGroupMemberEntity> existing = groupMembers.findInGroup(groupId, user);
                if (existing.isPresent()) {
                    AssignmentGroupMemberEntity row = existing.get();
                    if (!"active".equals(row.getStatus())
                            && groupMembers.setStatus(row.getId(), "active", row.getRowVersion(), now, actor) == 0) {
                        throw new PreconditionFailedException("The row changed since it was read.");
                    }
                } else {
                    AssignmentGroupMemberEntity row = AssignmentGroupMemberEntity.builder().assignmentGroupId(groupId)
                            .userId(user).contentItemId(itemId).status("active").build();
                    row.onCreatingData(Actor.current(), clock);
                    row.setTenantId(tenant);
                    groupMembers.insertWithStatus(row);
                }
            }
            for (UUID user : remove) {
                groupMembers.findInGroup(groupId, user)
                        .filter(row -> !"removed".equals(row.getStatus()))
                        .ifPresent(row -> {
                            if (groupMembers.setStatus(row.getId(), "removed", row.getRowVersion(), now, actor) == 0) {
                                throw new PreconditionFailedException("The row changed since it was read.");
                            }
                        });
            }
            audit.recordTransition(ASSIGNMENT_GROUP_AGGREGATE, groupId, "set_members", List.of("members"), null);
            outbox.publish(ASSIGNMENT_GROUP_AGGREGATE + ".set_members", ASSIGNMENT_GROUP_AGGREGATE, groupId,
                    LmsEvents.payload(groupId));
            return new AssignmentGroupMemberList(groupMembers.ofGroup(groupId).stream()
                    .map(memberMapper::toDto).toList());
        });
    }

    /**
     * lms.SubmitPeerReview: an active member records points for each other active member of their group; a review
     * written before is replaced. Reviewing oneself or an outsider is 422.
     */
    public PeerReviewList submitPeerReview(UUID itemId, UUID groupId, SubmitPeerReviewRequest request) {
        validator.validate(request);
        UUID user = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        List<FieldError> wire = new ArrayList<>();
        for (PeerPoints review : request.getReviews()) {
            WireValues.checkDecimal("reviews.points", review.getPoints(), 8, 2, wire);
        }
        if (request.getReviews().isEmpty()) {
            wire.add(new FieldError("reviews", "review at least one member", "Size"));
        }
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            ContentItemEntity item = repository.findById(itemId).filter(e -> repository.isLearnerOf(e, user))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, itemId));
            groupOf(item, groupId);
            if (groupMembers.findInGroup(groupId, user).filter(m -> "active".equals(m.getStatus())).isEmpty()) {
                throw new ResourceNotFoundException(ASSIGNMENT_GROUP_AGGREGATE, groupId);
            }
            Set<UUID> members = groupMembers.ofGroup(groupId).stream()
                    .filter(m -> "active".equals(m.getStatus())).map(AssignmentGroupMemberEntity::getUserId)
                    .collect(Collectors.toSet());
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            for (PeerPoints review : request.getReviews()) {
                if (user.equals(review.getUserId()) || !members.contains(review.getUserId())) {
                    throw new ValidationFailedException(List.of(new FieldError("reviews.userId",
                            "must name another active member of the group", "NOT_FOUND")));
                }
                BigDecimal points = WireValues.decimal(review.getPoints());
                Optional<PeerReviewEntity> existing = peerReviews.findReview(groupId, user, review.getUserId());
                if (existing.isPresent()) {
                    if (peerReviews.setPoints(existing.get().getId(), points, existing.get().getRowVersion(), now,
                            actor) == 0) {
                        throw new PreconditionFailedException("The row changed since it was read.");
                    }
                } else {
                    PeerReviewEntity row = PeerReviewEntity.builder().assignmentGroupId(groupId).reviewerUserId(user)
                            .revieweeUserId(review.getUserId()).points(points).build();
                    row.onCreatingData(Actor.current(), clock);
                    row.setTenantId(tenant);
                    peerReviews.insert(row);
                }
            }
            audit.recordTransition(ASSIGNMENT_GROUP_AGGREGATE, groupId, "submit_peer_review", List.of("peerReviews"),
                    null);
            outbox.publish(ASSIGNMENT_GROUP_AGGREGATE + ".submit_peer_review", ASSIGNMENT_GROUP_AGGREGATE, groupId,
                    LmsEvents.payload(groupId));
            return new PeerReviewList(peerReviews.ofReviewer(groupId, user).stream()
                    .map(peerReviewMapper::toDto).toList());
        });
    }

    /**
     * lms.FinalizePeerReview: writes each active member's peer points into their gradebook entry for the item — the
     * teacher's figure when one is passed, otherwise the mean of the points the member received.
     */
    public GradebookEntryList finalizePeerReview(UUID itemId, UUID groupId, FinalizePeerReviewRequest request) {
        validator.validate(request);
        List<PeerPoints> named = request.getPoints() == null ? List.of() : request.getPoints();
        List<FieldError> wire = new ArrayList<>();
        for (PeerPoints points : named) {
            WireValues.checkDecimal("points.points", points.getPoints(), 8, 2, wire);
        }
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        DataScope scope = DataScope.current(PERMISSION_FINALIZE_PEER_REVIEW);
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            ContentItemEntity item = loadVisible(scope, itemId);
            scope.requireWritable(institutionOf(item));
            AssignmentGroupEntity group = groupOf(item, groupId);
            Map<UUID, BigDecimal> override = new LinkedHashMap<>();
            for (PeerPoints points : named) {
                override.put(points.getUserId(), WireValues.decimal(points.getPoints()));
            }
            Map<UUID, List<BigDecimal>> received = new LinkedHashMap<>();
            for (PeerReviewEntity review : peerReviews.ofGroup(groupId)) {
                if (review.getPoints() != null) {
                    received.computeIfAbsent(review.getRevieweeUserId(), k -> new ArrayList<>())
                            .add(review.getPoints());
                }
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            List<GradebookEntry> entries = new ArrayList<>();
            for (AssignmentGroupMemberEntity member : groupMembers.ofGroup(groupId)) {
                if (!"active".equals(member.getStatus())) {
                    continue;
                }
                BigDecimal points = override.get(member.getUserId());
                if (points == null) {
                    List<BigDecimal> got = received.getOrDefault(member.getUserId(), List.of());
                    points = got.isEmpty() ? null : got.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(got.size()), 2, RoundingMode.HALF_UP);
                }
                GradebookEntryEntity entry = gradebook.ofItemAndUser(itemId, member.getUserId()).orElse(null);
                if (entry == null) {
                    entry = GradebookEntryEntity.builder().cohortId(group.getCohortId()).contentItemId(itemId)
                            .userId(member.getUserId()).peerPoints(points).status("not_submitted")
                            .isPublished(false).build();
                    entry.onCreatingData(Actor.current(), clock);
                    entry.setTenantId(tenant);
                    entry = gradebook.insert(entry);
                }
                if (gradebook.setPeerPoints(entry.getId(), points, entry.getRowVersion(), now, actor) == 0) {
                    throw new PreconditionFailedException("The row changed since it was read.");
                }
                UUID entryId = entry.getId();
                entries.add(gradebookMapper.toDto(gradebook.findById(entryId).orElseThrow(
                        () -> new ResourceNotFoundException(GradebookEntryEntity.TABLE, entryId))));
            }
            audit.recordTransition(ASSIGNMENT_GROUP_AGGREGATE, groupId, "finalize_peer_review", List.of("peerPoints"),
                    null);
            outbox.publish(ASSIGNMENT_GROUP_AGGREGATE + ".finalize_peer_review", ASSIGNMENT_GROUP_AGGREGATE, groupId,
                    LmsEvents.payload(groupId));
            return new GradebookEntryList(entries);
        });
    }

    /**
     * lms.PublishItemGrades: shows the item's graded entries to their students — every one of them, or only the
     * named students' — and returns the rows it touched.
     */
    public GradebookEntryList publishItemGrades(UUID itemId, PublishItemGradesRequest request) {
        validator.validate(request);
        List<UUID> users = request == null || request.getUserIds() == null ? List.of()
                : request.getUserIds().stream().distinct().toList();
        DataScope scope = DataScope.current(PERMISSION_PUBLISH_GRADES);
        return doInWriteTransaction(() -> {
            ContentItemEntity item = loadVisible(scope, itemId);
            scope.requireWritable(institutionOf(item));
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            List<GradebookEntry> published = new ArrayList<>();
            for (GradebookEntryEntity entry : gradebook.ofItem(itemId, users)) {
                if (!"graded".equals(entry.getStatus())) {
                    continue;
                }
                if (!Boolean.TRUE.equals(entry.getIsPublished())
                        && gradebook.publish(entry.getId(), entry.getRowVersion(), now, actor) == 0) {
                    throw new PreconditionFailedException("The row changed since it was read.");
                }
                UUID entryId = entry.getId();
                published.add(gradebookMapper.toDto(gradebook.findById(entryId).orElseThrow(
                        () -> new ResourceNotFoundException(GradebookEntryEntity.TABLE, entryId))));
            }
            audit.recordTransition(AGGREGATE, itemId, "publish_item_grades", List.of("isPublished"), null);
            outbox.publish(AGGREGATE + ".publish_item_grades", AGGREGATE, itemId, LmsEvents.payload(itemId));
            return new GradebookEntryList(published);
        });
    }

    /**
     * lms.ReassignItem: gives the named students and groups a fresh attempt — their entries go back to reassigned,
     * the step is kept in the reassignment log, and new dates become an item override.
     */
    public ReassignmentLogList reassignItem(UUID itemId, ReassignItemRequest request) {
        validator.validate(request);
        List<UUID> users = request.getUserIds() == null ? List.of()
                : request.getUserIds().stream().distinct().toList();
        List<UUID> groups = request.getGroupIds() == null ? List.of()
                : request.getGroupIds().stream().distinct().toList();
        if (users.isEmpty() && groups.isEmpty()) {
            throw new ValidationFailedException(List.of(new FieldError("userIds",
                    "name at least one student or group", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_REASSIGN);
        UUID caller = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            ContentItemEntity item = loadVisible(scope, itemId);
            scope.requireWritable(institutionOf(item));
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            List<ReassignmentLog> logged = new ArrayList<>();
            for (UUID user : users) {
                gradebook.ofItemAndUser(itemId, user).ifPresent(entry -> {
                    if (gradebook.reassign(entry.getId(), entry.getRowVersion(), now, actor) == 0) {
                        throw new PreconditionFailedException("The row changed since it was read.");
                    }
                });
                logged.add(logReassignment(itemId, user, null, request, now, caller, tenant));
            }
            for (UUID groupId : groups) {
                AssignmentGroupEntity group = groupOf(item, groupId);
                gradebook.ofItemAndGroup(itemId, groupId).ifPresent(entry -> {
                    if (gradebook.reassign(entry.getId(), entry.getRowVersion(), now, actor) == 0) {
                        throw new PreconditionFailedException("The row changed since it was read.");
                    }
                });
                assignmentGroupRepository.markReassigned(group.getId(), now, actor);
                logged.add(logReassignment(itemId, null, groupId, request, now, caller, tenant));
            }
            audit.recordTransition(AGGREGATE, itemId, "reassign", List.of("gradebookEntries"), null);
            outbox.publish(AGGREGATE + ".reassign", AGGREGATE, itemId, LmsEvents.payload(itemId));
            return new ReassignmentLogList(logged);
        });
    }

    /** One reassignment-log row (and the override its dates call for). */
    private ReassignmentLog logReassignment(UUID itemId, UUID userId, UUID groupId, ReassignItemRequest request,
            OffsetDateTime now, UUID caller, UUID tenant) {
        ReassignmentLogEntity row = ReassignmentLogEntity.builder().contentItemId(itemId).userId(userId)
                .groupId(groupId).comment(request.getComment()).reassignedBy(caller).reassignedAt(now).build();
        row.onCreatingData(Actor.current(), clock);
        row.setTenantId(tenant);
        ReassignmentLogEntity stored = reassignments.insert(row);
        if (request.getAvailableFrom() != null || request.getDueAt() != null) {
            ItemOverrideEntity dates = ItemOverrideEntity.builder().contentItemId(itemId).userId(userId)
                    .groupId(groupId).availableFrom(request.getAvailableFrom()).dueAt(request.getDueAt())
                    .reason("reassigned").build();
            dates.onCreatingData(Actor.current(), clock);
            dates.setTenantId(tenant);
            itemOverrideRepository.insert(dates);
        }
        return reassignmentMapper.toDto(stored);
    }


    /**
     * lms.GetWebResourceLaunch: where a web resource, link, e-book or file item opens — the resource's own address,
     * the content host's copy of an uploaded package, or a signed short-lived link of the stored file.
     */
    public ExternalLink getWebResourceLaunch(UUID id) {
        UUID user = AuthContext.require().userId();
        record Target(String url, UUID fileId) {
        }
        Target target = doInReadTransaction(() -> {
            ContentItemEntity entity = repository.findById(id)
                    .filter(e -> repository.isLearnerOf(e, user)
                            || PublicReads.member(PERMISSION_GET)
                                    && DataScope.current(PERMISSION_GET).canRead(institutionOf(e)))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            return switch (entity.getItemKind()) {
                case "link" -> new Target(entity.getLinkUrl(), null);
                case "file" -> new Target(null, entity.getFileId());
                case "ebook" -> {
                    var ebook = ebooks.findById(entity.getEbookId())
                            .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
                    if (ebook.getFileId() == null) {
                        throw new ConflictException("EBOOK_NOT_READY", "The e-book has no converted file yet.");
                    }
                    yield new Target(null, ebook.getFileId());
                }
                case "web_resource" -> {
                    var resource = webResources.findById(entity.getWebResourceId())
                            .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
                    if ("url".equals(resource.getResourceType())) {
                        yield new Target(resource.getUrl(), null);
                    }
                    if (contentBaseUrl.isEmpty()) {
                        throw new NotConfiguredException("No host serves uploaded packages in this deployment.");
                    }
                    String base = contentBaseUrl.endsWith("/")
                            ? contentBaseUrl.substring(0, contentBaseUrl.length() - 1) : contentBaseUrl;
                    yield new Target(base + "/web-resources/" + resource.getId() + "/"
                            + (resource.getLaunchFile() == null ? "index.html" : resource.getLaunchFile()), null);
                }
                default -> throw new ConflictException("NOT_LAUNCHABLE",
                        "Only link, file, e-book and web-resource items open by address.");
            };
        });
        if (target.url() != null) {
            return new ExternalLink(target.url());
        }
        DownloadLink link = files.link(target.fileId(), "contentItemId");
        ExternalLink answer = new ExternalLink(link.getUrl());
        answer.setExpiresAt(link.getExpiresAt());
        return answer;
    }

    /**
     * lms.DownloadSubmissions: packs the item's final submissions — each student's or group's attachment or typed
     * answer — into one zip in the file store. The work runs before the 202 answer; the finished operation names
     * the stored file.
     */
    public Operation downloadSubmissions(UUID itemId) {
        DataScope scope = DataScope.current(PERMISSION_DOWNLOAD_SUBMISSIONS);
        List<LmsAssignmentSubmissionEntity> rows = doInReadTransaction(() -> {
            ContentItemEntity entity = loadVisible(scope, itemId);
            scope.requireWritable(institutionOf(entity));
            if (!"assignment".equals(entity.getItemKind())) {
                throw new ConflictException("NOT_AN_ASSIGNMENT", "Only an assignment item has submissions.");
            }
            return submissions.ofItem(itemId);
        });
        UUID operationId = operations.start("lms.DownloadSubmissions", AGGREGATE, itemId);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                int done = 0;
                for (LmsAssignmentSubmissionEntity submission : rows) {
                    String owner = submission.getUserId() != null ? submission.getUserId().toString()
                            : String.valueOf(submission.getGroupId());
                    if (submission.getAttachmentFileId() != null) {
                        zip.putNextEntry(new ZipEntry(owner + "/" + submission.getId() + ".bin"));
                        zip.write(files.read(submission.getAttachmentFileId(), "contentItemId", List.of(),
                                MAX_ARCHIVED_FILE_BYTES).content());
                    } else {
                        zip.putNextEntry(new ZipEntry(owner + "/" + submission.getId() + ".txt"));
                        zip.write((submission.getAnswer() == null ? "" : submission.getAnswer())
                                .getBytes(StandardCharsets.UTF_8));
                    }
                    zip.closeEntry();
                    done++;
                    if (!operations.progress(operationId, done * 100 / Math.max(rows.size(), 1))) {
                        break;
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("the in-memory zip cannot fail this way", e);
            }
            UUID fileId = files.write("submissions-" + itemId + ".zip", "application/zip",
                    bytes.toByteArray(), AGGREGATE, itemId);
            return operations.succeed(operationId, "platform.file_object", fileId);
        } catch (RuntimeException e) {
            operations.fail(operationId, "UNAVAILABLE", "The submissions could not be packed; run it again.");
            throw e;
        }
    }

    /** The live group of the item; another item's group is 404. */
    private AssignmentGroupEntity groupOf(ContentItemEntity item, UUID groupId) {
        return assignmentGroupRepository.findById(groupId)
                .filter(g -> item.getId().equals(g.getContentItemId()))
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT_GROUP_AGGREGATE, groupId));
    }

    /** A condition as a student reads it: its type, operator and values ("score_at_least >= 60"). */
    private static String describe(ReleaseRuleConditionEntity condition) {
        StringBuilder text = new StringBuilder(condition.getConditionType()).append(' ').append(condition.getOperator());
        if (condition.getValue() != null) {
            text.append(' ').append(condition.getValue());
        }
        if (condition.getValue2() != null) {
            text.append(" and ").append(condition.getValue2());
        }
        return text.toString();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private ContentItemEntity loadVisible(DataScope scope, UUID id) {
        ContentItemEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOf(ContentItemEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected ContentItemUpdate fields; explicit per field so a mask can clear values. */
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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfAssignmentGroup(AssignmentGroupEntity entity) {
        return assignmentGroupRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the selected AssignmentGroupUpdate fields; explicit per field so a mask can clear values. */
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
        entity.setUserId(OwnRows.caller());
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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ContentItemRating updateContentItemRating(UUID contentItemId, UUID id, String ifMatch,
            ContentItemRatingUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CONTENT_ITEM_RATING_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CONTENT_ITEM_RATING_PERMISSION_UPDATE);
            ContentItemRatingEntity entity = loadVisibleContentItemRating(scope, contentItemId, id);
            OwnRows.require(entity.getUserId(), CONTENT_ITEM_RATING_AGGREGATE, id);
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
            OwnRows.require(entity.getUserId(), CONTENT_ITEM_RATING_AGGREGATE, id);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfContentItemRating(ContentItemRatingEntity entity) {
        return contentItemRatingRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the selected ContentItemRatingUpdate fields; explicit per field so a mask can clear values. */
    private static void applyContentItemRating(ContentItemRatingEntity entity, ContentItemRatingUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
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

    /** Applies the selected ItemOverrideUpdate fields; explicit per field so a mask can clear values. */
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
