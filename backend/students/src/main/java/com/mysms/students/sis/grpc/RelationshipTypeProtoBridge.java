package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.RelationshipTypeUpdate;

/**
 * UpdateRelationshipType over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.RelationshipTypeUpdate} message as
 * REST's {@link RelationshipTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class RelationshipTypeProtoBridge {

    private RelationshipTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RelationshipTypeUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.RelationshipTypeUpdate request) {
        RelationshipTypeUpdate changes = new RelationshipTypeUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
