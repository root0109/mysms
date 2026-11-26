package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.WebResource;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.WebResourceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.web_resource to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface WebResourceMapper {

    @Mapping(target = "resourceType", expression =
            "java(entity.getResourceType() == null ? null : "
                    + "WebResource.ResourceTypeEnum.fromValue(entity.getResourceType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    WebResource toDto(WebResourceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "resourceType", expression =
            "java(dto.getResourceType() == null ? null : dto.getResourceType().getValue())")
    WebResourceEntity toEntity(WebResource dto);
}
