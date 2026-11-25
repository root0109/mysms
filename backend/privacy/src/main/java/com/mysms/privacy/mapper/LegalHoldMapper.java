package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.ActiveLegalHold;
import com.mysms.privacy.api.model.LegalHold;
import com.mysms.privacy.domain.LegalHoldEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps privacy.legal_hold to the contract: the staff's view of a hold and a module's view of what an active hold
 * covers. There is no way in: the legal-hold service builds rows from a placement, never from a client's copy.
 * Whether a review is due depends on the day of the read, which the service sets.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface LegalHoldMapper {

    @Mapping(target = "reasonCode", expression = "java(LegalHold.ReasonCodeEnum.fromValue(entity.getReasonCode()))")
    @Mapping(target = "scope", expression = "java(LegalHold.ScopeEnum.fromValue(entity.getScope()))")
    @Mapping(target = "status", expression = "java(entity.getReleasedAt() == null ? LegalHold.StatusEnum.ACTIVE"
            + " : LegalHold.StatusEnum.RELEASED)")
    @Mapping(target = "reviewDue", ignore = true)
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LegalHold toDto(LegalHoldEntity entity);

    @Mapping(target = "reasonCode",
            expression = "java(ActiveLegalHold.ReasonCodeEnum.fromValue(entity.getReasonCode()))")
    @Mapping(target = "scope", expression = "java(ActiveLegalHold.ScopeEnum.fromValue(entity.getScope()))")
    ActiveLegalHold toActive(LegalHoldEntity entity);
}
