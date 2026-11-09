package com.mysms.iam.business;

import com.mysms.contracts.iam.model.SettingDefinition;
import com.mysms.contracts.iam.model.SettingDefinitionList;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.SettingDefinitionEntity;
import com.mysms.iam.mapper.SettingDefinitionMapper;
import com.mysms.iam.repository.SettingDefinitionRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * iam.ListSettingDefinitions / iam.GetSettingDefinition: the read-only setting catalogue (global rows). Users see the
 * settings they can set themselves; setting administrators also those of the scopes they administer.
 */
@Service
public class SettingDefinitionService {

    private final SettingDefinitionRepository repository;
    private final SettingDefinitionMapper mapper;
    private final SettingScopes scopes;
    private final ModuleDatabase database;
    private final Clock clock;

    public SettingDefinitionService(SettingDefinitionRepository repository, SettingDefinitionMapper mapper,
                                    SettingScopes scopes,
                                    @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.scopes = scopes;
        this.database = database;
        this.clock = clock;
    }

    /** 404 for an administrator-only setting the caller does not administer at any of its scopes. */
    public SettingDefinition get(UUID id) {
        return database.tx().read(() -> {
            java.util.Set<String> administered = scopes.administeredScopeTypes(SettingRules.ADMINISTER);
            return repository.findById(id).filter(definition -> SettingRules.visibleTo(definition, administered))
                    .map(mapper::toDto)
                    .orElseThrow(() -> new ResourceNotFoundException(SettingDefinitionEntity.TABLE, id));
        });
    }

    public SettingDefinitionList list(Integer pageSize, String pageToken, String filter, String orderBy) {
        SortSpec sort = SortSpec.parse(orderBy, SettingDefinitionRepository.SORT_FIELDS,
                SettingDefinitionRepository.DEFAULT_ORDER, SettingDefinitionRepository.ID_COLUMN);
        SqlFragment parsed = FilterParser.parse(filter, SettingDefinitionRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageSize, pageToken, sort, filter, clock);
        return database.tx().read(() -> {
            // users list what they can set themselves, administrators also the settings of the scopes they administer
            SqlFragment where = parsed.and(SettingDefinitionRepository.visibleFor(
                    scopes.administeredScopeTypes(SettingRules.ADMINISTER)));
            Page<SettingDefinitionEntity> rows = repository.list(page, where);
            return new SettingDefinitionList(rows.items().stream().map(mapper::toDto).toList())
                    .nextPageToken(rows.nextPageToken())
                    .totalSize(repository.count(where));
        });
    }
}
