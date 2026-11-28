package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.WorkExperienceUpdate;

/**
 * UpdateWorkExperience over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.WorkExperienceUpdate} message
 * as REST's {@link WorkExperienceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class WorkExperienceProtoBridge {

    private WorkExperienceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static WorkExperienceUpdate changes(ProtoBridge bridge, com.mysms.people.v1.WorkExperienceUpdate request) {
        WorkExperienceUpdate changes = new WorkExperienceUpdate();
        changes.setExperienceType(bridge.choice(
                request.hasExperienceType(), request.getExperienceType(), WorkExperienceUpdate.ExperienceTypeEnum::fromValue));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setCompany(ProtoBridge.text(request.hasCompany(), request.getCompany()));
        changes.setLocation(ProtoBridge.text(request.hasLocation(), request.getLocation()));
        changes.setFromDate(bridge.date(request.hasFromDate(), request.getFromDate(), "from_date"));
        changes.setToDate(bridge.date(request.hasToDate(), request.getToDate(), "to_date"));
        changes.setIsCurrent(ProtoBridge.flag(request.hasIsCurrent(), request.getIsCurrent()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setCompanyWebsite(ProtoBridge.text(request.hasCompanyWebsite(), request.getCompanyWebsite()));
        changes.setIndustry(ProtoBridge.text(request.hasIndustry(), request.getIndustry()));
        changes.setResponsibilities(ProtoBridge.text(request.hasResponsibilities(), request.getResponsibilities()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
