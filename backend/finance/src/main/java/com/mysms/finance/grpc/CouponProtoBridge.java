package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.CouponUpdate;

/**
 * UpdateCoupon over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.CouponUpdate} message
 * as REST's {@link CouponUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CouponProtoBridge {

    private CouponProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CouponUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.CouponUpdate request) {
        CouponUpdate changes = new CouponUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setDiscountType(bridge.choice(
                request.hasDiscountType(), request.getDiscountType(), CouponUpdate.DiscountTypeEnum::fromValue));
        changes.setPercentageDiscount(ProtoBridge.text(
                request.hasPercentageDiscount(), request.getPercentageDiscount()));
        changes.setFixedDiscount(ProtoBridge.text(request.hasFixedDiscount(), request.getFixedDiscount()));
        changes.setExpiresOn(bridge.date(request.hasExpiresOn(), request.getExpiresOn(), "expires_on"));
        changes.setUsageLimit(ProtoBridge.number(request.hasUsageLimit(), request.getUsageLimit()));
        changes.setRedemptionCount(ProtoBridge.number(request.hasRedemptionCount(), request.getRedemptionCount()));
        changes.setIsEnabled(ProtoBridge.flag(request.hasIsEnabled(), request.getIsEnabled()));
        return bridge.patch(changes);
    }
}
