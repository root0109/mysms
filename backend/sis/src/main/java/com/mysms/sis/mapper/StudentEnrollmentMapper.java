package com.mysms.sis.mapper;

import com.mysms.sis.api.model.StudentEnrollment;
import com.mysms.core.web.Etags;
import com.mysms.sis.domain.StudentEnrollmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for sis.student_enrollment. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = SisMapperConfig.class, imports = {Etags.class})
public interface StudentEnrollmentMapper {

    @Mapping(target = "changeReason", expression =
            "java(entity.getChangeReason() == null ? null : "
                    + "StudentEnrollment.ChangeReasonEnum.fromValue(entity.getChangeReason()))")
    @Mapping(target = "enrollmentStatus", expression =
            "java(entity.getEnrollmentStatus() == null ? null : "
                    + "StudentEnrollment.EnrollmentStatusEnum.fromValue(entity.getEnrollmentStatus()))")
    @Mapping(target = "resultStatus", expression =
            "java(entity.getResultStatus() == null ? null : "
                    + "StudentEnrollment.ResultStatusEnum.fromValue(entity.getResultStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StudentEnrollment toDto(StudentEnrollmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "effectiveTo", ignore = true)
    @Mapping(target = "previousEnrollmentId", ignore = true)
    @Mapping(target = "changeReason", expression =
            "java(dto.getChangeReason() == null ? null : dto.getChangeReason().getValue())")
    @Mapping(target = "enrollmentStatus", expression =
            "java(dto.getEnrollmentStatus() == null ? null : dto.getEnrollmentStatus().getValue())")
    @Mapping(target = "resultStatus", expression =
            "java(dto.getResultStatus() == null ? null : dto.getResultStatus().getValue())")
    StudentEnrollmentEntity toEntity(StudentEnrollment dto);
}
