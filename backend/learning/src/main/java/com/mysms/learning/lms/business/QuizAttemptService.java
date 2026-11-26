package com.mysms.learning.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.api.model.AnswerGrade;
import com.mysms.learning.lms.api.model.GradeQuizAnswersRequest;
import com.mysms.learning.lms.api.model.PauseQuizAttemptRequest;
import com.mysms.learning.lms.api.model.QuizAnswerInput;
import com.mysms.learning.lms.api.model.QuizAutosaveResult;
import com.mysms.learning.lms.api.model.QuizSubmission;
import com.mysms.learning.lms.api.model.ResumeQuizAttemptRequest;
import com.mysms.learning.lms.api.model.SaveQuizResponsesRequest;
import com.mysms.learning.lms.api.model.SubmitQuizRequest;
import com.mysms.learning.lms.api.model.ViolationEvent;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.QuizSubmissionEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.QuizSubmissionMapper;
import com.mysms.learning.lms.model.QuizAnswerKey;
import com.mysms.learning.lms.repository.GradebookPostingRepository;
import com.mysms.learning.lms.repository.QuizAttemptRepository;
import com.mysms.learning.lms.repository.QuizAttemptRepository.Attempt;
import com.mysms.learning.lms.repository.QuizAttemptRepository.Outcome;
import com.mysms.learning.lms.repository.QuizAttemptRepository.StoredAnswer;
import com.mysms.learning.lms.repository.QuizSubmissionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * The steps of a running quiz attempt (lms.quiz_submission, started by StartQuizAttempt): autosave, pause, resume,
 * submit and the teacher's marking of answers that need a person.
 *
 * <p>The student's own steps act only on the student's own attempt — anyone else's is 404 — and need the attempt's
 * token (409 {@code ATTEMPT_TOKEN_MISMATCH}). The time is up when the timer (the quiz's duration plus a student's
 * extra time; paused time does not count) runs out or the placement closes — its due time, or the end of its late
 * days when it takes late work. Saves and submissions sent after that (plus {@link #GRACE} for the network) are not
 * taken: a save is 409 {@code ATTEMPT_TIME_UP}, a submission is marked on the answers saved before. Every step locks the
 * attempt, so two calls on one attempt never interleave. A marked attempt of a cohort placement posts its score
 * to the gradebook by the quiz's attempt scoring (highest, last or average of the marked attempts).
 */
@Service
public class QuizAttemptService extends AppTransactionManager {

    static final String AGGREGATE = QuizSubmissionEntity.TABLE;
    static final String PERMISSION_GRADE_ANSWERS = "lms.quiz_submission.grade_quiz_answers";
    /** How long after the deadline a save or submission sent in time is still taken. */
    static final Duration GRACE = Duration.ofSeconds(30);
    private static final Set<String> REASONS = Set.of("manual", "time_up", "violations");
    private static final Set<String> OPEN = Set.of("in_progress", "paused");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_FEEDBACK = 4000;

    private final QuizAttemptRepository attempts;
    private final QuizSubmissionRepository submissions;
    private final QuizSubmissionMapper mapper;
    private final GradebookPostingRepository gradebook;
    private final GradebookPostings postings;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final JsonMapper json;
    private final Clock clock;
    private final CohortAccess access;

    public QuizAttemptService(QuizAttemptRepository attempts, QuizSubmissionRepository submissions,
            QuizSubmissionMapper mapper, GradebookPostingRepository gradebook, GradebookPostings postings,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, JsonMapper json,
            Clock clock, CohortAccess access) {
        super(database, transactions);
        this.access = access;
        this.attempts = attempts;
        this.submissions = submissions;
        this.mapper = mapper;
        this.gradebook = gradebook;
        this.postings = postings;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.json = json;
        this.clock = clock;
    }

    /**
     * Keeps the answers changed since the last save. A save whose client sequence is not above the last kept one is
     * ignored as a whole (an older request arriving late); within one save the latest {@code answeredAt} of a
     * question wins.
     */
    public QuizAutosaveResult saveQuizResponses(UUID id, SaveQuizResponsesRequest request) {
        validator.validate(request);
        long sequence = sequence(request.getClientSequence());
        UUID student = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            Attempt attempt = ownAttempt(id, student);
            requireToken(attempt, request.getAttemptToken());
            if (!"in_progress".equals(attempt.status())) {
                throw new InvalidStateException("Only an attempt in progress takes answers; resume it first.");
            }
            if (timeUp(attempt, now)) {
                throw new ConflictException("ATTEMPT_TIME_UP", "The time for this attempt is up; submit it.");
            }
            QuizAutosaveResult result = new QuizAutosaveResult().serverTime(now);
            timer(attempt, now, result);
            if (attempt.lastClientSequence() != null && sequence <= attempt.lastClientSequence()) {
                return result.savedCount(0).ignoredStale(request.getAnswers().size());
            }
            Map<UUID, QuizAnswerInput> latest = latestPerQuestion(request.getAnswers());
            Questions questions = questions(attempt);
            questions.requireAll(latest.keySet(), "answers");
            store(attempt, questions, latest, now, student);
            int answered = (int) attempts.answers(id).stream()
                    .filter(a -> QuizGrader.answered(read(a.answer()))).count();
            if (attempts.recordSave(id, attempt.rowVersion(), sequence, answered, now, student) == 0) {
                throw new PreconditionFailedException("The attempt changed meanwhile; save again.");
            }
            audit.recordTransition(AGGREGATE, id, "saveResponses", List.of("answeredCount"), null);
            return result.savedCount(latest.size()).ignoredStale(request.getAnswers().size() - latest.size());
        });
    }

    /** Stops the timer of an attempt in progress, for quizzes that allow breaks. */
    public QuizSubmission pauseQuizAttempt(UUID id, PauseQuizAttemptRequest request) {
        UUID student = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            Attempt attempt = ownAttempt(id, student);
            requireEtag(attempt, request == null ? null : request.getEtag());
            if (!"in_progress".equals(attempt.status())) {
                throw new InvalidStateException("Only an attempt in progress can be paused.");
            }
            if (!attempt.allowPause()) {
                throw new ConflictException("PAUSE_NOT_ALLOWED", "This quiz does not allow breaks.");
            }
            if (attempts.pause(id, attempt.rowVersion(), usedSeconds(attempt, now), now, student) == 0) {
                throw new PreconditionFailedException("The attempt changed meanwhile; read it again.");
            }
            return transitioned(id, "pause", List.of("status", "timeTakenSeconds"));
        });
    }

    /** Starts the timer again from where the pause left it. */
    public QuizSubmission resumeQuizAttempt(UUID id, ResumeQuizAttemptRequest request) {
        UUID student = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            Attempt attempt = ownAttempt(id, student);
            requireEtag(attempt, request == null ? null : request.getEtag());
            if (!"paused".equals(attempt.status())) {
                throw new InvalidStateException("Only a paused attempt can be resumed.");
            }
            if (attempts.resume(id, attempt.rowVersion(), now, student) == 0) {
                throw new PreconditionFailedException("The attempt changed meanwhile; read it again.");
            }
            return transitioned(id, "resume", List.of("status"));
        });
    }

    /**
     * Takes the final answers, records the proctoring events, marks every question of the attempt and stores the
     * outcome: {@code graded} when every answer could be marked, {@code submitted} while open-ended answers wait for
     * a teacher. An attempt submitted late is marked on what was saved in time.
     */
    public QuizSubmission submitQuiz(UUID id, SubmitQuizRequest request) {
        validator.validate(request);
        String reason = request.getSubmissionReason();
        if (reason != null && !REASONS.contains(reason)) {
            throw new ValidationFailedException(List.of(new FieldError("submissionReason",
                    "must be one of manual, time_up, violations", "AllowedValues")));
        }
        UUID student = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            Attempt attempt = ownAttempt(id, student);
            requireToken(attempt, request.getAttemptToken());
            if (!OPEN.contains(attempt.status())) {
                throw new InvalidStateException("The attempt has already been submitted.");
            }
            Questions questions = questions(attempt);
            boolean late = timeUp(attempt, now);
            if (!late) {
                Map<UUID, QuizAnswerInput> latest = latestPerQuestion(request.getAnswers());
                questions.requireAll(latest.keySet(), "answers");
                store(attempt, questions, latest, now, student);
            }
            int violations = attempt.violationCount() + recordViolations(id, request.getViolations(), now, student);
            store(attempt, questions, Map.of(), now, student);
            Totals totals = mark(attempt, questions.keys(), now, student);
            String why = reason(attempt, late, violations, reason, now);
            int timeTaken = "paused".equals(attempt.status()) ? orZero(attempt.timeTakenSeconds())
                    : Math.min(usedSeconds(attempt, now), limitSeconds(attempt).orElse(Integer.MAX_VALUE));
            Outcome outcome = totals.outcome(timeTaken, why, violations, attempt.passingPercentage());
            if (attempts.submit(id, attempt.rowVersion(), outcome, now, student) == 0) {
                throw new PreconditionFailedException("The attempt changed meanwhile; submit again.");
            }
            postScore(attempt, outcome, now, student);
            return transitioned(id, "submit", List.of("status", "submittedAt", "score", "percentage",
                    "timeTakenSeconds", "submissionReason", "violationCount"));
        });
    }

    /**
     * A teacher's marks for answers of a submitted attempt (open-ended answers, or a correction of an automatic
     * mark): each from 0 to the question's marks. Only the teaching staff of the attempt's cohort mark it (403); the
     * attempt is {@code graded} once no answer waits for marks.
     */
    public QuizSubmission gradeQuizAnswers(UUID id, GradeQuizAnswersRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_GRADE_ANSWERS);
        UUID teacher = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            Attempt attempt = attempts.lock(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            Optional<UUID> institution = Optional.ofNullable(attempt.institutionId());
            if (!scope.canRead(institution)) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institution);
            Optional.ofNullable(attempt.cohortId())
                    .or(() -> Optional.ofNullable(attempt.contentItemId()).flatMap(gradebook::item)
                            .map(GradebookPostingRepository.GradedItem::cohortId))
                    .ifPresent(cohort -> access.taught(cohort, PERMISSION_GRADE_ANSWERS));
            if (OPEN.contains(attempt.status())) {
                throw new InvalidStateException("Answers are marked once the attempt has been submitted.");
            }
            Map<UUID, StoredAnswer> byQuestion = new LinkedHashMap<>();
            attempts.answers(id).forEach(a -> byQuestion.put(a.questionId(), a));
            List<FieldError> errors = new ArrayList<>();
            List<Runnable> writes = new ArrayList<>();
            int index = 0;
            for (AnswerGrade grade : request.getAnswers()) {
                String field = "answers[" + index++ + "]";
                StoredAnswer answer = byQuestion.get(grade.getQuestionId());
                BigDecimal marks = WireValues.decimal(grade.getMarks());
                BigDecimal possible = answer == null ? null : answer.marksPossible();
                String feedback = grade.getFeedback();
                if (answer == null) {
                    errors.add(new FieldError(field + ".questionId", "is not a question of this attempt", "NOT_FOUND"));
                } else if (marks == null || marks.signum() < 0 || marks.scale() > 2 || possible == null
                        || marks.compareTo(possible) > 0) {
                    errors.add(new FieldError(field + ".marks",
                            "must be from 0 to the question's marks with at most 2 decimals", "Range"));
                } else if (feedback != null && feedback.length() > MAX_FEEDBACK) {
                    errors.add(new FieldError(field + ".feedback", "must be at most 4000 characters", "Size"));
                } else {
                    boolean correct = marks.compareTo(possible) == 0;
                    writes.add(() -> attempts.markAnswer(answer.id(), correct, marks.setScale(2),
                            possible, feedback, teacher, now, teacher));
                }
            }
            if (request.getAnswers().stream().map(AnswerGrade::getQuestionId).distinct().count()
                    != request.getAnswers().size()) {
                errors.add(new FieldError("answers", "a question appears more than once", "DUPLICATE"));
            }
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            writes.forEach(Runnable::run);
            Totals totals = Totals.of(attempts.answers(id));
            Outcome outcome = totals.outcome(0, null, 0, attempt.passingPercentage());
            if (attempts.recordMarks(id, attempt.rowVersion(), outcome, now, teacher) == 0) {
                throw new PreconditionFailedException("The attempt changed meanwhile; read it again.");
            }
            postScore(attempt, outcome, now, teacher);
            return transitioned(id, "gradeAnswers", List.of("status", "score", "percentage", "correctCount",
                    "incorrectCount"));
        });
    }

    // ---- helpers

    /**
     * Why the attempt ended, as recorded: time_up after the deadline, violations once the quiz's limit of them is
     * reached, late when handed in after the due time where late work is taken, else what the student said.
     */
    static String reason(Attempt attempt, boolean timeUp, int violations, String claimed, OffsetDateTime now) {
        if (timeUp) {
            return "time_up";
        }
        if (attempt.maxViolations() != null && attempt.maxViolations() > 0 && violations >= attempt.maxViolations()) {
            return "violations";
        }
        if (attempt.dueAt() != null && now.isAfter(attempt.dueAt())) {
            return "late";
        }
        return claimed == null ? "manual" : claimed;
    }

    /** The questions of the attempt: the ones it was delivered with (its answer rows), else the quiz's. */
    private Questions questions(Attempt attempt) {
        Map<UUID, BigDecimal> marks = new LinkedHashMap<>();
        Map<UUID, Integer> order = new LinkedHashMap<>();
        List<StoredAnswer> stored = attempts.answers(attempt.id());
        if (stored.isEmpty()) {
            attempts.quizQuestions(attempt.quizId()).forEach(q -> {
                marks.put(q.questionId(), q.marks());
                order.put(q.questionId(), q.sortOrder());
            });
        } else {
            stored.stream().filter(a -> a.questionId() != null).forEach(a -> {
                marks.put(a.questionId(), a.marksPossible() == null ? BigDecimal.ZERO : a.marksPossible());
                order.put(a.questionId(), a.sortOrder());
            });
        }
        return new Questions(attempts.keys(marks), order);
    }

    /** Writes the given answers, and a blank answer row for every question of the attempt that has none. */
    private void store(Attempt attempt, Questions questions, Map<UUID, QuizAnswerInput> answers, OffsetDateTime now,
            UUID actor) {
        Map<UUID, StoredAnswer> stored = new LinkedHashMap<>();
        attempts.answers(attempt.id()).forEach(a -> stored.put(a.questionId(), a));
        answers.forEach((questionId, input) -> {
            StoredAnswer current = stored.get(questionId);
            String text = write(input);
            if (current == null) {
                attempts.insertAnswer(UuidV7.next(clock), attempt.id(), questions.keys().get(questionId), text,
                        questions.order().get(questionId), null, now, actor);
            } else {
                attempts.saveAnswer(current.id(), text, now, actor);
            }
        });
        if (answers.isEmpty()) {
            questions.keys().forEach((questionId, key) -> {
                if (!stored.containsKey(questionId)) {
                    attempts.insertAnswer(UuidV7.next(clock), attempt.id(), key, null,
                            questions.order().get(questionId), null, now, actor);
                }
            });
        }
    }

    /** Marks every answer of the attempt against its key. */
    private Totals mark(Attempt attempt, Map<UUID, QuizAnswerKey> keys, OffsetDateTime now, UUID actor) {
        BigDecimal penalty = attempt.negativeMarking() ? attempt.negativeMarks() : null;
        for (StoredAnswer answer : attempts.answers(attempt.id())) {
            QuizAnswerKey key = keys.get(answer.questionId());
            if (key == null) {
                attempts.markAnswer(answer.id(), false, BigDecimal.ZERO.setScale(2), answer.marksPossible(), null,
                        null, now, actor);
                continue;
            }
            QuizGrader.Marked marked = QuizGrader.mark(key, read(answer.answer()), penalty);
            attempts.markAnswer(answer.id(), marked.correct(), marked.marks(), key.marks(), marked.feedback(), null,
                    now, actor);
        }
        return Totals.of(attempts.answers(attempt.id()));
    }

    /** Posts a fully marked attempt of a cohort placement to the student's gradebook entry. */
    private void postScore(Attempt attempt, Outcome outcome, OffsetDateTime now, UUID actor) {
        if (!"graded".equals(outcome.status()) || attempt.contentItemId() == null) {
            return;
        }
        Optional<GradebookPostingRepository.GradedItem> item = gradebook.item(attempt.contentItemId())
                .filter(i -> i.cohortId() != null);
        if (item.isEmpty()) {
            return;
        }
        List<BigDecimal> scored = attempts.scoredAttempts(attempt.quizId(), attempt.userId(), attempt.contentItemId());
        BigDecimal percentage = scoring(attempt.attemptScoring(), scored, outcome.percentage());
        postings.postAutomatic(item.get(), attempt.userId(), GradebookPostings.pointsOf(item.get(), percentage),
                attempt.id(), now, actor);
    }

    /** The percentage that counts for the student: highest, last or the average of the marked attempts. */
    static BigDecimal scoring(String rule, List<BigDecimal> scored, BigDecimal current) {
        List<BigDecimal> all = new ArrayList<>(scored);
        if (all.isEmpty()) {
            all.add(current);
        }
        return switch (rule) {
            case "last" -> all.get(all.size() - 1);
            case "average" -> all.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP);
            default -> all.stream().max(Comparator.naturalOrder()).orElse(current);
        };
    }

    private int recordViolations(UUID id, List<ViolationEvent> events, OffsetDateTime now, UUID actor) {
        if (events == null) {
            return 0;
        }
        int counted = 0;
        for (ViolationEvent event : events) {
            attempts.insertViolation(UuidV7.next(clock), id, event.getEventType().getValue(),
                    event.getSeverity().getValue(), event.getOccurredAt(), now, actor);
            if (event.getSeverity() == ViolationEvent.SeverityEnum.VIOLATION) {
                counted++;
            }
        }
        return counted;
    }

    private Attempt ownAttempt(UUID id, UUID student) {
        Attempt attempt = attempts.lock(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!attempt.userId().equals(student)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return attempt;
    }

    private static void requireToken(Attempt attempt, String sent) {
        String expected = QuizAttemptToken.of(attempt.id(), attempt.attemptNumber(), attempt.startedAt());
        if (!QuizAttemptToken.matches(sent, expected)) {
            throw new ConflictException("ATTEMPT_TOKEN_MISMATCH",
                    "This answer sheet belongs to another delivery of the attempt; reload the quiz.");
        }
    }

    private static void requireEtag(Attempt attempt, String etag) {
        OptionalInt expected = Etags.bodyVersion(etag);
        if (expected.isPresent() && expected.getAsInt() != attempt.rowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
    }

    private QuizSubmission transitioned(UUID id, String step, List<String> fields) {
        audit.recordTransition(AGGREGATE, id, step, fields, null);
        outbox.publish(AGGREGATE + "." + step, AGGREGATE, id, LmsEvents.payload(id));
        return mapper.toDto(submissions.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
    }

    /** Seconds the attempt has run: the time kept at the last pause plus the run since it last (re)started. */
    static int usedSeconds(Attempt attempt, OffsetDateTime now) {
        OffsetDateTime since = attempt.resumedAt() != null ? attempt.resumedAt() : attempt.startedAt();
        long running = since == null || !"in_progress".equals(attempt.status()) ? 0
                : Math.max(0, Duration.between(since, now).getSeconds());
        return (int) Math.min(Integer.MAX_VALUE, orZero(attempt.timeTakenSeconds()) + running);
    }

    private static Optional<Integer> limitSeconds(Attempt attempt) {
        return Optional.ofNullable(attempt.durationMinutes()).filter(m -> m > 0)
                .map(m -> (m + Math.max(0, orZero(attempt.extraMinutes()))) * 60);
    }

    static boolean timeUp(Attempt attempt, OffsetDateTime now) {
        boolean timerOut = limitSeconds(attempt).map(limit -> usedSeconds(attempt, now) > limit + GRACE.getSeconds())
                .orElse(false);
        return timerOut || attempt.closesAt() != null && now.isAfter(attempt.closesAt().plus(GRACE));
    }

    /** The seconds left: the timer's, capped by the time until the placement closes. */
    private static void timer(Attempt attempt, OffsetDateTime now, QuizAutosaveResult result) {
        Optional<Long> byTimer = limitSeconds(attempt).map(limit -> (long) Math.max(0, limit - usedSeconds(attempt,
                now)));
        Optional<Long> byClose = Optional.ofNullable(attempt.closesAt())
                .map(closes -> Math.max(0, Duration.between(now, closes).getSeconds()));
        Optional<Long> remaining = byTimer.isEmpty() ? byClose
                : byClose.map(close -> Math.min(close, byTimer.get())).or(() -> byTimer);
        remaining.ifPresent(left -> result.remainingSeconds((int) Math.min(Integer.MAX_VALUE, left))
                .deadlineAt(now.plusSeconds(left)));
    }

    private static Map<UUID, QuizAnswerInput> latestPerQuestion(List<QuizAnswerInput> answers) {
        Map<UUID, QuizAnswerInput> latest = new LinkedHashMap<>();
        for (QuizAnswerInput answer : answers) {
            latest.merge(answer.getQuestionId(), answer, (kept, next) -> answeredAt(next).isBefore(answeredAt(kept))
                    ? kept : next);
        }
        return latest;
    }

    private static OffsetDateTime answeredAt(QuizAnswerInput answer) {
        return answer.getAnsweredAt() == null ? OffsetDateTime.MIN : answer.getAnsweredAt();
    }

    private static long sequence(String value) {
        Long parsed = null;
        try {
            parsed = WireValues.int64(value);
        } catch (RuntimeException e) {
            // reported below
        }
        if (parsed == null || parsed < 0) {
            throw new ValidationFailedException(List.of(new FieldError("clientSequence",
                    "must be a non-negative whole number", "Pattern")));
        }
        return parsed;
    }

    private String write(QuizAnswerInput input) {
        QuizAnswerInput copy = json.readValue(json.writeValueAsString(input), QuizAnswerInput.class);
        copy.setQuestionId(null);
        copy.setAnsweredAt(null);
        return json.writeValueAsString(copy);
    }

    private QuizAnswerInput read(String stored) {
        return stored == null ? null : json.readValue(stored, QuizAnswerInput.class);
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** The questions of an attempt with their keys and positions. */
    private record Questions(Map<UUID, QuizAnswerKey> keys, Map<UUID, Integer> order) {

        void requireAll(Set<UUID> questionIds, String field) {
            List<FieldError> errors = questionIds.stream().filter(q -> !keys.containsKey(q))
                    .map(q -> new FieldError(field, "question " + q + " is not part of this attempt", "NOT_FOUND"))
                    .toList();
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
        }
    }

    /** The marks of an attempt added up. */
    record Totals(BigDecimal score, BigDecimal outOf, int answered, int correct, int incorrect, boolean pending) {

        static Totals of(List<StoredAnswer> answers) {
            BigDecimal score = BigDecimal.ZERO;
            BigDecimal outOf = BigDecimal.ZERO;
            int correct = 0;
            int incorrect = 0;
            int answered = 0;
            boolean pending = false;
            for (StoredAnswer answer : answers) {
                outOf = outOf.add(answer.marksPossible() == null ? BigDecimal.ZERO : answer.marksPossible());
                if (answer.answer() != null) {
                    answered++;
                }
                if (answer.correct() == null) {
                    pending = true;
                    continue;
                }
                score = score.add(answer.marksAwarded() == null ? BigDecimal.ZERO : answer.marksAwarded());
                if (answer.correct()) {
                    correct++;
                } else if (answer.answer() != null) {
                    incorrect++;
                }
            }
            return new Totals(score.max(BigDecimal.ZERO).setScale(2), outOf.setScale(2), answered, correct, incorrect,
                    pending);
        }

        Outcome outcome(int timeTaken, String reason, int violations, int passingPercentage) {
            BigDecimal percentage = outOf.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                    : score.multiply(HUNDRED).divide(outOf, 2, RoundingMode.HALF_UP);
            return new Outcome(pending ? "submitted" : "graded", timeTaken, reason, violations,
                    BigDecimal.valueOf(passingPercentage).setScale(2), score, outOf, percentage, answered, correct,
                    incorrect);
        }
    }
}
