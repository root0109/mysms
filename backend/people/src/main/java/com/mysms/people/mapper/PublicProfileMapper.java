package com.mysms.people.mapper;

import com.mysms.people.api.model.PublicProfile;
import com.mysms.core.web.Etags;
import com.mysms.people.domain.PublicProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for people.public_profile. The way in ignores the id, tenant, audit set and read-only fields,
 * so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PeopleMapperConfig.class, imports = {Etags.class})
public interface PublicProfileMapper {

    @Mapping(target = "personaCategory", expression =
            "java(entity.getPersonaCategory() == null ? null : "
                    + "PublicProfile.PersonaCategoryEnum.fromValue(entity.getPersonaCategory()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    PublicProfile toDto(PublicProfileEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "personaCategory", expression =
            "java(dto.getPersonaCategory() == null ? null : dto.getPersonaCategory().getValue())")
    PublicProfileEntity toEntity(PublicProfile dto);
}
