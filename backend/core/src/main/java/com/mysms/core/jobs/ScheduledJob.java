package com.mysms.core.jobs;

import java.time.Duration;

/**
 * A job that runs for every tenant enrolled in it, again {@link #interval()} after each finished run (ROADMAP ARC-4,
 * docs/design/BACKGROUND-JOB-PATTERN.md). Its module's {@link JobSchedule} keeps when each tenant is next due;
 * {@link JobRuntime} claims a due tenant, binds the tenant's call context and calls {@link #run()}.
 *
 * <p>A run must be safe to repeat: one that fails, or outlives its lease, runs again. It does its work through its
 * module's services, in transactions of their own, in bounded batches — never as one long transaction.
 */
public interface ScheduledJob {

    /** The module whose database keeps the job's schedule. */
    String module();

    /** The job's code, unique in the process: {@code <module>.<name>}. */
    String code();

    /** How long after a finished run the tenant is due again. */
    Duration interval();

    /** One run for the tenant bound to the call context. */
    void run();
}
