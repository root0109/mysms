package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.PlagiarismUserLinkUpdate;

/**
 * UpdatePlagiarismUserLink over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.PlagiarismUserLinkUpdate} message as
 * REST's {@link PlagiarismUserLinkUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class PlagiarismUserLinkProtoBridge {

    private PlagiarismUserLinkProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PlagiarismUserLinkUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.PlagiarismUserLinkUpdate request) {
        PlagiarismUserLinkUpdate changes = new PlagiarismUserLinkUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setAccountRef(ProtoBridge.text(request.hasAccountRef(), request.getAccountRef()));
        changes.setProviderUserRef(ProtoBridge.text(request.hasProviderUserRef(), request.getProviderUserRef()));
        return bridge.patch(changes);
    }
}
