package com.mysms.lms.mapper;

import com.mysms.lms.api.model.CohortEnrollment;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.CohortEnrollmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.cohort_enrollment to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface CohortEnrollmentMapper {

    @Mapping(target = "finalGpa", expression = "java(WireValues.decimal(entity.getFinalGpa()))")
    @Mapping(target = "removalReason", expression =
            "java(entity.getRemovalReason() == null ? null : "
                    + "CohortEnrollment.RemovalReasonEnum.fromValue(entity.getRemovalReason()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CohortEnrollment toDto(CohortEnrollmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "finalGpa", expression = "java(WireValues.decimal(dto.getFinalGpa()))")
    @Mapping(target = "removalReason", expression =
            "java(dto.getRemovalReason() == null ? null : dto.getRemovalReason().getValue())")
    CohortEnrollmentEntity toEntity(CohortEnrollment dto);
}
