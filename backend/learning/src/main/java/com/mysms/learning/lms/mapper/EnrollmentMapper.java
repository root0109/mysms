package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.Enrollment;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.EnrollmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.enrollment to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface EnrollmentMapper {

    @Mapping(target = "memberType", expression =
            "java(entity.getMemberType() == null ? null : Enrollment.MemberTypeEnum.fromValue(entity.getMemberType()))")
    @Mapping(target = "memberRole", expression =
            "java(entity.getMemberRole() == null ? null : Enrollment.MemberRoleEnum.fromValue(entity.getMemberRole()))")
    @Mapping(target = "progressPercent", expression = "java(WireValues.decimal(entity.getProgressPercent()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Enrollment toDto(EnrollmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "memberType", expression =
            "java(dto.getMemberType() == null ? null : dto.getMemberType().getValue())")
    @Mapping(target = "memberRole", expression =
            "java(dto.getMemberRole() == null ? null : dto.getMemberRole().getValue())")
    @Mapping(target = "progressPercent", expression = "java(WireValues.decimal(dto.getProgressPercent()))")
    EnrollmentEntity toEntity(Enrollment dto);
}
