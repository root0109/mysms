package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.ExamResultEntity;
import com.mysms.assessment.api.model.ExamResult;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for assessment.exam_result. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface ExamResultMapper {

    @Mapping(target = "maxMarks", expression = "java(WireValues.decimal(entity.getMaxMarks()))")
    @Mapping(target = "gradePoint", expression = "java(WireValues.decimal(entity.getGradePoint()))")
    @Mapping(target = "credits", expression = "java(WireValues.decimal(entity.getCredits()))")
    @Mapping(target = "resultStatus", expression =
            "java(entity.getResultStatus() == null ? null : "
                    + "ExamResult.ResultStatusEnum.fromValue(entity.getResultStatus()))")
    @Mapping(target = "attendanceStatus", expression =
            "java(entity.getAttendanceStatus() == null ? null : "
                    + "ExamResult.AttendanceStatusEnum.fromValue(entity.getAttendanceStatus()))")
    @Mapping(target = "marks", expression = "java(WireValues.decimal(entity.getMarks()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ExamResult toDto(ExamResultEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "maxMarks", ignore = true)
    @Mapping(target = "gradeLabel", ignore = true)
    @Mapping(target = "gradePoint", ignore = true)
    @Mapping(target = "credits", ignore = true)
    @Mapping(target = "resultStatus", ignore = true)
    @Mapping(target = "marksheetLineId", ignore = true)
    @Mapping(target = "evaluatedByUserId", ignore = true)
    @Mapping(target = "evaluatedAt", ignore = true)
    @Mapping(target = "attendanceStatus", expression =
            "java(dto.getAttendanceStatus() == null ? null : dto.getAttendanceStatus().getValue())")
    @Mapping(target = "marks", ignore = true)
    ExamResultEntity toEntity(ExamResult dto);
}
