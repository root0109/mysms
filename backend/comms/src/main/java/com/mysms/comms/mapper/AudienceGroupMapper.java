package com.mysms.comms.mapper;

import com.mysms.comms.domain.AudienceGroupEntity;
import com.mysms.comms.api.model.AudienceGroup;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.audience_group to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface AudienceGroupMapper {

    @Mapping(target = "groupScope", expression =
            "java(entity.getGroupScope() == null ? null : "
                    + "AudienceGroup.GroupScopeEnum.fromValue(entity.getGroupScope()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AudienceGroup toDto(AudienceGroupEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "groupScope", expression =
            "java(dto.getGroupScope() == null ? null : dto.getGroupScope().getValue())")
    AudienceGroupEntity toEntity(AudienceGroup dto);
}
