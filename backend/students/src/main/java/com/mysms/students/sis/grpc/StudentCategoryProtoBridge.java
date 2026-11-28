package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.StudentCategoryUpdate;

/**
 * UpdateStudentCategory over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.StudentCategoryUpdate} message as REST's
 * {@link StudentCategoryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class StudentCategoryProtoBridge {

    private StudentCategoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentCategoryUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.StudentCategoryUpdate request) {
        StudentCategoryUpdate changes = new StudentCategoryUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
