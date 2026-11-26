package com.mysms.identity.people.mapper;

import com.mysms.identity.people.api.model.EducationHistory;
import com.mysms.core.web.Etags;
import com.mysms.identity.people.domain.EducationHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for people.education_history. The way in ignores the id, tenant, audit set and read-only
 * fields, so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PeopleMapperConfig.class, imports = {Etags.class})
public interface EducationHistoryMapper {

    @Mapping(target = "gradeType", expression =
            "java(entity.getGradeType() == null ? null : "
                    + "EducationHistory.GradeTypeEnum.fromValue(entity.getGradeType()))")
    @Mapping(target = "studyMode", expression =
            "java(entity.getStudyMode() == null ? null : "
                    + "EducationHistory.StudyModeEnum.fromValue(entity.getStudyMode()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    EducationHistory toDto(EducationHistoryEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "gradeType", expression =
            "java(dto.getGradeType() == null ? null : dto.getGradeType().getValue())")
    @Mapping(target = "studyMode", expression =
            "java(dto.getStudyMode() == null ? null : dto.getStudyMode().getValue())")
    EducationHistoryEntity toEntity(EducationHistory dto);
}
