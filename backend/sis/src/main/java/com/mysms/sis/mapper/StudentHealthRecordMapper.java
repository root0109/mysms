package com.mysms.sis.mapper;

import com.mysms.sis.api.model.StudentHealthRecord;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.sis.domain.StudentHealthRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for sis.student_health_record. The way in ignores the id, tenant, audit set and read-only
 * fields, so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = SisMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface StudentHealthRecordMapper {

    @Mapping(target = "heightCm", expression = "java(WireValues.decimal(entity.getHeightCm()))")
    @Mapping(target = "weightKg", expression = "java(WireValues.decimal(entity.getWeightKg()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StudentHealthRecord toDto(StudentHealthRecordEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "heightCm", expression = "java(WireValues.decimal(dto.getHeightCm()))")
    @Mapping(target = "weightKg", expression = "java(WireValues.decimal(dto.getWeightKg()))")
    StudentHealthRecordEntity toEntity(StudentHealthRecord dto);
}
