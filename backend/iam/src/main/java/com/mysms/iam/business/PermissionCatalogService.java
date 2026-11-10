package com.mysms.iam.business;

import java.util.List;
import com.mysms.iam.api.model.Permission;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.PermissionEntity;
import com.mysms.iam.mapper.PermissionMapper;
import com.mysms.iam.repository.PermissionCatalogRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/** iam.ListPermissions / iam.GetPermission: the permission catalogue administrators compose roles from. */
@Service
public class PermissionCatalogService extends AppTransactionManager {

    private final PermissionCatalogRepository repository;
    private final PermissionMapper mapper;
    private final Clock clock;

    public PermissionCatalogService(PermissionCatalogRepository repository, PermissionMapper mapper,
            @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Permission get(UUID id) {
        return doInReadTransaction(() -> {
            return repository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(PermissionEntity.TABLE, id));
        });
    }

    public Page<Permission> list(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PermissionCatalogRepository.SORT_FIELDS,
                PermissionCatalogRepository.DEFAULT_ORDER, PermissionCatalogRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PermissionCatalogRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PermissionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }
}
