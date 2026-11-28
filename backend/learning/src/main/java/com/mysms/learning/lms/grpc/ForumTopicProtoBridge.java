package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ForumTopicUpdate;

/**
 * UpdateForumTopic over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ForumTopicUpdate} message as REST's {@link
 * ForumTopicUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ForumTopicProtoBridge {

    private ForumTopicProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ForumTopicUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ForumTopicUpdate request) {
        ForumTopicUpdate changes = new ForumTopicUpdate();
        changes.setForumSectionId(bridge.uuid(
                request.hasForumSectionId(), request.getForumSectionId(), "forum_section_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setTopicType(bridge.choice(
                request.hasTopicType(), request.getTopicType(), ForumTopicUpdate.TopicTypeEnum::fromValue));
        changes.setOpensAt(ProtoBridge.time(request.hasOpensAt(), request.getOpensAt()));
        changes.setClosesAt(ProtoBridge.time(request.hasClosesAt(), request.getClosesAt()));
        changes.setContentItemId(bridge.uuid(
                request.hasContentItemId(), request.getContentItemId(), "content_item_id"));
        changes.setDiscussionPromptId(bridge.uuid(
                request.hasDiscussionPromptId(), request.getDiscussionPromptId(), "discussion_prompt_id"));
        changes.setLinkedEntityType(ProtoBridge.text(request.hasLinkedEntityType(), request.getLinkedEntityType()));
        changes.setLinkedEntityId(bridge.uuid(
                request.hasLinkedEntityId(), request.getLinkedEntityId(), "linked_entity_id"));
        changes.setIndividualUserId(bridge.uuid(
                request.hasIndividualUserId(), request.getIndividualUserId(), "individual_user_id"));
        changes.setIsGraded(ProtoBridge.flag(request.hasIsGraded(), request.getIsGraded()));
        changes.setPointsPossible(ProtoBridge.text(request.hasPointsPossible(), request.getPointsPossible()));
        changes.setGradeWeightPercent(ProtoBridge.text(
                request.hasGradeWeightPercent(), request.getGradeWeightPercent()));
        changes.setGradingDueAt(ProtoBridge.time(request.hasGradingDueAt(), request.getGradingDueAt()));
        changes.setIsGradingCompleted(ProtoBridge.flag(
                request.hasIsGradingCompleted(), request.getIsGradingCompleted()));
        changes.setIsHidden(ProtoBridge.flag(request.hasIsHidden(), request.getIsHidden()));
        changes.setIsTeacherTopic(ProtoBridge.flag(request.hasIsTeacherTopic(), request.getIsTeacherTopic()));
        changes.setIsSystemGenerated(ProtoBridge.flag(request.hasIsSystemGenerated(), request.getIsSystemGenerated()));
        changes.setLastPostId(bridge.uuid(request.hasLastPostId(), request.getLastPostId(), "last_post_id"));
        return bridge.patch(changes);
    }
}
