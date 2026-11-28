package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.StudentGuardianUpdate;

/**
 * UpdateStudentGuardian over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.StudentGuardianUpdate} message as REST's
 * {@link StudentGuardianUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class StudentGuardianProtoBridge {

    private StudentGuardianProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentGuardianUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.StudentGuardianUpdate request) {
        StudentGuardianUpdate changes = new StudentGuardianUpdate();
        changes.setIsLegalGuardian(ProtoBridge.flag(request.hasIsLegalGuardian(), request.getIsLegalGuardian()));
        changes.setHasCustody(ProtoBridge.flag(request.hasHasCustody(), request.getHasCustody()));
        changes.setGuardianId(bridge.uuid(request.hasGuardianId(), request.getGuardianId(), "guardian_id"));
        changes.setRelationshipTypeId(bridge.uuid(
                request.hasRelationshipTypeId(), request.getRelationshipTypeId(), "relationship_type_id"));
        changes.setIsPrimaryContact(ProtoBridge.flag(request.hasIsPrimaryContact(), request.getIsPrimaryContact()));
        changes.setReceivesCommunication(ProtoBridge.flag(
                request.hasReceivesCommunication(), request.getReceivesCommunication()));
        changes.setCanPickUp(ProtoBridge.flag(request.hasCanPickUp(), request.getCanPickUp()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), StudentGuardianUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
