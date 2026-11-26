package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.Evaluator;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.EvaluatorEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.evaluator to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface EvaluatorMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Evaluator toDto(EvaluatorEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "calendarProvider", ignore = true)
    @Mapping(target = "calendarState", ignore = true)
    @Mapping(target = "calendarRequestedAt", ignore = true)
    @Mapping(target = "calendarConnectedAt", ignore = true)
    EvaluatorEntity toEntity(Evaluator dto);
}
