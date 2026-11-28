package com.mysms.students.careers.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.careers.api.model.JobOpeningUpdate;

/**
 * UpdateJobOpening over gRPC (JAVA-CODING-RULES §7.2): the {@code careers.v1.JobOpeningUpdate} message as REST's {@link
 * JobOpeningUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class JobOpeningProtoBridge {

    private JobOpeningProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static JobOpeningUpdate changes(ProtoBridge bridge, com.mysms.careers.v1.JobOpeningUpdate request) {
        JobOpeningUpdate changes = new JobOpeningUpdate();
        changes.setPostedByUserId(bridge.uuid(
                request.hasPostedByUserId(), request.getPostedByUserId(), "posted_by_user_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setLocation(ProtoBridge.text(request.hasLocation(), request.getLocation()));
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        changes.setEmploymentType(bridge.choice(
                request.hasEmploymentType(), request.getEmploymentType(),
                JobOpeningUpdate.EmploymentTypeEnum::fromValue));
        changes.setWorkMode(bridge.choice(
                request.hasWorkMode(), request.getWorkMode(), JobOpeningUpdate.WorkModeEnum::fromValue));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setCompanyName(ProtoBridge.text(request.hasCompanyName(), request.getCompanyName()));
        changes.setCompanyWebsite(ProtoBridge.text(request.hasCompanyWebsite(), request.getCompanyWebsite()));
        changes.setCompanyEmail(ProtoBridge.text(request.hasCompanyEmail(), request.getCompanyEmail()));
        changes.setCompanyLogoFileId(bridge.uuid(
                request.hasCompanyLogoFileId(), request.getCompanyLogoFileId(), "company_logo_file_id"));
        return bridge.patch(changes);
    }
}
