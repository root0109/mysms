package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.ExternalToolOption;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.ExternalToolOptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.external_tool_option to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface ExternalToolOptionMapper {

    @Mapping(target = "optionKind", expression =
            "java(entity.getOptionKind() == null ? null : "
                    + "ExternalToolOption.OptionKindEnum.fromValue(entity.getOptionKind()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ExternalToolOption toDto(ExternalToolOptionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "externalToolId", ignore = true)
    @Mapping(target = "optionKind", expression =
            "java(dto.getOptionKind() == null ? null : dto.getOptionKind().getValue())")
    ExternalToolOptionEntity toEntity(ExternalToolOption dto);
}
