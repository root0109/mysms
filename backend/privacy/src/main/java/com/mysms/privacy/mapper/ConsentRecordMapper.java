package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.ConsentRecord;
import com.mysms.privacy.domain.ConsentRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps privacy.consent_record to its contract DTO. There is no way in: a consent record is only ever built by the
 * consent service from a decision request, never from a client's copy of the record.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface ConsentRecordMapper {

    @Mapping(target = "giverCapacity",
            expression = "java(ConsentRecord.GiverCapacityEnum.fromValue(entity.getGiverCapacity()))")
    @Mapping(target = "scope", expression = "java(ConsentRecord.ScopeEnum.fromValue(entity.getScope()))")
    @Mapping(target = "decision", expression = "java(ConsentRecord.DecisionEnum.fromValue(entity.getDecision()))")
    @Mapping(target = "channel", expression = "java(ConsentRecord.ChannelEnum.fromValue(entity.getChannel()))")
    @Mapping(target = "verificationStatus",
            expression = "java(ConsentRecord.VerificationStatusEnum.fromValue(entity.getVerificationStatus()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ConsentRecord toDto(ConsentRecordEntity entity);
}
