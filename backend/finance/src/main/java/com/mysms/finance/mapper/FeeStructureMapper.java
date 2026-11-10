package com.mysms.finance.mapper;

import com.mysms.finance.api.model.FeeStructure;
import com.mysms.core.web.Etags;
import com.mysms.finance.domain.FeeStructureEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for finance.fee_structure. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class})
public interface FeeStructureMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : FeeStructure.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    FeeStructure toDto(FeeStructureEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "status", ignore = true)
    FeeStructureEntity toEntity(FeeStructure dto);
}
