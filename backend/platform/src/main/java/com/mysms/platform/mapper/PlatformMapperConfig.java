package com.mysms.platform.mapper;

import org.mapstruct.Builder;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct configuration of the module: Spring components, and every unmapped target property is a compile
 * error, so a new column or contract field forces a conscious mapping decision.
 */
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @Builder(disableBuilder = true))
public interface PlatformMapperConfig {
}
