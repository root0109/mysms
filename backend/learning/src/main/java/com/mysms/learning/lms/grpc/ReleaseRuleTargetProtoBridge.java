package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ReleaseRuleTargetUpdate;

/**
 * UpdateReleaseRuleTarget over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ReleaseRuleTargetUpdate} message as
 * REST's {@link ReleaseRuleTargetUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ReleaseRuleTargetProtoBridge {

    private ReleaseRuleTargetProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ReleaseRuleTargetUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ReleaseRuleTargetUpdate request) {
        ReleaseRuleTargetUpdate changes = new ReleaseRuleTargetUpdate();
        changes.setContentItemId(bridge.uuid(
                request.hasContentItemId(), request.getContentItemId(), "content_item_id"));
        changes.setChapterId(bridge.uuid(request.hasChapterId(), request.getChapterId(), "chapter_id"));
        return bridge.patch(changes);
    }
}
