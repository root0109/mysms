package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.Breach;
import com.mysms.privacy.api.model.BreachNotification;
import com.mysms.privacy.domain.BreachEntity;
import com.mysms.privacy.domain.BreachNotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps privacy.breach and its notices to the contract. There is no way in: the breach service builds rows from a
 * report and the steps, never from a client's copy. The clock fields and the notices depend on the read, which the
 * service sets.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface BreachMapper {

    @Mapping(target = "status", expression = "java(Breach.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "overdue", ignore = true)
    @Mapping(target = "hoursRemaining", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Breach toDto(BreachEntity entity);

    @Mapping(target = "audience",
            expression = "java(BreachNotification.AudienceEnum.fromValue(entity.getAudience()))")
    @Mapping(target = "kind", expression = "java(BreachNotification.KindEnum.fromValue(entity.getKind()))")
    BreachNotification toDto(BreachNotificationEntity entity);
}
