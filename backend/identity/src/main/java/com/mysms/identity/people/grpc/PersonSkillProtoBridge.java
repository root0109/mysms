package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.PersonSkillUpdate;

/**
 * UpdatePersonSkill over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.PersonSkillUpdate} message
 * as REST's {@link PersonSkillUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PersonSkillProtoBridge {

    private PersonSkillProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PersonSkillUpdate changes(ProtoBridge bridge, com.mysms.people.v1.PersonSkillUpdate request) {
        PersonSkillUpdate changes = new PersonSkillUpdate();
        changes.setSkillId(bridge.uuid(request.hasSkillId(), request.getSkillId(), "skill_id"));
        return bridge.patch(changes);
    }
}
