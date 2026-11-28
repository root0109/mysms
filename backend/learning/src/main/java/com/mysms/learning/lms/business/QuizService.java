package com.mysms.learning.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.error.ConflictException;
import com.mysms.learning.lms.api.model.BlankView;
import com.mysms.learning.lms.api.model.OptionView;
import com.mysms.learning.lms.api.model.QuizAttempt;
import com.mysms.learning.lms.api.model.QuizQuestionView;
import com.mysms.learning.lms.domain.ContentItemEntity;
import com.mysms.learning.lms.domain.ItemOverrideEntity;
import com.mysms.learning.lms.domain.QuestionBlankEntity;
import com.mysms.learning.lms.domain.QuestionEntity;
import com.mysms.learning.lms.domain.QuestionHotspotEntity;
import com.mysms.learning.lms.domain.QuestionMatchPairEntity;
import com.mysms.learning.lms.domain.QuestionOptionEntity;
import com.mysms.learning.lms.domain.QuizSubmissionEntity;
import com.mysms.learning.lms.repository.ContentItemRepository;
import com.mysms.learning.lms.repository.ItemOverrideRepository;
import com.mysms.learning.lms.repository.QuestionBlankRepository;
import com.mysms.learning.lms.repository.QuestionHotspotRepository;
import com.mysms.learning.lms.repository.QuestionMatchPairRepository;
import com.mysms.learning.lms.repository.QuestionOptionRepository;
import com.mysms.learning.lms.repository.QuestionRepository;
import com.mysms.learning.lms.repository.QuizSubmissionRepository;
import com.mysms.learning.lms.repository.QuizAttemptRepository;
import com.mysms.learning.lms.model.QuizAnswerKey;
import com.mysms.core.id.UuidV7;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Random;
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
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.api.model.Quiz;
import com.mysms.learning.lms.api.model.QuizQuestion;
import com.mysms.learning.lms.api.model.QuizQuestionHistory;
import com.mysms.learning.lms.api.model.QuizQuestionUpdate;
import com.mysms.learning.lms.api.model.QuizUpdate;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.QuizEntity;
import com.mysms.learning.lms.domain.QuizQuestionEntity;
import com.mysms.learning.lms.domain.QuizQuestionHistoryEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.QuizMapper;
import com.mysms.learning.lms.mapper.QuizQuestionHistoryMapper;
import com.mysms.learning.lms.mapper.QuizQuestionMapper;
import com.mysms.learning.lms.repository.QuizQuestionHistoryRepository;
import com.mysms.learning.lms.repository.QuizQuestionRepository;
import com.mysms.learning.lms.repository.QuizRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.quiz and of the records that exist only as part of a Quiz (QuizQuestionHistory,
 * QuizQuestion): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class QuizService extends AppTransactionManager {

    public static final String AGGREGATE = QuizEntity.TABLE;
    static final String PERMISSION_LIST = "lms.quiz.read";
    static final String PERMISSION_GET = "lms.quiz.read";
    static final String PERMISSION_CREATE = "lms.quiz.create";
    static final String PERMISSION_UPDATE = "lms.quiz.update";
    static final String PERMISSION_DELETE = "lms.quiz.delete";

    public static final String QUIZ_QUESTION_HISTORY_AGGREGATE = QuizQuestionHistoryEntity.TABLE;
    static final String QUIZ_QUESTION_HISTORY_PERMISSION_LIST = "lms.quiz_question_history.read";
    static final String QUIZ_QUESTION_HISTORY_PERMISSION_GET = "lms.quiz_question_history.read";

    public static final String QUIZ_QUESTION_AGGREGATE = QuizQuestionEntity.TABLE;
    static final String QUIZ_QUESTION_PERMISSION_LIST = "lms.quiz_question.read";
    static final String QUIZ_QUESTION_PERMISSION_GET = "lms.quiz_question.read";
    static final String QUIZ_QUESTION_PERMISSION_CREATE = "lms.quiz_question.create";
    static final String QUIZ_QUESTION_PERMISSION_UPDATE = "lms.quiz_question.update";
    static final String QUIZ_QUESTION_PERMISSION_DELETE = "lms.quiz_question.delete";

    private final QuizRepository repository;
    private final QuizMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final QuizQuestionHistoryRepository quizQuestionHistoryRepository;
    private final QuizQuestionHistoryMapper quizQuestionHistoryMapper;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizQuestionMapper quizQuestionMapper;
    private final ContentItemRepository contentItems;
    private final ItemOverrideRepository itemOverrides;
    private final QuestionRepository questions;
    private final QuestionOptionRepository questionOptions;
    private final QuestionBlankRepository questionBlanks;
    private final QuestionMatchPairRepository questionMatchPairs;
    private final QuestionHotspotRepository questionHotspots;
    private final QuizSubmissionRepository quizSubmissions;
    private final QuizAttemptRepository attempts;

    public QuizService(QuizRepository repository,
            QuizMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            QuizQuestionHistoryRepository quizQuestionHistoryRepository,
            QuizQuestionHistoryMapper quizQuestionHistoryMapper,
            QuizQuestionRepository quizQuestionRepository,
            QuizQuestionMapper quizQuestionMapper,
            ContentItemRepository contentItems,
            ItemOverrideRepository itemOverrides,
            QuestionRepository questions,
            QuestionOptionRepository questionOptions,
            QuestionBlankRepository questionBlanks,
            QuestionMatchPairRepository questionMatchPairs,
            QuestionHotspotRepository questionHotspots,
            QuizSubmissionRepository quizSubmissions,
            QuizAttemptRepository attempts) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.quizQuestionHistoryRepository = quizQuestionHistoryRepository;
        this.quizQuestionHistoryMapper = quizQuestionHistoryMapper;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizQuestionMapper = quizQuestionMapper;
        this.contentItems = contentItems;
        this.itemOverrides = itemOverrides;
        this.questions = questions;
        this.questionOptions = questionOptions;
        this.questionBlanks = questionBlanks;
        this.questionMatchPairs = questionMatchPairs;
        this.questionHotspots = questionHotspots;
        this.quizSubmissions = quizSubmissions;
        this.attempts = attempts;
    }

    /**
     * lms.StartQuizAttempt: a student of the placement opens their next attempt — their open attempt is resumed,
     * otherwise a fresh in-progress submission is created within the attempt window, the schedule and the attempt
     * limit. The question set (shuffled and limited as the quiz asks, stably per attempt) comes back without answers.
     */
    public QuizAttempt startQuizAttempt(UUID contentItemId) {
        UUID user = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            ContentItemEntity item = contentItems.findById(contentItemId)
                    .filter(e -> "quiz".equals(e.getItemKind()) && contentItems.isLearnerOf(e, user))
                    .orElseThrow(() -> new ResourceNotFoundException(ContentItemEntity.TABLE, contentItemId));
            QuizEntity quiz = repository.findById(item.getQuizId())
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, item.getQuizId()));
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            requireOpenWindow(item, quiz, user, now);
            int used = quizSubmissions.attemptsUsed(quiz.getId(), user, contentItemId);
            QuizSubmissionEntity attempt = quizSubmissions.openAttempt(quiz.getId(), user, contentItemId)
                    .orElse(null);
            boolean fresh = attempt == null;
            if (fresh) {
                if (quiz.getMaxAttempts() != null && used >= quiz.getMaxAttempts()) {
                    throw new ConflictException("NO_ATTEMPTS_LEFT", "Every allowed attempt has been used.");
                }
                attempt = QuizSubmissionEntity.builder().quizId(quiz.getId()).userId(user)
                        .courseId(item.getCourseId()).cohortId(item.getCohortId()).contentItemId(contentItemId)
                        .status("in_progress").startedAt(now).attemptNumber(used + 1).violationCount(0)
                        .isReassigned(false)
                        .passingPercentage(quiz.getPassingPercentage() == null ? null
                                : BigDecimal.valueOf(quiz.getPassingPercentage()))
                        .build();
                attempt.onCreatingData(Actor.current(), clock);
                attempt.setTenantId(tenant);
                attempt = quizSubmissions.insert(attempt);
                quizSubmissions.startAttempt(attempt.getId(), used + 1, now);
                attempt.setStatus("in_progress");
                attempt.setStartedAt(now);
                attempt.setAttemptNumber(used + 1);
                audit.recordCreate(QuizSubmissionEntity.TABLE, attempt.getId());
                outbox.publish(QuizSubmissionEntity.TABLE + ".start", QuizSubmissionEntity.TABLE, attempt.getId(),
                        LmsEvents.payload(attempt.getId()));
            }
            List<QuizQuestionView> delivered = questionViews(quiz, attempt.getId());
            if (fresh) {
                recordDelivery(attempt.getId(), delivered, now);
            }
            QuizAttempt view = new QuizAttempt(mapper.toDto(quiz), delivered, used,
                    QuizAttemptToken.of(attempt.getId(), attempt.getAttemptNumber(), attempt.getStartedAt()),
                    attempt.getId());
            view.setAttemptNumber(attempt.getAttemptNumber());
            view.setServerTime(now);
            view.setDeadlineAt(deadline(item, itemOverrides.ownOverride(item.getId(), user), quiz,
                    attempt.getStartedAt() == null ? now : attempt.getStartedAt()));
            return view;
        });
    }

    /**
     * Records the questions a fresh attempt was delivered with, in their order and with their marks, as blank answer
     * rows: saving, submitting and grading (QuizAttemptService) then work on exactly this set.
     */
    private void recordDelivery(UUID attemptId, List<QuizQuestionView> delivered, OffsetDateTime now) {
        Map<UUID, BigDecimal> marks = new LinkedHashMap<>();
        delivered.forEach(q -> marks.put(q.getQuestionId(), q.getMarks() == null ? BigDecimal.ZERO
                : new BigDecimal(q.getMarks())));
        Map<UUID, QuizAnswerKey> keys = attempts.keys(marks);
        UUID actor = AuthContext.require().userId();
        for (int order = 0; order < delivered.size(); order++) {
            attempts.insertAnswer(UuidV7.next(clock), attemptId, keys.get(delivered.get(order).getQuestionId()), null,
                    order, null, now, actor);
        }
    }

    /** 409 when the placement's attempt window or the quiz's schedule is closed for the student now. */
    private void requireOpenWindow(ContentItemEntity item, QuizEntity quiz, UUID user, OffsetDateTime now) {
        OffsetDateTime from = item.getAttemptFrom();
        OffsetDateTime until = item.getDueAt();
        Optional<ItemOverrideEntity> override = itemOverrides.ownOverride(item.getId(), user);
        if (override.isPresent()) {
            from = override.get().getAvailableFrom() == null ? from : override.get().getAvailableFrom();
            until = override.get().getDueAt() == null ? until : override.get().getDueAt();
        }
        if (from != null && now.isBefore(from) || until != null && now.isAfter(until)
                && !Boolean.TRUE.equals(item.getLateSubmissionAllowed())) {
            throw new ConflictException("ATTEMPT_WINDOW_CLOSED", "The placement takes no attempts now.");
        }
        if (Boolean.TRUE.equals(quiz.getSchedulingEnabled())
                && (quiz.getScheduleStart() != null && now.isBefore(quiz.getScheduleStart())
                        || quiz.getScheduleEnd() != null && now.isAfter(quiz.getScheduleEnd()))) {
            throw new ConflictException("QUIZ_NOT_SCHEDULED", "The quiz is outside its scheduled window.");
        }
    }

    /**
     * When the attempt must be handed in: the duration (and the student's extra time) from its start, capped by the
     * placement's close — its due time, or the end of its late days when it takes late work (none without them).
     */
    static OffsetDateTime deadline(ContentItemEntity item, Optional<ItemOverrideEntity> override, QuizEntity quiz,
            OffsetDateTime startedAt) {
        int extra = override.map(ItemOverrideEntity::getExtraTimeMinutes).filter(m -> m > 0).orElse(0);
        OffsetDateTime byDuration = quiz.getDurationMinutes() == null || quiz.getDurationMinutes() <= 0 ? null
                : startedAt.plusMinutes(quiz.getDurationMinutes() + extra);
        OffsetDateTime due = override.map(ItemOverrideEntity::getDueAt).orElse(item.getDueAt());
        OffsetDateTime closes = due == null || !Boolean.TRUE.equals(item.getLateSubmissionAllowed()) ? due
                : item.getLateSubmissionDays() == null ? null : due.plusDays(item.getLateSubmissionDays());
        if (byDuration == null) {
            return closes;
        }
        return closes == null || byDuration.isBefore(closes) ? byDuration : closes;
    }

    /** The attempt's questions without answers, shuffled and limited as the quiz asks, stable per attempt. */
    private List<QuizQuestionView> questionViews(QuizEntity quiz, UUID attemptId) {
        List<QuizQuestionEntity> links = new ArrayList<>(quizQuestionRepository.ofQuiz(quiz.getId()));
        if (Boolean.TRUE.equals(quiz.getShuffleQuestions())) {
            Collections.shuffle(links, new Random(attemptId.getMostSignificantBits()));
        }
        if (quiz.getLimitQuestionsTo() != null && links.size() > quiz.getLimitQuestionsTo()) {
            links = links.subList(0, quiz.getLimitQuestionsTo());
        }
        Map<UUID, QuestionEntity> byId = new HashMap<>();
        questions.findByIds(links.stream().map(QuizQuestionEntity::getQuestionId).toList())
                .forEach(question -> byId.put(question.getId(), question));
        Map<UUID, List<QuestionOptionEntity>> options = questionOptions.ofQuestions(ofType(byId, "choices", "true_false"));
        Map<UUID, List<QuestionBlankEntity>> blanks = questionBlanks.ofQuestions(ofType(byId, "fill_blank"));
        Map<UUID, List<QuestionMatchPairEntity>> matchPairs = questionMatchPairs.ofQuestions(ofType(byId, "matching"));
        Map<UUID, List<QuestionHotspotEntity>> hotspots = questionHotspots.ofQuestions(ofType(byId, "hotspot"));
        List<QuizQuestionView> views = new ArrayList<>();
        for (QuizQuestionEntity link : links) {
            QuestionEntity question = byId.get(link.getQuestionId());
            if (question == null) {
                continue;
            }
            BigDecimal marks = Boolean.TRUE.equals(quiz.getEqualQuestionWeighting())
                    && quiz.getMarksPerQuestion() != null ? quiz.getMarksPerQuestion()
                    : link.getMarks() != null ? link.getMarks() : question.getMarks();
            QuizQuestionView view = new QuizQuestionView(link.getId(), question.getId(),
                    question.getQuestionText(), question.getQuestionType(),
                    Boolean.TRUE.equals(question.getAllowsMultiple()), WireValues.decimal(marks));
            view.setMaxSelections(question.getMaxSelections());
            view.setCaseSensitive(question.getCaseSensitive());
            switch (question.getQuestionType()) {
                case "choices", "true_false" -> view.setOptions(options.getOrDefault(question.getId(), List.of()).stream()
                        .map(o -> new OptionView(o.getId(), o.getOptionText())).toList());
                case "fill_blank" -> view.setBlanks(blanks.getOrDefault(question.getId(), List.of()).stream()
                        .map(b -> new BlankView(b.getId(), b.getBlankText())).toList());
                case "matching" -> {
                    List<QuestionMatchPairEntity> pairs = matchPairs.getOrDefault(question.getId(), List.of());
                    view.setMatchLeft(pairs.stream().map(m -> new OptionView(m.getId(), m.getLeftText())).toList());
                    List<OptionView> right = new ArrayList<>(pairs.stream()
                            .map(m -> new OptionView(m.getId(), m.getRightText())).toList());
                    Collections.shuffle(right, new Random(attemptId.getLeastSignificantBits()));
                    view.setMatchRight(right);
                }
                case "hotspot" -> hotspots.getOrDefault(question.getId(), List.of()).stream().findFirst()
                        .ifPresent(h -> {
                            view.setHotspotImageFileId(h.getImageFileId());
                            view.setHotspotSelection(QuizQuestionView.HotspotSelectionEnum.fromValue(
                                    h.getHotspotType()));
                        });
                default -> { }
            }
            views.add(view);
        }
        return views;
    }

    /** The ids of the questions of these types, so each kind of answer part is read once for the whole attempt. */
    private static List<UUID> ofType(Map<UUID, QuestionEntity> questions, String... types) {
        Set<String> wanted = Set.of(types);
        return questions.values().stream().filter(q -> q.getQuestionType() != null
                && wanted.contains(q.getQuestionType())).map(QuestionEntity::getId).toList();
    }

    public Quiz getQuiz(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Quiz> listQuizs(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, QuizRepository.SORT_FIELDS, QuizRepository.DEFAULT_ORDER,
                QuizRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuizRepository.FILTER_FIELDS)
                .and(QuizRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<QuizEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Quiz createQuiz(Quiz request, boolean validateOnly) {
        validator.validate(request);
        QuizEntity entity = mapper.toEntity(request);
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
            QuizEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Quiz updateQuiz(UUID id, String ifMatch, QuizUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            QuizEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
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
    public void deleteQuiz(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            QuizEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByContentAuthorQuizId(id)) {
                throw new InvalidStateException(
                        "The quiz is still referenced by lms.content_author rows; remove those first.");
            }
            if (repository.referencedByContentItemQuizId(id)) {
                throw new InvalidStateException(
                        "The quiz is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByLessonActivityQuizId(id)) {
                throw new InvalidStateException(
                        "The quiz is still referenced by lms.lesson_activity rows; remove those first.");
            }
            if (repository.referencedByQuizSupersededById(id)) {
                throw new InvalidStateException("The quiz is still referenced by lms.quiz rows; remove those first.");
            }
            if (repository.referencedByQuizQuestionQuizId(id)) {
                throw new InvalidStateException(
                        "The quiz is still referenced by lms.quiz_question rows; remove those first.");
            }
            if (repository.referencedByQuizQuestionHistoryQuizId(id)) {
                throw new InvalidStateException(
                        "The quiz is still referenced by lms.quiz_question_history rows; remove those first.");
            }
            if (repository.referencedByQuizSubmissionQuizId(id)) {
                throw new InvalidStateException(
                        "The quiz is still referenced by lms.quiz_submission rows; remove those first.");
            }
            if (repository.referencedByTimetableEntryQuizId(id)) {
                throw new InvalidStateException(
                        "The quiz is still referenced by lms.timetable_entry rows; remove those first.");
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
    private QuizEntity loadVisible(DataScope scope, UUID id) {
        QuizEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(QuizEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(QuizEntity entity, QuizUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        patch.apply("lessonId", changes.getLessonId(), entity::setLessonId);
        patch.apply("maxAttempts", changes.getMaxAttempts(), entity::setMaxAttempts);
        patch.apply("passingPercentage", changes.getPassingPercentage(), entity::setPassingPercentage);
        patch.apply("totalMarks", changes.getTotalMarks(), v -> entity.setTotalMarks(WireValues.decimal(v)));
        patch.apply("showAnswers", changes.getShowAnswers(), entity::setShowAnswers);
        patch.apply("showSubmissionHistory", changes.getShowSubmissionHistory(), entity::setShowSubmissionHistory);
        patch.apply("shuffleQuestions", changes.getShuffleQuestions(), entity::setShuffleQuestions);
        patch.apply("limitQuestionsTo", changes.getLimitQuestionsTo(), entity::setLimitQuestionsTo);
        patch.apply("durationMinutes", changes.getDurationMinutes(), entity::setDurationMinutes);
        patch.apply("proctoringEnabled", changes.getProctoringEnabled(), entity::setProctoringEnabled);
        patch.apply("maxViolations", changes.getMaxViolations(), entity::setMaxViolations);
        patch.apply("negativeMarkingEnabled", changes.getNegativeMarkingEnabled(), entity::setNegativeMarkingEnabled);
        patch.apply("negativeMarks", changes.getNegativeMarks(), v -> entity.setNegativeMarks(WireValues.decimal(v)));
        patch.apply("schedulingEnabled", changes.getSchedulingEnabled(), entity::setSchedulingEnabled);
        patch.apply("scheduleStart", changes.getScheduleStart(), entity::setScheduleStart);
        patch.apply("scheduleEnd", changes.getScheduleEnd(), entity::setScheduleEnd);
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("isAutoGraded", changes.getIsAutoGraded(), entity::setIsAutoGraded);
        patch.apply("isAdaptive", changes.getIsAdaptive(), entity::setIsAdaptive);
        patch.apply("equalQuestionWeighting", changes.getEqualQuestionWeighting(), entity::setEqualQuestionWeighting);
        patch.apply("marksPerQuestion", changes.getMarksPerQuestion(), v -> entity.setMarksPerQuestion(WireValues.decimal(v)));
        patch.apply("allowPause", changes.getAllowPause(), entity::setAllowPause);
        patch.apply("attemptScoring", changes.getAttemptScoring(),
                                                v -> entity.setAttemptScoring(v == null ? null : v.getValue()));
        patch.apply("dropLowestAttempt", changes.getDropLowestAttempt(), entity::setDropLowestAttempt);
        patch.apply("drawsFromPool", changes.getDrawsFromPool(), entity::setDrawsFromPool);
        patch.apply("showFeedback", changes.getShowFeedback(), entity::setShowFeedback);
        patch.apply("isInRepository", changes.getIsInRepository(), entity::setIsInRepository);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(QuizEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getPassingPercentage() == null) {
            errors.add(new FieldError("passingPercentage", "must not be null", "NotNull"));
        }
        if (entity.getTotalMarks() == null) {
            errors.add(new FieldError("totalMarks", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("totalMarks", WireValues.decimal(entity.getTotalMarks()), 8, 2, errors);
        if (!creating && entity.getShowAnswers() == null) {
            errors.add(new FieldError("showAnswers", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShowSubmissionHistory() == null) {
            errors.add(new FieldError("showSubmissionHistory", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShuffleQuestions() == null) {
            errors.add(new FieldError("shuffleQuestions", "must not be null", "NotNull"));
        }
        if (!creating && entity.getProctoringEnabled() == null) {
            errors.add(new FieldError("proctoringEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getNegativeMarkingEnabled() == null) {
            errors.add(new FieldError("negativeMarkingEnabled", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("negativeMarks", WireValues.decimal(entity.getNegativeMarks()), 8, 2, errors);
        if (!creating && entity.getSchedulingEnabled() == null) {
            errors.add(new FieldError("schedulingEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsAutoGraded() == null) {
            errors.add(new FieldError("isAutoGraded", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsAdaptive() == null) {
            errors.add(new FieldError("isAdaptive", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEqualQuestionWeighting() == null) {
            errors.add(new FieldError("equalQuestionWeighting", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("marksPerQuestion", WireValues.decimal(entity.getMarksPerQuestion()), 8, 2, errors);
        if (!creating && entity.getAllowPause() == null) {
            errors.add(new FieldError("allowPause", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAttemptScoring() == null) {
            errors.add(new FieldError("attemptScoring", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDropLowestAttempt() == null) {
            errors.add(new FieldError("dropLowestAttempt", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDrawsFromPool() == null) {
            errors.add(new FieldError("drawsFromPool", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShowFeedback() == null) {
            errors.add(new FieldError("showFeedback", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsInRepository() == null) {
            errors.add(new FieldError("isInRepository", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getLessonId() != null && !repository.existsLessonId(entity.getLessonId())) {
            errors.add(new FieldError("lessonId", "lesson does not exist", "NOT_FOUND"));
        }
        if (entity.getSupersededById() != null && !repository.existsSupersededById(entity.getSupersededById())) {
            errors.add(new FieldError("supersededById", "quiz does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(QuizEntity entity) {

    }

    // ---- QuizQuestionHistory: a record of the Quiz aggregate ----

    public QuizQuestionHistory getQuizQuestionHistory(UUID quizId, UUID id) {
        return doInReadTransaction(() -> {
            return quizQuestionHistoryMapper.toDto(loadVisibleQuizQuestionHistory(DataScope.current(
                    QUIZ_QUESTION_HISTORY_PERMISSION_GET), quizId, id));
        });
    }

    public Page<QuizQuestionHistory> listQuizQuestionHistorys(UUID quizId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuizQuestionHistoryRepository.SORT_FIELDS,
                        QuizQuestionHistoryRepository.DEFAULT_ORDER,
                QuizQuestionHistoryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuizQuestionHistoryRepository.FILTER_FIELDS)
                .and(QuizQuestionHistoryRepository.ofParent(quizId))
                        .and(QuizQuestionHistoryRepository.scope(DataScope.current(QUIZ_QUESTION_HISTORY_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + quizId, clock);
        return doInReadTransaction(() -> {
            Page<QuizQuestionHistoryEntity> rows = quizQuestionHistoryRepository.list(page, where);
            return rows.map(quizQuestionHistoryMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuizQuestionHistoryEntity loadVisibleQuizQuestionHistory(DataScope scope, UUID quizId, UUID id) {
        QuizQuestionHistoryEntity entity = quizQuestionHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUIZ_QUESTION_HISTORY_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuizQuestionHistory(entity)) || !quizId.equals(entity.getQuizId())) {
            throw new ResourceNotFoundException(QUIZ_QUESTION_HISTORY_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.quiz the row belongs to. */
    private Optional<UUID> institutionOfQuizQuestionHistory(QuizQuestionHistoryEntity entity) {
        return quizQuestionHistoryRepository.institutionOf(entity.getQuizId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuizQuestionHistory(QuizQuestionHistoryEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuizId() == null) {
            errors.add(new FieldError("quizId", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("marks", WireValues.decimal(entity.getMarks()), 8, 2, errors);
        if (entity.getQuestionId() != null && !quizQuestionHistoryRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizId() != null && !quizQuestionHistoryRepository.existsQuizId(entity.getQuizId())) {
            errors.add(new FieldError("quizId", "quiz does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuizQuestionHistory(QuizQuestionHistoryEntity entity) {

    }

    // ---- QuizQuestion: a record of the Quiz aggregate ----

    public QuizQuestion getQuizQuestion(UUID quizId, UUID id) {
        return doInReadTransaction(() -> {
            return quizQuestionMapper.toDto(loadVisibleQuizQuestion(DataScope.current(QUIZ_QUESTION_PERMISSION_GET), quizId, id));
        });
    }

    public Page<QuizQuestion> listQuizQuestions(UUID quizId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuizQuestionRepository.SORT_FIELDS, QuizQuestionRepository.DEFAULT_ORDER,
                QuizQuestionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuizQuestionRepository.FILTER_FIELDS)
                .and(QuizQuestionRepository.ofParent(quizId))
                        .and(QuizQuestionRepository.scope(DataScope.current(QUIZ_QUESTION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + quizId, clock);
        return doInReadTransaction(() -> {
            Page<QuizQuestionEntity> rows = quizQuestionRepository.list(page, where);
            return rows.map(quizQuestionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuizQuestion createQuizQuestion(UUID quizId, QuizQuestion request, boolean validateOnly) {
        validator.validate(request);
        QuizQuestionEntity entity = quizQuestionMapper.toEntity(request);
        entity.setQuizId(quizId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(QUIZ_QUESTION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!quizQuestionRepository.existsQuizId(entity.getQuizId())) {
                throw new ResourceNotFoundException("lms.quiz", entity.getQuizId());
            }
            if (!scope.canRead(institutionOfQuizQuestion(entity))) {
                throw new ResourceNotFoundException("lms.quiz", entity.getQuizId());
            }
            assertValidQuizQuestion(entity, true);
            scope.requireWritable(institutionOfQuizQuestion(entity));
            assertUniqueQuizQuestion(entity);
            if (validateOnly) {
                return quizQuestionMapper.toDto(entity);
            }
            QuizQuestionEntity stored = quizQuestionRepository.insert(entity);
            audit.recordCreate(QUIZ_QUESTION_AGGREGATE, stored.getId());
            outbox.publish(QUIZ_QUESTION_AGGREGATE + LmsEvents.CREATED, QUIZ_QUESTION_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return quizQuestionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuizQuestion updateQuizQuestion(UUID quizId, UUID id, String ifMatch, QuizQuestionUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUIZ_QUESTION_PERMISSION_UPDATE);
            QuizQuestionEntity entity = loadVisibleQuizQuestion(scope, quizId, id);
            scope.requireWritable(institutionOfQuizQuestion(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyQuizQuestion(entity, changes);
            validator.validate(quizQuestionMapper.toDto(entity));
            assertValidQuizQuestion(entity, false);
            scope.requireWritable(institutionOfQuizQuestion(entity));
            assertUniqueQuizQuestion(entity);
            if (fields.isEmpty()) {
                return quizQuestionMapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return quizQuestionMapper.toDto(entity);
            }
            if (quizQuestionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(QUIZ_QUESTION_AGGREGATE, id, fields);
            outbox.publish(QUIZ_QUESTION_AGGREGATE + LmsEvents.UPDATED, QUIZ_QUESTION_AGGREGATE, id, LmsEvents.payload(id));
            return quizQuestionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteQuizQuestion(UUID quizId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUIZ_QUESTION_PERMISSION_DELETE);
            QuizQuestionEntity entity = loadVisibleQuizQuestion(scope, quizId, id);
            scope.requireWritable(institutionOfQuizQuestion(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (quizQuestionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(QUIZ_QUESTION_AGGREGATE, id);
            outbox.publish(QUIZ_QUESTION_AGGREGATE + LmsEvents.DELETED, QUIZ_QUESTION_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuizQuestionEntity loadVisibleQuizQuestion(DataScope scope, UUID quizId, UUID id) {
        QuizQuestionEntity entity = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUIZ_QUESTION_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuizQuestion(entity)) || !quizId.equals(entity.getQuizId())) {
            throw new ResourceNotFoundException(QUIZ_QUESTION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.quiz the row belongs to. */
    private Optional<UUID> institutionOfQuizQuestion(QuizQuestionEntity entity) {
        return quizQuestionRepository.institutionOf(entity.getQuizId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyQuizQuestion(QuizQuestionEntity entity, QuizQuestionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("questionId", changes.getQuestionId(), entity::setQuestionId);
        patch.apply("marks", changes.getMarks(), v -> entity.setMarks(WireValues.decimal(v)));
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuizQuestion(QuizQuestionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuizId() == null) {
            errors.add(new FieldError("quizId", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        if (entity.getMarks() == null) {
            errors.add(new FieldError("marks", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("marks", WireValues.decimal(entity.getMarks()), 8, 2, errors);
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() != null && !quizQuestionRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizId() != null && !quizQuestionRepository.existsQuizId(entity.getQuizId())) {
            errors.add(new FieldError("quizId", "quiz does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuizQuestion(QuizQuestionEntity entity) {
        if (quizQuestionRepository.takenQuizIdQuestionId1(entity)) {
            throw new DuplicateResourceException(
                    "A quiz question with the same quiz id and question id already exists.");
        }
    }
}
