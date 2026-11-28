package com.mysms.students.admission.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.admission.api.model.AdmissionCycleUpdate;

/**
 * UpdateAdmissionCycle over gRPC (JAVA-CODING-RULES §7.2): the {@code admission.v1.AdmissionCycleUpdate} message as
 * REST's {@link AdmissionCycleUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class AdmissionCycleProtoBridge {

    private AdmissionCycleProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AdmissionCycleUpdate changes(ProtoBridge bridge, com.mysms.admission.v1.AdmissionCycleUpdate request) {
        AdmissionCycleUpdate changes = new AdmissionCycleUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setAdmissionBasis(bridge.choice(
                request.hasAdmissionBasis(), request.getAdmissionBasis(),
                AdmissionCycleUpdate.AdmissionBasisEnum::fromValue));
        changes.setProgramId(bridge.uuid(request.hasProgramId(), request.getProgramId(), "program_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setGradeLevelId(bridge.uuid(request.hasGradeLevelId(), request.getGradeLevelId(), "grade_level_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setAcademicTermId(bridge.uuid(
                request.hasAcademicTermId(), request.getAcademicTermId(), "academic_term_id"));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        changes.setMinApplications(ProtoBridge.number(request.hasMinApplications(), request.getMinApplications()));
        changes.setMaxApplications(ProtoBridge.number(request.hasMaxApplications(), request.getMaxApplications()));
        changes.setMinimumAgeYears(ProtoBridge.number(request.hasMinimumAgeYears(), request.getMinimumAgeYears()));
        changes.setApplicationFeeHeadId(bridge.uuid(
                request.hasApplicationFeeHeadId(), request.getApplicationFeeHeadId(), "application_fee_head_id"));
        return bridge.patch(changes);
    }
}
