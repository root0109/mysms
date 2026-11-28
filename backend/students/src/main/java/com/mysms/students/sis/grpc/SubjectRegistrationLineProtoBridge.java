package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.SubjectRegistrationLineUpdate;

/**
 * UpdateSubjectRegistrationLine over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.SubjectRegistrationLineUpdate}
 * message as REST's {@link SubjectRegistrationLineUpdate} body, one explicit line per field: unset keeps the value,
 * null clears it, a value sets it.
 */
final class SubjectRegistrationLineProtoBridge {

    private SubjectRegistrationLineProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SubjectRegistrationLineUpdate changes(
            ProtoBridge bridge, com.mysms.sis.v1.SubjectRegistrationLineUpdate request) {
        SubjectRegistrationLineUpdate changes = new SubjectRegistrationLineUpdate();
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        changes.setSelectionType(bridge.choice(
                request.hasSelectionType(), request.getSelectionType(),
                SubjectRegistrationLineUpdate.SelectionTypeEnum::fromValue));
        return bridge.patch(changes);
    }
}
