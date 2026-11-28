package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.PersonUpdate;

/**
 * UpdatePerson over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.PersonUpdate} message
 * as REST's {@link PersonUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PersonProtoBridge {

    private PersonProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PersonUpdate changes(ProtoBridge bridge, com.mysms.people.v1.PersonUpdate request) {
        PersonUpdate changes = new PersonUpdate();
        changes.setHonorificId(bridge.uuid(request.hasHonorificId(), request.getHonorificId(), "honorific_id"));
        changes.setFirstName(ProtoBridge.text(request.hasFirstName(), request.getFirstName()));
        changes.setMiddleName(ProtoBridge.text(request.hasMiddleName(), request.getMiddleName()));
        changes.setLastName(ProtoBridge.text(request.hasLastName(), request.getLastName()));
        changes.setDisplayName(ProtoBridge.text(request.hasDisplayName(), request.getDisplayName()));
        changes.setDateOfBirth(bridge.date(request.hasDateOfBirth(), request.getDateOfBirth(), "date_of_birth"));
        changes.setGender(bridge.choice(request.hasGender(), request.getGender(), PersonUpdate.GenderEnum::fromValue));
        changes.setBloodGroup(bridge.choice(
                request.hasBloodGroup(), request.getBloodGroup(), PersonUpdate.BloodGroupEnum::fromValue));
        changes.setMaritalStatus(bridge.choice(
                request.hasMaritalStatus(), request.getMaritalStatus(), PersonUpdate.MaritalStatusEnum::fromValue));
        changes.setBirthPlace(ProtoBridge.text(request.hasBirthPlace(), request.getBirthPlace()));
        changes.setNativePlace(ProtoBridge.text(request.hasNativePlace(), request.getNativePlace()));
        changes.setNationalityCountryId(bridge.uuid(
                request.hasNationalityCountryId(), request.getNationalityCountryId(), "nationality_country_id"));
        changes.setMotherTongue(ProtoBridge.text(request.hasMotherTongue(), request.getMotherTongue()));
        changes.setReligion(ProtoBridge.text(request.hasReligion(), request.getReligion()));
        changes.setSocialCategory(ProtoBridge.text(request.hasSocialCategory(), request.getSocialCategory()));
        changes.setSocialSubCategory(ProtoBridge.text(request.hasSocialSubCategory(), request.getSocialSubCategory()));
        changes.setNationalIdType(ProtoBridge.text(request.hasNationalIdType(), request.getNationalIdType()));
        changes.setNationalId(ProtoBridge.text(request.hasNationalId(), request.getNationalId()));
        changes.setVisaInfo(ProtoBridge.text(request.hasVisaInfo(), request.getVisaInfo()));
        changes.setPrimaryEmail(ProtoBridge.text(request.hasPrimaryEmail(), request.getPrimaryEmail()));
        changes.setPrimaryPhone(ProtoBridge.text(request.hasPrimaryPhone(), request.getPrimaryPhone()));
        changes.setAlternatePhone(ProtoBridge.text(request.hasAlternatePhone(), request.getAlternatePhone()));
        changes.setPhotoFileId(bridge.uuid(request.hasPhotoFileId(), request.getPhotoFileId(), "photo_file_id"));
        changes.setRemarks(ProtoBridge.text(request.hasRemarks(), request.getRemarks()));
        changes.setWorkPhone(ProtoBridge.text(request.hasWorkPhone(), request.getWorkPhone()));
        changes.setWebsiteUrl(ProtoBridge.text(request.hasWebsiteUrl(), request.getWebsiteUrl()));
        changes.setAboutMe(ProtoBridge.text(request.hasAboutMe(), request.getAboutMe()));
        return bridge.patch(changes);
    }
}
