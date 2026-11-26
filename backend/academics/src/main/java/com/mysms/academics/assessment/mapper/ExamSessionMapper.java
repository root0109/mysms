package com.mysms.academics.assessment.mapper;

import com.mysms.academics.assessment.domain.ExamSessionEntity;
import com.mysms.academics.assessment.api.model.ExamSession;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.exam_session to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface ExamSessionMapper {

    @Mapping(target = "evaluationType", expression =
            "java(entity.getEvaluationType() == null ? null : "
                    + "ExamSession.EvaluationTypeEnum.fromValue(entity.getEvaluationType()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : ExamSession.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ExamSession toDto(ExamSessionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "evaluationType", expression =
            "java(dto.getEvaluationType() == null ? null : dto.getEvaluationType().getValue())")
    @Mapping(target = "status", ignore = true)
    ExamSessionEntity toEntity(ExamSession dto);
}
