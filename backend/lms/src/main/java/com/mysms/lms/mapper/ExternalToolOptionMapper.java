package com.mysms.lms.mapper;

import com.mysms.lms.api.model.ExternalToolOption;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ExternalToolOptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.external_tool_option. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
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
