package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.GrievanceContact;
import com.mysms.privacy.api.model.NoticeVersionUpdate;

/**
 * UpdateNoticeVersion over gRPC (JAVA-CODING-RULES §7.2): the {@code privacy.v1.NoticeVersionUpdate} message
 * as REST's {@link NoticeVersionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NoticeVersionProtoBridge {

    private NoticeVersionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NoticeVersionUpdate changes(ProtoBridge bridge, com.mysms.privacy.v1.NoticeVersionUpdate request) {
        NoticeVersionUpdate changes = new NoticeVersionUpdate();
        changes.setRequiresReconsent(ProtoBridge.flag(request.hasRequiresReconsent(), request.getRequiresReconsent()));
        changes.setPurposeCodes(ProtoBridge.texts(request.hasPurposeCodes(), request.getPurposeCodes()));
        changes.setGrievanceContact(bridge.message(
                request.hasGrievanceContact(), request.getGrievanceContact(), GrievanceContact.class));
        changes.setDocumentFileId(bridge.uuid(
                request.hasDocumentFileId(), request.getDocumentFileId(), "document_file_id"));
        return bridge.patch(changes);
    }
}
