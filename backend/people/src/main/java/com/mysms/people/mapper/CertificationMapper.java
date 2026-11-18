package com.mysms.people.mapper;

import com.mysms.people.api.model.Certification;
import com.mysms.core.web.Etags;
import com.mysms.people.domain.CertificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for people.certification. The way in ignores the id, tenant, audit set and read-only fields, so
 * a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PeopleMapperConfig.class, imports = {Etags.class})
public interface CertificationMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Certification toDto(CertificationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "personId", ignore = true)
    CertificationEntity toEntity(Certification dto);
}
