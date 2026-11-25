package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.MyNominee;
import com.mysms.privacy.domain.NomineeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps privacy.nominee to the principal's contract DTO. The masked name and contact come from the stored envelopes,
 * which only the nominee service opens (core FieldCipher); there is no way in: a nominee is built by the service from
 * the principal's request.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface NomineeMapper {

    @Mapping(target = "nomineeNameMasked", ignore = true)
    @Mapping(target = "nomineeContactMasked", ignore = true)
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    MyNominee toMine(NomineeEntity entity);
}
