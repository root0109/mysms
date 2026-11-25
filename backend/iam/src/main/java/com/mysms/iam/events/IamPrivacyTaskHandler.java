package com.mysms.iam.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.iam.business.UserAccountService;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.repository.PrivacyErasureRepository;
import com.mysms.privacyclient.ActiveHolds;
import com.mysms.privacyclient.Erasure;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTask;
import com.mysms.privacyclient.PrivacyTaskHandler;
import com.mysms.privacyclient.TaskResult;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * iam's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1), in wave 1 so nothing
 * reaches an account being erased: each account of the subject is anonymised and then deactivated through
 * {@link UserAccountService#deactivateForErasure}, the path of iam.DeactivateUser (signed out everywhere); an account
 * a legal hold covers is kept and counted. iam.after_inactivity is not swept: its 48 hours' notice (design §10) is not
 * built, so its runs answer failed.
 */
@Component
public class IamPrivacyTaskHandler extends PrivacyTaskHandler {

    static final String ACCOUNT = "iam.user_account";

    static final List<ErasureStep> ERASURE = List.of(new ErasureStep(ACCOUNT, "identity",
            PrivacyErasureRepository.ACCOUNT_ROWS, PrivacyErasureRepository.ANONYMISE_ACCOUNT, false,
            List.of("username", "email", "mobileNo", "password", "emailVerifiedAt", "mobileVerifiedAt", "signature",
                    "useSignature", "externalRef", "locale", "timezone", "mustResetPassword")));

    private final UserAccountService accounts;

    public IamPrivacyTaskHandler(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(IamDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval,
            UserAccountService accounts) {
        super("iam", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
        this.accounts = accounts;
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    /** The accounts anonymised, then each signed off as iam.DeactivateUser does, in the same transaction. */
    @Override
    protected TaskResult erase(PrivacyTask task, ActiveHolds active, OffsetDateTime now) {
        Erasure erasure = eraseSteps(ERASURE, task.subjectPersonId(), Map.of(), active, now);
        erasure.changed(ACCOUNT).forEach(accounts::deactivateForErasure);
        return TaskResult.erased(erasure, List.of());
    }
}
