package com.mysms.frontoffice.mapper;

import com.mysms.frontoffice.api.model.DirectoryContact;
import com.mysms.core.web.Etags;
import com.mysms.frontoffice.domain.DirectoryContactEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps frontoffice.directory_contact to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = FrontofficeMapperConfig.class, imports = {Etags.class})
public interface DirectoryContactMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    DirectoryContact toDto(DirectoryContactEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)

    DirectoryContactEntity toEntity(DirectoryContact dto);
}
