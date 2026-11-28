package com.mysms.comms.grpc;

import com.mysms.comms.api.model.EventUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateEvent over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.EventUpdate} message
 * as REST's {@link EventUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EventProtoBridge {

    private EventProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EventUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.EventUpdate request) {
        EventUpdate changes = new EventUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setAudience(bridge.choice(
                request.hasAudience(), request.getAudience(), EventUpdate.AudienceEnum::fromValue));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setAudienceLabel(ProtoBridge.text(request.hasAudienceLabel(), request.getAudienceLabel()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setCategory(bridge.choice(
                request.hasCategory(), request.getCategory(), EventUpdate.CategoryEnum::fromValue));
        changes.setLocation(ProtoBridge.text(request.hasLocation(), request.getLocation()));
        changes.setOrganisedBy(ProtoBridge.text(request.hasOrganisedBy(), request.getOrganisedBy()));
        changes.setImageFileId(bridge.uuid(request.hasImageFileId(), request.getImageFileId(), "image_file_id"));
        changes.setColor(ProtoBridge.text(request.hasColor(), request.getColor()));
        changes.setStartAt(ProtoBridge.time(request.hasStartAt(), request.getStartAt()));
        changes.setEndAt(ProtoBridge.time(request.hasEndAt(), request.getEndAt()));
        changes.setIsAllDay(ProtoBridge.flag(request.hasIsAllDay(), request.getIsAllDay()));
        changes.setRepeatCount(ProtoBridge.number(request.hasRepeatCount(), request.getRepeatCount()));
        changes.setRepeatInterval(ProtoBridge.number(request.hasRepeatInterval(), request.getRepeatInterval()));
        changes.setRepeatUnit(bridge.choice(
                request.hasRepeatUnit(), request.getRepeatUnit(), EventUpdate.RepeatUnitEnum::fromValue));
        changes.setUrl(ProtoBridge.text(request.hasUrl(), request.getUrl()));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setOwnerUserId(bridge.uuid(request.hasOwnerUserId(), request.getOwnerUserId(), "owner_user_id"));
        changes.setRecurrenceRule(ProtoBridge.text(request.hasRecurrenceRule(), request.getRecurrenceRule()));
        changes.setForAllMembers(ProtoBridge.flag(request.hasForAllMembers(), request.getForAllMembers()));
        changes.setStaffCanEdit(ProtoBridge.flag(request.hasStaffCanEdit(), request.getStaffCanEdit()));
        changes.setImportance(bridge.choice(
                request.hasImportance(), request.getImportance(), EventUpdate.ImportanceEnum::fromValue));
        return bridge.patch(changes);
    }
}
