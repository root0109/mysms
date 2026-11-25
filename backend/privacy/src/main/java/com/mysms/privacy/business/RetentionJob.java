package com.mysms.privacy.business;

import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.jobs.ScheduledJob;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The retention scheduler (design §10), a {@link ScheduledJob} of each tenant with personal data: every
 * {@code mysms.privacy.retention-job.interval} (a day by default) it sends each retention policy's cutoff to the
 * module holding the data ({@link RetentionRunService#dispatchDue}). A repeated run of the same UTC day sends nothing
 * again. A tenant enrols when privacy first learns of one of its persons ({@link #enrol}).
 */
@Component
public class RetentionJob implements ScheduledJob {

    public static final String CODE = "privacy.retention";

    private final RetentionRunService runs;
    private final JobSchedule schedule;
    private final Duration interval;

    public RetentionJob(RetentionRunService runs, @Qualifier(PrivacyDatabaseConfig.JOBS) JobSchedule schedule,
            @Value("${mysms.privacy.retention-job.interval}") Duration interval) {
        this.runs = runs;
        this.schedule = schedule;
        this.interval = interval;
    }

    @Override
    public String module() {
        return "privacy";
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Duration interval() {
        return interval;
    }

    /** Enrols the caller's tenant (inside the caller's transaction): privacy holds one of its persons. */
    public void enrol(OffsetDateTime now) {
        schedule.enrol(CODE, now);
    }

    @Override
    public void run() {
        runs.dispatchDue();
    }
}
