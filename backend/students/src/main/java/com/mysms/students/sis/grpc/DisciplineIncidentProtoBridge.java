package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.DisciplineIncidentUpdate;

/**
 * UpdateDisciplineIncident over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.DisciplineIncidentUpdate} message as
 * REST's {@link DisciplineIncidentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class DisciplineIncidentProtoBridge {

    private DisciplineIncidentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static DisciplineIncidentUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.DisciplineIncidentUpdate request) {
        DisciplineIncidentUpdate changes = new DisciplineIncidentUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setIncidentAt(ProtoBridge.time(request.hasIncidentAt(), request.getIncidentAt()));
        changes.setReportedByUserId(bridge.uuid(
                request.hasReportedByUserId(), request.getReportedByUserId(), "reported_by_user_id"));
        changes.setIncidentType(bridge.choice(
                request.hasIncidentType(), request.getIncidentType(),
                DisciplineIncidentUpdate.IncidentTypeEnum::fromValue));
        changes.setDetails(ProtoBridge.text(request.hasDetails(), request.getDetails()));
        changes.setResponse(ProtoBridge.text(request.hasResponse(), request.getResponse()));
        changes.setActionTaken(ProtoBridge.flag(request.hasActionTaken(), request.getActionTaken()));
        changes.setEvidenceMediaType(bridge.choice(
                request.hasEvidenceMediaType(), request.getEvidenceMediaType(),
                DisciplineIncidentUpdate.EvidenceMediaTypeEnum::fromValue));
        changes.setEvidenceFileId(bridge.uuid(
                request.hasEvidenceFileId(), request.getEvidenceFileId(), "evidence_file_id"));
        changes.setNotifyGuardians(ProtoBridge.flag(request.hasNotifyGuardians(), request.getNotifyGuardians()));
        changes.setSeenByGuardian(ProtoBridge.flag(request.hasSeenByGuardian(), request.getSeenByGuardian()));
        return bridge.patch(changes);
    }
}
