package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.CouponScopeUpdate;

/**
 * UpdateCouponScope over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.CouponScopeUpdate} message
 * as REST's {@link CouponScopeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CouponScopeProtoBridge {

    private CouponScopeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CouponScopeUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.CouponScopeUpdate request) {
        CouponScopeUpdate changes = new CouponScopeUpdate();
        changes.setLmsCourseId(bridge.uuid(request.hasLmsCourseId(), request.getLmsCourseId(), "lms_course_id"));
        changes.setLmsCohortId(bridge.uuid(request.hasLmsCohortId(), request.getLmsCohortId(), "lms_cohort_id"));
        return bridge.patch(changes);
    }
}
