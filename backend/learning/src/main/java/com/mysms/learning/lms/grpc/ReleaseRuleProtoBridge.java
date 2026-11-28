package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ReleaseRuleUpdate;

/**
 * UpdateReleaseRule over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ReleaseRuleUpdate} message as REST's {@link
 * ReleaseRuleUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ReleaseRuleProtoBridge {

    private ReleaseRuleProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ReleaseRuleUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ReleaseRuleUpdate request) {
        ReleaseRuleUpdate changes = new ReleaseRuleUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setSourceContentItemId(bridge.uuid(
                request.hasSourceContentItemId(), request.getSourceContentItemId(), "source_content_item_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setStopIfTrue(ProtoBridge.flag(request.hasStopIfTrue(), request.getStopIfTrue()));
        changes.setIsDisabled(ProtoBridge.flag(request.hasIsDisabled(), request.getIsDisabled()));
        changes.setReassignSource(ProtoBridge.flag(request.hasReassignSource(), request.getReassignSource()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setCopiedFromId(bridge.uuid(request.hasCopiedFromId(), request.getCopiedFromId(), "copied_from_id"));
        return bridge.patch(changes);
    }
}
