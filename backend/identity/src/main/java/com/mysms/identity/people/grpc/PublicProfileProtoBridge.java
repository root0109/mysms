package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.PublicProfileUpdate;

/**
 * UpdatePublicProfile over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.PublicProfileUpdate} message
 * as REST's {@link PublicProfileUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PublicProfileProtoBridge {

    private PublicProfileProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PublicProfileUpdate changes(ProtoBridge bridge, com.mysms.people.v1.PublicProfileUpdate request) {
        PublicProfileUpdate changes = new PublicProfileUpdate();
        changes.setHeadline(ProtoBridge.text(request.hasHeadline(), request.getHeadline()));
        changes.setProfession(ProtoBridge.text(request.hasProfession(), request.getProfession()));
        changes.setPersonaCategory(bridge.choice(
                request.hasPersonaCategory(), request.getPersonaCategory(), PublicProfileUpdate.PersonaCategoryEnum::fromValue));
        changes.setCoverImageFileId(bridge.uuid(
                request.hasCoverImageFileId(), request.getCoverImageFileId(), "cover_image_file_id"));
        changes.setCity(ProtoBridge.text(request.hasCity(), request.getCity()));
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        changes.setCollege(ProtoBridge.text(request.hasCollege(), request.getCollege()));
        changes.setBranch(ProtoBridge.text(request.hasBranch(), request.getBranch()));
        changes.setLinkedinUrl(ProtoBridge.text(request.hasLinkedinUrl(), request.getLinkedinUrl()));
        changes.setGithubUrl(ProtoBridge.text(request.hasGithubUrl(), request.getGithubUrl()));
        changes.setTwitterUrl(ProtoBridge.text(request.hasTwitterUrl(), request.getTwitterUrl()));
        changes.setMediumUrl(ProtoBridge.text(request.hasMediumUrl(), request.getMediumUrl()));
        changes.setHidePrivateInfo(ProtoBridge.flag(request.hasHidePrivateInfo(), request.getHidePrivateInfo()));
        return bridge.patch(changes);
    }
}
