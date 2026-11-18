package com.mysms.platform.business;

import com.mysms.authclient.DataScope;
import com.mysms.platform.api.model.DeletionLog;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.DeletionLogEntity;
import com.mysms.platform.mapper.DeletionLogMapper;
import com.mysms.platform.repository.DeletionLogRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.deletion_log: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class DeletionLogService extends AppTransactionManager {

    public static final String AGGREGATE = DeletionLogEntity.TABLE;
    static final String PERMISSION_LIST = "platform.deletion_log.read";
    static final String PERMISSION_GET = "platform.deletion_log.read";


    private final DeletionLogRepository repository;
    private final DeletionLogMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public DeletionLogService(DeletionLogRepository repository, DeletionLogMapper mapper,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public DeletionLog getDeletionLog(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<DeletionLog> listDeletionLogs(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, DeletionLogRepository.SORT_FIELDS, DeletionLogRepository.DEFAULT_ORDER,
                DeletionLogRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, DeletionLogRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<DeletionLogEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private DeletionLogEntity loadVisible(DataScope scope, UUID id) {
        DeletionLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(DeletionLogEntity entity) {
        return Optional.empty();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(DeletionLogEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getEntityType() == null) {
            errors.add(new FieldError("entityType", "must not be null", "NotNull"));
        }
        if (entity.getEntityId() == null) {
            errors.add(new FieldError("entityId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(DeletionLogEntity entity) {

    }
}
