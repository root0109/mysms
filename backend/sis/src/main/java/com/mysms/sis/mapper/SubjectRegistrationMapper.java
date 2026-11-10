package com.mysms.sis.mapper;

import com.mysms.sis.api.model.SubjectRegistration;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.sis.domain.SubjectRegistrationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for sis.subject_registration. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = SisMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface SubjectRegistrationMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : SubjectRegistration.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "minUnitLoad", expression = "java(WireValues.decimal(entity.getMinUnitLoad()))")
    @Mapping(target = "maxUnitLoad", expression = "java(WireValues.decimal(entity.getMaxUnitLoad()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    SubjectRegistration toDto(SubjectRegistrationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "minUnitLoad", expression = "java(WireValues.decimal(dto.getMinUnitLoad()))")
    @Mapping(target = "maxUnitLoad", expression = "java(WireValues.decimal(dto.getMaxUnitLoad()))")
    SubjectRegistrationEntity toEntity(SubjectRegistration dto);
}
