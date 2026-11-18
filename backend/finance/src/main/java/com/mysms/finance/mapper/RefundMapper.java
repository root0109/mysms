package com.mysms.finance.mapper;

import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.finance.api.model.Refund;
import com.mysms.finance.domain.RefundEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity → contract DTO for finance.refund. Refunds are written only by finance.RefundPayment and the gateway's
 * notifications, never built from a request, so there is no way in; the ETag always comes from row_version.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface RefundMapper {

    @Mapping(target = "amount", expression = "java(WireValues.decimal(entity.getAmount()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Refund.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "refundMethod", expression = "java(entity.getRefundMethod() == null ? null"
            + " : Refund.RefundMethodEnum.fromValue(entity.getRefundMethod()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Refund toDto(RefundEntity entity);
}
