package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.NoticeUpdate;

/**
 * UpdateNotice over gRPC (JAVA-CODING-RULES §7.2): the {@code privacy.v1.NoticeUpdate} message
 * as REST's {@link NoticeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NoticeProtoBridge {

    private NoticeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NoticeUpdate changes(ProtoBridge bridge, com.mysms.privacy.v1.NoticeUpdate request) {
        NoticeUpdate changes = new NoticeUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        return bridge.patch(changes);
    }
}
