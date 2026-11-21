package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.GrievanceContact;
import com.mysms.privacy.api.model.NoticeVersion;
import com.mysms.privacy.domain.NoticeVersionEntity;
import com.mysms.privacy.model.GrievanceOfficer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps privacy.notice_version to and from its contract DTO. The way in ignores id, tenant, the audit set and the
 * workflow's output-only fields (version, status, publication, successor), which only the service sets.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface NoticeVersionMapper {

    @Mapping(target = "status", expression = "java(NoticeVersion.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    NoticeVersion toDto(NoticeVersionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "noticeId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "supersededById", ignore = true)
    NoticeVersionEntity toEntity(NoticeVersion dto);

    GrievanceContact toContract(GrievanceOfficer officer);

    GrievanceOfficer toOfficer(GrievanceContact contact);
}
