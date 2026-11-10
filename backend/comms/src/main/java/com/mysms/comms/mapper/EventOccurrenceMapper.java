package com.mysms.comms.mapper;

import com.mysms.comms.domain.EventOccurrenceEntity;
import com.mysms.comms.api.model.EventOccurrence;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for comms.event_occurrence. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface EventOccurrenceMapper {

    @Mapping(target = "responseStatus", expression =
            "java(entity.getResponseStatus() == null ? null : "
                    + "EventOccurrence.ResponseStatusEnum.fromValue(entity.getResponseStatus()))")
    @Mapping(target = "importance", expression =
            "java(entity.getImportance() == null ? null : "
                    + "EventOccurrence.ImportanceEnum.fromValue(entity.getImportance()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    EventOccurrence toDto(EventOccurrenceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "responseStatus", expression =
            "java(dto.getResponseStatus() == null ? null : dto.getResponseStatus().getValue())")
    @Mapping(target = "importance", expression =
            "java(dto.getImportance() == null ? null : dto.getImportance().getValue())")
    EventOccurrenceEntity toEntity(EventOccurrence dto);
}
