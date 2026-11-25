package com.mysms.payroll.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.config.PayrollEventsConfig;
import com.mysms.payroll.repository.PrivacyErasureRepository;
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
 * payroll's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10): the
 * subject's bank accounts are cleared (holder's name, the account number's envelope, bank details) and soft-deleted;
 * the wage records — salary structures and their components, payslips and their lines, payments — are kept under
 * payroll.after_leaving (eight years) and counted with the policy code, untouched (their remarks belong to the record).
 * Rows a legal hold covers are kept and counted. payroll.after_leaving archives, which is not built: its runs answer
 * failed (privacy-client).
 */
@Component
public class PayrollPrivacyTaskHandler extends PrivacyTaskHandler {

    static final String RETENTION_POLICY = "payroll.after_leaving";

    static final List<ErasureStep> ERASURE = List.of(new ErasureStep("payroll.staff_bank_account", "financial",
            PrivacyErasureRepository.BANK_ACCOUNT_ROWS, PrivacyErasureRepository.ERASE_BANK_ACCOUNT, true, List.of()));

    /** The wage records kept under {@link #RETENTION_POLICY}. */
    static final List<KeptRows> KEPT = List.of(
            new KeptRows("payroll.staff_salary", PrivacyErasureRepository.SALARY_KEPT, RETENTION_POLICY),
            new KeptRows("payroll.staff_salary_component", PrivacyErasureRepository.SALARY_COMPONENT_KEPT,
                    RETENTION_POLICY),
            new KeptRows("payroll.payslip", PrivacyErasureRepository.PAYSLIP_KEPT, RETENTION_POLICY),
            new KeptRows("payroll.payslip_line", PrivacyErasureRepository.PAYSLIP_LINE_KEPT, RETENTION_POLICY),
            new KeptRows("payroll.payroll_payment", PrivacyErasureRepository.PAYMENT_KEPT, RETENTION_POLICY));

    public PayrollPrivacyTaskHandler(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(PayrollDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("payroll", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    /** The wage records, kept and counted under {@link #RETENTION_POLICY}. */
    @Override
    protected List<KeptRows> keptRows() {
        return KEPT;
    }
}
