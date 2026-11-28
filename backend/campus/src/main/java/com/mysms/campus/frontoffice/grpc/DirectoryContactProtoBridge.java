package com.mysms.campus.frontoffice.grpc;

import com.mysms.campus.frontoffice.api.model.DirectoryContactUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateDirectoryContact over gRPC (JAVA-CODING-RULES §7.2): the {@code frontoffice.v1.DirectoryContactUpdate} message
 * as REST's {@link DirectoryContactUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class DirectoryContactProtoBridge {

    private DirectoryContactProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static DirectoryContactUpdate changes(ProtoBridge bridge, com.mysms.frontoffice.v1.DirectoryContactUpdate request) {
        DirectoryContactUpdate changes = new DirectoryContactUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDesignation(ProtoBridge.text(request.hasDesignation(), request.getDesignation()));
        changes.setPhone(ProtoBridge.text(request.hasPhone(), request.getPhone()));
        return bridge.patch(changes);
    }
}
