package com.mysms.students.admission.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.admission.api.model.ApplicationGuardianUpdate;

/**
 * UpdateApplicationGuardian over gRPC (JAVA-CODING-RULES §7.2): the {@code admission.v1.ApplicationGuardianUpdate}
 * message as REST's {@link ApplicationGuardianUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ApplicationGuardianProtoBridge {

    private ApplicationGuardianProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ApplicationGuardianUpdate changes(
            ProtoBridge bridge, com.mysms.admission.v1.ApplicationGuardianUpdate request) {
        ApplicationGuardianUpdate changes = new ApplicationGuardianUpdate();
        changes.setRelationship(bridge.choice(
                request.hasRelationship(), request.getRelationship(),
                ApplicationGuardianUpdate.RelationshipEnum::fromValue));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setQualificationId(bridge.uuid(
                request.hasQualificationId(), request.getQualificationId(), "qualification_id"));
        changes.setDesignation(ProtoBridge.text(request.hasDesignation(), request.getDesignation()));
        changes.setOccupation(ProtoBridge.text(request.hasOccupation(), request.getOccupation()));
        changes.setOrganisation(ProtoBridge.text(request.hasOrganisation(), request.getOrganisation()));
        changes.setAnnualIncome(ProtoBridge.text(request.hasAnnualIncome(), request.getAnnualIncome()));
        changes.setMobile(ProtoBridge.text(request.hasMobile(), request.getMobile()));
        changes.setEmail(ProtoBridge.text(request.hasEmail(), request.getEmail()));
        changes.setNationalId(ProtoBridge.text(request.hasNationalId(), request.getNationalId()));
        changes.setPhotoFileId(bridge.uuid(request.hasPhotoFileId(), request.getPhotoFileId(), "photo_file_id"));
        return bridge.patch(changes);
    }
}
