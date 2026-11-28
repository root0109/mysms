package com.mysms.students.admission.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.admission.api.model.ApplicationUpdate;

/**
 * UpdateApplication over gRPC (JAVA-CODING-RULES §7.2): the {@code admission.v1.ApplicationUpdate} message as REST's
 * {@link ApplicationUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ApplicationProtoBridge {

    private ApplicationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ApplicationUpdate changes(ProtoBridge bridge, com.mysms.admission.v1.ApplicationUpdate request) {
        ApplicationUpdate changes = new ApplicationUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAdmissionCycleId(bridge.uuid(
                request.hasAdmissionCycleId(), request.getAdmissionCycleId(), "admission_cycle_id"));
        changes.setApplicationNumber(ProtoBridge.text(request.hasApplicationNumber(), request.getApplicationNumber()));
        changes.setApplicationDate(ProtoBridge.time(request.hasApplicationDate(), request.getApplicationDate()));
        changes.setAdmissionDate(bridge.date(request.hasAdmissionDate(), request.getAdmissionDate(), "admission_date"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setGradeLevelId(bridge.uuid(request.hasGradeLevelId(), request.getGradeLevelId(), "grade_level_id"));
        changes.setSectionId(bridge.uuid(request.hasSectionId(), request.getSectionId(), "section_id"));
        changes.setProgramId(bridge.uuid(request.hasProgramId(), request.getProgramId(), "program_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setBatchId(bridge.uuid(request.hasBatchId(), request.getBatchId(), "batch_id"));
        changes.setApplicantPersonId(bridge.uuid(
                request.hasApplicantPersonId(), request.getApplicantPersonId(), "applicant_person_id"));
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setIsExistingStudent(ProtoBridge.flag(request.hasIsExistingStudent(), request.getIsExistingStudent()));
        changes.setHonorificId(bridge.uuid(request.hasHonorificId(), request.getHonorificId(), "honorific_id"));
        changes.setFullName(ProtoBridge.text(request.hasFullName(), request.getFullName()));
        changes.setFirstName(ProtoBridge.text(request.hasFirstName(), request.getFirstName()));
        changes.setMiddleName(ProtoBridge.text(request.hasMiddleName(), request.getMiddleName()));
        changes.setLastName(ProtoBridge.text(request.hasLastName(), request.getLastName()));
        changes.setDateOfBirth(bridge.date(request.hasDateOfBirth(), request.getDateOfBirth(), "date_of_birth"));
        changes.setGender(bridge.choice(
                request.hasGender(), request.getGender(), ApplicationUpdate.GenderEnum::fromValue));
        changes.setBirthPlace(ProtoBridge.text(request.hasBirthPlace(), request.getBirthPlace()));
        changes.setNationality(ProtoBridge.text(request.hasNationality(), request.getNationality()));
        changes.setReligion(ProtoBridge.text(request.hasReligion(), request.getReligion()));
        changes.setSocialCategory(ProtoBridge.text(request.hasSocialCategory(), request.getSocialCategory()));
        changes.setMotherTongue(ProtoBridge.text(request.hasMotherTongue(), request.getMotherTongue()));
        changes.setIdentificationMarks(ProtoBridge.text(
                request.hasIdentificationMarks(), request.getIdentificationMarks()));
        changes.setNationalId(ProtoBridge.text(request.hasNationalId(), request.getNationalId()));
        changes.setBloodGroup(ProtoBridge.text(request.hasBloodGroup(), request.getBloodGroup()));
        changes.setHeightCm(ProtoBridge.text(request.hasHeightCm(), request.getHeightCm()));
        changes.setWeightKg(ProtoBridge.text(request.hasWeightKg(), request.getWeightKg()));
        changes.setPhotoFileId(bridge.uuid(request.hasPhotoFileId(), request.getPhotoFileId(), "photo_file_id"));
        changes.setEmail(ProtoBridge.text(request.hasEmail(), request.getEmail()));
        changes.setPhone(ProtoBridge.text(request.hasPhone(), request.getPhone()));
        changes.setMobile(ProtoBridge.text(request.hasMobile(), request.getMobile()));
        changes.setAddressLine1(ProtoBridge.text(request.hasAddressLine1(), request.getAddressLine1()));
        changes.setAddressLine2(ProtoBridge.text(request.hasAddressLine2(), request.getAddressLine2()));
        changes.setCityName(ProtoBridge.text(request.hasCityName(), request.getCityName()));
        changes.setPostalCode(ProtoBridge.text(request.hasPostalCode(), request.getPostalCode()));
        changes.setStateProvinceId(bridge.uuid(
                request.hasStateProvinceId(), request.getStateProvinceId(), "state_province_id"));
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        changes.setPermanentAddress(ProtoBridge.text(request.hasPermanentAddress(), request.getPermanentAddress()));
        changes.setCommunicationAddress(ProtoBridge.text(
                request.hasCommunicationAddress(), request.getCommunicationAddress()));
        changes.setPreviousInstitution(ProtoBridge.text(
                request.hasPreviousInstitution(), request.getPreviousInstitution()));
        changes.setPreviousCourse(ProtoBridge.text(request.hasPreviousCourse(), request.getPreviousCourse()));
        changes.setPreviousResult(ProtoBridge.text(request.hasPreviousResult(), request.getPreviousResult()));
        changes.setPreviousBoard(bridge.choice(
                request.hasPreviousBoard(), request.getPreviousBoard(),
                ApplicationUpdate.PreviousBoardEnum::fromValue));
        changes.setBoardRegistrationNumber(ProtoBridge.text(
                request.hasBoardRegistrationNumber(), request.getBoardRegistrationNumber()));
        changes.setReasonForLeaving(ProtoBridge.text(request.hasReasonForLeaving(), request.getReasonForLeaving()));
        changes.setLanguageChoice(ProtoBridge.text(request.hasLanguageChoice(), request.getLanguageChoice()));
        changes.setStreamChoice(ProtoBridge.text(request.hasStreamChoice(), request.getStreamChoice()));
        changes.setHasSiblings(ProtoBridge.flag(request.hasHasSiblings(), request.getHasSiblings()));
        changes.setHasMedicalHistory(ProtoBridge.flag(request.hasHasMedicalHistory(), request.getHasMedicalHistory()));
        changes.setMedicalDetails(ProtoBridge.text(request.hasMedicalDetails(), request.getMedicalDetails()));
        changes.setHasExtracurricular(ProtoBridge.flag(
                request.hasHasExtracurricular(), request.getHasExtracurricular()));
        changes.setExtracurricularDetails(ProtoBridge.text(
                request.hasExtracurricularDetails(), request.getExtracurricularDetails()));
        changes.setTransportMode(bridge.choice(
                request.hasTransportMode(), request.getTransportMode(),
                ApplicationUpdate.TransportModeEnum::fromValue));
        changes.setTransportDetails(ProtoBridge.text(request.hasTransportDetails(), request.getTransportDetails()));
        changes.setFamilyBusiness(ProtoBridge.text(request.hasFamilyBusiness(), request.getFamilyBusiness()));
        changes.setFamilyIncome(ProtoBridge.text(request.hasFamilyIncome(), request.getFamilyIncome()));
        changes.setApplicationFee(ProtoBridge.text(request.hasApplicationFee(), request.getApplicationFee()));
        changes.setDiscountPercent(ProtoBridge.text(request.hasDiscountPercent(), request.getDiscountPercent()));
        changes.setFeePlanId(bridge.uuid(request.hasFeePlanId(), request.getFeePlanId(), "fee_plan_id"));
        changes.setFeeStartDate(bridge.date(request.hasFeeStartDate(), request.getFeeStartDate(), "fee_start_date"));
        changes.setPaymentDueDate(bridge.date(
                request.hasPaymentDueDate(), request.getPaymentDueDate(), "payment_due_date"));
        changes.setPaymentStatus(bridge.choice(
                request.hasPaymentStatus(), request.getPaymentStatus(),
                ApplicationUpdate.PaymentStatusEnum::fromValue));
        changes.setRemarks(ProtoBridge.text(request.hasRemarks(), request.getRemarks()));
        changes.setPaymentStatusNote(ProtoBridge.text(request.hasPaymentStatusNote(), request.getPaymentStatusNote()));
        return bridge.patch(changes);
    }
}
