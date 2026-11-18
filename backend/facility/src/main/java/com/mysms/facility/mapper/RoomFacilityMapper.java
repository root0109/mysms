package com.mysms.facility.mapper;

import com.mysms.facility.api.model.RoomFacility;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.facility.domain.RoomFacilityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps facility.room_facility to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = FacilityMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface RoomFacilityMapper {

    @Mapping(target = "quantity", expression = "java(WireValues.decimal(entity.getQuantity()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    RoomFacility toDto(RoomFacilityEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "quantity", expression = "java(WireValues.decimal(dto.getQuantity()))")
    RoomFacilityEntity toEntity(RoomFacility dto);
}
