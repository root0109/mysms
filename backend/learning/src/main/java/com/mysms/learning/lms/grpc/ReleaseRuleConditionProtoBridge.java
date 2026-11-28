package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ReleaseRuleConditionUpdate;

/**
 * UpdateReleaseRuleCondition over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ReleaseRuleConditionUpdate} message
 * as REST's {@link ReleaseRuleConditionUpdate} body, one explicit line per field: unset keeps the value, null clears
 * it, a value sets it.
 */
final class ReleaseRuleConditionProtoBridge {

    private ReleaseRuleConditionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ReleaseRuleConditionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ReleaseRuleConditionUpdate request) {
        ReleaseRuleConditionUpdate changes = new ReleaseRuleConditionUpdate();
        changes.setConditionType(bridge.choice(
                request.hasConditionType(), request.getConditionType(),
                ReleaseRuleConditionUpdate.ConditionTypeEnum::fromValue));
        changes.setOperator(ProtoBridge.text(request.hasOperator(), request.getOperator()));
        changes.setValue(ProtoBridge.text(request.hasValue(), request.getValue()));
        changes.setValue2(ProtoBridge.text(request.hasValue2(), request.getValue2()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
