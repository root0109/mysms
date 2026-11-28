package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.BreachUpdate;

/**
 * UpdateBreach over gRPC (JAVA-CODING-RULES §7.2): the {@code privacy.v1.BreachUpdate} message
 * as REST's {@link BreachUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class BreachProtoBridge {

    private BreachProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static BreachUpdate changes(ProtoBridge bridge, com.mysms.privacy.v1.BreachUpdate request) {
        BreachUpdate changes = new BreachUpdate();
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setCategories(ProtoBridge.texts(request.hasCategories(), request.getCategories()));
        changes.setModules(ProtoBridge.texts(request.hasModules(), request.getModules()));
        changes.setEstimatedPrincipals(ProtoBridge.number(
                request.hasEstimatedPrincipals(), request.getEstimatedPrincipals()));
        changes.setLikelyConsequences(ProtoBridge.text(
                request.hasLikelyConsequences(), request.getLikelyConsequences()));
        changes.setMitigation(ProtoBridge.text(request.hasMitigation(), request.getMitigation()));
        changes.setOwnerUserId(bridge.uuid(request.hasOwnerUserId(), request.getOwnerUserId(), "owner_user_id"));
        return bridge.patch(changes);
    }
}
