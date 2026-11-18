package com.mysms.careers.mapper;

import com.mysms.careers.domain.JobApplicationEntity;
import com.mysms.careers.api.model.JobApplication;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps careers.job_application to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CareersMapperConfig.class, imports = {Etags.class})
public interface JobApplicationMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : JobApplication.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    JobApplication toDto(JobApplicationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "jobOpeningId", ignore = true)
    @Mapping(target = "status", ignore = true)
    JobApplicationEntity toEntity(JobApplication dto);
}
