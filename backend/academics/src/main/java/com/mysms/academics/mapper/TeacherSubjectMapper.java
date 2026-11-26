package com.mysms.academics.mapper;

import com.mysms.academics.api.model.TeacherSubject;
import com.mysms.core.web.Etags;
import com.mysms.academics.domain.TeacherSubjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps academics.teacher_subject to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AcademicsMapperConfig.class, imports = {Etags.class})
public interface TeacherSubjectMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    TeacherSubject toDto(TeacherSubjectEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    TeacherSubjectEntity toEntity(TeacherSubject dto);
}
