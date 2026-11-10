package com.mysms.assessment.mapper;

import com.mysms.assessment.api.model.Operation;
import com.mysms.assessment.domain.OperationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** assessment.operation ⇄ the contract's Operation: the module is fixed, a file result is the output file. */
@Mapper(config = AssessmentMapperConfig.class)
public interface OperationMapper {

    @Mapping(target = "operationId", source = "id")
    @Mapping(target = "module", constant = "assessment")
    @Mapping(target = "kind", source = "operationName")
    @Mapping(target = "status", expression = "java(Operation.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "resultFileId", expression =
            "java(\"file\".equals(entity.getResultEntityType()) ? entity.getResultEntityId() : null)")
    @Mapping(target = "result", ignore = true)
    @Mapping(target = "createTime", source = "startedAt")
    @Mapping(target = "doneTime", source = "finishedAt")
    Operation toDto(OperationEntity entity);
}
