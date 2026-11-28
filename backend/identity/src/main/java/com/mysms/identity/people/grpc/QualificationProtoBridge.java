package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.QualificationUpdate;

/**
 * UpdateQualification over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.QualificationUpdate} message
 * as REST's {@link QualificationUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class QualificationProtoBridge {

    private QualificationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QualificationUpdate changes(ProtoBridge bridge, com.mysms.people.v1.QualificationUpdate request) {
        QualificationUpdate changes = new QualificationUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setLevel(bridge.choice(
                request.hasLevel(), request.getLevel(), QualificationUpdate.LevelEnum::fromValue));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
