package com.mysms.students.careers.mapper;

import com.mysms.students.careers.domain.JobOpeningEntity;
import com.mysms.students.careers.api.model.JobOpening;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps careers.job_opening to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CareersMapperConfig.class, imports = {Etags.class})
public interface JobOpeningMapper {

    @Mapping(target = "employmentType", expression =
            "java(entity.getEmploymentType() == null ? null : "
                    + "JobOpening.EmploymentTypeEnum.fromValue(entity.getEmploymentType()))")
    @Mapping(target = "workMode", expression =
            "java(entity.getWorkMode() == null ? null : JobOpening.WorkModeEnum.fromValue(entity.getWorkMode()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : JobOpening.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    JobOpening toDto(JobOpeningEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "employmentType", expression =
            "java(dto.getEmploymentType() == null ? null : dto.getEmploymentType().getValue())")
    @Mapping(target = "workMode", expression = "java(dto.getWorkMode() == null ? null : dto.getWorkMode().getValue())")
    @Mapping(target = "status", ignore = true)
    JobOpeningEntity toEntity(JobOpening dto);
}
