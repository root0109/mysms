package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.MentorshipUpdate;

/**
 * UpdateMentorship over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.MentorshipUpdate} message as REST's {@link
 * MentorshipUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class MentorshipProtoBridge {

    private MentorshipProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MentorshipUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.MentorshipUpdate request) {
        MentorshipUpdate changes = new MentorshipUpdate();
        changes.setMentorUserId(bridge.uuid(request.hasMentorUserId(), request.getMentorUserId(), "mentor_user_id"));
        changes.setMenteeUserId(bridge.uuid(request.hasMenteeUserId(), request.getMenteeUserId(), "mentee_user_id"));
        changes.setValidFrom(bridge.date(request.hasValidFrom(), request.getValidFrom(), "valid_from"));
        changes.setValidTo(bridge.date(request.hasValidTo(), request.getValidTo(), "valid_to"));
        return bridge.patch(changes);
    }
}
