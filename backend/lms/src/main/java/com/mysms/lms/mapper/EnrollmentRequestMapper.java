package com.mysms.lms.mapper;

import com.mysms.lms.api.model.EnrollmentRequest;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.EnrollmentRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.enrollment_request to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface EnrollmentRequestMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : EnrollmentRequest.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    EnrollmentRequest toDto(EnrollmentRequestEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "decidedBy", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    EnrollmentRequestEntity toEntity(EnrollmentRequest dto);
}
