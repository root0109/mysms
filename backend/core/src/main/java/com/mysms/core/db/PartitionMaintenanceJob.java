package com.mysms.core.db;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Runs {@link PartitionMaintenance} over every module database of this process: at startup and then every
 * {@code mysms.partitions.interval} (default 24 hours), on one background thread. A failed run is logged and retried
 * at the next interval; it never stops the service.
 */
public final class PartitionMaintenanceJob implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(PartitionMaintenanceJob.class);

    private final List<ModuleDatabase> databases;
    private final Clock clock;
    private final Duration interval;
    private final int monthsAhead;
    private final int retentionMonths;
    private ScheduledExecutorService executor;

    public PartitionMaintenanceJob(List<ModuleDatabase> databases, Clock clock, Duration interval, int monthsAhead,
                                   int retentionMonths) {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("mysms.partitions.interval must be positive");
        }
        this.databases = List.copyOf(databases);
        this.clock = clock;
        this.interval = interval;
        this.monthsAhead = monthsAhead;
        this.retentionMonths = retentionMonths;
    }

    /** One pass over every module database; returns the reports in module order. */
    public List<PartitionMaintenance.Report> runOnce() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return databases.stream().map(d -> {
            try {
                return PartitionMaintenance.run(d.module(), d.ownerDataSource(), today, monthsAhead, retentionMonths);
            } catch (RuntimeException e) {
                LOG.error("partition maintenance of module {} failed", d.module(), e);
                return new PartitionMaintenance.Report(List.of(), List.of(), List.of(), List.of(d.module()));
            }
        }).toList();
    }

    @Override
    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "partition-maintenance");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::runOnce, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
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
