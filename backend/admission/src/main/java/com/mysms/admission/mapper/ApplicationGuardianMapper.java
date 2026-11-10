package com.mysms.admission.mapper;

import com.mysms.admission.domain.ApplicationGuardianEntity;
import com.mysms.admission.api.model.ApplicationGuardian;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for admission.application_guardian. The way in ignores the id, the tenant, the audit set and
 * every read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AdmissionMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface ApplicationGuardianMapper {

    @Mapping(target = "nationalIdMasked", ignore = true)
    @Mapping(target = "relationship", expression =
            "java(entity.getRelationship() == null ? null : "
                    + "ApplicationGuardian.RelationshipEnum.fromValue(entity.getRelationship()))")
    @Mapping(target = "annualIncome", expression = "java(WireValues.decimal(entity.getAnnualIncome()))")
    @Mapping(target = "nationalId", ignore = true)

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ApplicationGuardian toDto(ApplicationGuardianEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "applicationId", ignore = true)
    @Mapping(target = "relationship", expression =
            "java(dto.getRelationship() == null ? null : dto.getRelationship().getValue())")
    @Mapping(target = "annualIncome", expression = "java(WireValues.decimal(dto.getAnnualIncome()))")
    @Mapping(target = "nationalIdEnc", ignore = true)
    ApplicationGuardianEntity toEntity(ApplicationGuardian dto);
}
