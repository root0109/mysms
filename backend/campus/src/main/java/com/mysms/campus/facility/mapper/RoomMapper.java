package com.mysms.campus.facility.mapper;

import com.mysms.campus.facility.api.model.Room;
import com.mysms.core.web.Etags;
import com.mysms.campus.facility.domain.RoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps facility.room to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = FacilityMapperConfig.class, imports = {Etags.class})
public interface RoomMapper {

    @Mapping(target = "roomType", expression =
            "java(entity.getRoomType() == null ? null : Room.RoomTypeEnum.fromValue(entity.getRoomType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Room toDto(RoomEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "roomType", expression = "java(dto.getRoomType() == null ? null : dto.getRoomType().getValue())")
    RoomEntity toEntity(Room dto);
}
