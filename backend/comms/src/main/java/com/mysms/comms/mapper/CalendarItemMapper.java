package com.mysms.comms.mapper;

import com.mysms.comms.api.model.CalendarItem;
import com.mysms.comms.repository.CalendarRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps an item of a person's calendar to the contract's CalendarItem; comms has no item due dates of its own. */
@Mapper(config = CommsMapperConfig.class)
public interface CalendarItemMapper {

    @Mapping(target = "itemType", expression = "java(CalendarItem.ItemTypeEnum.fromValue(row.itemType()))")
    @Mapping(target = "contentItemId", ignore = true)
    CalendarItem toDto(CalendarRow row);
}
