package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.RetentionPolicyUpdate;

/**
 * UpdateRetentionPolicy over gRPC (JAVA-CODING-RULES §7.2): the {@code privacy.v1.RetentionPolicyUpdate} message
 * as REST's {@link RetentionPolicyUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class RetentionPolicyProtoBridge {

    private RetentionPolicyProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RetentionPolicyUpdate changes(ProtoBridge bridge, com.mysms.privacy.v1.RetentionPolicyUpdate request) {
        RetentionPolicyUpdate changes = new RetentionPolicyUpdate();
        changes.setPeriodDays(ProtoBridge.number(request.hasPeriodDays(), request.getPeriodDays()));
        changes.setAction(bridge.choice(
                request.hasAction(), request.getAction(), RetentionPolicyUpdate.ActionEnum::fromValue));
        return bridge.patch(changes);
    }
}
