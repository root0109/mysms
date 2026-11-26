package com.mysms.hrms.payroll.mapper;

import com.mysms.hrms.payroll.api.model.PayrollPayment;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.hrms.payroll.domain.PayrollPaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps payroll.payroll_payment to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = PayrollMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface PayrollPaymentMapper {

    @Mapping(target = "amount", expression = "java(WireValues.decimal(entity.getAmount()))")
    @Mapping(target = "paymentMethod", expression =
            "java(entity.getPaymentMethod() == null ? null : "
                    + "PayrollPayment.PaymentMethodEnum.fromValue(entity.getPaymentMethod()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    PayrollPayment toDto(PayrollPaymentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "amount", expression = "java(WireValues.decimal(dto.getAmount()))")
    @Mapping(target = "paymentMethod", expression =
            "java(dto.getPaymentMethod() == null ? null : dto.getPaymentMethod().getValue())")
    PayrollPaymentEntity toEntity(PayrollPayment dto);
}
