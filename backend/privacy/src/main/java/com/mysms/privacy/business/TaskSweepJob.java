package com.mysms.privacy.business;

import com.mysms.core.jobs.ScheduledJob;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The SLA sweep of erasure and export requests (design §9.2), a {@link ScheduledJob} of each tenant that started a
 * fulfilment: every module task still unanswered {@code mysms.privacy.erasure.redispatch-after} after its dispatch is
 * dispatched again under the same task id ({@link RightsRequestSaga#redispatchDue}), so a module that lost the
 * event, or was down, gets it again; so is every failed task whose back-off ({@code mysms.privacy.erasure.retry-delay},
 * doubled per attempt) has run out. A repeated run changes nothing: a task re-dispatched waits its full period
 * again. A tenant enrols when it starts a fulfilment.
 */
@Component
public class TaskSweepJob implements ScheduledJob {

    public static final String CODE = "privacy.task_sweep";
    private static final int BATCH = 100;

    private final RightsRequestSaga saga;
    private final Duration interval;

    public TaskSweepJob(RightsRequestSaga saga, @Value("${mysms.privacy.erasure.sweep-interval}") Duration interval) {
        this.saga = saga;
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

    @Override
    public void run() {
        saga.redispatchDue(BATCH);
    }
}
