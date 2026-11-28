package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LmsCourseUpdate;

/**
 * UpdateLmsCourse over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LmsCourseUpdate} message as REST's {@link
 * LmsCourseUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LmsCourseProtoBridge {

    private LmsCourseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LmsCourseUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.LmsCourseUpdate request) {
        LmsCourseUpdate changes = new LmsCourseUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setShortIntroduction(ProtoBridge.text(request.hasShortIntroduction(), request.getShortIntroduction()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setImageFileId(bridge.uuid(request.hasImageFileId(), request.getImageFileId(), "image_file_id"));
        changes.setVideoUrl(ProtoBridge.text(request.hasVideoUrl(), request.getVideoUrl()));
        changes.setCategoryId(bridge.uuid(request.hasCategoryId(), request.getCategoryId(), "category_id"));
        changes.setPublishedOn(bridge.date(request.hasPublishedOn(), request.getPublishedOn(), "published_on"));
        changes.setIsUpcoming(ProtoBridge.flag(request.hasIsUpcoming(), request.getIsUpcoming()));
        changes.setIsFeatured(ProtoBridge.flag(request.hasIsFeatured(), request.getIsFeatured()));
        changes.setDisableSelfLearning(ProtoBridge.flag(
                request.hasDisableSelfLearning(), request.getDisableSelfLearning()));
        changes.setEnforceLessonOrder(ProtoBridge.flag(
                request.hasEnforceLessonOrder(), request.getEnforceLessonOrder()));
        changes.setCertificationEnabled(ProtoBridge.flag(
                request.hasCertificationEnabled(), request.getCertificationEnabled()));
        changes.setPaidCertificate(ProtoBridge.flag(request.hasPaidCertificate(), request.getPaidCertificate()));
        changes.setDefaultEvaluatorId(bridge.uuid(
                request.hasDefaultEvaluatorId(), request.getDefaultEvaluatorId(), "default_evaluator_id"));
        changes.setIsPaid(ProtoBridge.flag(request.hasIsPaid(), request.getIsPaid()));
        changes.setPrice(ProtoBridge.text(request.hasPrice(), request.getPrice()));
        changes.setCurrencyId(bridge.uuid(request.hasCurrencyId(), request.getCurrencyId(), "currency_id"));
        changes.setPriceUsd(ProtoBridge.text(request.hasPriceUsd(), request.getPriceUsd()));
        changes.setCardGradient(bridge.choice(
                request.hasCardGradient(), request.getCardGradient(), LmsCourseUpdate.CardGradientEnum::fromValue));
        changes.setTimezone(ProtoBridge.text(request.hasTimezone(), request.getTimezone()));
        changes.setPublishNotificationSent(ProtoBridge.flag(
                request.hasPublishNotificationSent(), request.getPublishNotificationSent()));
        changes.setEnrollmentCount(ProtoBridge.number(request.hasEnrollmentCount(), request.getEnrollmentCount()));
        changes.setLessonCount(ProtoBridge.number(request.hasLessonCount(), request.getLessonCount()));
        changes.setAverageRating(ProtoBridge.text(request.hasAverageRating(), request.getAverageRating()));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setStateStandardCode(ProtoBridge.text(request.hasStateStandardCode(), request.getStateStandardCode()));
        changes.setCredits(ProtoBridge.text(request.hasCredits(), request.getCredits()));
        changes.setPrerequisites(ProtoBridge.text(request.hasPrerequisites(), request.getPrerequisites()));
        changes.setCorequisites(ProtoBridge.text(request.hasCorequisites(), request.getCorequisites()));
        changes.setEffectiveFrom(bridge.date(request.hasEffectiveFrom(), request.getEffectiveFrom(), "effective_from"));
        changes.setLifecycleStatus(bridge.choice(
                request.hasLifecycleStatus(), request.getLifecycleStatus(),
                LmsCourseUpdate.LifecycleStatusEnum::fromValue));
        changes.setDeactivatedOn(bridge.date(request.hasDeactivatedOn(), request.getDeactivatedOn(), "deactivated_on"));
        changes.setVersionMajor(ProtoBridge.number(request.hasVersionMajor(), request.getVersionMajor()));
        changes.setVersionMinor(ProtoBridge.number(request.hasVersionMinor(), request.getVersionMinor()));
        changes.setPreviousVersionId(bridge.uuid(
                request.hasPreviousVersionId(), request.getPreviousVersionId(), "previous_version_id"));
        changes.setRootCourseId(bridge.uuid(request.hasRootCourseId(), request.getRootCourseId(), "root_course_id"));
        changes.setAuthorComments(ProtoBridge.text(request.hasAuthorComments(), request.getAuthorComments()));
        changes.setExternalRef(ProtoBridge.text(request.hasExternalRef(), request.getExternalRef()));
        changes.setNominalDuration(ProtoBridge.number(request.hasNominalDuration(), request.getNominalDuration()));
        changes.setDeliveryModality(bridge.choice(
                request.hasDeliveryModality(), request.getDeliveryModality(),
                LmsCourseUpdate.DeliveryModalityEnum::fromValue));
        changes.setIsShared(ProtoBridge.flag(request.hasIsShared(), request.getIsShared()));
        changes.setContentUpdatedAt(ProtoBridge.time(request.hasContentUpdatedAt(), request.getContentUpdatedAt()));
        changes.setGradeDisplay(bridge.choice(
                request.hasGradeDisplay(), request.getGradeDisplay(), LmsCourseUpdate.GradeDisplayEnum::fromValue));
        changes.setGradingSchemeId(bridge.uuid(
                request.hasGradingSchemeId(), request.getGradingSchemeId(), "grading_scheme_id"));
        changes.setIsInCatalog(ProtoBridge.flag(request.hasIsInCatalog(), request.getIsInCatalog()));
        changes.setCatalogDescription(ProtoBridge.text(
                request.hasCatalogDescription(), request.getCatalogDescription()));
        changes.setCatalogVisibleFrom(ProtoBridge.time(
                request.hasCatalogVisibleFrom(), request.getCatalogVisibleFrom()));
        changes.setCatalogVisibleUntil(ProtoBridge.time(
                request.hasCatalogVisibleUntil(), request.getCatalogVisibleUntil()));
        changes.setOutline(ProtoBridge.text(request.hasOutline(), request.getOutline()));
        changes.setWorkloadHours(ProtoBridge.number(request.hasWorkloadHours(), request.getWorkloadHours()));
        changes.setCatalogStatus(bridge.choice(
                request.hasCatalogStatus(), request.getCatalogStatus(), LmsCourseUpdate.CatalogStatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
