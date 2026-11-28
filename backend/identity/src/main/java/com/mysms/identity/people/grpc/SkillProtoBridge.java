package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.SkillUpdate;

/**
 * UpdateSkill over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.SkillUpdate} message
 * as REST's {@link SkillUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class SkillProtoBridge {

    private SkillProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SkillUpdate changes(ProtoBridge bridge, com.mysms.people.v1.SkillUpdate request) {
        SkillUpdate changes = new SkillUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
