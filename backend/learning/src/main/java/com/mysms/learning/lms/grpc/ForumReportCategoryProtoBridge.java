package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ForumReportCategoryUpdate;

/**
 * UpdateForumReportCategory over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ForumReportCategoryUpdate} message as
 * REST's {@link ForumReportCategoryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ForumReportCategoryProtoBridge {

    private ForumReportCategoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ForumReportCategoryUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ForumReportCategoryUpdate request) {
        ForumReportCategoryUpdate changes = new ForumReportCategoryUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        return bridge.patch(changes);
    }
}
