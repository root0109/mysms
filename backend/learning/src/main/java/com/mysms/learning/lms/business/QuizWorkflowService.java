package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.learning.lms.api.model.AnswerCheck;
import com.mysms.learning.lms.api.model.CheckQuizAnswerRequest;
import com.mysms.learning.lms.api.model.QuizQuestionList;
import com.mysms.learning.lms.api.model.SetQuizQuestionsRequest;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.QuizEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.QuizQuestionMapper;
import com.mysms.learning.lms.model.QuizAnswerKey;
import com.mysms.learning.lms.repository.QuizAttemptRepository;
import com.mysms.learning.lms.repository.QuizContentRepository;
import com.mysms.learning.lms.repository.QuizContentRepository.Placed;
import com.mysms.learning.lms.repository.QuizContentRepository.QuizHead;
import com.mysms.learning.lms.repository.QuizQuestionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The workflows on a quiz's question set: SetQuizQuestions changes which
 * bank questions the quiz asks, CheckQuizAnswer gives a student instant feedback on a practice quiz.
 *
 * <p>A published quiz never changes (a new version is made instead): 409 {@code QUIZ_PUBLISHED}. When the quiz has
 * attempts, a removed question's marks and position are kept in {@code lms.quiz_question_history}, so earlier attempts
 * can still be explained. The quiz's total marks follow its questions. Instant feedback is given only for a practice
 * quiz — one that shows feedback and answers and that no graded placement counts — else 409
 * {@code FEEDBACK_NOT_AVAILABLE}, so it can never reveal the key of a graded quiz.
 */
@Service
public class QuizWorkflowService extends AppTransactionManager {

    static final String AGGREGATE = QuizEntity.TABLE;
    static final String PERMISSION_SET_QUESTIONS = "lms.quiz.set_quiz_questions";
    static final String PERMISSION_CHECK_ANSWER = "lms.quiz.check_quiz_answer";
    static final int MAX_QUESTIONS = 500;

    private final QuizContentRepository repository;
    private final QuizAttemptRepository attempts;
    private final QuizQuestionRepository quizQuestions;
    private final QuizQuestionMapper quizQuestionMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public QuizWorkflowService(QuizContentRepository repository, QuizAttemptRepository attempts,
            QuizQuestionRepository quizQuestions, QuizQuestionMapper quizQuestionMapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.attempts = attempts;
        this.quizQuestions = quizQuestions;
        this.quizQuestionMapper = quizQuestionMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * Adds the named questions (and every question of the named pool) at the end, with the question's own marks (else
     * the quiz's marks per question, else 1), and removes the named ones; a question already in the quiz is not
     * added twice. Answers the quiz's questions as they now stand.
     */
    public QuizQuestionList setQuizQuestions(UUID id, SetQuizQuestionsRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_SET_QUESTIONS);
        UUID actor = Actor.current().orElse(null);
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            QuizHead quiz = visible(scope, repository.lock(id), id);
            scope.requireWritable(Optional.ofNullable(quiz.institutionId()));
            if (quiz.publishedAt() != null) {
                throw new ConflictException("QUIZ_PUBLISHED", "A published quiz does not change; make a new version.");
            }
            Set<UUID> adding = new LinkedHashSet<>(orEmpty(request.getAddQuestionIds()));
            Set<UUID> removing = new LinkedHashSet<>(orEmpty(request.getRemoveQuestionIds()));
            List<FieldError> errors = new ArrayList<>();
            if (request.getQuestionPoolId() != null) {
                if (repository.poolExists(request.getQuestionPoolId())) {
                    adding.addAll(repository.poolQuestions(request.getQuestionPoolId()));
                } else {
                    errors.add(new FieldError("questionPoolId", "question pool does not exist", "NOT_FOUND"));
                }
            }
            adding.removeAll(removing);
            Map<UUID, Optional<BigDecimal>> marks = repository.questionMarks(adding);
            orEmpty(request.getAddQuestionIds()).stream().filter(q -> !marks.containsKey(q) && !removing.contains(q))
                    .distinct().forEach(q -> errors.add(new FieldError("addQuestionIds",
                            "question " + q + " does not exist or is retired", "NOT_FOUND")));
            List<Placed> placed = repository.placed(id);
            Set<UUID> present = new LinkedHashSet<>();
            placed.forEach(p -> present.add(p.questionId()));
            removing.stream().filter(q -> !present.contains(q)).forEach(q -> errors.add(
                    new FieldError("removeQuestionIds", "question " + q + " is not in the quiz", "NOT_FOUND")));
            adding.removeIf(present::contains);
            if (present.size() - removing.size() + adding.size() > MAX_QUESTIONS) {
                errors.add(new FieldError("addQuestionIds", "a quiz has at most " + MAX_QUESTIONS + " questions",
                        "Size"));
            }
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            boolean keepHistory = repository.hasAttempts(id);
            for (Placed p : placed) {
                if (removing.contains(p.questionId())) {
                    repository.remove(p.id(), now, actor);
                    if (keepHistory) {
                        repository.keepHistory(UuidV7.next(clock), id, p, now, actor);
                    }
                }
            }
            int next = placed.stream().mapToInt(Placed::sortOrder).max().orElse(0);
            for (UUID questionId : adding) {
                if (marks.containsKey(questionId)) {
                    BigDecimal value = marks.get(questionId).orElse(quiz.marksPerQuestion() != null
                            ? quiz.marksPerQuestion() : BigDecimal.ONE);
                    repository.place(UuidV7.next(clock), id, questionId, value, ++next, now, actor);
                }
            }
            List<Placed> after = repository.placed(id);
            BigDecimal total = after.stream().map(Placed::marks).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (repository.setTotal(id, quiz.rowVersion(), total, now, actor) == 0) {
                throw new PreconditionFailedException("The quiz changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, id, "setQuestions", List.of("questions", "totalMarks"), null);
            outbox.publish(AGGREGATE + ".setQuestions", AGGREGATE, id, LmsEvents.payload(id));
            QuizQuestionList list = new QuizQuestionList();
            list.setItems(after.stream().map(p -> quizQuestions.findById(p.id()).orElseThrow())
                    .map(quizQuestionMapper::toDto).toList());
            return list;
        });
    }

    /** Whether the answer to one question of a practice quiz is right, with its explanation; nothing is stored. */
    public AnswerCheck checkQuizAnswer(UUID id, CheckQuizAnswerRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_CHECK_ANSWER);
        return doInReadTransaction(() -> {
            QuizHead quiz = visible(scope, repository.head(id), id);
            if (!quiz.showFeedback() || !quiz.showAnswers() || repository.gradedPlacement(id)) {
                throw new ConflictException("FEEDBACK_NOT_AVAILABLE",
                        "Instant feedback is given only for practice quizzes.");
            }
            Optional<Placed> placed = repository.placed(id).stream()
                    .filter(p -> p.questionId().equals(request.getQuestionId())).findFirst();
            if (placed.isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("questionId",
                        "is not a question of this quiz", "NOT_FOUND")));
            }
            QuizAnswerKey key = attempts.keys(Map.of(placed.get().questionId(), placed.get().marks()))
                    .get(placed.get().questionId());
            if (key == null || key.markedByTeacher()) {
                throw new ConflictException("FEEDBACK_NOT_AVAILABLE", "This question is marked by a teacher.");
            }
            QuizGrader.Marked marked = QuizGrader.mark(key, request.getAnswer(), null);
            AnswerCheck check = new AnswerCheck().isCorrect(Boolean.TRUE.equals(marked.correct()));
            check.setExplanation(marked.feedback());
            check.setCorrectOptionIds(key.correctOptionIds());
            return check;
        });
    }

    private static QuizHead visible(DataScope scope, Optional<QuizHead> found, UUID id) {
        QuizHead quiz = found.orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(Optional.ofNullable(quiz.institutionId()))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return quiz;
    }

    private static <T> List<T> orEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
