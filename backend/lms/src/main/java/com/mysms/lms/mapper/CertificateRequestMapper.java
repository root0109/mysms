package com.mysms.lms.mapper;

import com.mysms.lms.api.model.CertificateRequest;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.CertificateRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.certificate_request. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface CertificateRequestMapper {

    @Mapping(target = "meetingLinkMasked", ignore = true)
    @Mapping(target = "weekday", expression =
            "java(entity.getWeekday() == null ? null : CertificateRequest.WeekdayEnum.fromValue(entity.getWeekday()))")
    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(entity.getEndTime()))")
    @Mapping(target = "meetingLink", ignore = true)
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : CertificateRequest.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CertificateRequest toDto(CertificateRequestEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "weekday", expression = "java(dto.getWeekday() == null ? null : dto.getWeekday().getValue())")
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(dto.getEndTime()))")
    @Mapping(target = "meetingLinkEnc", ignore = true)
    @Mapping(target = "status", ignore = true)
    CertificateRequestEntity toEntity(CertificateRequest dto);
}
