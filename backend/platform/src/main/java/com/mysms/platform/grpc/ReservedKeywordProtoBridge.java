package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.ReservedKeywordUpdate;

/**
 * UpdateReservedKeyword over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.ReservedKeywordUpdate} message
 * as REST's {@link ReservedKeywordUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ReservedKeywordProtoBridge {

    private ReservedKeywordProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ReservedKeywordUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.ReservedKeywordUpdate request) {
        ReservedKeywordUpdate changes = new ReservedKeywordUpdate();
        changes.setKeyword(ProtoBridge.text(request.hasKeyword(), request.getKeyword()));
        return bridge.patch(changes);
    }
}
