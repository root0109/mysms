package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ForumUpdate;

/**
 * UpdateForum over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ForumUpdate} message as REST's {@link ForumUpdate}
 * body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ForumProtoBridge {

    private ForumProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ForumUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ForumUpdate request) {
        ForumUpdate changes = new ForumUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setEarlyAccessFrom(ProtoBridge.time(request.hasEarlyAccessFrom(), request.getEarlyAccessFrom()));
        changes.setOpensAt(ProtoBridge.time(request.hasOpensAt(), request.getOpensAt()));
        changes.setClosesAt(ProtoBridge.time(request.hasClosesAt(), request.getClosesAt()));
        changes.setLateAccessUntil(ProtoBridge.time(request.hasLateAccessUntil(), request.getLateAccessUntil()));
        return bridge.patch(changes);
    }
}
