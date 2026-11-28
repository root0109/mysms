package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.BadgeUpdate;

/**
 * UpdateBadge over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.BadgeUpdate} message as REST's {@link BadgeUpdate}
 * body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class BadgeProtoBridge {

    private BadgeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static BadgeUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.BadgeUpdate request) {
        BadgeUpdate changes = new BadgeUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setImageFileId(bridge.uuid(request.hasImageFileId(), request.getImageFileId(), "image_file_id"));
        changes.setTargetEntity(ProtoBridge.text(request.hasTargetEntity(), request.getTargetEntity()));
        changes.setTriggerEvent(bridge.choice(
                request.hasTriggerEvent(), request.getTriggerEvent(), BadgeUpdate.TriggerEventEnum::fromValue));
        changes.setConditionExpr(ProtoBridge.text(request.hasConditionExpr(), request.getConditionExpr()));
        changes.setFieldToCheck(ProtoBridge.text(request.hasFieldToCheck(), request.getFieldToCheck()));
        changes.setUserField(ProtoBridge.text(request.hasUserField(), request.getUserField()));
        changes.setGrantOnlyOnce(ProtoBridge.flag(request.hasGrantOnlyOnce(), request.getGrantOnlyOnce()));
        changes.setIsEnabled(ProtoBridge.flag(request.hasIsEnabled(), request.getIsEnabled()));
        return bridge.patch(changes);
    }
}
