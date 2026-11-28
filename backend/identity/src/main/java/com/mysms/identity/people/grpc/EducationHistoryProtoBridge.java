package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.EducationHistoryUpdate;

/**
 * UpdateEducationHistory over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.EducationHistoryUpdate} message
 * as REST's {@link EducationHistoryUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EducationHistoryProtoBridge {

    private EducationHistoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EducationHistoryUpdate changes(ProtoBridge bridge, com.mysms.people.v1.EducationHistoryUpdate request) {
        EducationHistoryUpdate changes = new EducationHistoryUpdate();
        changes.setQualificationId(bridge.uuid(
                request.hasQualificationId(), request.getQualificationId(), "qualification_id"));
        changes.setInstitutionName(ProtoBridge.text(request.hasInstitutionName(), request.getInstitutionName()));
        changes.setLocation(ProtoBridge.text(request.hasLocation(), request.getLocation()));
        changes.setDegreeType(ProtoBridge.text(request.hasDegreeType(), request.getDegreeType()));
        changes.setMajor(ProtoBridge.text(request.hasMajor(), request.getMajor()));
        changes.setGradeType(bridge.choice(
                request.hasGradeType(), request.getGradeType(), EducationHistoryUpdate.GradeTypeEnum::fromValue));
        changes.setGrade(ProtoBridge.text(request.hasGrade(), request.getGrade()));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        changes.setCertification(ProtoBridge.text(request.hasCertification(), request.getCertification()));
        changes.setMinor(ProtoBridge.text(request.hasMinor(), request.getMinor()));
        changes.setStudyMode(bridge.choice(
                request.hasStudyMode(), request.getStudyMode(), EducationHistoryUpdate.StudyModeEnum::fromValue));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
