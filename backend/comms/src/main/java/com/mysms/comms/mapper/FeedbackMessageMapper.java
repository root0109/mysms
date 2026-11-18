package com.mysms.comms.mapper;

import com.mysms.comms.domain.FeedbackMessageEntity;
import com.mysms.comms.api.model.FeedbackMessage;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.feedback_message to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface FeedbackMessageMapper {

    @Mapping(target = "category", expression =
            "java(entity.getCategory() == null ? null : FeedbackMessage.CategoryEnum.fromValue(entity.getCategory()))")
    @Mapping(target = "reviewStatus", expression =
            "java(entity.getReviewStatus() == null ? null : "
                    + "FeedbackMessage.ReviewStatusEnum.fromValue(entity.getReviewStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    FeedbackMessage toDto(FeedbackMessageEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "threadId", ignore = true)
    @Mapping(target = "category", expression = "java(dto.getCategory() == null ? null : dto.getCategory().getValue())")
    @Mapping(target = "reviewStatus", expression =
            "java(dto.getReviewStatus() == null ? null : dto.getReviewStatus().getValue())")
    FeedbackMessageEntity toEntity(FeedbackMessage dto);
}
