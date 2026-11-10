package com.mysms.finance.mapper;

import com.mysms.finance.api.model.Coupon;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.finance.domain.CouponEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for finance.coupon. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface CouponMapper {

    @Mapping(target = "discountType", expression =
            "java(entity.getDiscountType() == null ? null : "
                    + "Coupon.DiscountTypeEnum.fromValue(entity.getDiscountType()))")
    @Mapping(target = "percentageDiscount", expression = "java(WireValues.decimal(entity.getPercentageDiscount()))")
    @Mapping(target = "fixedDiscount", expression = "java(WireValues.decimal(entity.getFixedDiscount()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Coupon toDto(CouponEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "discountType", expression =
            "java(dto.getDiscountType() == null ? null : dto.getDiscountType().getValue())")
    @Mapping(target = "percentageDiscount", expression = "java(WireValues.decimal(dto.getPercentageDiscount()))")
    @Mapping(target = "fixedDiscount", expression = "java(WireValues.decimal(dto.getFixedDiscount()))")
    CouponEntity toEntity(Coupon dto);
}
