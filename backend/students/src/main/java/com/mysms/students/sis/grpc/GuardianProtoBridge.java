package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.GuardianUpdate;

/**
 * UpdateGuardian over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.GuardianUpdate} message as REST's {@link
 * GuardianUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class GuardianProtoBridge {

    private GuardianProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static GuardianUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.GuardianUpdate request) {
        GuardianUpdate changes = new GuardianUpdate();
        changes.setPersonId(bridge.uuid(request.hasPersonId(), request.getPersonId(), "person_id"));
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setProfession(bridge.choice(
                request.hasProfession(), request.getProfession(), GuardianUpdate.ProfessionEnum::fromValue));
        changes.setSubOccupation(ProtoBridge.text(request.hasSubOccupation(), request.getSubOccupation()));
        changes.setDesignation(ProtoBridge.text(request.hasDesignation(), request.getDesignation()));
        changes.setOrganizationName(ProtoBridge.text(request.hasOrganizationName(), request.getOrganizationName()));
        changes.setAnnualIncome(ProtoBridge.text(request.hasAnnualIncome(), request.getAnnualIncome()));
        changes.setQualificationId(bridge.uuid(
                request.hasQualificationId(), request.getQualificationId(), "qualification_id"));
        changes.setMobile(ProtoBridge.text(request.hasMobile(), request.getMobile()));
        changes.setEmail(ProtoBridge.text(request.hasEmail(), request.getEmail()));
        return bridge.patch(changes);
    }
}
