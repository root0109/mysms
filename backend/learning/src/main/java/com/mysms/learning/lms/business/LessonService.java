package com.mysms.learning.lms.business;

import com.mysms.core.error.InvalidStateException;
import tools.jackson.databind.json.JsonMapper;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.learning.lms.repository.SettingsViewRepository;
import com.mysms.learning.lms.repository.SettingsView;
import com.mysms.learning.lms.repository.LmsCourseRepository;
import com.mysms.learning.lms.repository.LessonRepository;
import com.mysms.learning.lms.repository.LessonProgressRepository;
import com.mysms.learning.lms.repository.LessonNoteRepository;
import com.mysms.learning.lms.repository.EnrollmentRepository;
import com.mysms.learning.lms.mapper.LessonProgressMapper;
import com.mysms.learning.lms.mapper.LessonNoteMapper;
import com.mysms.learning.lms.mapper.LessonMapper;
import com.mysms.learning.lms.domain.LessonProgressEntity;
import com.mysms.learning.lms.domain.LessonNoteEntity;
import com.mysms.learning.lms.domain.LessonEntity;
import com.mysms.learning.lms.model.LessonDocument;
import com.mysms.learning.lms.api.model.TrackVideoWatchRequest;
import com.mysms.learning.lms.api.model.MoveLessonRequest;
import com.mysms.learning.lms.api.model.LessonUpdate;
import com.mysms.learning.lms.api.model.LessonProgress;
import com.mysms.learning.lms.api.model.LessonNoteUpdate;
import com.mysms.learning.lms.api.model.LessonNote;
import com.mysms.learning.lms.api.model.LessonContent;
import com.mysms.learning.lms.api.model.Lesson;
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
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.api.model.LessonActivity;
import com.mysms.learning.lms.api.model.LessonActivityUpdate;
import com.mysms.learning.lms.api.model.VideoWatch;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.LessonActivityEntity;
import com.mysms.learning.lms.domain.VideoWatchEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.LessonActivityMapper;
import com.mysms.learning.lms.mapper.VideoWatchMapper;
import com.mysms.learning.lms.repository.LessonActivityRepository;
import com.mysms.learning.lms.repository.VideoWatchRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Service of the Lesson aggregate: lessons with their activities, video watch records and students' notes, plus the
 * learner's side — reading a lesson, completing it under the tenant's rules, and video time. Notes are read and
 * changed by their writer only.
 */
@Service
public class LessonService extends AppTransactionManager {

    public static final String AGGREGATE = LessonEntity.TABLE;
    static final String PERMISSION_READ = "lms.lesson.read";
    static final String PERMISSION_CREATE = "lms.lesson.create";
    static final String PERMISSION_UPDATE = "lms.lesson.update";
    static final String PERMISSION_DELETE = "lms.lesson.delete";
    static final String PERMISSION_MOVE = "lms.lesson.move_lesson";
    static final String PROGRESS_AGGREGATE = LessonProgressEntity.TABLE;
    static final String COMPLETE = "complete";
    /** lms.video_watch.video_source is varchar(500). */
    private static final int MAX_VIDEO_SOURCE = 500;

    public static final String LESSON_ACTIVITY_AGGREGATE = LessonActivityEntity.TABLE;
    static final String LESSON_ACTIVITY_PERMISSION_LIST = "lms.lesson_activity.read";
    static final String LESSON_ACTIVITY_PERMISSION_GET = "lms.lesson_activity.read";
    static final String LESSON_ACTIVITY_PERMISSION_CREATE = "lms.lesson_activity.create";
    static final String LESSON_ACTIVITY_PERMISSION_UPDATE = "lms.lesson_activity.update";
    static final String LESSON_ACTIVITY_PERMISSION_DELETE = "lms.lesson_activity.delete";

    public static final String VIDEO_WATCH_AGGREGATE = VideoWatchEntity.TABLE;
    static final String VIDEO_WATCH_PERMISSION_LIST = "lms.video_watch.read";
    static final String VIDEO_WATCH_PERMISSION_GET = "lms.video_watch.read";

    public static final String LESSON_NOTE_AGGREGATE = LessonNoteEntity.TABLE;

    private final LessonActivityRepository lessonActivityRepository;
    private final LessonActivityMapper lessonActivityMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final VideoWatchRepository videoWatchRepository;
    private final VideoWatchMapper videoWatchMapper;
    private final LessonRepository repository;
    private final LessonMapper mapper;
    private final LessonNoteRepository lessonNoteRepository;
    private final LessonNoteMapper lessonNoteMapper;
    private final LessonProgressRepository progress;
    private final LessonProgressMapper progressMapper;
    private final EnrollmentRepository enrollments;
    private final LmsCourseRepository courses;
    private final SettingsViewRepository settings;
    private final JsonMapper json;
    private final PublicReads publicReads;

    public LessonService(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            LessonActivityRepository lessonActivityRepository,
            LessonActivityMapper lessonActivityMapper,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            VideoWatchRepository videoWatchRepository,
            VideoWatchMapper videoWatchMapper,
            LessonRepository repository,
            LessonMapper mapper,
            LessonNoteRepository lessonNoteRepository,
            LessonNoteMapper lessonNoteMapper,
            LessonProgressRepository progress,
            LessonProgressMapper progressMapper,
            EnrollmentRepository enrollments,
            LmsCourseRepository courses,
            SettingsViewRepository settings,
            JsonMapper json, PublicReads publicReads) {
        super(database, transactions);
        this.publicReads = publicReads;
        this.lessonActivityRepository = lessonActivityRepository;
        this.lessonActivityMapper = lessonActivityMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.videoWatchRepository = videoWatchRepository;
        this.videoWatchMapper = videoWatchMapper;
        this.repository = repository;
        this.mapper = mapper;
        this.lessonNoteRepository = lessonNoteRepository;
        this.lessonNoteMapper = lessonNoteMapper;
        this.progress = progress;
        this.progressMapper = progressMapper;
        this.enrollments = enrollments;
        this.courses = courses;
        this.settings = settings;
        this.json = json;
    }

    // ---- Lesson ----

    /** A lesson within the caller's scope (its chapter's course's institution). */
    public Lesson getLesson(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_READ);
        return doInReadTransaction(() -> toDto(loadVisible(scope, id)));
    }

    /** The lessons within the caller's scope, page by page. */
    public Page<Lesson> listLessons(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, LessonRepository.SORT_FIELDS, LessonRepository.DEFAULT_ORDER,
                LessonRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LessonRepository.FILTER_FIELDS)
                .and(LessonRepository.scope(DataScope.current(PERMISSION_READ)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(this::toDto));
    }

    /** Creates the lesson in its chapter; with validateOnly every check runs and nothing is written. */
    public Lesson createLesson(Lesson request, boolean validateOnly) {
        validator.validate(request);
        LessonEntity entity = mapper.toEntity(request);
        entity.setContent(document(request.getContent()));
        entity.setTeacherContent(document(request.getTeacherContent()));
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        entity.setVersion(1);
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity);
            scope.requireWritable(repository.institutionOfChapter(entity.getChapterId()));
            if (validateOnly) {
                return toDto(entity);
            }
            repository.insert(entity);
            audit.recordCreate(AGGREGATE, entity.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, entity.getId(), LmsEvents.payload(entity.getId()));
            return toDto(repository.findById(entity.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, entity.getId())));
        });
    }

    /**
     * Partial update under If-Match. A published lesson version is immutable (409); a move to another chapter must
     * stay in the caller's scope at both ends.
     */
    public Lesson updateLesson(UUID id, String ifMatch, LessonUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LessonEntity entity = loadVisible(scope, id);
            scope.requireWritable(repository.institutionOfChapter(entity.getChapterId()));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (entity.getPublishedAt() != null) {
                throw new InvalidStateException("A published lesson version cannot change; edit a new version.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(toDto(entity));
            assertValid(entity);
            scope.requireWritable(repository.institutionOfChapter(entity.getChapterId()));
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** Soft delete under If-Match; refused (409) while live rows of this module still reference the lesson. */
    public void deleteLesson(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LessonEntity entity = loadVisible(scope, id);
            scope.requireWritable(repository.institutionOfChapter(entity.getChapterId()));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            Optional<String> referenced = repository.firstReference(id);
            if (referenced.isPresent()) {
                throw new InvalidStateException("The lesson is still referenced by " + referenced.get()
                        + " rows; remove those first.");
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
     * lms.MoveLesson: puts the lesson at a place (0-based sort order) in a chapter of the same course; the lessons
     * from that place on move one down. Another course's chapter is 422; both chapters must be writable.
     */
    public Lesson moveLesson(UUID id, MoveLessonRequest request) {
        validator.validate(request);
        if (request.getPosition() < 0) {
            throw new ValidationFailedException(List.of(new FieldError("position", "must be 0 or more", "Min")));
        }
        DataScope scope = DataScope.current(PERMISSION_MOVE);
        return doInWriteTransaction(() -> {
            LessonEntity entity = loadVisible(scope, id);
            scope.requireWritable(repository.institutionOfChapter(entity.getChapterId()));
            UUID target = request.getTargetChapterId();
            Optional<UUID> course = repository.courseOfChapter(entity.getChapterId());
            if (course.isEmpty() || !course.equals(repository.courseOfChapter(target))) {
                throw new ValidationFailedException(List.of(new FieldError("targetChapterId",
                        "must be a chapter of the lesson's course", "NOT_FOUND")));
            }
            OffsetDateTime now = now();
            if (repository.move(id, entity.getRowVersion(), target, request.getPosition(), now,
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "move", List.of("chapterId", "sortOrder"), null);
            outbox.publish(AGGREGATE + ".move", AGGREGATE, id, LmsEvents.payload(id));
            return toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * lms.GetLessonContent: the lesson with its activities, neighbours and the caller's progress. A non-member gets
     * the full lesson only when enrolled or when it is a preview, else its title and place ({@code canAccess} false);
     * teacher material is for members only.
     */
    public LessonContent getLessonContent(UUID id) {
        boolean member = publicReads.asMember(PERMISSION_READ);
        Optional<UUID> user = AuthContext.current().map(AuthContext::userId);
        return doInReadTransaction(() -> {
            LessonEntity entity = member ? loadVisible(DataScope.current(PERMISSION_READ), id)
                    : repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            UUID course = repository.courseOfChapter(entity.getChapterId())
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            boolean canAccess = member;
            if (!member) {
                // an enrolled student keeps reading a course that was unpublished or deactivated since
                boolean enrolled = user.flatMap(u -> enrollments.findOwn(course, u)).isPresent();
                if (!enrolled && courses.findById(course).filter(c -> Boolean.TRUE.equals(c.getIsPublished()))
                        .isEmpty()) {
                    throw new ResourceNotFoundException(AGGREGATE, id);
                }
                canAccess = enrolled || Boolean.TRUE.equals(entity.getIncludeInPreview());
            }
            Lesson lesson = toDto(entity);
            if (!member) {
                withoutTeacherMaterial(lesson);
                if (!canAccess) {
                    lesson.setBody(null);
                    lesson.setContent(null);
                    lesson.setYoutubeVideoId(null);
                    lesson.setEmbeddedQuestion(null);
                }
            }
            List<UUID> order = repository.inCourseOrder(course);
            int at = order.indexOf(id);
            LessonContent content = new LessonContent(lesson, canAccess);
            content.setPreviousLessonId(at > 0 ? order.get(at - 1) : null);
            content.setNextLessonId(at >= 0 && at < order.size() - 1 ? order.get(at + 1) : null);
            content.setActivities(canAccess ? lessonActivityRepository.ofLesson(id, member).stream()
                    .map(lessonActivityMapper::toDto).toList() : List.of());
            user.flatMap(u -> progress.findOwn(u, entity.getChapterId(), id))
                    .ifPresent(p -> content.setProgressStatus(p.getStatus()));
            return content;
        });
    }

    /**
     * lms.MarkLessonComplete: completes a lesson of the caller's course under the tenant's rules (409 NOT_ENROLLED,
     * VIDEO_NOT_WATCHED, QUIZ_NOT_COMPLETED), then recomputes the enrolment's progress in the same transaction.
     */
    public LessonProgress markLessonComplete(UUID id) {
        UUID user = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            LessonEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            UUID course = enrolledCourse(entity, user);
            SettingsView rules = settings.find();
            if (rules.enforceVideoCompletion() && repository.videoUnwatched(id, user)) {
                throw new ConflictException("VIDEO_NOT_WATCHED", "Watch the lesson's video before completing it.");
            }
            if (rules.enforceQuizCompletion() && repository.quizzesOpen(id, user, rules.enforceQuizPassing()) > 0) {
                throw new ConflictException("QUIZ_NOT_COMPLETED", rules.enforceQuizPassing()
                        ? "Pass the lesson's quizzes before completing it." : "Finish the lesson's quizzes first.");
            }
            LessonProgressEntity row = new LessonProgressEntity();
            row.onCreatingData(Actor.current(), clock);
            row.setTenantId(tenant);
            row.setUserId(user);
            row.setCourseId(course);
            row.setChapterId(entity.getChapterId());
            row.setLessonId(id);
            row.setStatus(COMPLETE);
            row.setIsScormChapter(false);
            OffsetDateTime now = now();
            LessonProgressEntity stored = progress.upsert(row, now, user);
            enrollments.recordProgress(course, user, id, now, user);
            audit.recordTransition(PROGRESS_AGGREGATE, stored.getId(), COMPLETE, List.of("status"), null);
            outbox.publish(PROGRESS_AGGREGATE + "." + COMPLETE, PROGRESS_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return progressMapper.toDto(stored);
        });
    }

    /**
     * lms.TrackVideoWatch: the caller's watch time of a video of a lesson in a course they are enrolled in (409
     * NOT_ENROLLED otherwise). One row per lesson, student and video keeps the longest time reported.
     */
    public VideoWatch trackVideoWatch(UUID id, TrackVideoWatchRequest request) {
        validator.validate(request);
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkDecimal("watchSeconds", request.getWatchSeconds(), 12, 2, errors);
        if (errors.isEmpty() && WireValues.decimal(request.getWatchSeconds()).signum() < 0) {
            errors.add(new FieldError("watchSeconds", "must be 0 or more", "Min"));
        }
        if (request.getVideoSource().isBlank() || request.getVideoSource().length() > MAX_VIDEO_SOURCE) {
            errors.add(new FieldError("videoSource", "must be 1 to " + MAX_VIDEO_SOURCE + " characters", "Size"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        UUID user = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            LessonEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            enrolledCourse(entity, user);
            VideoWatchEntity row = new VideoWatchEntity();
            row.onCreatingData(Actor.current(), clock);
            row.setTenantId(tenant);
            row.setLessonId(id);
            row.setUserId(user);
            row.setVideoSource(request.getVideoSource().trim());
            row.setWatchSeconds(WireValues.decimal(request.getWatchSeconds()));
            VideoWatchEntity stored = videoWatchRepository.upsert(row, now(), user);
            audit.recordTransition(VIDEO_WATCH_AGGREGATE, stored.getId(), "track", List.of("watchSeconds"), null);
            return videoWatchMapper.toDto(stored);
        });
    }

    /** The course of the lesson, which the student must be enrolled in (409 NOT_ENROLLED). */
    private UUID enrolledCourse(LessonEntity lesson, UUID user) {
        UUID course = repository.courseOfChapter(lesson.getChapterId())
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, lesson.getId()));
        if (enrollments.findOwn(course, user).isEmpty()) {
            throw new ConflictException("NOT_ENROLLED", "Enrol in the course first.");
        }
        return course;
    }

    /** The live lesson, if the caller may see it: outside the caller's scope it is 404. */
    private LessonEntity loadVisible(DataScope scope, UUID id) {
        LessonEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(repository.institutionOfChapter(entity.getChapterId()))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private List<String> apply(LessonEntity entity, LessonUpdate changes) {
        Patches patch = new Patches();
        patch.apply("chapterId", changes.getChapterId(), entity::setChapterId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("includeInPreview", changes.getIncludeInPreview(), entity::setIncludeInPreview);
        patch.apply("body", changes.getBody(), entity::setBody);
        patch.apply("content", changes.getContent(), v -> entity.setContent(document(v)));
        patch.apply("teacherNotes", changes.getTeacherNotes(), entity::setTeacherNotes);
        patch.apply("teacherContent", changes.getTeacherContent(), v -> entity.setTeacherContent(document(v)));
        patch.apply("youtubeVideoId", changes.getYoutubeVideoId(), entity::setYoutubeVideoId);
        patch.apply("embeddedQuestion", changes.getEmbeddedQuestion(), entity::setEmbeddedQuestion);
        patch.apply("fileType", changes.getFileType(), v -> entity.setFileType(v == null ? null : v.getValue()));
        patch.apply("isScormPackage", changes.getIsScormPackage(), entity::setIsScormPackage);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LessonEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getChapterId() == null) {
            errors.add(new FieldError("chapterId", "must not be null", "NotNull"));
        } else if (!repository.existsChapter(entity.getChapterId())) {
            errors.add(new FieldError("chapterId", "chapter does not exist", "NOT_FOUND"));
        }
        if (entity.getIncludeInPreview() == null) {
            entity.setIncludeInPreview(false);
        }
        if (entity.getIsScormPackage() == null) {
            entity.setIsScormPackage(false);
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private Lesson toDto(LessonEntity entity) {
        Lesson lesson = mapper.toDto(entity);
        lesson.setContent(value(entity.getContent()));
        lesson.setTeacherContent(value(entity.getTeacherContent()));
        return lesson;
    }

    /** What a student never reads of a lesson: the teacher's notes and material. */
    private static void withoutTeacherMaterial(Lesson lesson) {
        lesson.setTeacherNotes(null);
        lesson.setTeacherContent(null);
        lesson.setCreatedBy(null);
        lesson.setUpdatedBy(null);
    }

    /** A contract JSON value as the stored document (an editor document is a JSON object). */
    private LessonDocument document(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof java.util.Map<?, ?>)) {
            throw new ValidationFailedException(List.of(new FieldError("content", "must be a JSON object", "Type")));
        }
        return new LessonDocument(json.writeValueAsString(value));
    }

    private Object value(LessonDocument document) {
        return document == null ? null : json.readValue(document.json(), Object.class);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    // ---- LessonNote: a record of the Lesson aggregate, private to its writer ----

    /** The caller's notes on a lesson they may read. */
    public Page<LessonNote> listLessonNotes(UUID lessonId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        UUID user = AuthContext.require().userId();
        SortSpec sort = SortSpec.parse(sortBy, LessonNoteRepository.SORT_FIELDS, LessonNoteRepository.DEFAULT_ORDER,
                LessonNoteRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LessonNoteRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + lessonId, clock);
        return doInReadTransaction(() -> {
            requireReadableLesson(lessonId, user);
            return lessonNoteRepository.listOwn(user, lessonId, page, where).map(lessonNoteMapper::toDto);
        });
    }

    /** One of the caller's notes (another user's note is 404, as if it did not exist). */
    public LessonNote getLessonNote(UUID lessonId, UUID id) {
        UUID user = AuthContext.require().userId();
        return doInReadTransaction(() -> lessonNoteMapper.toDto(lessonNoteRepository.findOwn(user, lessonId, id)
                .orElseThrow(() -> new ResourceNotFoundException(LESSON_NOTE_AGGREGATE, id))));
    }

    /** The caller writes a note on a lesson they may read; the writer is always the caller. */
    public LessonNote createLessonNote(UUID lessonId, LessonNote request, boolean validateOnly) {
        validator.validate(request);
        UUID user = AuthContext.require().userId();
        LessonNoteEntity entity = lessonNoteMapper.toEntity(request);
        entity.setLessonId(lessonId);
        entity.setUserId(user);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        assertValidLessonNote(entity);
        return doInWriteTransaction(() -> {
            requireReadableLesson(lessonId, user);
            if (validateOnly) {
                return lessonNoteMapper.toDto(entity);
            }
            LessonNoteEntity stored = lessonNoteRepository.insert(entity);
            audit.recordCreate(LESSON_NOTE_AGGREGATE, stored.getId());
            return lessonNoteMapper.toDto(stored);
        });
    }

    /** Partial update of one of the caller's notes under If-Match. */
    public LessonNote updateLessonNote(UUID lessonId, UUID id, String ifMatch, LessonNoteUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        UUID user = AuthContext.require().userId();
        Optional<UUID> writer = changes.getUserId();
        if (writer != null && !writer.equals(Optional.of(user))) {
            throw new PermissionDeniedException("A note keeps its writer.");
        }
        return doInWriteTransaction(() -> {
            LessonNoteEntity entity = lessonNoteRepository.findOwn(user, lessonId, id)
                    .orElseThrow(() -> new ResourceNotFoundException(LESSON_NOTE_AGGREGATE, id));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyLessonNote(entity, changes);
            assertValidLessonNote(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return lessonNoteMapper.toDto(entity);
            }
            if (lessonNoteRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(LESSON_NOTE_AGGREGATE, id, fields);
            return lessonNoteMapper.toDto(entity);
        });
    }

    /** Applies the fields the request carries (§7.2); the writer ({@code userId}) is checked, never changed. */
    private static List<String> applyLessonNote(LessonNoteEntity entity, LessonNoteUpdate changes) {
        Patches patch = new Patches();
        patch.apply("color", changes.getColor(), v -> entity.setColor(v == null ? null : v.getValue()));
        patch.apply("highlightedText", changes.getHighlightedText(), entity::setHighlightedText);
        patch.apply("note", changes.getNote(), entity::setNote);
        return patch.applied();
    }

    /** Soft delete of one of the caller's notes under If-Match. */
    public void deleteLessonNote(UUID lessonId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        UUID user = AuthContext.require().userId();
        runInWriteTransaction(() -> {
            LessonNoteEntity entity = lessonNoteRepository.findOwn(user, lessonId, id)
                    .orElseThrow(() -> new ResourceNotFoundException(LESSON_NOTE_AGGREGATE, id));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (lessonNoteRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(LESSON_NOTE_AGGREGATE, id);
        });
    }

    /** The lesson must be one the caller reads: in scope for a holder of lms.lesson.read, else enrolled (404). */
    private void requireReadableLesson(UUID lessonId, UUID user) {
        LessonEntity lesson = repository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, lessonId));
        boolean member = PublicReads.member(PERMISSION_READ)
                && DataScope.current(PERMISSION_READ).canRead(repository.institutionOfChapter(lesson.getChapterId()));
        UUID course = repository.courseOfChapter(lesson.getChapterId())
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, lessonId));
        if (!member && enrollments.findOwn(course, user).isEmpty()) {
            throw new ResourceNotFoundException(AGGREGATE, lessonId);
        }
    }

    /** A note holds a highlight, a note or both (ck_lesson_note_1). */
    private static void assertValidLessonNote(LessonNoteEntity entity) {
        if (isBlank(entity.getHighlightedText()) && isBlank(entity.getNote())) {
            throw new ValidationFailedException(List.of(new FieldError("note",
                    "a note needs its text or the highlighted text", "NotBlank")));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LessonActivity updateLessonActivity(UUID lessonId, UUID id, String ifMatch, LessonActivityUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LESSON_ACTIVITY_PERMISSION_UPDATE);
            LessonActivityEntity entity = loadVisibleLessonActivity(scope, lessonId, id);
            scope.requireWritable(institutionOfLessonActivity(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyLessonActivity(entity, changes);
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

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyLessonActivity(LessonActivityEntity entity, LessonActivityUpdate changes) {
        Patches patch = new Patches();
        patch.apply("quizId", changes.getQuizId(), entity::setQuizId);
        patch.apply("assignmentId", changes.getAssignmentId(), entity::setAssignmentId);
        patch.apply("exerciseId", changes.getExerciseId(), entity::setExerciseId);
        patch.apply("teacherOnly", changes.getTeacherOnly(), entity::setTeacherOnly);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
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
