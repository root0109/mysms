package com.mysms.lms.mapper;

import com.mysms.lms.api.model.GradeChangeLog;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.GradeChangeLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.grade_change_log. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface GradeChangeLogMapper {

    @Mapping(target = "points", expression = "java(WireValues.decimal(entity.getPoints()))")
    @Mapping(target = "autoPoints", expression = "java(WireValues.decimal(entity.getAutoPoints()))")
    @Mapping(target = "manualPoints", expression = "java(WireValues.decimal(entity.getManualPoints()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    GradeChangeLog toDto(GradeChangeLogEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "points", expression = "java(WireValues.decimal(dto.getPoints()))")
    @Mapping(target = "autoPoints", expression = "java(WireValues.decimal(dto.getAutoPoints()))")
    @Mapping(target = "manualPoints", expression = "java(WireValues.decimal(dto.getManualPoints()))")
    GradeChangeLogEntity toEntity(GradeChangeLog dto);
}
