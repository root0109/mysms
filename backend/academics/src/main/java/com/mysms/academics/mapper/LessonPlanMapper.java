package com.mysms.academics.mapper;

import com.mysms.academics.domain.LessonPlanEntity;
import com.mysms.academics.api.model.LessonPlan;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for academics.lesson_plan. The way in ignores the id, tenant, audit set and read-only fields,
 * so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = AcademicsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface LessonPlanMapper {

    @Mapping(target = "plannedDurationMinutes", expression =
            "java(WireValues.duration(entity.getPlannedDurationMinutes(), 60))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : LessonPlan.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LessonPlan toDto(LessonPlanEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "plannedDurationMinutes", expression =
            "java(WireValues.duration(dto.getPlannedDurationMinutes(), 60))")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    LessonPlanEntity toEntity(LessonPlan dto);
}
