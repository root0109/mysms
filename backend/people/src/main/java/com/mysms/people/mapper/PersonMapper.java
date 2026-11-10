package com.mysms.people.mapper;

import com.mysms.people.api.model.Person;
import com.mysms.core.web.Etags;
import com.mysms.people.domain.PersonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for people.person. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = PeopleMapperConfig.class, imports = {Etags.class})
public interface PersonMapper {

    @Mapping(target = "nationalIdMasked", ignore = true)
    @Mapping(target = "gender", expression =
            "java(entity.getGender() == null ? null : Person.GenderEnum.fromValue(entity.getGender()))")
    @Mapping(target = "bloodGroup", expression =
            "java(entity.getBloodGroup() == null ? null : Person.BloodGroupEnum.fromValue(entity.getBloodGroup()))")
    @Mapping(target = "maritalStatus", expression =
            "java(entity.getMaritalStatus() == null ? null : "
                    + "Person.MaritalStatusEnum.fromValue(entity.getMaritalStatus()))")
    @Mapping(target = "nationalId", ignore = true)

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Person toDto(PersonEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "gender", expression = "java(dto.getGender() == null ? null : dto.getGender().getValue())")
    @Mapping(target = "bloodGroup", expression =
            "java(dto.getBloodGroup() == null ? null : dto.getBloodGroup().getValue())")
    @Mapping(target = "maritalStatus", expression =
            "java(dto.getMaritalStatus() == null ? null : dto.getMaritalStatus().getValue())")
    @Mapping(target = "nationalIdEnc", ignore = true)
    PersonEntity toEntity(Person dto);
}
