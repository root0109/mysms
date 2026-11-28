package com.mysms.academics.grpc;

import com.mysms.academics.api.model.SectionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateSection over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.SectionUpdate} message as REST's {@link
 * SectionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class SectionProtoBridge {

    private SectionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SectionUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.SectionUpdate request) {
        SectionUpdate changes = new SectionUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
