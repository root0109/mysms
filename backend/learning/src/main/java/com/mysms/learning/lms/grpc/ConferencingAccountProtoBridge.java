package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ConferencingAccountUpdate;

/**
 * UpdateConferencingAccount over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ConferencingAccountUpdate} message as
 * REST's {@link ConferencingAccountUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ConferencingAccountProtoBridge {

    private ConferencingAccountProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ConferencingAccountUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ConferencingAccountUpdate request) {
        ConferencingAccountUpdate changes = new ConferencingAccountUpdate();
        changes.setProvider(bridge.choice(
                request.hasProvider(), request.getProvider(), ConferencingAccountUpdate.ProviderEnum::fromValue));
        changes.setAccountName(ProtoBridge.text(request.hasAccountName(), request.getAccountName()));
        changes.setOwnerUserId(bridge.uuid(request.hasOwnerUserId(), request.getOwnerUserId(), "owner_user_id"));
        changes.setIsEnabled(ProtoBridge.flag(request.hasIsEnabled(), request.getIsEnabled()));
        changes.setExternalAccountId(ProtoBridge.text(request.hasExternalAccountId(), request.getExternalAccountId()));
        changes.setClientId(ProtoBridge.text(request.hasClientId(), request.getClientId()));
        changes.setClientSecret(ProtoBridge.text(request.hasClientSecret(), request.getClientSecret()));
        changes.setExternalCalendarId(ProtoBridge.text(
                request.hasExternalCalendarId(), request.getExternalCalendarId()));
        return bridge.patch(changes);
    }
}
