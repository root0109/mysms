package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.EntityNoteUpdate;

/**
 * UpdateEntityNote over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.EntityNoteUpdate} message
 * as REST's {@link EntityNoteUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EntityNoteProtoBridge {

    private EntityNoteProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EntityNoteUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.EntityNoteUpdate request) {
        EntityNoteUpdate changes = new EntityNoteUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setEntityType(ProtoBridge.text(request.hasEntityType(), request.getEntityType()));
        changes.setEntityId(bridge.uuid(request.hasEntityId(), request.getEntityId(), "entity_id"));
        changes.setNoteText(ProtoBridge.text(request.hasNoteText(), request.getNoteText()));
        return bridge.patch(changes);
    }
}
