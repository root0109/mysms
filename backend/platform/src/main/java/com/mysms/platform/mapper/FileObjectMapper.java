package com.mysms.platform.mapper;

import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.platform.api.model.FileObject;
import com.mysms.platform.domain.FileObjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * platform.file_object → the contract's FileObject. Files are created only by the upload workflow (CreateUploadUrl,
 * CopyFile, report output), never from a client's FileObject, so there is no way in. Where the bytes live (the storage
 * disk and key) never leaves the module.
 */
@Mapper(config = PlatformMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface FileObjectMapper {

    @Mapping(target = "sizeBytes", expression = "java(WireValues.int64(entity.getSizeBytes()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : FileObject.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "fileKind", expression =
            "java(entity.getFileKind() == null ? null : FileObject.FileKindEnum.fromValue(entity.getFileKind()))")
    @Mapping(target = "processingStatus", expression = "java(entity.getProcessingStatus() == null ? null"
            + " : FileObject.ProcessingStatusEnum.fromValue(entity.getProcessingStatus()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    FileObject toDto(FileObjectEntity entity);
}
