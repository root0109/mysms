package com.mysms.library.mapper;

import org.mapstruct.Builder;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Module MapStruct configuration: an unmapped target property is a compile error, so a new column or contract field
 * forces a mapping decision.
 */
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @Builder(disableBuilder = true))
public interface LibraryMapperConfig {
}
