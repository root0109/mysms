package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.MarksheetLineEntity;
import com.mysms.assessment.api.model.MarksheetLine;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for assessment.marksheet_line. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface MarksheetLineMapper {

    @Mapping(target = "creditsAttempted", expression = "java(WireValues.decimal(entity.getCreditsAttempted()))")
    @Mapping(target = "creditsEarned", expression = "java(WireValues.decimal(entity.getCreditsEarned()))")
    @Mapping(target = "gradePoints", expression = "java(WireValues.decimal(entity.getGradePoints()))")
    @Mapping(target = "sgpa", expression = "java(WireValues.decimal(entity.getSgpa()))")
    @Mapping(target = "cgpa", expression = "java(WireValues.decimal(entity.getCgpa()))")
    @Mapping(target = "totalMarks", expression = "java(WireValues.decimal(entity.getTotalMarks()))")
    @Mapping(target = "percentage", expression = "java(WireValues.decimal(entity.getPercentage()))")
    @Mapping(target = "resultStatus", expression =
            "java(entity.getResultStatus() == null ? null : "
                    + "MarksheetLine.ResultStatusEnum.fromValue(entity.getResultStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    MarksheetLine toDto(MarksheetLineEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "creditsAttempted", ignore = true)
    @Mapping(target = "creditsEarned", ignore = true)
    @Mapping(target = "gradePoints", ignore = true)
    @Mapping(target = "sgpa", ignore = true)
    @Mapping(target = "cgpa", ignore = true)
    @Mapping(target = "totalMarks", ignore = true)
    @Mapping(target = "percentage", ignore = true)
    @Mapping(target = "gradeLabel", ignore = true)
    @Mapping(target = "resultStatus", ignore = true)
    MarksheetLineEntity toEntity(MarksheetLine dto);
}
