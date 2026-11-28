package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ExternalToolLinkUpdate;

/**
 * UpdateExternalToolLink over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ExternalToolLinkUpdate} message as
 * REST's {@link ExternalToolLinkUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ExternalToolLinkProtoBridge {

    private ExternalToolLinkProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExternalToolLinkUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ExternalToolLinkUpdate request) {
        ExternalToolLinkUpdate changes = new ExternalToolLinkUpdate();
        changes.setExternalToolId(bridge.uuid(
                request.hasExternalToolId(), request.getExternalToolId(), "external_tool_id"));
        changes.setLinkRef(ProtoBridge.text(request.hasLinkRef(), request.getLinkRef()));
        changes.setLinkTitle(ProtoBridge.text(request.hasLinkTitle(), request.getLinkTitle()));
        return bridge.patch(changes);
    }
}
