package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.CohortScheduleSlot;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.CohortScheduleSlotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.cohort_schedule_slot to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface CohortScheduleSlotMapper {

    @Mapping(target = "slotType", expression =
            "java(entity.getSlotType() == null ? null : "
                    + "CohortScheduleSlot.SlotTypeEnum.fromValue(entity.getSlotType()))")
    @Mapping(target = "weekday", expression =
            "java(entity.getWeekday() == null ? null : CohortScheduleSlot.WeekdayEnum.fromValue(entity.getWeekday()))")
    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(entity.getEndTime()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CohortScheduleSlot toDto(CohortScheduleSlotEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "cohortId", ignore = true)
    @Mapping(target = "slotType", expression = "java(dto.getSlotType() == null ? null : dto.getSlotType().getValue())")
    @Mapping(target = "weekday", expression = "java(dto.getWeekday() == null ? null : dto.getWeekday().getValue())")
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(dto.getEndTime()))")
    CohortScheduleSlotEntity toEntity(CohortScheduleSlot dto);
}
