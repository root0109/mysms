package com.mysms.comms.grpc;

import com.mysms.comms.api.model.PostAudienceClassUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePostAudienceClass over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.PostAudienceClassUpdate} message
 * as REST's {@link PostAudienceClassUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PostAudienceClassProtoBridge {

    private PostAudienceClassProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PostAudienceClassUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.PostAudienceClassUpdate request) {
        PostAudienceClassUpdate changes = new PostAudienceClassUpdate();
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        return bridge.patch(changes);
    }
}
