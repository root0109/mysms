package com.mysms.academics.grpc;

import com.mysms.academics.api.model.ClassSubjectUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateClassSubject over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.ClassSubjectUpdate} message as REST's
 * {@link ClassSubjectUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ClassSubjectProtoBridge {

    private ClassSubjectProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ClassSubjectUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.ClassSubjectUpdate request) {
        ClassSubjectUpdate changes = new ClassSubjectUpdate();
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        changes.setOfferingType(bridge.choice(
                request.hasOfferingType(), request.getOfferingType(), ClassSubjectUpdate.OfferingTypeEnum::fromValue));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
