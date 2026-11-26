package com.mysms.campus.transport.mapper;

import com.mysms.campus.transport.api.model.StudentTransport;
import com.mysms.core.web.Etags;
import com.mysms.campus.transport.domain.StudentTransportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps transport.student_transport to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = TransportMapperConfig.class, imports = {Etags.class})
public interface StudentTransportMapper {

    @Mapping(target = "transportMode", expression =
            "java(entity.getTransportMode() == null ? null : "
                    + "StudentTransport.TransportModeEnum.fromValue(entity.getTransportMode()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StudentTransport toDto(StudentTransportEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "transportMode", expression =
            "java(dto.getTransportMode() == null ? null : dto.getTransportMode().getValue())")
    StudentTransportEntity toEntity(StudentTransport dto);
}
