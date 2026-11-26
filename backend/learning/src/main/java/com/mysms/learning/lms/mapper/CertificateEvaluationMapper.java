package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.CertificateEvaluation;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.CertificateEvaluationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.certificate_evaluation to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface CertificateEvaluationMapper {

    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(entity.getEndTime()))")
    @Mapping(target = "rating", expression = "java(WireValues.decimal(entity.getRating()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : CertificateEvaluation.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CertificateEvaluation toDto(CertificateEvaluationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(dto.getEndTime()))")
    @Mapping(target = "rating", expression = "java(WireValues.decimal(dto.getRating()))")
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    CertificateEvaluationEntity toEntity(CertificateEvaluation dto);
}
