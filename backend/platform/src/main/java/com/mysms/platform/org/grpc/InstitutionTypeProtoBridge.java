package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.InstitutionTypeUpdate;

/**
 * UpdateInstitutionType over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.InstitutionTypeUpdate} message
 * as REST's {@link InstitutionTypeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class InstitutionTypeProtoBridge {

    private InstitutionTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static InstitutionTypeUpdate changes(ProtoBridge bridge, com.mysms.org.v1.InstitutionTypeUpdate request) {
        InstitutionTypeUpdate changes = new InstitutionTypeUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
