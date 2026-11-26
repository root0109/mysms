package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.AccessLog;
import com.mysms.learning.lms.repository.AccessLogRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** An access record → the contract's AccessLog; the log is immutable, so a record has no version and no ETag. */
@Mapper(config = LmsMapperConfig.class)
public interface AccessLogMapper {

    @Mapping(target = "etag", ignore = true)
    AccessLog toDto(AccessLogRow row);
}
