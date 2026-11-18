package com.mysms.finance.mapper;

import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.domain.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity → contract DTO for finance.payment. Payments are written only by the payment workflows (checkout, the
 * gateway's notifications, counter payments), never built from a request, so there is no way in. The tax identity is a
 * secret: its plaintext is never mapped, and its masked form and the allocations are the service's to fill.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface PaymentMapper {

    @Mapping(target = "originalAmount", expression = "java(WireValues.decimal(entity.getOriginalAmount()))")
    @Mapping(target = "discountAmount", expression = "java(WireValues.decimal(entity.getDiscountAmount()))")
    @Mapping(target = "amount", expression = "java(WireValues.decimal(entity.getAmount()))")
    @Mapping(target = "amountWithTax", expression = "java(WireValues.decimal(entity.getAmountWithTax()))")
    @Mapping(target = "purpose", expression =
            "java(entity.getPurpose() == null ? null : Payment.PurposeEnum.fromValue(entity.getPurpose()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Payment.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "paymentMethod", expression = "java(entity.getPaymentMethod() == null ? null"
            + " : Payment.PaymentMethodEnum.fromValue(entity.getPaymentMethod()))")
    @Mapping(target = "taxIdentity", ignore = true)
    @Mapping(target = "taxIdentityMasked", ignore = true)
    @Mapping(target = "allocations", ignore = true)
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Payment toDto(PaymentEntity entity);
}
