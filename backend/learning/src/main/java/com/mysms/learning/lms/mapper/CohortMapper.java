package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.Cohort;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.CohortEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.cohort to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a request
 * cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface CohortMapper {

    @Mapping(target = "medium", expression =
            "java(entity.getMedium() == null ? null : Cohort.MediumEnum.fromValue(entity.getMedium()))")
    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(entity.getEndTime()))")
    @Mapping(target = "price", expression = "java(WireValues.decimal(entity.getPrice()))")
    @Mapping(target = "priceUsd", expression = "java(WireValues.decimal(entity.getPriceUsd()))")
    @Mapping(target = "conferencingProvider", expression =
            "java(entity.getConferencingProvider() == null ? null : "
                    + "Cohort.ConferencingProviderEnum.fromValue(entity.getConferencingProvider()))")
    @Mapping(target = "lifecycleStatus", expression =
            "java(entity.getLifecycleStatus() == null ? null : "
                    + "Cohort.LifecycleStatusEnum.fromValue(entity.getLifecycleStatus()))")
    @Mapping(target = "gradeDisplay", expression =
            "java(entity.getGradeDisplay() == null ? null : "
                    + "Cohort.GradeDisplayEnum.fromValue(entity.getGradeDisplay()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Cohort toDto(CohortEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "medium", expression = "java(dto.getMedium() == null ? null : dto.getMedium().getValue())")
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(dto.getEndTime()))")
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "price", expression = "java(WireValues.decimal(dto.getPrice()))")
    @Mapping(target = "priceUsd", expression = "java(WireValues.decimal(dto.getPriceUsd()))")
    @Mapping(target = "conferencingProvider", expression =
            "java(dto.getConferencingProvider() == null ? null : dto.getConferencingProvider().getValue())")
    @Mapping(target = "lifecycleStatus", expression =
            "java(dto.getLifecycleStatus() == null ? null : dto.getLifecycleStatus().getValue())")
    @Mapping(target = "gradeDisplay", expression =
            "java(dto.getGradeDisplay() == null ? null : dto.getGradeDisplay().getValue())")
    CohortEntity toEntity(Cohort dto);
}
