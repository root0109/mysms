package com.mysms.iam.business;

import com.mysms.contracts.iam.model.Permission;
import com.mysms.contracts.iam.model.PermissionList;
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

/** iam.ListPermissions / iam.GetPermission: the permission catalogue administrators compose roles from. */
@Service
public class PermissionCatalogService {

    private final PermissionCatalogRepository repository;
    private final PermissionMapper mapper;
    private final ModuleDatabase database;
    private final Clock clock;

    public PermissionCatalogService(PermissionCatalogRepository repository, PermissionMapper mapper,
            @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.database = database;
        this.clock = clock;
    }

    public Permission get(UUID id) {
        return database.tx().read(() -> repository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(PermissionEntity.TABLE, id)));
    }

    public PermissionList list(Integer pageSize, String pageToken, String filter, String orderBy) {
        SortSpec sort = SortSpec.parse(orderBy, PermissionCatalogRepository.SORT_FIELDS,
                PermissionCatalogRepository.DEFAULT_ORDER, PermissionCatalogRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PermissionCatalogRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageSize, pageToken, sort, filter, clock);
        return database.tx().read(() -> {
            Page<PermissionEntity> rows = repository.list(page, where);
            return new PermissionList(rows.items().stream().map(mapper::toDto).toList())
                    .nextPageToken(rows.nextPageToken())
                    .totalSize(repository.count(where));
        });
    }
}
