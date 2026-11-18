package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.lms.api.model.AccessLog;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.mapper.AccessLogMapper;
import com.mysms.lms.repository.AccessLogRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The learning access log, read-only (lms.access_log: page and API accesses kept for analytics and security review).
 * A record names a user, their network address and browser, across every institution's cohorts, so only a tenant
 * administrator reads it: {@code lms.access_log.read} through a tenant-wide grant; a school-level grant is refused
 * (403). A list must name an accessedAt range of at most 366 days (the table is partitioned by month).
 */
@Service
public class AccessLogService extends AppTransactionManager {

    static final String AGGREGATE = "lms.access_log";
    static final String PERMISSION_READ = "lms.access_log.read";

    private final AccessLogRepository repository;
    private final AccessLogMapper mapper;
    private final Clock clock;

    public AccessLogService(AccessLogRepository repository, AccessLogMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public AccessLog getAccessLog(UUID id) {
        requireTenantAdministrator();
        return doInReadTransaction(() -> mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id))));
    }

    public Page<AccessLog> listAccessLogs(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        requireTenantAdministrator();
        FilterParser.requireRange(filter, AccessLogRepository.FILTER_FIELDS, AccessLogRepository.RANGE_FIELD);
        SortSpec sort = SortSpec.parse(sortBy, AccessLogRepository.SORT_FIELDS, AccessLogRepository.DEFAULT_ORDER,
                AccessLogRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AccessLogRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    private static void requireTenantAdministrator() {
        if (!AuthContext.require().hasTenantWidePermission(PERMISSION_READ)) {
            throw new PermissionDeniedException("The access log is read by tenant administrators only.");
        }
    }
}
