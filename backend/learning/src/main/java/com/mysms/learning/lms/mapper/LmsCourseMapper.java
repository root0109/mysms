package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.LmsCourse;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.LmsCourseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.course to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a request
 * cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface LmsCourseMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : LmsCourse.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "price", expression = "java(WireValues.decimal(entity.getPrice()))")
    @Mapping(target = "priceUsd", expression = "java(WireValues.decimal(entity.getPriceUsd()))")
    @Mapping(target = "cardGradient", expression =
            "java(entity.getCardGradient() == null ? null : "
                    + "LmsCourse.CardGradientEnum.fromValue(entity.getCardGradient()))")
    @Mapping(target = "averageRating", expression = "java(WireValues.decimal(entity.getAverageRating()))")
    @Mapping(target = "credits", expression = "java(WireValues.decimal(entity.getCredits()))")
    @Mapping(target = "lifecycleStatus", expression =
            "java(entity.getLifecycleStatus() == null ? null : "
                    + "LmsCourse.LifecycleStatusEnum.fromValue(entity.getLifecycleStatus()))")
    @Mapping(target = "deliveryModality", expression =
            "java(entity.getDeliveryModality() == null ? null : "
                    + "LmsCourse.DeliveryModalityEnum.fromValue(entity.getDeliveryModality()))")
    @Mapping(target = "gradeDisplay", expression =
            "java(entity.getGradeDisplay() == null ? null : "
                    + "LmsCourse.GradeDisplayEnum.fromValue(entity.getGradeDisplay()))")
    @Mapping(target = "catalogStatus", expression =
            "java(entity.getCatalogStatus() == null ? null : "
                    + "LmsCourse.CatalogStatusEnum.fromValue(entity.getCatalogStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LmsCourse toDto(LmsCourseEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "price", expression = "java(WireValues.decimal(dto.getPrice()))")
    @Mapping(target = "priceUsd", expression = "java(WireValues.decimal(dto.getPriceUsd()))")
    @Mapping(target = "cardGradient", expression =
            "java(dto.getCardGradient() == null ? null : dto.getCardGradient().getValue())")
    @Mapping(target = "averageRating", expression = "java(WireValues.decimal(dto.getAverageRating()))")
    @Mapping(target = "credits", expression = "java(WireValues.decimal(dto.getCredits()))")
    @Mapping(target = "lifecycleStatus", expression =
            "java(dto.getLifecycleStatus() == null ? null : dto.getLifecycleStatus().getValue())")
    @Mapping(target = "deliveryModality", expression =
            "java(dto.getDeliveryModality() == null ? null : dto.getDeliveryModality().getValue())")
    @Mapping(target = "gradeDisplay", expression =
            "java(dto.getGradeDisplay() == null ? null : dto.getGradeDisplay().getValue())")
    @Mapping(target = "catalogStatus", expression =
            "java(dto.getCatalogStatus() == null ? null : dto.getCatalogStatus().getValue())")
    LmsCourseEntity toEntity(LmsCourse dto);
}
