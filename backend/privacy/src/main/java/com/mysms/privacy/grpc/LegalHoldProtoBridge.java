package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.LegalHoldUpdate;

/**
 * UpdateLegalHold over gRPC (JAVA-CODING-RULES §7.2): the {@code privacy.v1.LegalHoldUpdate} message
 * as REST's {@link LegalHoldUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class LegalHoldProtoBridge {

    private LegalHoldProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LegalHoldUpdate changes(ProtoBridge bridge, com.mysms.privacy.v1.LegalHoldUpdate request) {
        LegalHoldUpdate changes = new LegalHoldUpdate();
        changes.setReviewBy(bridge.date(request.hasReviewBy(), request.getReviewBy(), "review_by"));
        return bridge.patch(changes);
    }
}
