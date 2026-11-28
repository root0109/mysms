package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.InstitutionAttributeUpdate;

/**
 * UpdateInstitutionAttribute over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.InstitutionAttributeUpdate} message
 * as REST's {@link InstitutionAttributeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class InstitutionAttributeProtoBridge {

    private InstitutionAttributeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static InstitutionAttributeUpdate changes(ProtoBridge bridge, com.mysms.org.v1.InstitutionAttributeUpdate request) {
        InstitutionAttributeUpdate changes = new InstitutionAttributeUpdate();
        changes.setAttrKey(ProtoBridge.text(request.hasAttrKey(), request.getAttrKey()));
        changes.setAttrValue(ProtoBridge.text(request.hasAttrValue(), request.getAttrValue()));
        changes.setAttributeDefinitionId(bridge.uuid(
                request.hasAttributeDefinitionId(), request.getAttributeDefinitionId(), "attribute_definition_id"));
        return bridge.patch(changes);
    }
}
