package com.mysms.people.mapper;

import com.mysms.people.api.model.Person;
import com.mysms.core.web.Etags;
import com.mysms.people.domain.SelfPersonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * people.person → contract DTO. The national id is write-only (never returned); its masked form needs the
 * per-tenant key service, which this release does not have, so it is left out too.
 */
@Mapper(config = PeopleMapperConfig.class, imports = Etags.class)
public interface SelfPersonMapper {

    @Mapping(target = "nationalId", ignore = true)
    @Mapping(target = "nationalIdMasked", ignore = true)
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Person toDto(SelfPersonEntity entity);

    default Person.GenderEnum gender(String value) {
        return value == null ? null : Person.GenderEnum.fromValue(value);
    }

    default Person.BloodGroupEnum bloodGroup(String value) {
        return value == null ? null : Person.BloodGroupEnum.fromValue(value);
    }

    default Person.MaritalStatusEnum maritalStatus(String value) {
        return value == null ? null : Person.MaritalStatusEnum.fromValue(value);
    }
}
