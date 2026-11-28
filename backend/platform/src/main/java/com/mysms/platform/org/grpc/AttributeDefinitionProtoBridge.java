package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.AttributeDefinitionUpdate;

/**
 * UpdateAttributeDefinition over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.AttributeDefinitionUpdate} message
 * as REST's {@link AttributeDefinitionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AttributeDefinitionProtoBridge {

    private AttributeDefinitionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AttributeDefinitionUpdate changes(ProtoBridge bridge, com.mysms.org.v1.AttributeDefinitionUpdate request) {
        AttributeDefinitionUpdate changes = new AttributeDefinitionUpdate();
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDataType(bridge.choice(
                request.hasDataType(), request.getDataType(), AttributeDefinitionUpdate.DataTypeEnum::fromValue));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
