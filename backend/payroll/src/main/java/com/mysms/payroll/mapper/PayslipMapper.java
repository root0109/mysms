package com.mysms.payroll.mapper;

import com.mysms.payroll.api.model.Payslip;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.payroll.domain.PayslipEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps payroll.payslip to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = PayrollMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface PayslipMapper {

    @Mapping(target = "grossAmount", expression = "java(WireValues.decimal(entity.getGrossAmount()))")
    @Mapping(target = "deductionTotal", expression = "java(WireValues.decimal(entity.getDeductionTotal()))")
    @Mapping(target = "netAmount", expression = "java(WireValues.decimal(entity.getNetAmount()))")
    @Mapping(target = "payPercentage", expression = "java(WireValues.decimal(entity.getPayPercentage()))")
    @Mapping(target = "leaveDays", expression = "java(WireValues.decimal(entity.getLeaveDays()))")
    @Mapping(target = "leaveDeduction", expression = "java(WireValues.decimal(entity.getLeaveDeduction()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Payslip.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Payslip toDto(PayslipEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "grossAmount", ignore = true)
    @Mapping(target = "deductionTotal", ignore = true)
    @Mapping(target = "netAmount", ignore = true)
    @Mapping(target = "documentFileId", ignore = true)
    @Mapping(target = "payPercentage", expression = "java(WireValues.decimal(dto.getPayPercentage()))")
    @Mapping(target = "leaveDays", expression = "java(WireValues.decimal(dto.getLeaveDays()))")
    @Mapping(target = "leaveDeduction", expression = "java(WireValues.decimal(dto.getLeaveDeduction()))")
    @Mapping(target = "status", ignore = true)
    PayslipEntity toEntity(Payslip dto);
}
