package com.mysms.core.jobs;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.trace.TraceContext;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Runs this process's {@link ScheduledJob}s: every {@code mysms.jobs.poll-interval}, on one background thread, it
 * leases each module's due runs of its own jobs from the module's {@link JobSchedule} one at a time (so each lease
 * starts with its run), up to {@code mysms.jobs.batch-size} per module and pass, and runs each in its tenant's call
 * context. A finished run is due again after the job's interval; a failed one is logged and retried after a delay
 * that doubles with every failure in a row (from the poll interval up to the job's interval). A failure never stops
 * the runtime, and one tenant's failure never stops another tenant's run.
 */
public final class JobRuntime implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(JobRuntime.class);
    private static final int MAX_DOUBLINGS = 20;

    private final List<JobSchedule> schedules;
    private final Map<String, ScheduledJob> jobs;
    private final Map<String, List<String>> codesByModule;
    private final Clock clock;
    private final Duration pollInterval;
    private final Duration lease;
    private final int batchSize;
    private final UUID owner = UUID.randomUUID();
    private ScheduledExecutorService executor;

    public JobRuntime(List<JobSchedule> schedules, List<ScheduledJob> jobs, Clock clock, Duration pollInterval,
                      Duration lease, int batchSize) {
        if (pollInterval.isNegative() || pollInterval.isZero() || lease.isNegative() || lease.isZero()
                || batchSize < 1) {
            throw new IllegalArgumentException("mysms.jobs.poll-interval and mysms.jobs.lease must be positive, "
                    + "mysms.jobs.batch-size at least 1");
        }
        Map<String, JobSchedule> byModule = new HashMap<>();
        schedules.forEach(s -> byModule.put(s.module(), s));
        Map<String, ScheduledJob> byCode = new HashMap<>();
        for (ScheduledJob job : jobs) {
            if (!byModule.containsKey(job.module())) {
                throw new IllegalStateException("job " + job.code() + " has no job schedule of module " + job.module()
                        + " in this process");
            }
            if (!job.code().startsWith(job.module() + ".")) {
                throw new IllegalStateException("job " + job.code() + " must be named " + job.module() + ".<name>");
            }
            if (job.interval().isNegative() || job.interval().isZero()) {
                throw new IllegalStateException("job " + job.code() + " needs a positive interval");
            }
            if (byCode.putIfAbsent(job.code(), job) != null) {
                throw new IllegalStateException("two jobs are named " + job.code());
            }
        }
        this.schedules = List.copyOf(schedules);
        this.jobs = Map.copyOf(byCode);
        this.codesByModule = Map.copyOf(byCode.values().stream().collect(Collectors.groupingBy(ScheduledJob::module,
                Collectors.mapping(ScheduledJob::code, Collectors.toUnmodifiableList()))));
        this.clock = clock;
        this.pollInterval = pollInterval;
        this.lease = lease;
        this.batchSize = batchSize;
    }

    /** One pass over every module's schedule; answers the number of runs that finished (tests and the scheduler). */
    public int runOnce() {
        int finished = 0;
        for (JobSchedule schedule : schedules) {
            List<String> codes = codesByModule.getOrDefault(schedule.module(), List.of());
            for (int run = 0; run < batchSize && !codes.isEmpty(); run++) {
                try {
                    OffsetDateTime now = now();
                    List<JobSchedule.Claim> claimed = schedule.claim(owner, codes, now, now.plus(lease), 1);
                    if (claimed.isEmpty()) {
                        break;
                    }
                    finished += run(schedule, claimed.get(0)) ? 1 : 0;
                } catch (RuntimeException e) {
                    LOG.warn("job schedule of module {} failed; its due runs are taken up at the next pass",
                            schedule.module(), e);
                    break;
                }
            }
        }
        return finished;
    }

    /** Runs one claimed job (one of this process's) in its tenant's context and ends the lease; whether it finished. */
    private boolean run(JobSchedule schedule, JobSchedule.Claim claim) {
        ScheduledJob job = jobs.get(claim.jobCode());
        CallContext context = new CallContext(Optional.of(claim.tenantId()), Optional.empty(),
                TraceContext.newRoot().traceId());
        try {
            CallContextHolder.callAs(context, () -> {
                job.run();
                return null;
            });
        } catch (RuntimeException e) {
            LOG.warn("job {} failed for tenant {} ({} failures in a row before)", job.code(), claim.tenantId(),
                    claim.failures(), e);
            schedule.fail(claim, owner, now(), now().plus(retryDelay(job, claim.failures())), describe(e));
            return false;
        }
        if (!schedule.complete(claim, owner, now(), now().plus(job.interval()))) {
            LOG.warn("job {} for tenant {} outlived its lease of {}", job.code(), claim.tenantId(), lease);
        }
        return true;
    }

    /** The poll interval, doubled for each earlier failure in a row, at most the job's interval. */
    private Duration retryDelay(ScheduledJob job, int failures) {
        Duration delay = pollInterval.multipliedBy(1L << Math.min(failures, MAX_DOUBLINGS));
        return delay.compareTo(job.interval()) < 0 ? delay : job.interval();
    }

    private static String describe(RuntimeException failure) {
        return failure.getClass().getName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public synchronized void start() {
        if (executor != null || schedules.isEmpty()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "scheduled-jobs");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::runOnce, pollInterval.toMillis(), pollInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return executor != null;
    }
}
