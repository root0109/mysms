package com.mysms.academics.grpc;

import com.mysms.academics.api.model.CourseUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateCourse over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.CourseUpdate} message as REST's {@link
 * CourseUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CourseProtoBridge {

    private CourseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CourseUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.CourseUpdate request) {
        CourseUpdate changes = new CourseUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setProgramId(bridge.uuid(request.hasProgramId(), request.getProgramId(), "program_id"));
        changes.setDepartmentId(bridge.uuid(request.hasDepartmentId(), request.getDepartmentId(), "department_id"));
        changes.setParentCourseId(bridge.uuid(
                request.hasParentCourseId(), request.getParentCourseId(), "parent_course_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setEvaluationType(bridge.choice(
                request.hasEvaluationType(), request.getEvaluationType(), CourseUpdate.EvaluationTypeEnum::fromValue));
        changes.setMinUnitLoad(ProtoBridge.text(request.hasMinUnitLoad(), request.getMinUnitLoad()));
        changes.setMaxUnitLoad(ProtoBridge.text(request.hasMaxUnitLoad(), request.getMaxUnitLoad()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
