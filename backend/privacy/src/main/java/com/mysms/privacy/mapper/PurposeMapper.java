package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.Purpose;
import com.mysms.privacy.repository.PurposeView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps a catalogue purpose with the tenant's setting to its contract DTO; the ETag is the setting's version. */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface PurposeMapper {

    @Mapping(target = "scopeKind", expression = "java(Purpose.ScopeKindEnum.fromValue(view.scopeKind()))")
    @Mapping(target = "fiduciary", expression = "java(Purpose.FiduciaryEnum.fromValue(view.fiduciary()))")
    @Mapping(target = "majorityGrace",
            expression = "java(Purpose.MajorityGraceEnum.fromValue(view.majorityGrace()))")
    @Mapping(target = "status", expression = "java(Purpose.StatusEnum.fromValue(view.status()))")
    @Mapping(target = "maxScope", expression = "java(view.maxScope() == null ? null : "
            + "Purpose.MaxScopeEnum.fromValue(view.maxScope()))")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(view.rowVersion()))")
    Purpose toDto(PurposeView view);
}
