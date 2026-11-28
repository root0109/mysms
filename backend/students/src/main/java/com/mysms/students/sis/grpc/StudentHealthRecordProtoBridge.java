package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.StudentHealthRecordUpdate;

/**
 * UpdateStudentHealthRecord over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.StudentHealthRecordUpdate} message as
 * REST's {@link StudentHealthRecordUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class StudentHealthRecordProtoBridge {

    private StudentHealthRecordProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentHealthRecordUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.StudentHealthRecordUpdate request) {
        StudentHealthRecordUpdate changes = new StudentHealthRecordUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setHeightCm(ProtoBridge.text(request.hasHeightCm(), request.getHeightCm()));
        changes.setWeightKg(ProtoBridge.text(request.hasWeightKg(), request.getWeightKg()));
        changes.setMedicalConditions(ProtoBridge.text(request.hasMedicalConditions(), request.getMedicalConditions()));
        changes.setMedicationNeeds(ProtoBridge.text(request.hasMedicationNeeds(), request.getMedicationNeeds()));
        changes.setMedicationAllergies(ProtoBridge.text(
                request.hasMedicationAllergies(), request.getMedicationAllergies()));
        changes.setFoodAllergies(ProtoBridge.text(request.hasFoodAllergies(), request.getFoodAllergies()));
        changes.setOtherAllergies(ProtoBridge.text(request.hasOtherAllergies(), request.getOtherAllergies()));
        changes.setOtherMedicalInfo(ProtoBridge.text(request.hasOtherMedicalInfo(), request.getOtherMedicalInfo()));
        return bridge.patch(changes);
    }
}
