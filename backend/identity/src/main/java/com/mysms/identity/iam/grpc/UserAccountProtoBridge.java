package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.UserAccountUpdate;

/**
 * UpdateUserAccount over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.UserAccountUpdate} message
 * as REST's {@link UserAccountUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class UserAccountProtoBridge {

    private UserAccountProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static UserAccountUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.UserAccountUpdate request) {
        UserAccountUpdate changes = new UserAccountUpdate();
        changes.setUserType(bridge.choice(
                request.hasUserType(), request.getUserType(), UserAccountUpdate.UserTypeEnum::fromValue));
        changes.setPersonId(bridge.uuid(request.hasPersonId(), request.getPersonId(), "person_id"));
        changes.setUsername(ProtoBridge.text(request.hasUsername(), request.getUsername()));
        changes.setEmail(ProtoBridge.text(request.hasEmail(), request.getEmail()));
        changes.setMobileNo(ProtoBridge.text(request.hasMobileNo(), request.getMobileNo()));
        changes.setReferredByUserId(bridge.uuid(
                request.hasReferredByUserId(), request.getReferredByUserId(), "referred_by_user_id"));
        changes.setLocale(ProtoBridge.text(request.hasLocale(), request.getLocale()));
        changes.setTimezone(ProtoBridge.text(request.hasTimezone(), request.getTimezone()));
        changes.setJoinedOn(bridge.date(request.hasJoinedOn(), request.getJoinedOn(), "joined_on"));
        changes.setExternalRef(ProtoBridge.text(request.hasExternalRef(), request.getExternalRef()));
        changes.setSignature(ProtoBridge.text(request.hasSignature(), request.getSignature()));
        changes.setUseSignature(ProtoBridge.flag(request.hasUseSignature(), request.getUseSignature()));
        return bridge.patch(changes);
    }
}
