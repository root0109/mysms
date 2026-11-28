package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.org.api.model.InstitutionUpdate;

/**
 * UpdateInstitution over gRPC (JAVA-CODING-RULES §7.2): the {@code org.v1.InstitutionUpdate} message
 * as REST's {@link InstitutionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class InstitutionProtoBridge {

    private InstitutionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static InstitutionUpdate changes(ProtoBridge bridge, com.mysms.org.v1.InstitutionUpdate request) {
        InstitutionUpdate changes = new InstitutionUpdate();
        changes.setParentInstitutionId(bridge.uuid(
                request.hasParentInstitutionId(), request.getParentInstitutionId(), "parent_institution_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setSlug(ProtoBridge.text(request.hasSlug(), request.getSlug()));
        changes.setInstitutionType(bridge.choice(
                request.hasInstitutionType(), request.getInstitutionType(), InstitutionUpdate.InstitutionTypeEnum::fromValue));
        changes.setEmail(ProtoBridge.text(request.hasEmail(), request.getEmail()));
        changes.setPhone(ProtoBridge.text(request.hasPhone(), request.getPhone()));
        changes.setAddressLine1(ProtoBridge.text(request.hasAddressLine1(), request.getAddressLine1()));
        changes.setAddressLine2(ProtoBridge.text(request.hasAddressLine2(), request.getAddressLine2()));
        changes.setCityId(bridge.uuid(request.hasCityId(), request.getCityId(), "city_id"));
        changes.setStateProvinceId(bridge.uuid(
                request.hasStateProvinceId(), request.getStateProvinceId(), "state_province_id"));
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        changes.setPostalCode(ProtoBridge.text(request.hasPostalCode(), request.getPostalCode()));
        changes.setTimezone(ProtoBridge.text(request.hasTimezone(), request.getTimezone()));
        changes.setCurrencyId(bridge.uuid(request.hasCurrencyId(), request.getCurrencyId(), "currency_id"));
        changes.setLogoFileId(bridge.uuid(request.hasLogoFileId(), request.getLogoFileId(), "logo_file_id"));
        changes.setSignatureFileId(bridge.uuid(
                request.hasSignatureFileId(), request.getSignatureFileId(), "signature_file_id"));
        changes.setAccreditation(ProtoBridge.text(request.hasAccreditation(), request.getAccreditation()));
        changes.setApprovalAuthority(ProtoBridge.text(request.hasApprovalAuthority(), request.getApprovalAuthority()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), InstitutionUpdate.StatusEnum::fromValue));
        changes.setInstitutionTypeId(bridge.uuid(
                request.hasInstitutionTypeId(), request.getInstitutionTypeId(), "institution_type_id"));
        changes.setThemeId(bridge.uuid(request.hasThemeId(), request.getThemeId(), "theme_id"));
        return bridge.patch(changes);
    }
}
