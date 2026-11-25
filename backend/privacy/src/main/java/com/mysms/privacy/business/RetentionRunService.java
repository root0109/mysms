package com.mysms.privacy.business;

import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.privacy.api.model.RetentionRun;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.RetentionRunEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.RetentionMapper;
import com.mysms.privacy.repository.RetentionPolicyRepository;
import com.mysms.privacy.repository.RetentionPolicyView;
import com.mysms.privacy.repository.RetentionRunRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The retention runs of a tenant (privacy.retention_run, design §10): once a day each policy's cutoff — the start of
 * the UTC day minus the period in force — is written as a run and sent to the module holding the data as
 * privacy.retention.due {runId, policyCode, categoryCode, module, cutoff, action}. One run per policy and cutoff, so
 * a repeated run of the day sends nothing again. A run stays dispatched until its module reports
 * ({@code <module>.privacy_retention.completed}, {@link #complete}); a module without a sweep never reports.
 */
@Service
public class RetentionRunService extends AppTransactionManager {

    public static final String AGGREGATE = RetentionRunEntity.TABLE;

    /** What became of a module's report on a run. */
    public enum Completed {
        /** The run was finished with the module's counts. */
        RECORDED,
        /** The run had already finished: a late or repeated report changes nothing. */
        IGNORED,
        /** The tenant sent that module no such run. */
        UNKNOWN
    }

    private static final String DISPATCHED = "dispatched";

    private final RetentionRunRepository repository;
    private final RetentionPolicyRepository policies;
    private final RetentionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;
    /** The modules that sweep (mysms.privacy.erasure.modules): a policy of another module dispatches no run. */
    private final Set<String> modules;

    public RetentionRunService(RetentionRunRepository repository,
            RetentionPolicyRepository policies,
            RetentionMapper mapper,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            Clock clock,
            @Value("${mysms.privacy.erasure.modules}") String modules) {
        super(database, transactions);
        this.repository = repository;
        this.policies = policies;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
        this.modules = Arrays.stream(modules.split(",")).map(String::strip).filter(m -> !m.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** The tenant's runs page by page, the newest first. */
    public Page<RetentionRun> listRetentionRuns(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RetentionRunRepository.SORT_FIELDS,
                RetentionRunRepository.DEFAULT_ORDER, RetentionRunRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RetentionRunRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /**
     * The day's run of every policy for the caller's tenant (the retention job), in one transaction: each policy not
     * yet run for today's cutoff gets its run, audit record and privacy.retention.due.
     */
    public void dispatchDue() {
        runInWriteTransaction(() -> {
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            OffsetDateTime today = now.truncatedTo(ChronoUnit.DAYS);
            for (RetentionPolicyView policy : policies.all()) {
                if (!modules.contains(policy.module())) {
                    continue; // no handler sweeps this module's data yet: a run would wait for ever
                }
                RetentionRunEntity run = RetentionRunEntity.builder()
                        .policyId(policy.id())
                        .module(policy.module())
                        .action(policy.action())
                        .cutoff(today.minusDays(policy.periodDays()))
                        .status(DISPATCHED)
                        .startedAt(now)
                        .build();
                run.onCreatingData(Optional.empty(), clock);
                run.setTenantId(tenant());
                if (repository.insert(run)) {
                    audit.recordCreate(AGGREGATE, run.getId());
                    outbox.publish(PrivacyEvents.RETENTION_DUE, AGGREGATE, run.getId(),
                            PrivacyEvents.retentionDue(run, policy.code(), policy.categoryCode()));
                }
            }
        });
    }

    /**
     * A module's report on its run (inside the inbox's transaction): the dispatched run finished with its status,
     * the rows the module removed and the rows it kept under holds, audited.
     */
    public Completed complete(UUID runId, String module, String status, int affected, int held) {
        return doInWriteTransaction(() -> {
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.complete(runId, module, status, affected, held, now) == 1) {
                audit.recordTransition(AGGREGATE, runId, "complete",
                        List.of("status", "affected", "held", "completedAt"), null);
                return Completed.RECORDED;
            }
            return repository.exists(runId, module) ? Completed.IGNORED : Completed.UNKNOWN;
        });
    }

    private static UUID tenant() {
        return CallContextHolder.require().tenantId().orElseThrow();
    }
}
