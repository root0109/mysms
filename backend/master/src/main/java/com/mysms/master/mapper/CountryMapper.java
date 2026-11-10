package com.mysms.master.mapper;

import com.mysms.master.api.model.Country;
import com.mysms.core.web.Etags;
import com.mysms.master.domain.CountryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for master.country. Output-only fields (id, audit stamps, etag) are never read from a
 * request: creating ignores them, and the ETag is always derived from row_version.
 */
@Mapper(config = MasterMapperConfig.class, imports = Etags.class)
public interface CountryMapper {

    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Country toDto(CountryEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "active", source = "isActive", defaultValue = "true")
    CountryEntity toEntity(Country dto);
}
