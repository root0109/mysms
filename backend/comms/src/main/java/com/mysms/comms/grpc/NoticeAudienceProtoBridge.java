package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NoticeAudienceUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNoticeAudience over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.NoticeAudienceUpdate} message
 * as REST's {@link NoticeAudienceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NoticeAudienceProtoBridge {

    private NoticeAudienceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NoticeAudienceUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.NoticeAudienceUpdate request) {
        NoticeAudienceUpdate changes = new NoticeAudienceUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setRoleId(bridge.uuid(request.hasRoleId(), request.getRoleId(), "role_id"));
        return bridge.patch(changes);
    }
}
