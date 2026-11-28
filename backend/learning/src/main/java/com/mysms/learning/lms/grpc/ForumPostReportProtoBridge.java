package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ForumPostReportUpdate;

/**
 * UpdateForumPostReport over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ForumPostReportUpdate} message as REST's
 * {@link ForumPostReportUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class ForumPostReportProtoBridge {

    private ForumPostReportProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ForumPostReportUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ForumPostReportUpdate request) {
        ForumPostReportUpdate changes = new ForumPostReportUpdate();
        changes.setForumPostId(bridge.uuid(request.hasForumPostId(), request.getForumPostId(), "forum_post_id"));
        changes.setReportCategoryId(bridge.uuid(
                request.hasReportCategoryId(), request.getReportCategoryId(), "report_category_id"));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        return bridge.patch(changes);
    }
}
