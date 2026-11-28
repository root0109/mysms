package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.EnrollmentUpdate;

/**
 * UpdateEnrollment over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.EnrollmentUpdate} message as REST's {@link
 * EnrollmentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class EnrollmentProtoBridge {

    private EnrollmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EnrollmentUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.EnrollmentUpdate request) {
        EnrollmentUpdate changes = new EnrollmentUpdate();
        changes.setMemberRole(bridge.choice(
                request.hasMemberRole(), request.getMemberRole(), EnrollmentUpdate.MemberRoleEnum::fromValue));
        return bridge.patch(changes);
    }
}
