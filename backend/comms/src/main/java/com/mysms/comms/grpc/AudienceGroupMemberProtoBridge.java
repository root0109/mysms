package com.mysms.comms.grpc;

import com.mysms.comms.api.model.AudienceGroupMemberUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAudienceGroupMember over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.AudienceGroupMemberUpdate} message
 * as REST's {@link AudienceGroupMemberUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AudienceGroupMemberProtoBridge {

    private AudienceGroupMemberProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AudienceGroupMemberUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.AudienceGroupMemberUpdate request) {
        AudienceGroupMemberUpdate changes = new AudienceGroupMemberUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setMemberType(bridge.choice(
                request.hasMemberType(), request.getMemberType(), AudienceGroupMemberUpdate.MemberTypeEnum::fromValue));
        return bridge.patch(changes);
    }
}
