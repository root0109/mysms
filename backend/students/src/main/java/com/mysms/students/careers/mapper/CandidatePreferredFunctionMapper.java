package com.mysms.students.careers.mapper;

import com.mysms.students.careers.domain.CandidatePreferredFunctionEntity;
import com.mysms.students.careers.api.model.CandidatePreferredFunction;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps careers.candidate_preferred_function to and from its contract DTO. The way in ignores id, tenant, audit and
 * read-only fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CareersMapperConfig.class, imports = {Etags.class})
public interface CandidatePreferredFunctionMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CandidatePreferredFunction toDto(CandidatePreferredFunctionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "candidatePreferenceId", ignore = true)
    CandidatePreferredFunctionEntity toEntity(CandidatePreferredFunction dto);
}
