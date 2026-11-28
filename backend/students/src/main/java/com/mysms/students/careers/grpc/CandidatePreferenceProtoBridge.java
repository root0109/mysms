package com.mysms.students.careers.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.careers.api.model.CandidatePreferenceUpdate;

/**
 * UpdateCandidatePreference over gRPC (JAVA-CODING-RULES §7.2): the {@code careers.v1.CandidatePreferenceUpdate}
 * message as REST's {@link CandidatePreferenceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CandidatePreferenceProtoBridge {

    private CandidatePreferenceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CandidatePreferenceUpdate changes(
            ProtoBridge bridge, com.mysms.careers.v1.CandidatePreferenceUpdate request) {
        CandidatePreferenceUpdate changes = new CandidatePreferenceUpdate();
        changes.setPersonId(bridge.uuid(request.hasPersonId(), request.getPersonId(), "person_id"));
        changes.setOpenTo(bridge.choice(
                request.hasOpenTo(), request.getOpenTo(), CandidatePreferenceUpdate.OpenToEnum::fromValue));
        changes.setPreferredLocation(ProtoBridge.text(request.hasPreferredLocation(), request.getPreferredLocation()));
        changes.setDreamCompanies(ProtoBridge.text(request.hasDreamCompanies(), request.getDreamCompanies()));
        changes.setAttire(bridge.choice(
                request.hasAttire(), request.getAttire(), CandidatePreferenceUpdate.AttireEnum::fromValue));
        changes.setCollaboration(bridge.choice(
                request.hasCollaboration(), request.getCollaboration(),
                CandidatePreferenceUpdate.CollaborationEnum::fromValue));
        changes.setRoleClarity(bridge.choice(
                request.hasRoleClarity(), request.getRoleClarity(),
                CandidatePreferenceUpdate.RoleClarityEnum::fromValue));
        changes.setLocationPreference(bridge.choice(
                request.hasLocationPreference(), request.getLocationPreference(),
                CandidatePreferenceUpdate.LocationPreferenceEnum::fromValue));
        changes.setWorkHours(bridge.choice(
                request.hasWorkHours(), request.getWorkHours(), CandidatePreferenceUpdate.WorkHoursEnum::fromValue));
        changes.setCompanyType(bridge.choice(
                request.hasCompanyType(), request.getCompanyType(),
                CandidatePreferenceUpdate.CompanyTypeEnum::fromValue));
        return bridge.patch(changes);
    }
}
