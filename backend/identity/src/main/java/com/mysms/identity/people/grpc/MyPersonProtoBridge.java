package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.MyPersonUpdate;

/**
 * UpdateMyPerson over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.MyPersonUpdate} message
 * as REST's {@link MyPersonUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class MyPersonProtoBridge {

    private MyPersonProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MyPersonUpdate changes(ProtoBridge bridge, com.mysms.people.v1.MyPersonUpdate request) {
        MyPersonUpdate changes = new MyPersonUpdate();
        changes.setPrimaryEmail(ProtoBridge.text(request.hasPrimaryEmail(), request.getPrimaryEmail()));
        changes.setPrimaryPhone(ProtoBridge.text(request.hasPrimaryPhone(), request.getPrimaryPhone()));
        changes.setAlternatePhone(ProtoBridge.text(request.hasAlternatePhone(), request.getAlternatePhone()));
        changes.setWorkPhone(ProtoBridge.text(request.hasWorkPhone(), request.getWorkPhone()));
        changes.setWebsiteUrl(ProtoBridge.text(request.hasWebsiteUrl(), request.getWebsiteUrl()));
        changes.setAboutMe(ProtoBridge.text(request.hasAboutMe(), request.getAboutMe()));
        changes.setMotherTongue(ProtoBridge.text(request.hasMotherTongue(), request.getMotherTongue()));
        return bridge.patch(changes);
    }
}
