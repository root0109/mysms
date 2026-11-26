package com.mysms.learning.lms.mapper;

import com.mysms.core.web.Etags;
import com.mysms.learning.lms.api.model.CohortActivity;
import com.mysms.learning.lms.domain.ContentItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity → contract DTO of a cohort activity. A cohort activity is stored as the cohort's own lms.content_item row of
 * kind quiz, assignment or exercise, so this mapper reads that row; the service builds the row on the way in, where the
 * item kind follows from the reference.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface CohortActivityMapper {

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CohortActivity toDto(ContentItemEntity entity);
}
