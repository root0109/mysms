package com.mysms.lms.mapper;

import com.mysms.lms.api.model.ImportExportJob;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ImportExportJobEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.import_export_job to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface ImportExportJobMapper {

    @Mapping(target = "direction", expression =
            "java(entity.getDirection() == null ? null : "
                    + "ImportExportJob.DirectionEnum.fromValue(entity.getDirection()))")
    @Mapping(target = "packageFormat", expression =
            "java(entity.getPackageFormat() == null ? null : "
                    + "ImportExportJob.PackageFormatEnum.fromValue(entity.getPackageFormat()))")
    @Mapping(target = "scope", expression =
            "java(entity.getScope() == null ? null : ImportExportJob.ScopeEnum.fromValue(entity.getScope()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : ImportExportJob.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "unarchiveStatus", expression =
            "java(entity.getUnarchiveStatus() == null ? null : "
                    + "ImportExportJob.UnarchiveStatusEnum.fromValue(entity.getUnarchiveStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ImportExportJob toDto(ImportExportJobEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "direction", expression =
            "java(dto.getDirection() == null ? null : dto.getDirection().getValue())")
    @Mapping(target = "packageFormat", expression =
            "java(dto.getPackageFormat() == null ? null : dto.getPackageFormat().getValue())")
    @Mapping(target = "scope", expression = "java(dto.getScope() == null ? null : dto.getScope().getValue())")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "unarchiveStatus", expression =
            "java(dto.getUnarchiveStatus() == null ? null : dto.getUnarchiveStatus().getValue())")
    ImportExportJobEntity toEntity(ImportExportJob dto);
}
