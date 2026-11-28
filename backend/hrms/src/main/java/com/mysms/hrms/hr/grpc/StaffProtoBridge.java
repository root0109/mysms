package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.hr.api.model.StaffUpdate;

/**
 * UpdateStaff over gRPC (JAVA-CODING-RULES §7.2): the {@code hr.v1.StaffUpdate} message as REST's {@link StaffUpdate}
 * body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class StaffProtoBridge {

    private StaffProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StaffUpdate changes(ProtoBridge bridge, com.mysms.hr.v1.StaffUpdate request) {
        StaffUpdate changes = new StaffUpdate();
        changes.setPersonId(bridge.uuid(request.hasPersonId(), request.getPersonId(), "person_id"));
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setEmployeeCode(ProtoBridge.text(request.hasEmployeeCode(), request.getEmployeeCode()));
        changes.setDesignationId(bridge.uuid(request.hasDesignationId(), request.getDesignationId(), "designation_id"));
        changes.setSubDesignation(ProtoBridge.text(request.hasSubDesignation(), request.getSubDesignation()));
        changes.setEmploymentType(bridge.choice(
                request.hasEmploymentType(), request.getEmploymentType(), StaffUpdate.EmploymentTypeEnum::fromValue));
        changes.setJoiningDate(bridge.date(request.hasJoiningDate(), request.getJoiningDate(), "joining_date"));
        changes.setReportingToStaffId(bridge.uuid(
                request.hasReportingToStaffId(), request.getReportingToStaffId(), "reporting_to_staff_id"));
        changes.setPrimaryDepartmentId(bridge.uuid(
                request.hasPrimaryDepartmentId(), request.getPrimaryDepartmentId(), "primary_department_id"));
        changes.setHighestQualificationId(bridge.uuid(
                request.hasHighestQualificationId(), request.getHighestQualificationId(), "highest_qualification_id"));
        changes.setQualificationDetail(ProtoBridge.text(
                request.hasQualificationDetail(), request.getQualificationDetail()));
        changes.setSpecialization(ProtoBridge.text(request.hasSpecialization(), request.getSpecialization()));
        changes.setInterests(ProtoBridge.text(request.hasInterests(), request.getInterests()));
        changes.setMaxPeriodsPerWeek(ProtoBridge.number(
                request.hasMaxPeriodsPerWeek(), request.getMaxPeriodsPerWeek()));
        return bridge.patch(changes);
    }
}
