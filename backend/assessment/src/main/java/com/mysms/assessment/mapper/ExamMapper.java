package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.ExamEntity;
import com.mysms.assessment.api.model.Exam;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.exam to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface ExamMapper {

    @Mapping(target = "totalMarks", expression = "java(WireValues.decimal(entity.getTotalMarks()))")
    @Mapping(target = "passMarks", expression = "java(WireValues.decimal(entity.getPassMarks()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Exam.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Exam toDto(ExamEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "totalMarks", expression = "java(WireValues.decimal(dto.getTotalMarks()))")
    @Mapping(target = "passMarks", expression = "java(WireValues.decimal(dto.getPassMarks()))")
    @Mapping(target = "status", ignore = true)
    ExamEntity toEntity(Exam dto);
}
