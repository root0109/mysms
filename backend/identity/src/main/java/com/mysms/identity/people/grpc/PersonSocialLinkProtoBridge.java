package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.PersonSocialLinkUpdate;

/**
 * UpdatePersonSocialLink over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.PersonSocialLinkUpdate} message
 * as REST's {@link PersonSocialLinkUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PersonSocialLinkProtoBridge {

    private PersonSocialLinkProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PersonSocialLinkUpdate changes(ProtoBridge bridge, com.mysms.people.v1.PersonSocialLinkUpdate request) {
        PersonSocialLinkUpdate changes = new PersonSocialLinkUpdate();
        changes.setNetwork(bridge.choice(
                request.hasNetwork(), request.getNetwork(), PersonSocialLinkUpdate.NetworkEnum::fromValue));
        changes.setHandle(ProtoBridge.text(request.hasHandle(), request.getHandle()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
