package com.mysms.academics.mapper;

import com.mysms.academics.domain.ProgramEntity;
import com.mysms.academics.api.model.Program;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for academics.program. The way in ignores the id, tenant, audit set and read-only fields, so a
 * request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = AcademicsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface ProgramMapper {

    @Mapping(target = "minUnitLoad", expression = "java(WireValues.decimal(entity.getMinUnitLoad()))")
    @Mapping(target = "maxUnitLoad", expression = "java(WireValues.decimal(entity.getMaxUnitLoad()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Program toDto(ProgramEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "minUnitLoad", expression = "java(WireValues.decimal(dto.getMinUnitLoad()))")
    @Mapping(target = "maxUnitLoad", expression = "java(WireValues.decimal(dto.getMaxUnitLoad()))")
    ProgramEntity toEntity(Program dto);
}
