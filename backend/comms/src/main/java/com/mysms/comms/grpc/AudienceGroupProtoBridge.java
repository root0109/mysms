package com.mysms.comms.grpc;

import com.mysms.comms.api.model.AudienceGroupUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAudienceGroup over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.AudienceGroupUpdate} message
 * as REST's {@link AudienceGroupUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AudienceGroupProtoBridge {

    private AudienceGroupProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AudienceGroupUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.AudienceGroupUpdate request) {
        AudienceGroupUpdate changes = new AudienceGroupUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setGroupScope(bridge.choice(
                request.hasGroupScope(), request.getGroupScope(), AudienceGroupUpdate.GroupScopeEnum::fromValue));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
