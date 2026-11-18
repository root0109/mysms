package com.mysms.lms.mapper;

import com.mysms.lms.api.model.ForumTopic;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ForumTopicEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.forum_topic to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface ForumTopicMapper {

    @Mapping(target = "topicType", expression =
            "java(entity.getTopicType() == null ? null : ForumTopic.TopicTypeEnum.fromValue(entity.getTopicType()))")
    @Mapping(target = "pointsPossible", expression = "java(WireValues.decimal(entity.getPointsPossible()))")
    @Mapping(target = "gradeWeightPercent", expression = "java(WireValues.decimal(entity.getGradeWeightPercent()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ForumTopic toDto(ForumTopicEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "topicType", expression =
            "java(dto.getTopicType() == null ? null : dto.getTopicType().getValue())")
    @Mapping(target = "pointsPossible", expression = "java(WireValues.decimal(dto.getPointsPossible()))")
    @Mapping(target = "gradeWeightPercent", expression = "java(WireValues.decimal(dto.getGradeWeightPercent()))")
    ForumTopicEntity toEntity(ForumTopic dto);
}
