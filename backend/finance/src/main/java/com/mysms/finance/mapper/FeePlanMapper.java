package com.mysms.finance.mapper;

import com.mysms.finance.api.model.FeePlan;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.finance.domain.FeePlanEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for finance.fee_plan. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface FeePlanMapper {

    @Mapping(target = "scheduleBasis", expression =
            "java(entity.getScheduleBasis() == null ? null : "
                    + "FeePlan.ScheduleBasisEnum.fromValue(entity.getScheduleBasis()))")
    @Mapping(target = "offsetDirection", expression =
            "java(entity.getOffsetDirection() == null ? null : "
                    + "FeePlan.OffsetDirectionEnum.fromValue(entity.getOffsetDirection()))")
    @Mapping(target = "discountPercent", expression = "java(WireValues.decimal(entity.getDiscountPercent()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    FeePlan toDto(FeePlanEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "scheduleBasis", expression =
            "java(dto.getScheduleBasis() == null ? null : dto.getScheduleBasis().getValue())")
    @Mapping(target = "offsetDirection", expression =
            "java(dto.getOffsetDirection() == null ? null : dto.getOffsetDirection().getValue())")
    @Mapping(target = "discountPercent", expression = "java(WireValues.decimal(dto.getDiscountPercent()))")
    FeePlanEntity toEntity(FeePlan dto);
}
