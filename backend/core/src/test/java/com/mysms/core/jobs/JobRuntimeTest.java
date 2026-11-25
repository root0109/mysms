package com.mysms.core.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The runtime's rules: which jobs it accepts, and when a finished, failed or unknown run is due again. */
class JobRuntimeTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = CLOCK.instant().atOffset(ZoneOffset.UTC);
    private static final Duration POLL = Duration.ofMinutes(1);
    private static final Duration LEASE = Duration.ofMinutes(30);
    private static final UUID TENANT = UUID.randomUUID();

    private final JobSchedule schedule = mock(JobSchedule.class);

    private static ScheduledJob job(String module, String code, Duration interval, Runnable work) {
        return new ScheduledJob() {
            @Override
            public String module() {
                return module;
            }

            @Override
            public String code() {
                return code;
            }

            @Override
            public Duration interval() {
                return interval;
            }

            @Override
            public void run() {
                work.run();
            }
        };
    }

    private JobRuntime runtime(ScheduledJob... jobs) {
        when(schedule.module()).thenReturn("demo");
        return new JobRuntime(List.of(schedule), List.of(jobs), CLOCK, POLL, LEASE, 5);
    }

    /** One due run of the job, then none. */
    private void due(String code, int failures) {
        when(schedule.claim(any(), eq(List.of(code)), eq(NOW), eq(NOW.plus(LEASE)), eq(1)))
                .thenReturn(List.of(new JobSchedule.Claim(code, TENANT, failures)), List.of());
    }

    @Test
    void aFinishedRunIsDueAgainAfterTheJobsInterval() {
        JobRuntime runtime = runtime(job("demo", "demo.tidy", Duration.ofHours(6), () -> { }));
        due("demo.tidy", 0);
        when(schedule.complete(any(), any(), any(), any())).thenReturn(true);

        assertThat(runtime.runOnce()).isEqualTo(1);
        verify(schedule).complete(eq(new JobSchedule.Claim("demo.tidy", TENANT, 0)), any(), eq(NOW),
                eq(NOW.plusHours(6)));
    }

    @Test
    void aFailedRunIsRetriedAfterADelayDoublingWithEachFailureUpToTheInterval() {
        JobRuntime runtime = runtime(job("demo", "demo.tidy", Duration.ofHours(1), () -> {
            throw new IllegalStateException("boom");
        }));
        due("demo.tidy", 3);
        assertThat(runtime.runOnce()).isZero();
        verify(schedule).fail(any(), any(), eq(NOW), eq(NOW.plusMinutes(8)), startsWith(
                "java.lang.IllegalStateException: boom"));

        due("demo.tidy", 9);
        runtime.runOnce();
        verify(schedule).fail(any(), any(), eq(NOW), eq(NOW.plusHours(1)), any());
        verify(schedule, never()).complete(any(), any(), any(), any());
    }

    @Test
    void claimsOnlyRunsOfItsOwnJobsOneAtATimeUpToTheBatchSize() {
        JobRuntime runtime = runtime(job("demo", "demo.tidy", Duration.ofHours(1), () -> { }));
        when(schedule.claim(any(), eq(List.of("demo.tidy")), eq(NOW), eq(NOW.plus(LEASE)), eq(1)))
                .thenReturn(List.of(new JobSchedule.Claim("demo.tidy", TENANT, 0)));
        when(schedule.complete(any(), any(), any(), any())).thenReturn(true);

        assertThat(runtime.runOnce()).as("batch size 5").isEqualTo(5);
    }

    @Test
    void refusesAJobWithoutItsModulesScheduleAWrongNameOrATwin() {
        when(schedule.module()).thenReturn("demo");
        assertThatThrownBy(() -> new JobRuntime(List.of(schedule),
                List.of(job("other", "other.tidy", Duration.ofHours(1), () -> { })), CLOCK, POLL, LEASE, 5))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JobRuntime(List.of(schedule),
                List.of(job("demo", "tidy", Duration.ofHours(1), () -> { })), CLOCK, POLL, LEASE, 5))
                .isInstanceOf(IllegalStateException.class);
        ScheduledJob tidy = job("demo", "demo.tidy", Duration.ofHours(1), () -> { });
        assertThatThrownBy(() -> new JobRuntime(List.of(schedule), List.of(tidy, tidy), CLOCK, POLL, LEASE, 5))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JobRuntime(List.of(schedule), List.of(), CLOCK, Duration.ZERO, LEASE, 5))
                .isInstanceOf(IllegalArgumentException.class);
        verify(schedule, never()).claim(any(), any(), any(), any(), anyInt());
    }
}
