package com.mysms.assessment.mapper;

import com.mysms.assessment.api.model.Operation;
import com.mysms.assessment.domain.OperationEntity;
import java.util.Map;
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
    @Mapping(target = "result", expression = "java(OperationMapper.result(entity))")
    @Mapping(target = "createTime", source = "startedAt")
    @Mapping(target = "doneTime", source = "finishedAt")
    Operation toDto(OperationEntity entity);

    static Object result(OperationEntity entity) {
        if (entity.getResultEntityId() == null || entity.getResultEntityType() == null
                || "file".equals(entity.getResultEntityType())) {
            return null;
        }
        if ("assessment.marksheet".equals(entity.getResultEntityType())) {
            return Map.of("marksheetId", entity.getResultEntityId());
        }
        return Map.of("entityType", entity.getResultEntityType(), "entityId", entity.getResultEntityId());
    }
}
