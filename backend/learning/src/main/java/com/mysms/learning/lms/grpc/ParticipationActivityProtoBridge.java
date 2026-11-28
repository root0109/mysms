package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ParticipationActivityUpdate;

/**
 * UpdateParticipationActivity over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ParticipationActivityUpdate}
 * message as REST's {@link ParticipationActivityUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ParticipationActivityProtoBridge {

    private ParticipationActivityProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ParticipationActivityUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.ParticipationActivityUpdate request) {
        ParticipationActivityUpdate changes = new ParticipationActivityUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setIsInRepository(ProtoBridge.flag(request.hasIsInRepository(), request.getIsInRepository()));
        return bridge.patch(changes);
    }
}
