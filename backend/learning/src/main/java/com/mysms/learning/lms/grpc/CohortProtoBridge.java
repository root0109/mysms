package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortUpdate;

/**
 * UpdateCohort over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortUpdate} message as REST's {@link
 * CohortUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CohortProtoBridge {

    private CohortProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortUpdate request) {
        CohortUpdate changes = new CohortUpdate();
        changes.setClassSubjectId(bridge.uuid(
                request.hasClassSubjectId(), request.getClassSubjectId(), "class_subject_id"));
        changes.setBatchId(bridge.uuid(request.hasBatchId(), request.getBatchId(), "batch_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setDetails(ProtoBridge.text(request.hasDetails(), request.getDetails()));
        changes.setCategoryId(bridge.uuid(request.hasCategoryId(), request.getCategoryId(), "category_id"));
        changes.setMedium(bridge.choice(request.hasMedium(), request.getMedium(), CohortUpdate.MediumEnum::fromValue));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setEndTime(ProtoBridge.text(request.hasEndTime(), request.getEndTime()));
        changes.setTimezone(ProtoBridge.text(request.hasTimezone(), request.getTimezone()));
        changes.setSeatCount(ProtoBridge.number(request.hasSeatCount(), request.getSeatCount()));
        changes.setAllowSelfEnrollment(ProtoBridge.flag(
                request.hasAllowSelfEnrollment(), request.getAllowSelfEnrollment()));
        changes.setAllowFutureAccess(ProtoBridge.flag(request.hasAllowFutureAccess(), request.getAllowFutureAccess()));
        changes.setIsPaid(ProtoBridge.flag(request.hasIsPaid(), request.getIsPaid()));
        changes.setPrice(ProtoBridge.text(request.hasPrice(), request.getPrice()));
        changes.setCurrencyId(bridge.uuid(request.hasCurrencyId(), request.getCurrencyId(), "currency_id"));
        changes.setPriceUsd(ProtoBridge.text(request.hasPriceUsd(), request.getPriceUsd()));
        changes.setShowLiveClass(ProtoBridge.flag(request.hasShowLiveClass(), request.getShowLiveClass()));
        changes.setEvaluationEnabled(ProtoBridge.flag(request.hasEvaluationEnabled(), request.getEvaluationEnabled()));
        changes.setEvaluationEndDate(bridge.date(
                request.hasEvaluationEndDate(), request.getEvaluationEndDate(), "evaluation_end_date"));
        changes.setCertificationEnabled(ProtoBridge.flag(
                request.hasCertificationEnabled(), request.getCertificationEnabled()));
        changes.setConferencingProvider(bridge.choice(
                request.hasConferencingProvider(), request.getConferencingProvider(),
                CohortUpdate.ConferencingProviderEnum::fromValue));
        changes.setConferencingAccountId(bridge.uuid(
                request.hasConferencingAccountId(), request.getConferencingAccountId(), "conferencing_account_id"));
        changes.setTimetableTemplateId(bridge.uuid(
                request.hasTimetableTemplateId(), request.getTimetableTemplateId(), "timetable_template_id"));
        changes.setConfirmationTemplateId(bridge.uuid(
                request.hasConfirmationTemplateId(), request.getConfirmationTemplateId(), "confirmation_template_id"));
        changes.setMetaImageFileId(bridge.uuid(
                request.hasMetaImageFileId(), request.getMetaImageFileId(), "meta_image_file_id"));
        changes.setIntroVideoFileId(bridge.uuid(
                request.hasIntroVideoFileId(), request.getIntroVideoFileId(), "intro_video_file_id"));
        changes.setCustomComponent(ProtoBridge.text(request.hasCustomComponent(), request.getCustomComponent()));
        changes.setCustomScript(ProtoBridge.text(request.hasCustomScript(), request.getCustomScript()));
        changes.setPublishNotificationSent(ProtoBridge.flag(
                request.hasPublishNotificationSent(), request.getPublishNotificationSent()));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setAcademicTermId(bridge.uuid(
                request.hasAcademicTermId(), request.getAcademicTermId(), "academic_term_id"));
        changes.setProgramId(bridge.uuid(request.hasProgramId(), request.getProgramId(), "program_id"));
        changes.setExternalRef(ProtoBridge.text(request.hasExternalRef(), request.getExternalRef()));
        changes.setLifecycleStatus(bridge.choice(
                request.hasLifecycleStatus(), request.getLifecycleStatus(),
                CohortUpdate.LifecycleStatusEnum::fromValue));
        changes.setStaffAccessFrom(ProtoBridge.time(request.hasStaffAccessFrom(), request.getStaffAccessFrom()));
        changes.setStaffAccessUntil(ProtoBridge.time(request.hasStaffAccessUntil(), request.getStaffAccessUntil()));
        changes.setStudentAccessFrom(ProtoBridge.time(request.hasStudentAccessFrom(), request.getStudentAccessFrom()));
        changes.setStudentAccessUntil(ProtoBridge.time(
                request.hasStudentAccessUntil(), request.getStudentAccessUntil()));
        changes.setHasSchedule(ProtoBridge.flag(request.hasHasSchedule(), request.getHasSchedule()));
        changes.setStaffCanEditSchedule(ProtoBridge.flag(
                request.hasStaffCanEditSchedule(), request.getStaffCanEditSchedule()));
        changes.setGradeDisplay(bridge.choice(
                request.hasGradeDisplay(), request.getGradeDisplay(), CohortUpdate.GradeDisplayEnum::fromValue));
        changes.setGradingSchemeId(bridge.uuid(
                request.hasGradingSchemeId(), request.getGradingSchemeId(), "grading_scheme_id"));
        changes.setEnrollmentOpensAt(ProtoBridge.time(request.hasEnrollmentOpensAt(), request.getEnrollmentOpensAt()));
        changes.setEnrollmentClosesAt(ProtoBridge.time(
                request.hasEnrollmentClosesAt(), request.getEnrollmentClosesAt()));
        return bridge.patch(changes);
    }
}
