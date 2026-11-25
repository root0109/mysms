package com.mysms.privacy.business;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.jobs.ScheduledJob;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.ConsentRecordEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.repository.ConsentRecordRepository;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The age-of-majority transition (design §4.3), a {@link ScheduledJob} of each tenant with minors: every minor whose
 * majority date has come becomes an adult. In one transaction per person, under the subject's lock: the status
 * changes, each live decision a guardian or nominee gave about them leaves a {@code majority_lapsed} evidence entry
 * (from now on only the person's own decisions count), the person's consent answers are recomputed and
 * privacy.subject.majority_reached is published. A person who is no longer such a minor when locked is skipped, so a
 * repeated run changes nothing. A tenant enrols the first time privacy keeps a minor of it ({@link #enrol}).
 */
@Component
public class MajorityJob extends AppTransactionManager implements ScheduledJob {

    public static final String CODE = "privacy.majority";
    private static final String MAJORITY_LAPSED = "majority_lapsed";
    private static final String SYSTEM = "system";
    private static final int BATCH = 100;

    private final SubjectRepository subjects;
    private final ConsentRecordRepository records;
    private final ConsentEvidenceLedger ledger;
    private final ConsentStateWriter stateWriter;
    private final JobSchedule schedule;
    private final OutboxWriter outbox;
    private final Clock clock;
    private final Duration interval;

    public MajorityJob(SubjectRepository subjects, ConsentRecordRepository records, ConsentEvidenceLedger ledger,
            ConsentStateWriter stateWriter, @Qualifier(PrivacyDatabaseConfig.JOBS) JobSchedule schedule,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox, Clock clock,
            @Value("${mysms.privacy.majority-job.interval}") Duration interval) {
        super(database, transactions);
        this.subjects = subjects;
        this.records = records;
        this.ledger = ledger;
        this.stateWriter = stateWriter;
        this.schedule = schedule;
        this.outbox = outbox;
        this.clock = clock;
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

    /** Enrols the caller's tenant (inside the caller's transaction): privacy now keeps a minor of it. */
    public void enrol(OffsetDateTime now) {
        schedule.enrol(CODE, now);
    }

    /** Turns every minor of the tenant whose majority date has come into an adult, one person per transaction. */
    @Override
    public void run() {
        LocalDate today = now().toLocalDate();
        List<UUID> due;
        do {
            due = doInPrimaryReadTransaction(() -> subjects.comingOfAge(today, BATCH));
            due.forEach(person -> runInWriteTransaction(() -> comeOfAge(person, today)));
        } while (due.size() == BATCH);
    }

    private void comeOfAge(UUID person, LocalDate today) {
        OffsetDateTime now = now();
        subjects.lockStatus(person, now);
        Optional<LocalDate> majorityDate = subjects.comeOfAge(person, today, now);
        if (majorityDate.isEmpty()) {
            return; // no longer a minor coming of age (a person event or an earlier run got here first)
        }
        for (ConsentRecordEntity record : records.onBehalfOf(person)) {
            ledger.append(new ConsentEvidenceLedger.Step(record.getTenantId(), record.getId(), MAJORITY_LAPSED, null,
                    null, record.getGiverCapacity(), SYSTEM, null, null, null, null));
        }
        stateWriter.recomputeSubject(person);
        outbox.publish(PrivacyEvents.SUBJECT + PrivacyEvents.MAJORITY_REACHED, PrivacyEvents.SUBJECT, person,
                PrivacyEvents.majorityReached(person, majorityDate.get(), subjects.stateVersion(person)));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
