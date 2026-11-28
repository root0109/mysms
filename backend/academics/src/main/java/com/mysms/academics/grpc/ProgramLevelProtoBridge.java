package com.mysms.academics.grpc;

import com.mysms.academics.api.model.ProgramLevelUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateProgramLevel over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.ProgramLevelUpdate} message as REST's
 * {@link ProgramLevelUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ProgramLevelProtoBridge {

    private ProgramLevelProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ProgramLevelUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.ProgramLevelUpdate request) {
        ProgramLevelUpdate changes = new ProgramLevelUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
