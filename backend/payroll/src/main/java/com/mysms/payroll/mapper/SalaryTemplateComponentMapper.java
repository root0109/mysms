package com.mysms.payroll.mapper;

import com.mysms.payroll.api.model.SalaryTemplateComponent;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.payroll.domain.SalaryTemplateComponentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for payroll.salary_template_component. The way in ignores the id, the tenant, the audit set and
 * every read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = PayrollMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface SalaryTemplateComponentMapper {

    @Mapping(target = "value", expression = "java(WireValues.decimal(entity.getValue()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    SalaryTemplateComponent toDto(SalaryTemplateComponentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "salaryTemplateId", ignore = true)
    @Mapping(target = "value", expression = "java(WireValues.decimal(dto.getValue()))")
    SalaryTemplateComponentEntity toEntity(SalaryTemplateComponent dto);
}
