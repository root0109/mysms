package com.mysms.campus.facility.mapper;

import com.mysms.campus.facility.api.model.Facility;
import com.mysms.core.web.Etags;
import com.mysms.campus.facility.domain.FacilityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps facility.facility to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = FacilityMapperConfig.class, imports = {Etags.class})
public interface FacilityMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Facility toDto(FacilityEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)

    FacilityEntity toEntity(Facility dto);
}
