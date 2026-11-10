package com.mysms.lms.mapper;

import com.mysms.lms.api.model.Enrollment;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.EnrollmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.enrollment. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
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
