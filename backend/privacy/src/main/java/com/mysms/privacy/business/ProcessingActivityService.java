package com.mysms.privacy.business;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.privacy.api.model.ProcessingActivity;
import com.mysms.privacy.api.model.ProcessingRegisterExport;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.mapper.ProcessingActivityMapper;
import com.mysms.privacy.repository.ProcessingActivityRepository;
import com.mysms.privacy.repository.RegisterEntryView;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The processing register (design §5.1): the platform's catalogue of processing activities, the same for every tenant
 * and read-only through the API; the baseline seeds it (later from each module's privacy manifest). It is listed page by
 * page and exported whole as CSV (the RoPA an auditor asks for).
 */
@Service
public class ProcessingActivityService extends AppTransactionManager {

    private final ProcessingActivityRepository repository;
    private final ProcessingActivityMapper mapper;
    private final Clock clock;

    public ProcessingActivityService(ProcessingActivityRepository repository, ProcessingActivityMapper mapper,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Page<ProcessingActivity> listProcessingActivities(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ProcessingActivityRepository.SORT_FIELDS,
                ProcessingActivityRepository.DEFAULT_ORDER, ProcessingActivityRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ProcessingActivityRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /**
     * privacy.GetProcessingRegisterExport: the whole register as CSV text inside the answer, not a file: the register
     * is a few dozen rows, and one JSON answer serves REST and gRPC alike.
     */
    public ProcessingRegisterExport getProcessingRegisterExport() {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        List<RegisterEntryView> register = doInReadTransaction(repository::register);
        return new ProcessingRegisterExport()
                .fileName("processing-register-" + now.toLocalDate() + ".csv")
                .generatedAt(now)
                .activities(register.size())
                .csv(ProcessingRegisterCsv.write(register));
    }
}
