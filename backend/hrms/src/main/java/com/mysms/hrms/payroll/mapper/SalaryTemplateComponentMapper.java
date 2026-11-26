package com.mysms.hrms.payroll.mapper;

import com.mysms.hrms.payroll.api.model.SalaryTemplateComponent;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.hrms.payroll.domain.SalaryTemplateComponentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps payroll.salary_template_component to and from its contract DTO. The way in ignores id, tenant, audit and
 * read-only fields, so a request cannot set them; the ETag comes from row_version.
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
