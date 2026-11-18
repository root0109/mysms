package com.mysms.people.mapper;

import com.mysms.people.api.model.PersonAddress;
import com.mysms.core.web.Etags;
import com.mysms.people.domain.PersonAddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for people.person_address. The way in ignores the id, tenant, audit set and read-only fields,
 * so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PeopleMapperConfig.class, imports = {Etags.class})
public interface PersonAddressMapper {

    @Mapping(target = "addressType", expression =
            "java(entity.getAddressType() == null ? null : "
                    + "PersonAddress.AddressTypeEnum.fromValue(entity.getAddressType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    PersonAddress toDto(PersonAddressEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "addressType", expression =
            "java(dto.getAddressType() == null ? null : dto.getAddressType().getValue())")
    PersonAddressEntity toEntity(PersonAddress dto);
}
