package com.mysms.academics.attendance.grpc;

import com.mysms.academics.attendance.api.model.AttendanceRegisterUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAttendanceRegister over gRPC (JAVA-CODING-RULES §7.2): the {@code attendance.v1.AttendanceRegisterUpdate}
 * message as REST's {@link AttendanceRegisterUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class AttendanceRegisterProtoBridge {

    private AttendanceRegisterProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AttendanceRegisterUpdate changes(
            ProtoBridge bridge, com.mysms.attendance.v1.AttendanceRegisterUpdate request) {
        AttendanceRegisterUpdate changes = new AttendanceRegisterUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setRegisterScope(bridge.choice(
                request.hasRegisterScope(), request.getRegisterScope(),
                AttendanceRegisterUpdate.RegisterScopeEnum::fromValue));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setBatchId(bridge.uuid(request.hasBatchId(), request.getBatchId(), "batch_id"));
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
