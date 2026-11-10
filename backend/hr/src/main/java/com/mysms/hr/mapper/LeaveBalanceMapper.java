package com.mysms.hr.mapper;

import com.mysms.hr.api.model.LeaveBalance;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.hr.domain.LeaveBalanceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for hr.leave_balance. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = HrMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface LeaveBalanceMapper {

    @Mapping(target = "entitledDays", expression = "java(WireValues.decimal(entity.getEntitledDays()))")
    @Mapping(target = "usedDays", expression = "java(WireValues.decimal(entity.getUsedDays()))")
    @Mapping(target = "carriedForwardDays", expression = "java(WireValues.decimal(entity.getCarriedForwardDays()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LeaveBalance toDto(LeaveBalanceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "entitledDays", expression = "java(WireValues.decimal(dto.getEntitledDays()))")
    @Mapping(target = "usedDays", ignore = true)
    @Mapping(target = "carriedForwardDays", expression = "java(WireValues.decimal(dto.getCarriedForwardDays()))")
    LeaveBalanceEntity toEntity(LeaveBalance dto);
}
