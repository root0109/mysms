package com.mysms.finance.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.repository.PrivacyErasureRepository;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.KeptRows;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTaskHandler;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * finance's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10): the raw
 * bodies of the gateway's notifications about the subject's payments (the payer's contact and card details as the
 * gateway sent them) are cleared, the notifications kept; the books of account — fee dues, concessions, invoices,
 * payments (payer and billing details, the encrypted tax identity included) and refunds — are kept under
 * finance.after_leaving (eight years) and counted with the policy code, untouched. Rows a legal hold covers are kept
 * and counted. Invoices billed to an account alone and payments of a payer account alone are not found: finance keeps
 * no account's person. finance.after_leaving archives, which is not built: its runs answer failed (privacy-client).
 */
@Component
public class FinancePrivacyTaskHandler extends PrivacyTaskHandler {

    static final String RETENTION_POLICY = "finance.after_leaving";

    /** The tables of a subject's erasure. */
    static final List<ErasureStep> ERASURE = List.of(new ErasureStep("finance.gateway_event", "financial",
            PrivacyErasureRepository.GATEWAY_EVENT_ROWS, PrivacyErasureRepository.ANONYMISE_GATEWAY_EVENT, false,
            List.of("rawPayload")));

    /** The books of account kept under {@link #RETENTION_POLICY}. */
    static final List<KeptRows> KEPT = List.of(
            new KeptRows("finance.student_fee_due", PrivacyErasureRepository.STUDENT_FEE_DUE_KEPT, RETENTION_POLICY),
            new KeptRows("finance.fee_concession", PrivacyErasureRepository.FEE_CONCESSION_KEPT, RETENTION_POLICY),
            new KeptRows("finance.invoice", PrivacyErasureRepository.INVOICE_KEPT, RETENTION_POLICY),
            new KeptRows("finance.payment", PrivacyErasureRepository.PAYMENT_KEPT, RETENTION_POLICY),
            new KeptRows("finance.refund", PrivacyErasureRepository.REFUND_KEPT, RETENTION_POLICY));

    public FinancePrivacyTaskHandler(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(FinanceDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("finance", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    /** The books of account, kept and counted under {@link #RETENTION_POLICY}. */
    @Override
    protected List<KeptRows> keptRows() {
        return KEPT;
    }
}
