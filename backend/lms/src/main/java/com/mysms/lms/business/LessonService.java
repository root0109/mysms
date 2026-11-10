package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
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
import com.mysms.lms.api.model.LessonActivity;
import com.mysms.lms.api.model.LessonActivityUpdate;
import com.mysms.lms.api.model.VideoWatch;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.LessonActivityEntity;
import com.mysms.lms.domain.VideoWatchEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.LessonActivityMapper;
import com.mysms.lms.mapper.VideoWatchMapper;
import com.mysms.lms.repository.LessonActivityRepository;
import com.mysms.lms.repository.VideoWatchRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of the Lesson aggregate of lms (JAVA-CODING-RULES §4): the records that exist only as
 * part of a Lesson are served here, each with explicit methods; audit and domain event are written in the
 * transaction of every change, under the data-level rules of api/README "Authorization".
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Lesson
 * are served here too — LessonActivity, VideoWatch.
 */
@Service
public class LessonService extends AppTransactionManager {

    public static final String LESSON_ACTIVITY_AGGREGATE = LessonActivityEntity.TABLE;
    static final String LESSON_ACTIVITY_PERMISSION_LIST = "lms.lesson_activity.read";
    static final String LESSON_ACTIVITY_PERMISSION_GET = "lms.lesson_activity.read";
    static final String LESSON_ACTIVITY_PERMISSION_CREATE = "lms.lesson_activity.create";
    static final String LESSON_ACTIVITY_PERMISSION_UPDATE = "lms.lesson_activity.update";
    static final String LESSON_ACTIVITY_PERMISSION_DELETE = "lms.lesson_activity.delete";
    /** The contract's LessonActivityUpdate fields, in apply order. */
    static final List<String> LESSON_ACTIVITY_WRITABLE = List.of("quizId", "assignmentId", "exerciseId", "teacherOnly",
            "sortOrder");

    public static final String VIDEO_WATCH_AGGREGATE = VideoWatchEntity.TABLE;
    static final String VIDEO_WATCH_PERMISSION_LIST = "lms.video_watch.read";
    static final String VIDEO_WATCH_PERMISSION_GET = "lms.video_watch.read";

    private final LessonActivityRepository lessonActivityRepository;
    private final LessonActivityMapper lessonActivityMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final VideoWatchRepository videoWatchRepository;
    private final VideoWatchMapper videoWatchMapper;

    public LessonService(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            LessonActivityRepository lessonActivityRepository,
            LessonActivityMapper lessonActivityMapper,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            VideoWatchRepository videoWatchRepository,
            VideoWatchMapper videoWatchMapper) {
        super(database, transactions);
        this.lessonActivityRepository = lessonActivityRepository;
        this.lessonActivityMapper = lessonActivityMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.videoWatchRepository = videoWatchRepository;
        this.videoWatchMapper = videoWatchMapper;
    }

    // ---- LessonActivity: a record of the Lesson aggregate ----

    public LessonActivity getLessonActivity(UUID lessonId, UUID id) {
        return doInReadTransaction(() -> {
            return lessonActivityMapper.toDto(loadVisibleLessonActivity(DataScope.current(LESSON_ACTIVITY_PERMISSION_GET),
                    lessonId, id));
        });
    }

    public Page<LessonActivity> listLessonActivitys(UUID lessonId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LessonActivityRepository.SORT_FIELDS, LessonActivityRepository.DEFAULT_ORDER,
                LessonActivityRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LessonActivityRepository.FILTER_FIELDS)
                .and(LessonActivityRepository.ofParent(lessonId))
                        .and(LessonActivityRepository.scope(DataScope.current(LESSON_ACTIVITY_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + lessonId, clock);
        return doInReadTransaction(() -> {
            Page<LessonActivityEntity> rows = lessonActivityRepository.list(page, where);
            return rows.map(lessonActivityMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LessonActivity createLessonActivity(UUID lessonId, LessonActivity request, boolean validateOnly) {
        validator.validate(request);
        LessonActivityEntity entity = lessonActivityMapper.toEntity(request);
        entity.setLessonId(lessonId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(LESSON_ACTIVITY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!lessonActivityRepository.existsLessonId(entity.getLessonId())) {
                throw new ResourceNotFoundException("lms.lesson", entity.getLessonId());
            }
            if (!scope.canRead(institutionOfLessonActivity(entity))) {
                throw new ResourceNotFoundException("lms.lesson", entity.getLessonId());
            }
            assertValidLessonActivity(entity, true);
            scope.requireWritable(institutionOfLessonActivity(entity));
            assertUniqueLessonActivity(entity);
            if (validateOnly) {
                return lessonActivityMapper.toDto(entity);
            }
            LessonActivityEntity stored = lessonActivityRepository.insert(entity);
            audit.recordCreate(LESSON_ACTIVITY_AGGREGATE, stored.getId());
            outbox.publish(LESSON_ACTIVITY_AGGREGATE + LmsEvents.CREATED, LESSON_ACTIVITY_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return lessonActivityMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public LessonActivity updateLessonActivity(UUID lessonId, UUID id, String ifMatch, LessonActivityUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, LESSON_ACTIVITY_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LESSON_ACTIVITY_PERMISSION_UPDATE);
            LessonActivityEntity entity = loadVisibleLessonActivity(scope, lessonId, id);
            scope.requireWritable(institutionOfLessonActivity(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyLessonActivity(entity, changes, fields);
            validator.validate(lessonActivityMapper.toDto(entity));
            assertValidLessonActivity(entity, false);
            scope.requireWritable(institutionOfLessonActivity(entity));
            assertUniqueLessonActivity(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return lessonActivityMapper.toDto(entity);
            }
            if (lessonActivityRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(LESSON_ACTIVITY_AGGREGATE, id, fields);
            outbox.publish(LESSON_ACTIVITY_AGGREGATE + LmsEvents.UPDATED, LESSON_ACTIVITY_AGGREGATE, id, LmsEvents.payload(id));
            return lessonActivityMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLessonActivity(UUID lessonId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LESSON_ACTIVITY_PERMISSION_DELETE);
            LessonActivityEntity entity = loadVisibleLessonActivity(scope, lessonId, id);
            scope.requireWritable(institutionOfLessonActivity(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (lessonActivityRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(LESSON_ACTIVITY_AGGREGATE, id);
            outbox.publish(LESSON_ACTIVITY_AGGREGATE + LmsEvents.DELETED, LESSON_ACTIVITY_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LessonActivityEntity loadVisibleLessonActivity(DataScope scope, UUID lessonId, UUID id) {
        LessonActivityEntity entity = lessonActivityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LESSON_ACTIVITY_AGGREGATE, id));
        if (!scope.canRead(institutionOfLessonActivity(entity)) || !lessonId.equals(entity.getLessonId())) {
            throw new ResourceNotFoundException(LESSON_ACTIVITY_AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the lms.lesson → lms.chapter → lms.course it belongs to).
     */
    private Optional<UUID> institutionOfLessonActivity(LessonActivityEntity entity) {
        return lessonActivityRepository.institutionOf(entity.getLessonId());
    }

    /**
     * Applies the selected LessonActivityUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyLessonActivity(LessonActivityEntity entity, LessonActivityUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "quizId" -> entity.setQuizId(changes.getQuizId());
                case "assignmentId" -> entity.setAssignmentId(changes.getAssignmentId());
                case "exerciseId" -> entity.setExerciseId(changes.getExerciseId());
                case "teacherOnly" -> entity.setTeacherOnly(changes.getTeacherOnly());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidLessonActivity(LessonActivityEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getLessonId() == null) {
            errors.add(new FieldError("lessonId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getTeacherOnly() == null) {
            errors.add(new FieldError("teacherOnly", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentId() != null && !lessonActivityRepository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (entity.getExerciseId() != null && !lessonActivityRepository.existsExerciseId(entity.getExerciseId())) {
            errors.add(new FieldError("exerciseId", "programming exercise does not exist", "NOT_FOUND"));
        }
        if (entity.getLessonId() != null && !lessonActivityRepository.existsLessonId(entity.getLessonId())) {
            errors.add(new FieldError("lessonId", "lesson does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizId() != null && !lessonActivityRepository.existsQuizId(entity.getQuizId())) {
            errors.add(new FieldError("quizId", "quiz does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueLessonActivity(LessonActivityEntity entity) {

    }

    // ---- VideoWatch: a record of the Lesson aggregate ----

    public VideoWatch getVideoWatch(UUID lessonId, UUID id) {
        return doInReadTransaction(() -> {
            return videoWatchMapper.toDto(loadVisibleVideoWatch(DataScope.current(VIDEO_WATCH_PERMISSION_GET), lessonId, id));
        });
    }

    public Page<VideoWatch> listVideoWatchs(UUID lessonId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, VideoWatchRepository.SORT_FIELDS, VideoWatchRepository.DEFAULT_ORDER,
                VideoWatchRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, VideoWatchRepository.FILTER_FIELDS)
                .and(VideoWatchRepository.ofParent(lessonId))
                        .and(VideoWatchRepository.scope(DataScope.current(VIDEO_WATCH_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + lessonId, clock);
        return doInReadTransaction(() -> {
            Page<VideoWatchEntity> rows = videoWatchRepository.list(page, where);
            return rows.map(videoWatchMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private VideoWatchEntity loadVisibleVideoWatch(DataScope scope, UUID lessonId, UUID id) {
        VideoWatchEntity entity = videoWatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(VIDEO_WATCH_AGGREGATE, id));
        if (!scope.canRead(institutionOfVideoWatch(entity)) || !lessonId.equals(entity.getLessonId())) {
            throw new ResourceNotFoundException(VIDEO_WATCH_AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the lms.lesson → lms.chapter → lms.course it belongs to).
     */
    private Optional<UUID> institutionOfVideoWatch(VideoWatchEntity entity) {
        return videoWatchRepository.institutionOf(entity.getLessonId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidVideoWatch(VideoWatchEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getLessonId() == null) {
            errors.add(new FieldError("lessonId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getVideoSource() == null) {
            errors.add(new FieldError("videoSource", "must not be null", "NotNull"));
        }
        if (entity.getWatchSeconds() == null) {
            errors.add(new FieldError("watchSeconds", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("watchSeconds", WireValues.decimal(entity.getWatchSeconds()), 12, 2, errors);
        if (entity.getLessonId() != null && !videoWatchRepository.existsLessonId(entity.getLessonId())) {
            errors.add(new FieldError("lessonId", "lesson does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueVideoWatch(VideoWatchEntity entity) {
        if (videoWatchRepository.takenLessonIdUserIdVideoSource1(entity)) {
            throw new DuplicateResourceException(
                    "A video watch with the same lesson id and user id and video source already exists.");
        }
    }
}
