package com.mysms.privacy.business;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.RightsRequestEntity;
import com.mysms.privacy.domain.RightsRequestTaskEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.model.TaskAnswer;
import com.mysms.privacy.repository.RightsRequestRepository;
import com.mysms.privacy.repository.RightsRequestTaskRepository;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The fulfilment of an erasure or export request across the modules (design §9.2), a saga of the rights request
 * aggregate: one task per module of {@code mysms.privacy.erasure.modules}, dispatched as
 * privacy.rights_request.task_assigned and answered by each module's {@code <module>.privacy_task.completed}.
 *
 * <p>An erasure runs in two waves: the modules of {@code mysms.privacy.erasure.first-wave} (comms and iam, which stop
 * messages and sign-in) first, the rest once every first-wave task is done or not applicable, so nothing goes out to
 * an account being erased; an export's tasks all start at once. A failed answer is dispatched again under the same
 * task id until the fifth attempt, each time after a back-off: {@code mysms.privacy.erasure.retry-delay} doubled per
 * attempt made, at most {@code mysms.privacy.erasure.redispatch-after}, the waiting task dispatched by
 * {@link TaskSweepJob} once its next attempt has come; after the fifth, once every started task has answered, the
 * request waits for the staff (action_required) until a retry. When every task has answered the request is closed:
 * fulfilled, or partially fulfilled when a module kept rows under a hold or a legal-retention policy. An erasure,
 * partial or not, then withdraws every live consent decision about the subject, forgets the subject's birth date and
 * publishes privacy.erasure.completed, the tombstone a restore replays. A task no module answers stays dispatched and is
 * dispatched again by {@link TaskSweepJob} every {@code mysms.privacy.erasure.redispatch-after}: a module without a
 * handler leaves its tasks dispatched.
 *
 * <p>Everything runs in the caller's transaction (a staff step, a module's answer through the inbox, a sweep), the
 * request's row first: a module's answer locks it, so answers to the same request never race.
 */
@Component
public final class RightsRequestSaga extends AppTransactionManager {

    public static final String TASK_AGGREGATE = RightsRequestTaskEntity.TABLE;
    /** A failed answer is dispatched again until the task was dispatched this many times. */
    static final int MAX_ATTEMPTS = 5;

    private static final String AGGREGATE = RightsRequestEntity.TABLE;
    private static final Pattern MODULE = Pattern.compile("^[a-z][a-z_]{1,29}$");
    private static final String ERASURE = "erasure";
    private static final String ERASE = "erase";
    private static final String EXPORT = "export";
    private static final String PENDING = "pending";
    private static final String DISPATCHED = "dispatched";
    private static final String PARTIAL = "partial";
    private static final String FAILED = "failed";
    private static final String ACTION_REQUIRED = "action_required";
    private static final String CLOSED = "closed";
    private static final String FULFILLED = "fulfilled";
    private static final String PARTIALLY_FULFILLED = "partially_fulfilled";
    private static final Set<String> ANSWERS = Set.of("done", PARTIAL, FAILED, "not_applicable");
    private static final List<String> DISPATCH_FIELDS = List.of("status", "attempts", "dispatchedAt");

    /** What became of a module's answer. */
    public enum Answered {
        /** The answer was recorded. */
        RECORDED,
        /** The task had already been answered or retried: a late or repeated answer changes nothing. */
        IGNORED,
        /** No task of the tenant has that id, request and module. */
        UNKNOWN
    }

    private final RightsRequestRepository requests;
    private final RightsRequestTaskRepository tasks;
    private final ConsentService consents;
    private final SubjectRepository subjects;
    private final ConsentStateWriter stateWriter;
    private final JobSchedule schedule;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;
    private final List<String> modules;
    private final Set<String> firstWave;
    private final Duration redispatchAfter;
    private final Duration retryDelay;

    public RightsRequestSaga(RightsRequestRepository requests, RightsRequestTaskRepository tasks,
            ConsentService consents, SubjectRepository subjects, ConsentStateWriter stateWriter,
            @Qualifier(PrivacyDatabaseConfig.JOBS) JobSchedule schedule,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit, Clock clock,
            @Value("${mysms.privacy.erasure.modules}") String modules,
            @Value("${mysms.privacy.erasure.first-wave}") String firstWave,
            @Value("${mysms.privacy.erasure.redispatch-after}") Duration redispatchAfter,
            @Value("${mysms.privacy.erasure.retry-delay}") Duration retryDelay) {
        super(database, transactions);
        this.requests = requests;
        this.tasks = tasks;
        this.consents = consents;
        this.subjects = subjects;
        this.stateWriter = stateWriter;
        this.schedule = schedule;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
        this.modules = List.copyOf(codes("mysms.privacy.erasure.modules", modules));
        this.firstWave = Set.copyOf(codes("mysms.privacy.erasure.first-wave", firstWave));
        if (this.modules.isEmpty() || !this.modules.containsAll(this.firstWave)) {
            throw new IllegalArgumentException("mysms.privacy.erasure.modules must name modules, first-wave among them");
        }
        if (redispatchAfter.isNegative() || redispatchAfter.isZero()) {
            throw new IllegalArgumentException("mysms.privacy.erasure.redispatch-after must be positive");
        }
        if (retryDelay.isNegative() || retryDelay.isZero()) {
            throw new IllegalArgumentException("mysms.privacy.erasure.retry-delay must be positive");
        }
        this.redispatchAfter = redispatchAfter;
        this.retryDelay = retryDelay;
    }

    /** The modules every erasure or export request sends a task to, in dispatch order within a wave. */
    public List<String> modules() {
        return modules;
    }

    /** The tasks of the requests, by request, wave by wave. */
    public Map<UUID, List<RightsRequestTaskEntity>> tasksOf(List<UUID> requestIds) {
        if (requestIds.isEmpty()) {
            return Map.of();
        }
        return tasks.ofRequests(requestIds).stream()
                .collect(Collectors.groupingBy(RightsRequestTaskEntity::getRequestId));
    }

    /**
     * Starts the fulfilment of an erasure or export request (the staff's step, in its transaction): one task per
     * module, the first wave dispatched now, the tenant enrolled in the sweep.
     */
    public void start(RightsRequestEntity request, OffsetDateTime at, UUID actor) {
        boolean erasure = ERASURE.equals(request.getType());
        List<RightsRequestTaskEntity> firstWaveTasks = new ArrayList<>();
        for (String module : modules) {
            RightsRequestTaskEntity task = RightsRequestTaskEntity.builder()
                    .requestId(request.getId())
                    .module(module)
                    .action(erasure ? ERASE : EXPORT)
                    .wave(erasure && !firstWave.contains(module) ? 2 : 1)
                    .status(PENDING)
                    .build();
            task.onCreatingData(Optional.ofNullable(actor), clock);
            task.setTenantId(request.getTenantId());
            tasks.insert(task);
            audit.recordCreate(TASK_AGGREGATE, task.getId());
            if (task.getWave() == 1) {
                firstWaveTasks.add(task);
            }
        }
        firstWaveTasks.forEach(t -> dispatch(request, t, at, actor));
        schedule.enrol(TaskSweepJob.CODE, at);
    }

    /**
     * Dispatches a failed or unanswered task of the request again under the same id (the staff's retry, in its
     * transaction; a failed task waiting for its next attempt goes at once): 404 when the request has no such task,
     * 409 when the task is pending or answered.
     */
    public void retry(RightsRequestEntity request, UUID taskId, OffsetDateTime at, UUID actor) {
        RightsRequestTaskEntity task = tasks.findById(taskId).filter(t -> t.getRequestId().equals(request.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(TASK_AGGREGATE, taskId));
        if (!FAILED.equals(task.getStatus()) && !DISPATCHED.equals(task.getStatus())) {
            throw new InvalidStateException("Only a failed or unanswered task can be dispatched again.");
        }
        dispatch(request, task, at, actor);
    }

    /**
     * Records a module's answer to its task, the request locked first: a failed answer before the fifth attempt
     * waits for its next attempt (a back-off), which the sweep dispatches; then the request moves on as far as
     * its tasks allow ({@link #advance}).
     */
    public Answered answer(TaskAnswer answer) {
        if (!ANSWERS.contains(answer.status())) {
            throw new IllegalArgumentException("not a task status: " + answer.status());
        }
        return doInWriteTransaction(() -> {
            Optional<RightsRequestEntity> request = requests.lockById(answer.requestId());
            Optional<RightsRequestTaskEntity> found = tasks.findById(answer.taskId())
                    .filter(t -> t.getRequestId().equals(answer.requestId()) && t.getModule().equals(answer.module()));
            if (request.isEmpty() || found.isEmpty()) {
                return Answered.UNKNOWN;
            }
            RightsRequestTaskEntity task = found.get();
            if (!DISPATCHED.equals(task.getStatus())) {
                return Answered.IGNORED;
            }
            OffsetDateTime now = now();
            int expected = task.getRowVersion();
            task.setStatus(answer.status());
            task.setCompletedAt(now);
            task.setResult(answer.result());
            task.setLastErrorCode(FAILED.equals(answer.status()) ? answer.errorCode() : task.getLastErrorCode());
            boolean again = FAILED.equals(answer.status()) && task.getAttempts() < MAX_ATTEMPTS;
            task.setNextAttemptAt(again ? now.plus(backOff(task.getAttempts())) : null);
            if (tasks.answer(task, expected) == 0) {
                throw new PreconditionFailedException("The task changed since it was read.");
            }
            stepped(task, now, null);
            audit.recordTransition(TASK_AGGREGATE, task.getId(), "answer",
                    List.of("status", "completedAt", "result", "lastErrorCode", "nextAttemptAt"), null);
            advance(request.get(), now);
            return Answered.RECORDED;
        });
    }

    /**
     * The sweep (one tenant): every task dispatched longer ago than {@code mysms.privacy.erasure.redispatch-after}
     * and still unanswered, and every failed task whose next attempt has come, is dispatched again, one task per
     * transaction, its request locked first.
     */
    public void redispatchDue(int batch) {
        OffsetDateTime now = now();
        OffsetDateTime cutoff = now.minus(redispatchAfter);
        List<UUID> due;
        do {
            due = doInPrimaryReadTransaction(() -> tasks.due(cutoff, now, batch));
            due.forEach(id -> runInWriteTransaction(() -> redispatch(id, cutoff, now)));
        } while (due.size() == batch);
    }

    private void redispatch(UUID taskId, OffsetDateTime cutoff, OffsetDateTime now) {
        Optional<RightsRequestTaskEntity> found = tasks.findById(taskId);
        Optional<RightsRequestEntity> request = found.flatMap(t -> requests.lockById(t.getRequestId()));
        Optional<RightsRequestTaskEntity> task = request.flatMap(r -> tasks.findById(taskId))
                .filter(t -> unanswered(t, cutoff) || retryDue(t, now));
        if (task.isPresent()) {
            dispatch(request.get(), task.get(), now(), null); // answered or retried meanwhile otherwise
        }
    }

    private static boolean unanswered(RightsRequestTaskEntity task, OffsetDateTime cutoff) {
        return DISPATCHED.equals(task.getStatus()) && task.getDispatchedAt().isBefore(cutoff);
    }

    private static boolean retryDue(RightsRequestTaskEntity task, OffsetDateTime now) {
        OffsetDateTime next = task.getNextAttemptAt();
        return FAILED.equals(task.getStatus()) && next != null && !next.isAfter(now);
    }

    /**
     * How long a task that failed after its n-th attempt waits for the next: {@code mysms.privacy.erasure.retry-delay}
     * doubled per attempt after the first, at most {@code mysms.privacy.erasure.redispatch-after}.
     */
    private Duration backOff(int attempts) {
        Duration delay = retryDelay;
        for (int i = 1; i < attempts && delay.compareTo(redispatchAfter) < 0; i++) {
            delay = delay.multipliedBy(2);
        }
        return delay.compareTo(redispatchAfter) < 0 ? delay : redispatchAfter;
    }

    /** Whether the task still waits: dispatched and unanswered, or failed and waiting for its next attempt. */
    private static boolean waiting(RightsRequestTaskEntity task) {
        return DISPATCHED.equals(task.getStatus()) || task.getNextAttemptAt() != null;
    }

    /**
     * Moves the request as far as its tasks allow, once no started task waits (for an answer or its next attempt):
     * action_required when one failed for good; else the next wave dispatched; else, every task answered, closed with
     * its outcome.
     */
    private void advance(RightsRequestEntity request, OffsetDateTime now) {
        if (request.getClosedAt() != null) {
            return;
        }
        List<RightsRequestTaskEntity> all = tasks.ofRequests(List.of(request.getId()));
        if (all.stream().anyMatch(RightsRequestSaga::waiting)) {
            return;
        }
        if (all.stream().anyMatch(t -> FAILED.equals(t.getStatus()))) {
            if (!ACTION_REQUIRED.equals(request.getStatus())) {
                move(request, ACTION_REQUIRED, null, now, PrivacyEvents.ACTION_REQUIRED);
            }
            return;
        }
        List<RightsRequestTaskEntity> pending = all.stream().filter(t -> PENDING.equals(t.getStatus())).toList();
        if (!pending.isEmpty()) {
            pending.forEach(t -> dispatch(request, t, now, null));
            return;
        }
        boolean partial = all.stream().anyMatch(t -> PARTIAL.equals(t.getStatus()));
        if (ERASURE.equals(request.getType())) {
            erased(request, partial, now);
        }
        move(request, CLOSED, partial ? PARTIALLY_FULFILLED : FULFILLED, now, PrivacyEvents.CLOSED);
    }

    /**
     * What privacy itself erases once the modules erased theirs: every live consent decision about the subject is
     * withdrawn (with its evidence) and the tombstone published; the subject's birth date is forgotten only when no
     * module kept data under a hold or a legal-retention policy (a partial answer), since what is kept still needs the
     * subject's age for its consent answers.
     */
    private void erased(RightsRequestEntity request, boolean partial, OffsetDateTime now) {
        UUID subject = request.getSubjectPersonId();
        consents.withdrawForRightsRequest(subject, Set.of(), null);
        if (!partial) {
            subjects.anonymise(subject, now);
        }
        stateWriter.recomputeSubject(subject);
        outbox.publish(PrivacyEvents.ERASURE_COMPLETED, AGGREGATE, request.getId(),
                PrivacyEvents.erasureCompleted(request));
    }

    /** The request's guarded move to the status (and outcome), with its audit record and event. */
    private void move(RightsRequestEntity request, String status, String outcomeCode, OffsetDateTime now,
            String event) {
        int expected = request.getRowVersion();
        request.setStatus(status);
        request.setOutcomeCode(outcomeCode);
        request.setClosedAt(CLOSED.equals(status) ? now : null);
        request.setUpdatedAt(now);
        request.setUpdatedBy(null);
        if (requests.fulfilment(request, expected) == 0) {
            throw new PreconditionFailedException("The rights request changed since it was read.");
        }
        request.setRowVersion(expected + 1);
        audit.recordTransition(AGGREGATE, request.getId(), event.substring(1),
                CLOSED.equals(status) ? List.of("status", "outcomeCode", "closedAt") : List.of("status"), null);
        outbox.publish(AGGREGATE + event, AGGREGATE, request.getId(), PrivacyEvents.rightsRequest(request));
    }

    /** Dispatches the task (again): one more attempt, its audit record and privacy.rights_request.task_assigned. */
    private void dispatch(RightsRequestEntity request, RightsRequestTaskEntity task, OffsetDateTime at, UUID actor) {
        if (tasks.dispatch(task.getId(), task.getRowVersion(), at, actor) == 0) {
            throw new PreconditionFailedException("The task changed since it was read.");
        }
        task.setStatus(DISPATCHED);
        task.setAttempts(task.getAttempts() + 1);
        task.setDispatchedAt(at);
        task.setCompletedAt(null);
        task.setResult(null);
        task.setNextAttemptAt(null);
        stepped(task, at, actor);
        audit.recordTransition(TASK_AGGREGATE, task.getId(), "dispatch", DISPATCH_FIELDS, null);
        outbox.publish(PrivacyEvents.TASK_ASSIGNED, TASK_AGGREGATE, task.getId(),
                PrivacyEvents.taskAssigned(request, task));
    }

    private static void stepped(RightsRequestTaskEntity task, OffsetDateTime at, UUID actor) {
        task.setUpdatedAt(at);
        task.setUpdatedBy(actor);
        task.setRowVersion(task.getRowVersion() + 1);
    }

    /** A comma-separated list of module codes (blanks ignored); a malformed code stops the application. */
    private static List<String> codes(String key, String list) {
        List<String> codes = Arrays.stream(list.split(",")).map(String::strip).filter(c -> !c.isEmpty()).toList();
        if (!codes.stream().allMatch(c -> MODULE.matcher(c).matches()) || Set.copyOf(codes).size() != codes.size()) {
            throw new IllegalArgumentException(key + " must list distinct module codes");
        }
        return codes;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
