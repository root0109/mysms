package com.mysms.lms.mapper;

import com.mysms.lms.api.model.RelatedMaterial;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.RelatedMaterialEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.related_material to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface RelatedMaterialMapper {

    @Mapping(target = "contentLength", expression = "java(WireValues.int64(entity.getContentLength()))")
    @Mapping(target = "videoLengthSeconds", expression = "java(WireValues.int64(entity.getVideoLengthSeconds()))")
    @Mapping(target = "languageComplexity", expression = "java(WireValues.decimal(entity.getLanguageComplexity()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    RelatedMaterial toDto(RelatedMaterialEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "ebookId", ignore = true)
    @Mapping(target = "contentLength", expression = "java(WireValues.int64(dto.getContentLength()))")
    @Mapping(target = "videoLengthSeconds", expression = "java(WireValues.int64(dto.getVideoLengthSeconds()))")
    @Mapping(target = "languageComplexity", expression = "java(WireValues.decimal(dto.getLanguageComplexity()))")
    RelatedMaterialEntity toEntity(RelatedMaterial dto);
}
