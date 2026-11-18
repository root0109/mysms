package com.mysms.hr.mapper;

import com.mysms.hr.api.model.LeaveBalance;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.hr.domain.LeaveBalanceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps hr.leave_balance to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
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
