package com.mysms.platform.org.mapper;

import com.mysms.platform.org.api.model.AttributeDefinition;
import com.mysms.core.web.Etags;
import com.mysms.platform.org.domain.AttributeDefinitionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for org.attribute_definition. The way in ignores the id, tenant, audit set and read-only
 * fields, so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = OrgMapperConfig.class, imports = {Etags.class})
public interface AttributeDefinitionMapper {

    @Mapping(target = "dataType", expression =
            "java(entity.getDataType() == null ? null : "
                    + "AttributeDefinition.DataTypeEnum.fromValue(entity.getDataType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AttributeDefinition toDto(AttributeDefinitionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "dataType", expression = "java(dto.getDataType() == null ? null : dto.getDataType().getValue())")
    AttributeDefinitionEntity toEntity(AttributeDefinition dto);
}
