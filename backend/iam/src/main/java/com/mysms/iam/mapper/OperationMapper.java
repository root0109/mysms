package com.mysms.iam.mapper;

import com.mysms.iam.api.model.Operation;
import com.mysms.iam.domain.OperationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** iam.operation ⇄ the contract's Operation: the module is fixed, a file result is the output file. */
@Mapper(config = IamMapperConfig.class)
public interface OperationMapper {

    @Mapping(target = "operationId", source = "id")
    @Mapping(target = "module", constant = "iam")
    @Mapping(target = "kind", source = "operationName")
    @Mapping(target = "status", expression = "java(Operation.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "resultFileId", expression =
            "java(\"file\".equals(entity.getResultEntityType()) ? entity.getResultEntityId() : null)")
    @Mapping(target = "result", ignore = true)
    @Mapping(target = "createTime", source = "startedAt")
    @Mapping(target = "doneTime", source = "finishedAt")
    Operation toDto(OperationEntity entity);
}
