package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.RetentionPolicy;
import com.mysms.privacy.api.model.RetentionRun;
import com.mysms.privacy.repository.RetentionPolicyView;
import com.mysms.privacy.repository.RetentionRunView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the retention catalogue with the tenant's setting, and the tenant's retention runs, to the contract. There is
 * no way in: the tenant's setting is written field by field by the retention service.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface RetentionMapper {

    @Mapping(target = "trigger", expression = "java(RetentionPolicy.TriggerEnum.fromValue(view.trigger()))")
    @Mapping(target = "action", expression = "java(RetentionPolicy.ActionEnum.fromValue(view.action()))")
    @Mapping(target = "etag", expression = "java(Etags.of(view.rowVersion()))")
    RetentionPolicy toDto(RetentionPolicyView view);

    @Mapping(target = "action", expression = "java(RetentionRun.ActionEnum.fromValue(view.action()))")
    @Mapping(target = "status", expression = "java(RetentionRun.StatusEnum.fromValue(view.status()))")
    RetentionRun toDto(RetentionRunView view);
}
