package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.PlagiarismReport;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.PlagiarismReportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.plagiarism_report to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface PlagiarismReportMapper {

    @Mapping(target = "itemKind", expression =
            "java(entity.getItemKind() == null ? null : PlagiarismReport.ItemKindEnum.fromValue(entity.getItemKind()))")
    @Mapping(target = "tolerance", expression = "java(WireValues.decimal(entity.getTolerance()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    PlagiarismReport toDto(PlagiarismReportEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "itemKind", expression = "java(dto.getItemKind() == null ? null : dto.getItemKind().getValue())")
    @Mapping(target = "tolerance", expression = "java(WireValues.decimal(dto.getTolerance()))")
    PlagiarismReportEntity toEntity(PlagiarismReport dto);
}
