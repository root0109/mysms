package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.NoticeTranslationUpdate;

/**
 * UpdateNoticeTranslation over gRPC (JAVA-CODING-RULES §7.2): the {@code privacy.v1.NoticeTranslationUpdate} message
 * as REST's {@link NoticeTranslationUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NoticeTranslationProtoBridge {

    private NoticeTranslationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NoticeTranslationUpdate changes(ProtoBridge bridge, com.mysms.privacy.v1.NoticeTranslationUpdate request) {
        NoticeTranslationUpdate changes = new NoticeTranslationUpdate();
        changes.setLanguage(ProtoBridge.text(request.hasLanguage(), request.getLanguage()));
        changes.setBodyMarkdown(ProtoBridge.text(request.hasBodyMarkdown(), request.getBodyMarkdown()));
        changes.setSummaryForChildren(ProtoBridge.text(
                request.hasSummaryForChildren(), request.getSummaryForChildren()));
        return bridge.patch(changes);
    }
}
