package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.PurposeUpdate;

/**
 * UpdatePurpose over gRPC (JAVA-CODING-RULES §7.2): the {@code privacy.v1.PurposeUpdate} message
 * as REST's {@link PurposeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PurposeProtoBridge {

    private PurposeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PurposeUpdate changes(ProtoBridge bridge, com.mysms.privacy.v1.PurposeUpdate request) {
        PurposeUpdate changes = new PurposeUpdate();
        changes.setEnabled(ProtoBridge.flag(request.hasEnabled(), request.getEnabled()));
        changes.setMaxScope(bridge.choice(
                request.hasMaxScope(), request.getMaxScope(), PurposeUpdate.MaxScopeEnum::fromValue));
        changes.setNoticeId(bridge.uuid(request.hasNoticeId(), request.getNoticeId(), "notice_id"));
        return bridge.patch(changes);
    }
}
