package com.mysms.academics.mapper;

import com.mysms.academics.domain.SubjectEntity;
import com.mysms.academics.api.model.Subject;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for academics.subject. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AcademicsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface SubjectMapper {

    @Mapping(target = "deliveryType", expression =
            "java(entity.getDeliveryType() == null ? null : "
                    + "Subject.DeliveryTypeEnum.fromValue(entity.getDeliveryType()))")
    @Mapping(target = "defaultCategory", expression =
            "java(entity.getDefaultCategory() == null ? null : "
                    + "Subject.DefaultCategoryEnum.fromValue(entity.getDefaultCategory()))")
    @Mapping(target = "gradeWeightage", expression = "java(WireValues.decimal(entity.getGradeWeightage()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Subject toDto(SubjectEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "deliveryType", expression =
            "java(dto.getDeliveryType() == null ? null : dto.getDeliveryType().getValue())")
    @Mapping(target = "defaultCategory", expression =
            "java(dto.getDefaultCategory() == null ? null : dto.getDefaultCategory().getValue())")
    @Mapping(target = "gradeWeightage", expression = "java(WireValues.decimal(dto.getGradeWeightage()))")
    SubjectEntity toEntity(Subject dto);
}
