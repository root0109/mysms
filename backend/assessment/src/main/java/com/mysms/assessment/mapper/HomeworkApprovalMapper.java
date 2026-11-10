package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.HomeworkApprovalEntity;
import com.mysms.assessment.api.model.HomeworkApproval;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for assessment.homework_approval. The way in ignores the id, the tenant, the audit set and
 * every read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface HomeworkApprovalMapper {

    @Mapping(target = "decision", expression =
            "java(entity.getDecision() == null ? null : HomeworkApproval.DecisionEnum.fromValue(entity.getDecision()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    HomeworkApproval toDto(HomeworkApprovalEntity entity);
}
