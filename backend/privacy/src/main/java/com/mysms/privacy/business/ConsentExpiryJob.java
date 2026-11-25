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
 * The expiry of consents (design §4.3), a {@link ScheduledJob} of each tenant with time-limited decisions: a live
 * decision past its last day (valid_until) is marked expired. In one transaction per decision, under its subject's
 * lock and then its own: an {@code expired} evidence entry, privacy.consent_record.expired, and the recompute of the
 * subject's answer to the purpose. The consent rule already stops counting a lapsed decision; this run records that
 * it lapsed and keeps the stored answers true. A decision already marked, withdrawn or superseded meanwhile is
 * skipped, so a repeated run changes nothing. A tenant enrols when a decision with a last day is recorded
 * ({@link #enrol}).
 */
@Component
public class ConsentExpiryJob extends AppTransactionManager implements ScheduledJob {

    public static final String CODE = "privacy.consent_expiry";
    private static final String EXPIRED = "expired";
    private static final String SYSTEM = "system";
    private static final int BATCH = 100;

    private final ConsentRecordRepository records;
    private final SubjectRepository subjects;
    private final ConsentEvidenceLedger ledger;
    private final ConsentStateWriter stateWriter;
    private final JobSchedule schedule;
    private final OutboxWriter outbox;
    private final Clock clock;
    private final Duration interval;

    public ConsentExpiryJob(ConsentRecordRepository records, SubjectRepository subjects, ConsentEvidenceLedger ledger,
            ConsentStateWriter stateWriter, @Qualifier(PrivacyDatabaseConfig.JOBS) JobSchedule schedule,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox, Clock clock,
            @Value("${mysms.privacy.consent-expiry-job.interval}") Duration interval) {
        super(database, transactions);
        this.records = records;
        this.subjects = subjects;
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

    /** Enrols the caller's tenant (inside the caller's transaction): a decision with a last day was recorded. */
    public void enrol(OffsetDateTime now) {
        schedule.enrol(CODE, now);
    }

    /** Marks every lapsed live decision of the tenant expired, one decision per transaction. */
    @Override
    public void run() {
        LocalDate today = now().toLocalDate();
        List<UUID> due;
        do {
            due = doInPrimaryReadTransaction(() -> records.expiring(today, BATCH));
            due.forEach(id -> runInWriteTransaction(() -> expire(id, today)));
        } while (due.size() == BATCH);
    }

    private void expire(UUID id, LocalDate today) {
        Optional<ConsentRecordEntity> found = records.findById(id);
        if (found.isEmpty()) {
            return;
        }
        subjects.lockStatus(found.get().getSubjectPersonId(), now());
        Optional<ConsentRecordEntity> lapsed = records.lockExpiring(id, today);
        if (lapsed.isEmpty()) {
            return; // withdrawn, superseded or marked meanwhile
        }
        ConsentRecordEntity record = lapsed.get();
        ledger.append(new ConsentEvidenceLedger.Step(record.getTenantId(), record.getId(), EXPIRED, null, null,
                record.getGiverCapacity(), SYSTEM, null, null, null, null));
        outbox.publish(ConsentRecordEntity.TABLE + PrivacyEvents.EXPIRED, ConsentRecordEntity.TABLE, record.getId(),
                PrivacyEvents.consentRecord(record));
        stateWriter.recompute(record.getSubjectPersonId(), record.getPurposeCode());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
