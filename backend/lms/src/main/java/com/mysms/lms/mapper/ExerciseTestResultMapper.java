package com.mysms.lms.mapper;

import com.mysms.lms.api.model.ExerciseTestResult;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ExerciseTestResultEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.exercise_test_result to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface ExerciseTestResultMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : ExerciseTestResult.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ExerciseTestResult toDto(ExerciseTestResultEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "exerciseSubmissionId", ignore = true)
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    ExerciseTestResultEntity toEntity(ExerciseTestResult dto);
}
