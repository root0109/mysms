package com.mysms.org.mapper;

import com.mysms.org.api.model.AcademicYear;
import com.mysms.core.web.Etags;
import com.mysms.org.domain.AcademicYearEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for org.academic_year. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = OrgMapperConfig.class, imports = {Etags.class})
public interface AcademicYearMapper {

    @Mapping(target = "termStructure", expression =
            "java(entity.getTermStructure() == null ? null : "
                    + "AcademicYear.TermStructureEnum.fromValue(entity.getTermStructure()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : AcademicYear.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AcademicYear toDto(AcademicYearEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "termStructure", expression =
            "java(dto.getTermStructure() == null ? null : dto.getTermStructure().getValue())")
    @Mapping(target = "status", ignore = true)
    AcademicYearEntity toEntity(AcademicYear dto);
}
