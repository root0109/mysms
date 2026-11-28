package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.InstitutionTypeAttributeUpdate;

/**
 * UpdateInstitutionTypeAttribute over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.InstitutionTypeAttributeUpdate} message
 * as REST's {@link InstitutionTypeAttributeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class InstitutionTypeAttributeProtoBridge {

    private InstitutionTypeAttributeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static InstitutionTypeAttributeUpdate changes(ProtoBridge bridge, com.mysms.org.v1.InstitutionTypeAttributeUpdate request) {
        InstitutionTypeAttributeUpdate changes = new InstitutionTypeAttributeUpdate();
        changes.setAttributeDefinitionId(bridge.uuid(
                request.hasAttributeDefinitionId(), request.getAttributeDefinitionId(), "attribute_definition_id"));
        changes.setIsRequired(ProtoBridge.flag(request.hasIsRequired(), request.getIsRequired()));
        return bridge.patch(changes);
    }
}
