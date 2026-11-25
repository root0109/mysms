package com.mysms.privacyclient;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.jobs.ScheduledJob;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A module's part in privacy's rights requests and retention (design §9.1, §10), a Template Method the module extends
 * once, in its {@code events/} package, declaring its tables ({@link #erasureSteps}, {@link #keptRows},
 * {@link #retentionSweeps}).
 *
 * <p>Receiving (an {@link EventSubscriptions} handler, inside the inbox's transaction): privacy.rights_request.
 * task_assigned and privacy.retention.due naming this module are recorded once per task or run id in the module's
 * {@code <module>.privacy_task_receipt} and the tenant enrolled in the job; another module's are ignored. A task
 * dispatched again is retried when its receipt failed, answered again from the receipt when it finished, and left
 * alone while it runs.
 *
 * <p>Running (a {@link ScheduledJob}, {@code <module>.privacy_tasks}, in bounded batches): no gRPC call is made inside a
 * transaction, so the legal holds of the subject are asked first ({@link LegalHolds}); then one write transaction runs
 * the action's hook, finishes the receipt and publishes {@code <module>.privacy_task.completed}. When privacy cannot
 * say which holds apply, nothing is removed and the task answers failed. A retention sweep visits the subjects with
 * rows due one at a time, asks their holds, erases one subject per transaction, and answers
 * {@code <module>.privacy_retention.completed {runId, status, affected, held}}.
 */
public abstract class PrivacyTaskHandler extends AppTransactionManager implements EventSubscriptions, ScheduledJob {

    public static final String TASK_ASSIGNED = "privacy.rights_request.task_assigned";
    public static final String RETENTION_DUE = "privacy.retention.due";
    /** Error codes of a failed answer. */
    public static final String HOLDS_UNAVAILABLE = "LEGAL_HOLDS_UNAVAILABLE";
    public static final String TASK_ERROR = "PRIVACY_TASK_ERROR";
    public static final String POLICY_UNSUPPORTED = "RETENTION_POLICY_UNSUPPORTED";
    /** Receipts and subjects per batch. */
    static final int BATCH = 100;

    private static final Logger LOG = LoggerFactory.getLogger(PrivacyTaskHandler.class);
    private static final TypeReference<Map<String, Object>> ANSWER = new TypeReference<>() { };
    private static final String TASK = "task";
    private static final String RETENTION = "retention";
    private static final String RECEIVED = "received";
    private static final String FAILED = "failed";
    /** The retention actions a sweep carries out by erasing; archive is not built. */
    private static final Set<String> SWEPT = Set.of("delete", "anonymise");

    private static final String FIND = """
            SELECT kind, payload::text AS payload, status, result::text AS result
              FROM {schema}.privacy_task_receipt
             WHERE tenant_id = :tenantId AND task_id = :taskId
               FOR UPDATE
            """;
    private static final String INSERT = """
            INSERT INTO {schema}.privacy_task_receipt (task_id, tenant_id, kind, payload, status, created_at)
            VALUES (:taskId, :tenantId, :kind, CAST(:payload AS jsonb), 'received', :now)
            """;
    private static final String RETRY = """
            UPDATE {schema}.privacy_task_receipt
               SET status = 'received', payload = CAST(:payload AS jsonb), result = NULL, completed_at = NULL
             WHERE tenant_id = :tenantId AND task_id = :taskId AND status = 'failed'
            """;
    private static final String DUE = """
            SELECT task_id
              FROM {schema}.privacy_task_receipt
             WHERE tenant_id = :tenantId AND status = 'received'
             ORDER BY created_at, task_id
             LIMIT :limit
            """;
    private static final String FINISH = """
            UPDATE {schema}.privacy_task_receipt
               SET status = :status, result = CAST(:result AS jsonb), completed_at = :now
             WHERE tenant_id = :tenantId AND task_id = :taskId AND status = 'received'
            """;

    /** A receipt as stored: what was asked, how far it got and the answer once finished. */
    private record Receipt(String kind, String payload, String status, String result) {
    }

    private final String module;
    private final ModuleDatabase database;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final JobSchedule schedule;
    private final LegalHolds holds;
    private final JsonMapper json;
    private final Clock clock;
    private final Duration interval;
    private final String findSql;
    private final String insertSql;
    private final String retrySql;
    private final String dueSql;
    private final String finishSql;

    protected PrivacyTaskHandler(String module, ModuleDatabase database, PlatformTransactionManager transactions,
            OutboxWriter outbox, AuditWriter audit, JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, Duration interval) {
        super(database, transactions);
        this.module = module;
        this.database = database;
        this.outbox = outbox;
        this.audit = audit;
        this.schedule = schedule;
        this.holds = holds;
        this.json = json;
        this.clock = clock;
        this.interval = interval;
        this.findSql = FIND.replace("{schema}", module);
        this.insertSql = INSERT.replace("{schema}", module);
        this.retrySql = RETRY.replace("{schema}", module);
        this.dueSql = DUE.replace("{schema}", module);
        this.finishSql = FINISH.replace("{schema}", module);
    }

    // ---- the module's hooks ----

    /** The tables an erasure of a subject changes, in order (a person's own row last). */
    protected abstract List<ErasureStep> erasureSteps();

    /** The retention policies this module sweeps, by policy code; a run of another policy answers failed. */
    protected Map<String, RetentionSweep> retentionSweeps() {
        return Map.of();
    }

    /**
     * Extra named parameters the selects of {@link #erasureSteps} read besides {@code :subjectId}, for values a module
     * may not write into its SQL (another module's type name in a polymorphic reference, say).
     */
    protected Map<String, ?> stepParams() {
        return Map.of();
    }

    /** The tables whose rows of a subject are kept under a legal-retention policy, counted and left untouched. */
    protected List<KeptRows> keptRows() {
        return List.of();
    }

    /**
     * Erases the subject's rows (inside the write transaction), keeping what a hold covers; the rows of
     * {@link #keptRows} are counted as held, with the codes of the policies that kept any.
     */
    protected TaskResult erase(PrivacyTask task, ActiveHolds active, OffsetDateTime now) {
        Erasure erasure = eraseSteps(erasureSteps(), task.subjectPersonId(), stepParams(), active, now);
        int kept = 0;
        List<String> policyCodes = new ArrayList<>();
        for (KeptRows rows : keptRows()) {
            int count = count(rows, task.subjectPersonId());
            kept += count;
            if (count > 0 && !policyCodes.contains(rows.policyCode())) {
                policyCodes.add(rows.policyCode());
            }
        }
        return TaskResult.erased(kept == 0 ? erasure
                : new Erasure(erasure.counts(), erasure.held() + kept, erasure.changed()), policyCodes);
    }

    /** Anonymises the subject's rows; the module's erasure already anonymises what it keeps. */
    protected TaskResult anonymise(PrivacyTask task, ActiveHolds active, OffsetDateTime now) {
        return erase(task, active, now);
    }

    /**
     * The subject's rows per table, the kept ones of {@link #keptRows} included; no export part is written yet (design
     * §9.3: the package is planned).
     */
    protected TaskResult export(PrivacyTask task) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ErasureStep step : erasureSteps()) {
            counts.put(step.table(), rows(step, task.subjectPersonId(), stepParams()).size());
        }
        for (KeptRows rows : keptRows()) {
            counts.put(rows.table(), count(rows, task.subjectPersonId()));
        }
        return TaskResult.exported(counts, null);
    }

    /** Restriction (processing paused) is not built in any module: not applicable. */
    protected TaskResult restrict(PrivacyTask task) {
        return TaskResult.notApplicable();
    }

    /**
     * Runs the steps for one subject inside the caller's write transaction: each row a hold keeps is counted, every
     * other row erased, audited and announced on the module's outbox.
     */
    protected final Erasure eraseSteps(List<ErasureStep> steps, UUID subject, Map<String, ?> params,
            ActiveHolds active, OffsetDateTime now) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, List<UUID>> changed = new LinkedHashMap<>();
        int held = 0;
        for (ErasureStep step : steps) {
            List<UUID> erased = new ArrayList<>();
            for (UUID row : rows(step, subject, params)) {
                if (active.keeps(step.table(), step.categoryCode(), row)) {
                    held++;
                } else if (database.jdbc().sql(step.erase()).param("id", row).param("now", now).update() == 1) {
                    erased.add(row);
                    announce(step, row);
                }
            }
            if (!erased.isEmpty()) {
                counts.put(step.table(), erased.size());
                changed.put(step.table(), List.copyOf(erased));
            }
        }
        return new Erasure(counts, held, changed);
    }

    // ---- receiving ----

    @Override
    public final String module() {
        return module;
    }

    @Override
    public final Map<String, EventHandler> handlers() {
        return Map.of(TASK_ASSIGNED, this::receive, RETENTION_DUE, this::receive);
    }

    @Override
    public final String code() {
        return module + ".privacy_tasks";
    }

    @Override
    public final Duration interval() {
        return interval;
    }

    /** Records a task or run for this module once (inside the inbox's transaction); ignores another module's. */
    public final void receive(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!module.equals(data.path("module").asString(""))) {
            return;
        }
        boolean task = TASK_ASSIGNED.equals(event.type());
        UUID id;
        try {
            id = task ? PrivacyTask.from(data).taskId() : RetentionDue.from(data).runId();
        } catch (IllegalArgumentException e) {
            throw new RejectedEventException(event.type() + " event " + event.id() + " is malformed: " + e.getMessage());
        }
        if (!id.equals(event.aggregateId())) {
            throw new RejectedEventException(event.type() + " event " + event.id() + " is not about the id it names");
        }
        OffsetDateTime now = now();
        Optional<Receipt> receipt = find(id);
        if (receipt.isEmpty()) {
            database.jdbc().sql(insertSql).param("taskId", id).param("kind", task ? TASK : RETENTION)
                    .param("payload", event.data()).param("now", now).update();
            schedule.enrol(code(), now);
        } else if (FAILED.equals(receipt.get().status())) {
            database.jdbc().sql(retrySql).param("taskId", id).param("payload", event.data()).update();
            schedule.enrol(code(), now);
        } else if (!RECEIVED.equals(receipt.get().status())) {
            publish(receipt.get().kind(), id, json.readValue(receipt.get().result(), ANSWER));
        }
    }

    // ---- running ----

    /** Runs every received task and run of the tenant, a batch at a time, each to its answer. */
    @Override
    public final void run() {
        List<UUID> due;
        do {
            due = doInPrimaryReadTransaction(() -> database.jdbc().sql(dueSql).param("limit", BATCH)
                    .query(UUID.class).list());
            due.forEach(this::process);
        } while (due.size() == BATCH);
    }

    private void process(UUID id) {
        Optional<Receipt> receipt = doInWriteTransaction(() -> find(id)).filter(r -> RECEIVED.equals(r.status()));
        if (receipt.isEmpty()) {
            return;
        }
        JsonNode data = json.readTree(receipt.get().payload());
        try {
            if (TASK.equals(receipt.get().kind())) {
                runTask(PrivacyTask.from(data));
            } else {
                sweep(RetentionDue.from(data));
            }
        } catch (RuntimeException e) {
            LOG.warn("module {} could not run privacy task {}", module, id, e);
            runInWriteTransaction(() -> finish(id, receipt.get().kind(), FAILED, TASK.equals(receipt.get().kind())
                    ? TaskResult.failed(TASK_ERROR).answer(PrivacyTask.from(data))
                    : retentionAnswer(id, FAILED, 0, 0, TASK_ERROR)));
        }
    }

    private void runTask(PrivacyTask task) {
        ActiveHolds active = ActiveHolds.none();
        if (task.action() == PrivacyTaskAction.ERASE || task.action() == PrivacyTaskAction.ANONYMISE) {
            try {
                active = holds.activeHolds(module, task.subjectPersonId());
            } catch (UnavailableException e) {
                runInWriteTransaction(() -> finishTask(task, TaskResult.failed(HOLDS_UNAVAILABLE)));
                return;
            }
        }
        ActiveHolds kept = active;
        runInWriteTransaction(() -> {
            if (find(task.taskId()).filter(r -> RECEIVED.equals(r.status())).isEmpty()) {
                return;
            }
            OffsetDateTime now = now();
            finishTask(task, switch (task.action()) {
                case ERASE -> erase(task, kept, now);
                case ANONYMISE -> anonymise(task, kept, now);
                case EXPORT -> export(task);
                case RESTRICT -> restrict(task);
            });
        });
    }

    private void sweep(RetentionDue due) {
        RetentionSweep sweep = retentionSweeps().get(due.policyCode());
        if (sweep == null || !SWEPT.contains(due.action())) {
            runInWriteTransaction(() -> finish(due.runId(), RETENTION, FAILED,
                    retentionAnswer(due.runId(), FAILED, 0, 0, POLICY_UNSUPPORTED)));
            return;
        }
        int affected = 0;
        int held = 0;
        String failure = null;
        UUID after = new UUID(0L, 0L);
        List<UUID> subjects;
        do {
            UUID from = after;
            subjects = doInPrimaryReadTransaction(() -> database.jdbc().sql(sweep.subjects())
                    .param("cutoff", due.cutoff()).param("after", from).param("limit", BATCH)
                    .query(UUID.class).list());
            for (UUID subject : subjects) {
                ActiveHolds active;
                try {
                    active = holds.activeHolds(module, subject);
                } catch (UnavailableException e) {
                    failure = HOLDS_UNAVAILABLE;
                    break;
                }
                Erasure erasure = doInWriteTransaction(() -> eraseSteps(sweep.steps(), subject,
                        Map.of("cutoff", due.cutoff()), active, now()));
                affected += erasure.affected();
                held += erasure.held();
                after = subject;
            }
        } while (failure == null && subjects.size() == BATCH);
        String status = failure != null ? FAILED : held > 0 ? "partial" : "done";
        Map<String, Object> answer = retentionAnswer(due.runId(), status, affected, held, failure);
        runInWriteTransaction(() -> finish(due.runId(), RETENTION, status, answer));
    }

    private void finishTask(PrivacyTask task, TaskResult result) {
        finish(task.taskId(), TASK, result.status().value(), result.answer(task));
    }

    /** Finishes a received receipt and publishes its answer, in the caller's write transaction. */
    private void finish(UUID id, String kind, String status, Map<String, Object> answer) {
        int finished = database.jdbc().sql(finishSql).param("taskId", id).param("status", status)
                .param("result", json.writeValueAsString(answer)).param("now", now()).update();
        if (finished == 1) {
            publish(kind, id, answer);
        }
    }

    private void publish(String kind, UUID id, Map<String, Object> answer) {
        String aggregate = module + (TASK.equals(kind) ? ".privacy_task" : ".privacy_retention");
        outbox.publish(aggregate + ".completed", aggregate, id, answer);
    }

    private static Map<String, Object> retentionAnswer(UUID runId, String status, int affected, int held,
            String errorCode) {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("runId", runId.toString());
        answer.put("status", status);
        answer.put("affected", affected);
        answer.put("held", held);
        if (errorCode != null) {
            answer.put("errorCode", errorCode);
        }
        return answer;
    }

    private void announce(ErasureStep step, UUID row) {
        Map<String, String> payload = Map.of("id", row.toString());
        if (step.deletes()) {
            audit.recordDelete(step.table(), row);
            outbox.publish(step.table() + ".deleted", step.table(), row, payload);
        } else {
            audit.recordUpdate(step.table(), row, step.fields());
            outbox.publish(step.table() + ".updated", step.table(), row, payload);
        }
    }

    private List<UUID> rows(ErasureStep step, UUID subject, Map<String, ?> params) {
        return database.jdbc().sql(step.select()).param("subjectId", subject).params(params).query(UUID.class).list();
    }

    private int count(KeptRows kept, UUID subject) {
        return database.jdbc().sql(kept.count()).param("subjectId", subject).query(Integer.class).single();
    }

    private Optional<Receipt> find(UUID id) {
        return database.jdbc().sql(findSql).param("taskId", id)
                .query((rs, row) -> new Receipt(rs.getString("kind"), rs.getString("payload"), rs.getString("status"),
                        rs.getString("result")))
                .optional();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
