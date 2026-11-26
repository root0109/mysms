package com.mysms.campus.library.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.config.LibraryEventsConfig;
import com.mysms.campus.library.repository.PrivacyErasureRepository;
import com.mysms.privacyclient.ActiveHolds;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTask;
import com.mysms.privacyclient.PrivacyTaskHandler;
import com.mysms.privacyclient.TaskResult;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * library's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1): the subject's
 * reservations are cleared and soft-deleted, the requester of their purchase requests cleared, the borrower account of
 * their loans cleared (the circulation history stays against the card), and each library card deactivated,
 * anonymised and soft-deleted, last. A loan still open — the copy not back, or a fine charged and not yet invoiced —
 * is kept with its card and counted as held, like the rows a legal hold covers. Reservations made by an account alone
 * are not found: library keeps no account's person. library has no retention policy of its own.
 */
@Component
public class LibraryPrivacyTaskHandler extends PrivacyTaskHandler {

    static final ErasureStep LOAN = new ErasureStep("library.loan", "academic_record",
            PrivacyErasureRepository.LOAN_ROWS, PrivacyErasureRepository.ANONYMISE_LOAN, false,
            List.of("borrowerUserId"));
    static final ErasureStep CARD = new ErasureStep("library.library_card", "identity",
            PrivacyErasureRepository.CARD_ROWS, PrivacyErasureRepository.ERASE_CARD, true, List.of());

    /** The tables of a subject's erasure, the cards (which the loans are found through) last. */
    static final List<ErasureStep> ERASURE = List.of(
            new ErasureStep("library.reservation", "academic_record", PrivacyErasureRepository.RESERVATION_ROWS,
                    PrivacyErasureRepository.ERASE_RESERVATION, true, List.of()),
            new ErasureStep("library.purchase_request", "academic_record",
                    PrivacyErasureRepository.PURCHASE_REQUEST_ROWS,
                    PrivacyErasureRepository.ANONYMISE_PURCHASE_REQUEST, false, List.of("requestedByPersonId")),
            LOAN, CARD);

    private final ModuleDatabase database;

    public LibraryPrivacyTaskHandler(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LibraryDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LibraryEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LibraryEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(LibraryDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("library", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
        this.database = database;
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    /** The erasure, its open loans and their cards — left unchanged by it — counted as held (a hold's not twice). */
    @Override
    protected TaskResult erase(PrivacyTask task, ActiveHolds active, OffsetDateTime now) {
        TaskResult erased = super.erase(task, active, now);
        int open = open(LOAN, PrivacyErasureRepository.OPEN_LOAN_ROWS, task.subjectPersonId(), active)
                + open(CARD, PrivacyErasureRepository.OPEN_CARD_ROWS, task.subjectPersonId(), active);
        return open == 0 ? erased : new TaskResult(TaskResult.Status.PARTIAL, erased.counts(),
                erased.heldItems() + open, erased.policyCodes(), null, null);
    }

    private int open(ErasureStep step, String sql, UUID subject, ActiveHolds active) {
        return (int) database.jdbc().sql(sql).param("subjectId", subject).query(UUID.class).list().stream()
                .filter(row -> !active.keeps(step.table(), step.categoryCode(), row)).count();
    }
}
