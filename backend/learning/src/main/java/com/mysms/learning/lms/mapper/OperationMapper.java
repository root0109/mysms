package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.Operation;
import com.mysms.learning.lms.domain.OperationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * lms.operation ⇄ the contract's Operation: the module is fixed, a file result is the output file. The outcome
 * ({@code result}) is JSON the service reads, so the mapper stays a pure shape conversion.
 */
@Mapper(config = LmsMapperConfig.class)
public interface OperationMapper {

    @Mapping(target = "operationId", source = "id")
    @Mapping(target = "module", constant = "lms")
    @Mapping(target = "kind", source = "operationName")
    @Mapping(target = "status", expression = "java(Operation.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "resultFileId", expression =
            "java(\"file\".equals(entity.getResultEntityType()) ? entity.getResultEntityId() : null)")
    @Mapping(target = "result", ignore = true)
    @Mapping(target = "createTime", source = "startedAt")
    @Mapping(target = "doneTime", source = "finishedAt")
    Operation toDto(OperationEntity entity);
}
