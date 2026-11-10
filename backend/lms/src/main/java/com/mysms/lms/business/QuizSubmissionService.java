package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
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
import com.mysms.lms.api.model.QuizAnswer;
import com.mysms.lms.api.model.QuizSubmission;
import com.mysms.lms.api.model.QuizViolation;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.QuizAnswerEntity;
import com.mysms.lms.domain.QuizSubmissionEntity;
import com.mysms.lms.domain.QuizViolationEntity;
import com.mysms.lms.mapper.QuizAnswerMapper;
import com.mysms.lms.mapper.QuizSubmissionMapper;
import com.mysms.lms.mapper.QuizViolationMapper;
import com.mysms.lms.repository.QuizAnswerRepository;
import com.mysms.lms.repository.QuizSubmissionRepository;
import com.mysms.lms.repository.QuizViolationRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.quiz_submission (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a QuizSubmission
 * are served here too — QuizAnswer, QuizViolation.
 */
@Service
public class QuizSubmissionService extends AppTransactionManager {

    public static final String AGGREGATE = QuizSubmissionEntity.TABLE;
    static final String PERMISSION_LIST = "lms.quiz_submission.read";
    static final String PERMISSION_GET = "lms.quiz_submission.read";

    public static final String QUIZ_ANSWER_AGGREGATE = QuizAnswerEntity.TABLE;
    static final String QUIZ_ANSWER_PERMISSION_LIST = "lms.quiz_answer.read";
    static final String QUIZ_ANSWER_PERMISSION_GET = "lms.quiz_answer.read";

    public static final String QUIZ_VIOLATION_AGGREGATE = QuizViolationEntity.TABLE;
    static final String QUIZ_VIOLATION_PERMISSION_LIST = "lms.quiz_violation.read";
    static final String QUIZ_VIOLATION_PERMISSION_GET = "lms.quiz_violation.read";

    private final QuizSubmissionRepository repository;
    private final QuizSubmissionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizAnswerMapper quizAnswerMapper;
    private final QuizViolationRepository quizViolationRepository;
    private final QuizViolationMapper quizViolationMapper;

    public QuizSubmissionService(QuizSubmissionRepository repository,
            QuizSubmissionMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            QuizAnswerRepository quizAnswerRepository,
            QuizAnswerMapper quizAnswerMapper,
            QuizViolationRepository quizViolationRepository,
            QuizViolationMapper quizViolationMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizAnswerMapper = quizAnswerMapper;
        this.quizViolationRepository = quizViolationRepository;
        this.quizViolationMapper = quizViolationMapper;
    }

    public QuizSubmission getQuizSubmission(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<QuizSubmission> listQuizSubmissions(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuizSubmissionRepository.SORT_FIELDS, QuizSubmissionRepository.DEFAULT_ORDER,
                QuizSubmissionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuizSubmissionRepository.FILTER_FIELDS)
                .and(QuizSubmissionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<QuizSubmissionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuizSubmissionEntity loadVisible(DataScope scope, UUID id) {
        QuizSubmissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.quiz it belongs to). */
    private Optional<UUID> institutionOf(QuizSubmissionEntity entity) {
        return repository.institutionOf(entity.getQuizId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(QuizSubmissionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuizId() == null) {
            errors.add(new FieldError("quizId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("passingPercentage", WireValues.decimal(entity.getPassingPercentage()), 5, 2, errors);
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getContentItemId() != null && !repository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizId() != null && !repository.existsQuizId(entity.getQuizId())) {
            errors.add(new FieldError("quizId", "quiz does not exist", "NOT_FOUND"));
        }
        if (entity.getReassignedFromId() != null && !repository.existsReassignedFromId(entity.getReassignedFromId())) {
            errors.add(new FieldError("reassignedFromId", "quiz submission does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(QuizSubmissionEntity entity) {
        if (entity.getContentItemId() != null
                && repository.takenQuizIdUserIdContentItemIdAttemptNumberAttempt(entity)) {
            throw new DuplicateResourceException(
                    "A quiz submission with the same quiz id and user id and content item id and attempt number "
                            + "already exists.");
        }
    }

    // ---- QuizAnswer: a record of the QuizSubmission aggregate ----

    public QuizAnswer getQuizAnswer(UUID quizSubmissionId, UUID id) {
        return doInReadTransaction(() -> {
            return quizAnswerMapper.toDto(loadVisibleQuizAnswer(DataScope.current(QUIZ_ANSWER_PERMISSION_GET),
                    quizSubmissionId, id));
        });
    }

    public Page<QuizAnswer> listQuizAnswers(UUID quizSubmissionId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, QuizAnswerRepository.SORT_FIELDS, QuizAnswerRepository.DEFAULT_ORDER,
                QuizAnswerRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuizAnswerRepository.FILTER_FIELDS)
                .and(QuizAnswerRepository.ofParent(quizSubmissionId))
                        .and(QuizAnswerRepository.scope(DataScope.current(QUIZ_ANSWER_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + quizSubmissionId, clock);
        return doInReadTransaction(() -> {
            Page<QuizAnswerEntity> rows = quizAnswerRepository.list(page, where);
            return rows.map(quizAnswerMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuizAnswerEntity loadVisibleQuizAnswer(DataScope scope, UUID quizSubmissionId, UUID id) {
        QuizAnswerEntity entity = quizAnswerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUIZ_ANSWER_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuizAnswer(entity)) || !quizSubmissionId.equals(entity.getQuizSubmissionId())) {
            throw new ResourceNotFoundException(QUIZ_ANSWER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.quiz_submission → lms.quiz it belongs to). */
    private Optional<UUID> institutionOfQuizAnswer(QuizAnswerEntity entity) {
        return quizAnswerRepository.institutionOf(entity.getQuizSubmissionId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuizAnswer(QuizAnswerEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuizSubmissionId() == null) {
            errors.add(new FieldError("quizSubmissionId", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("marksAwarded", WireValues.decimal(entity.getMarksAwarded()), 8, 2, errors);
        WireValues.checkDecimal("marksPossible", WireValues.decimal(entity.getMarksPossible()), 8, 2, errors);
        if (entity.getLearningObjectiveId() != null
                && !quizAnswerRepository.existsLearningObjectiveId(entity.getLearningObjectiveId())) {
            errors.add(new FieldError("learningObjectiveId", "learning objective does not exist", "NOT_FOUND"));
        }
        if (entity.getQuestionId() != null && !quizAnswerRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizSubmissionId() != null && !quizAnswerRepository.existsQuizSubmissionId(entity.getQuizSubmissionId())) {
            errors.add(new FieldError("quizSubmissionId", "quiz submission does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuizAnswer(QuizAnswerEntity entity) {

    }

    // ---- QuizViolation: a record of the QuizSubmission aggregate ----

    public QuizViolation getQuizViolation(UUID quizSubmissionId, UUID id) {
        return doInReadTransaction(() -> {
            return quizViolationMapper.toDto(loadVisibleQuizViolation(DataScope.current(QUIZ_VIOLATION_PERMISSION_GET),
                    quizSubmissionId, id));
        });
    }

    public Page<QuizViolation> listQuizViolations(UUID quizSubmissionId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuizViolationRepository.SORT_FIELDS, QuizViolationRepository.DEFAULT_ORDER,
                QuizViolationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuizViolationRepository.FILTER_FIELDS)
                .and(QuizViolationRepository.ofParent(quizSubmissionId))
                        .and(QuizViolationRepository.scope(DataScope.current(QUIZ_VIOLATION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + quizSubmissionId, clock);
        return doInReadTransaction(() -> {
            Page<QuizViolationEntity> rows = quizViolationRepository.list(page, where);
            return rows.map(quizViolationMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuizViolationEntity loadVisibleQuizViolation(DataScope scope, UUID quizSubmissionId, UUID id) {
        QuizViolationEntity entity = quizViolationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUIZ_VIOLATION_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuizViolation(entity)) || !quizSubmissionId.equals(entity.getQuizSubmissionId())) {
            throw new ResourceNotFoundException(QUIZ_VIOLATION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.quiz_submission → lms.quiz it belongs to). */
    private Optional<UUID> institutionOfQuizViolation(QuizViolationEntity entity) {
        return quizViolationRepository.institutionOf(entity.getQuizSubmissionId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuizViolation(QuizViolationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuizSubmissionId() == null) {
            errors.add(new FieldError("quizSubmissionId", "must not be null", "NotNull"));
        }
        if (entity.getEventType() == null) {
            errors.add(new FieldError("eventType", "must not be null", "NotNull"));
        }
        if (entity.getSeverity() == null) {
            errors.add(new FieldError("severity", "must not be null", "NotNull"));
        }
        if (entity.getQuizSubmissionId() != null
                && !quizViolationRepository.existsQuizSubmissionId(entity.getQuizSubmissionId())) {
            errors.add(new FieldError("quizSubmissionId", "quiz submission does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuizViolation(QuizViolationEntity entity) {

    }
}
