package com.mysms.platform.business;

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
import com.mysms.platform.api.model.AuditLog;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.mapper.AuditLogMapper;
import com.mysms.platform.repository.AuditLogRepository;
import com.mysms.platform.repository.AuditLogRow;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * The tenant's audit trail, read-only (platform.audit_log, written by the audit events every module publishes). Only a
 * tenant administrator reads it: {@code platform.audit_log.read} held through a tenant-wide grant, since the trail
 * spans every institution; a school-level grant is refused (403). A record holds ids, the action, the changed field
 * names, the reason and the trace — never values — and is shown as stored. A list must name an occurredAt range of at
 * most 366 days (the table is partitioned by month).
 */
@Service
public class AuditLogService extends AppTransactionManager {

    static final String AGGREGATE = "platform.audit_log";
    static final String PERMISSION_READ = "platform.audit_log.read";

    private final AuditLogRepository repository;
    private final AuditLogMapper mapper;
    private final JsonMapper json;
    private final Clock clock;

    public AuditLogService(AuditLogRepository repository, AuditLogMapper mapper, JsonMapper json,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.json = json;
        this.clock = clock;
    }

    public AuditLog getAuditLog(UUID id) {
        requireTenantAdministrator();
        return doInReadTransaction(() -> toDto(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id))));
    }

    public Page<AuditLog> listAuditLogs(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        requireTenantAdministrator();
        FilterParser.requireRange(filter, AuditLogRepository.FILTER_FIELDS, AuditLogRepository.RANGE_FIELD);
        SortSpec sort = SortSpec.parse(sortBy, AuditLogRepository.SORT_FIELDS, AuditLogRepository.DEFAULT_ORDER,
                AuditLogRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AuditLogRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(this::toDto));
    }

    /** These records span every institution of the tenant: only a tenant-wide grant reads them. */
    private static void requireTenantAdministrator() {
        if (!AuthContext.require().hasTenantWidePermission(PERMISSION_READ)) {
            throw new PermissionDeniedException("The audit trail is read by tenant administrators only.");
        }
    }

    private AuditLog toDto(AuditLogRow row) {
        AuditLog log = mapper.toDto(row);
        if (row.properties() != null) {
            log.setProperties(json.readTree(row.properties()));
        }
        return log;
    }
}
