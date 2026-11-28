package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NoticeBackgroundUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNoticeBackground over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.NoticeBackgroundUpdate} message
 * as REST's {@link NoticeBackgroundUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NoticeBackgroundProtoBridge {

    private NoticeBackgroundProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NoticeBackgroundUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.NoticeBackgroundUpdate request) {
        NoticeBackgroundUpdate changes = new NoticeBackgroundUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setUsage(bridge.choice(
                request.hasUsage(), request.getUsage(), NoticeBackgroundUpdate.UsageEnum::fromValue));
        changes.setImageFileId(bridge.uuid(request.hasImageFileId(), request.getImageFileId(), "image_file_id"));
        return bridge.patch(changes);
    }
}
