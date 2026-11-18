package com.mysms.lms.mapper;

import com.mysms.lms.api.model.ForumSection;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ForumSectionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.forum_section to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface ForumSectionMapper {

    @Mapping(target = "sectionType", expression =
            "java(entity.getSectionType() == null ? null : "
                    + "ForumSection.SectionTypeEnum.fromValue(entity.getSectionType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ForumSection toDto(ForumSectionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "sectionType", expression =
            "java(dto.getSectionType() == null ? null : dto.getSectionType().getValue())")
    ForumSectionEntity toEntity(ForumSection dto);
}
