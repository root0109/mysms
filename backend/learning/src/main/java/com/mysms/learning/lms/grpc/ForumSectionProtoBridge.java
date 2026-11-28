package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ForumSectionUpdate;

/**
 * UpdateForumSection over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ForumSectionUpdate} message as REST's {@link
 * ForumSectionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ForumSectionProtoBridge {

    private ForumSectionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ForumSectionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ForumSectionUpdate request) {
        ForumSectionUpdate changes = new ForumSectionUpdate();
        changes.setForumId(bridge.uuid(request.hasForumId(), request.getForumId(), "forum_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setSectionType(bridge.choice(
                request.hasSectionType(), request.getSectionType(), ForumSectionUpdate.SectionTypeEnum::fromValue));
        changes.setCohortGroupId(bridge.uuid(
                request.hasCohortGroupId(), request.getCohortGroupId(), "cohort_group_id"));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
