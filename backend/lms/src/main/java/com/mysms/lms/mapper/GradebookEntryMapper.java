package com.mysms.lms.mapper;

import com.mysms.lms.api.model.GradebookEntry;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.GradebookEntryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.gradebook_entry. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface GradebookEntryMapper {

    @Mapping(target = "pointsPossible", expression = "java(WireValues.decimal(entity.getPointsPossible()))")
    @Mapping(target = "points", expression = "java(WireValues.decimal(entity.getPoints()))")
    @Mapping(target = "autoPoints", expression = "java(WireValues.decimal(entity.getAutoPoints()))")
    @Mapping(target = "manualPoints", expression = "java(WireValues.decimal(entity.getManualPoints()))")
    @Mapping(target = "peerPoints", expression = "java(WireValues.decimal(entity.getPeerPoints()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : GradebookEntry.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    GradebookEntry toDto(GradebookEntryEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "pointsPossible", ignore = true)
    @Mapping(target = "points", ignore = true)
    @Mapping(target = "autoPoints", ignore = true)
    @Mapping(target = "manualPoints", ignore = true)
    @Mapping(target = "peerPoints", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "gradedBy", ignore = true)
    @Mapping(target = "gradedAt", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    GradebookEntryEntity toEntity(GradebookEntry dto);
}
