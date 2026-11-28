package com.mysms.academics.assessment.mapper;

import com.mysms.academics.assessment.domain.MarksheetEntity;
import com.mysms.academics.assessment.api.model.Marksheet;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.marksheet to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface MarksheetMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Marksheet.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Marksheet toDto(MarksheetEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "publishedByUserId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "generatedByUserId", ignore = true)
    @Mapping(target = "totalPassed", ignore = true)
    @Mapping(target = "totalFailed", ignore = true)
    MarksheetEntity toEntity(Marksheet dto);
}
