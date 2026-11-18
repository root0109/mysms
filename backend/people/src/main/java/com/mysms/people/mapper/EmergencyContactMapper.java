package com.mysms.people.mapper;

import com.mysms.people.api.model.EmergencyContact;
import com.mysms.core.web.Etags;
import com.mysms.people.domain.EmergencyContactEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for people.emergency_contact. The way in ignores the id, tenant, audit set and read-only
 * fields, so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PeopleMapperConfig.class, imports = {Etags.class})
public interface EmergencyContactMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    EmergencyContact toDto(EmergencyContactEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "personId", ignore = true)
    EmergencyContactEntity toEntity(EmergencyContact dto);
}
