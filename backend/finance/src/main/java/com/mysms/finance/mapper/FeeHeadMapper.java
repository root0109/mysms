package com.mysms.finance.mapper;

import com.mysms.finance.api.model.FeeHead;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.finance.domain.FeeHeadEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for finance.fee_head. The way in ignores the id, tenant, audit set and read-only fields, so a
 * request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface FeeHeadMapper {

    @Mapping(target = "defaultAmount", expression = "java(WireValues.decimal(entity.getDefaultAmount()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    FeeHead toDto(FeeHeadEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "defaultAmount", expression = "java(WireValues.decimal(dto.getDefaultAmount()))")
    FeeHeadEntity toEntity(FeeHead dto);
}
