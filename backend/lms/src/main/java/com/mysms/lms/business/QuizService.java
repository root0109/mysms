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
import com.mysms.lms.api.model.Quiz;
import com.mysms.lms.api.model.QuizQuestion;
import com.mysms.lms.api.model.QuizQuestionHistory;
import com.mysms.lms.api.model.QuizQuestionUpdate;
import com.mysms.lms.api.model.QuizUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.QuizEntity;
import com.mysms.lms.domain.QuizQuestionEntity;
import com.mysms.lms.domain.QuizQuestionHistoryEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.QuizMapper;
import com.mysms.lms.mapper.QuizQuestionHistoryMapper;
import com.mysms.lms.mapper.QuizQuestionMapper;
import com.mysms.lms.repository.QuizQuestionHistoryRepository;
import com.mysms.lms.repository.QuizQuestionRepository;
import com.mysms.lms.repository.QuizRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.quiz (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Quiz
 * are served here too — QuizQuestionHistory, QuizQuestion.
 */
@Service
public class QuizService extends AppTransactionManager {

    public static final String AGGREGATE = QuizEntity.TABLE;
    static final String PERMISSION_LIST = "lms.quiz.read";
    static final String PERMISSION_GET = "lms.quiz.read";
    static final String PERMISSION_CREATE = "lms.quiz.create";
    static final String PERMISSION_UPDATE = "lms.quiz.update";
    static final String PERMISSION_DELETE = "lms.quiz.delete";
    /** The contract's QuizUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("title", "courseId", "lessonId", "maxAttempts", "passingPercentage", "totalMarks", "showAnswers",
            "showSubmissionHistory", "shuffleQuestions", "limitQuestionsTo", "durationMinutes", "proctoringEnabled",
            "maxViolations", "negativeMarkingEnabled", "negativeMarks", "schedulingEnabled", "scheduleStart",
            "scheduleEnd", "institutionId", "description", "isAutoGraded", "isAdaptive", "equalQuestionWeighting",
            "marksPerQuestion", "allowPause", "attemptScoring", "dropLowestAttempt", "drawsFromPool", "showFeedback",
            "isInRepository");

    public static final String QUIZ_QUESTION_HISTORY_AGGREGATE = QuizQuestionHistoryEntity.TABLE;
    static final String QUIZ_QUESTION_HISTORY_PERMISSION_LIST = "lms.quiz_question_history.read";
    static final String QUIZ_QUESTION_HISTORY_PERMISSION_GET = "lms.quiz_question_history.read";

    public static final String QUIZ_QUESTION_AGGREGATE = QuizQuestionEntity.TABLE;
    static final String QUIZ_QUESTION_PERMISSION_LIST = "lms.quiz_question.read";
    static final String QUIZ_QUESTION_PERMISSION_GET = "lms.quiz_question.read";
    static final String QUIZ_QUESTION_PERMISSION_CREATE = "lms.quiz_question.create";
    static final String QUIZ_QUESTION_PERMISSION_UPDATE = "lms.quiz_question.update";
    static final String QUIZ_QUESTION_PERMISSION_DELETE = "lms.quiz_question.delete";
    /** The contract's QuizQuestionUpdate fields, in apply order. */
    static final List<String> QUIZ_QUESTION_WRITABLE = List.of("questionId", "marks", "sortOrder");

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
            QuizQuestionMapper quizQuestionMapper) {
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Quiz updateQuiz(UUID id, String ifMatch, QuizUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            QuizEntity entity = loadVisible(scope, id);
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

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(QuizEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected QuizUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(QuizEntity entity, QuizUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "lessonId" -> entity.setLessonId(changes.getLessonId());
                case "maxAttempts" -> entity.setMaxAttempts(changes.getMaxAttempts());
                case "passingPercentage" -> entity.setPassingPercentage(changes.getPassingPercentage());
                case "totalMarks" -> entity.setTotalMarks(WireValues.decimal(changes.getTotalMarks()));
                case "showAnswers" -> entity.setShowAnswers(changes.getShowAnswers());
                case "showSubmissionHistory" -> entity.setShowSubmissionHistory(changes.getShowSubmissionHistory());
                case "shuffleQuestions" -> entity.setShuffleQuestions(changes.getShuffleQuestions());
                case "limitQuestionsTo" -> entity.setLimitQuestionsTo(changes.getLimitQuestionsTo());
                case "durationMinutes" -> entity.setDurationMinutes(changes.getDurationMinutes());
                case "proctoringEnabled" -> entity.setProctoringEnabled(changes.getProctoringEnabled());
                case "maxViolations" -> entity.setMaxViolations(changes.getMaxViolations());
                case "negativeMarkingEnabled" -> entity.setNegativeMarkingEnabled(changes.getNegativeMarkingEnabled());
                case "negativeMarks" -> entity.setNegativeMarks(WireValues.decimal(changes.getNegativeMarks()));
                case "schedulingEnabled" -> entity.setSchedulingEnabled(changes.getSchedulingEnabled());
                case "scheduleStart" -> entity.setScheduleStart(changes.getScheduleStart());
                case "scheduleEnd" -> entity.setScheduleEnd(changes.getScheduleEnd());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "description" -> entity.setDescription(changes.getDescription());
                case "isAutoGraded" -> entity.setIsAutoGraded(changes.getIsAutoGraded());
                case "isAdaptive" -> entity.setIsAdaptive(changes.getIsAdaptive());
                case "equalQuestionWeighting" -> entity.setEqualQuestionWeighting(changes.getEqualQuestionWeighting());
                case "marksPerQuestion" -> entity.setMarksPerQuestion(
                        WireValues.decimal(changes.getMarksPerQuestion()));
                case "allowPause" -> entity.setAllowPause(changes.getAllowPause());
                case "attemptScoring" -> entity.setAttemptScoring(Optional.ofNullable(changes.getAttemptScoring())
                        .map(v -> v.getValue()).orElse(null));
                case "dropLowestAttempt" -> entity.setDropLowestAttempt(changes.getDropLowestAttempt());
                case "drawsFromPool" -> entity.setDrawsFromPool(changes.getDrawsFromPool());
                case "showFeedback" -> entity.setShowFeedback(changes.getShowFeedback());
                case "isInRepository" -> entity.setIsInRepository(changes.getIsInRepository());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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

    /** The institution that owns the row (the institution of the lms.quiz it belongs to). */
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public QuizQuestion updateQuizQuestion(UUID quizId, UUID id, String ifMatch, QuizQuestionUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, QUIZ_QUESTION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUIZ_QUESTION_PERMISSION_UPDATE);
            QuizQuestionEntity entity = loadVisibleQuizQuestion(scope, quizId, id);
            scope.requireWritable(institutionOfQuizQuestion(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyQuizQuestion(entity, changes, fields);
            validator.validate(quizQuestionMapper.toDto(entity));
            assertValidQuizQuestion(entity, false);
            scope.requireWritable(institutionOfQuizQuestion(entity));
            assertUniqueQuizQuestion(entity);
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

    /** The institution that owns the row (the institution of the lms.quiz it belongs to). */
    private Optional<UUID> institutionOfQuizQuestion(QuizQuestionEntity entity) {
        return quizQuestionRepository.institutionOf(entity.getQuizId());
    }

    /**
     * Applies the selected QuizQuestionUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyQuizQuestion(QuizQuestionEntity entity, QuizQuestionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "questionId" -> entity.setQuestionId(changes.getQuestionId());
                case "marks" -> entity.setMarks(WireValues.decimal(changes.getMarks()));
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
