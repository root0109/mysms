package com.mysms.core.jobs;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One module's job schedule ({@code <module>.job_schedule}): per job and tenant, when the tenant is next due and which
 * runner holds it. A tenant enrols in a job the first time it has work for it ({@link #enrol}, inside the caller's
 * transaction), so no list of tenants is needed and a tenant without work never runs the job.
 *
 * <p>The table holds job codes, tenant ids and times only and has no row-level security, like iam.tenant_directory:
 * the runner claims due rows across tenants as the application role in a global transaction, then runs each one in
 * its tenant's own call context. A claim is a lease taken with {@code FOR UPDATE SKIP LOCKED}, so replicas never run
 * one tenant's job at the same time, and a runner that died is replaced once its lease has ended.
 */
public final class JobSchedule extends AppTransactionManager {

    /** A due job of one tenant, leased to this runner; {@code failures} counts its failed runs in a row. */
    public record Claim(String jobCode, UUID tenantId, int failures) {
    }

    private static final String ENROL = """
            INSERT INTO {schema}.job_schedule (job_code, tenant_id, next_run_at)
            VALUES (:jobCode, :tenantId, :now)
            ON CONFLICT (job_code, tenant_id) DO NOTHING
            """;
    private static final String CLAIM = """
            UPDATE {schema}.job_schedule s
               SET lease_owner = :owner, lease_until = :leaseUntil
             WHERE (s.job_code, s.tenant_id) IN (SELECT d.job_code, d.tenant_id
                                                   FROM {schema}.job_schedule d
                                                  WHERE d.job_code = ANY(CAST(:codes AS varchar[]))
                                                    AND d.next_run_at <= :now
                                                    AND (d.lease_until IS NULL OR d.lease_until < :now)
                                                  ORDER BY d.next_run_at
                                                  LIMIT :limit
                                                    FOR UPDATE SKIP LOCKED)
            RETURNING s.job_code, s.tenant_id, s.failures
            """;
    private static final String COMPLETE = """
            UPDATE {schema}.job_schedule
               SET next_run_at = :nextRunAt, last_run_at = :now, failures = 0, last_error = NULL,
                   lease_owner = NULL, lease_until = NULL
             WHERE job_code = :jobCode AND tenant_id = :tenant AND lease_owner = :owner
            """;
    private static final String FAIL = """
            UPDATE {schema}.job_schedule
               SET next_run_at = :nextRunAt, last_run_at = :now, failures = failures + 1, last_error = :error,
                   lease_owner = NULL, lease_until = NULL
             WHERE job_code = :jobCode AND tenant_id = :tenant AND lease_owner = :owner
            """;
    private static final int ERROR_LENGTH = 500;

    private final ModuleDatabase database;
    private final String enrol;
    private final String claim;
    private final String complete;
    private final String fail;

    public JobSchedule(ModuleDatabase database) {
        super(database, database.transactionManager());
        this.database = database;
        String schema = database.module();
        this.enrol = ENROL.replace("{schema}", schema);
        this.claim = CLAIM.replace("{schema}", schema);
        this.complete = COMPLETE.replace("{schema}", schema);
        this.fail = FAIL.replace("{schema}", schema);
    }

    public String module() {
        return database.module();
    }

    /**
     * Enrols the caller's tenant in the job, due at once, unless it is enrolled already. Runs inside the caller's
     * transaction, so the tenant is enrolled exactly when the work that needs the job is committed.
     */
    public void enrol(String jobCode, OffsetDateTime now) {
        database.jdbc().sql(enrol).param("jobCode", jobCode).param("now", now).update();
    }

    /**
     * Leases up to {@code limit} due runs of the given jobs (those this process has: a replica of another version
     * leaves the others alone), the longest overdue first, until {@code leaseUntil}.
     */
    public List<Claim> claim(UUID owner, List<String> jobCodes, OffsetDateTime now, OffsetDateTime leaseUntil,
            int limit) {
        return doInGlobalWriteTransaction(() -> database.jdbc().sql(claim).param("owner", owner)
                .param("codes", jobCodes.toArray(String[]::new)).param("now", now).param("leaseUntil", leaseUntil)
                .param("limit", limit)
                .query((rs, row) -> new Claim(rs.getString("job_code"), rs.getObject("tenant_id", UUID.class),
                        rs.getInt("failures")))
                .list());
    }

    /** Ends the owner's lease after a run that finished; false when the lease had ended and another runner holds it. */
    public boolean complete(Claim done, UUID owner, OffsetDateTime now, OffsetDateTime nextRunAt) {
        return doInGlobalWriteTransaction(() -> database.jdbc().sql(complete).param("jobCode", done.jobCode())
                .param("tenant", done.tenantId()).param("owner", owner).param("now", now)
                .param("nextRunAt", nextRunAt).update() == 1);
    }

    /** Ends the owner's lease after a run that failed, noting why; false when another runner holds the lease. */
    public boolean fail(Claim failed, UUID owner, OffsetDateTime now, OffsetDateTime nextRunAt, String error) {
        String noted = error.length() > ERROR_LENGTH ? error.substring(0, ERROR_LENGTH) : error;
        return doInGlobalWriteTransaction(() -> database.jdbc().sql(fail).param("jobCode", failed.jobCode())
                .param("tenant", failed.tenantId()).param("owner", owner).param("now", now)
                .param("nextRunAt", nextRunAt).param("error", noted).update() == 1);
    }
}
