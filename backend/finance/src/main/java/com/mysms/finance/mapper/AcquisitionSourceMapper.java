package com.mysms.finance.mapper;

import com.mysms.finance.api.model.AcquisitionSource;
import com.mysms.core.web.Etags;
import com.mysms.finance.domain.AcquisitionSourceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for finance.acquisition_source. The way in ignores the id, tenant, audit set and read-only
 * fields, so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class})
public interface AcquisitionSourceMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AcquisitionSource toDto(AcquisitionSourceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)

    AcquisitionSourceEntity toEntity(AcquisitionSource dto);
}
