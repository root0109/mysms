package com.mysms.academics.timetable.mapper;

import com.mysms.academics.timetable.api.model.Operation;
import com.mysms.academics.timetable.domain.OperationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** timetable.operation ⇄ the contract's Operation: the module is fixed, a file result is the output file. */
@Mapper(config = TimetableMapperConfig.class)
public interface OperationMapper {

    @Mapping(target = "operationId", source = "id")
    @Mapping(target = "module", constant = "timetable")
    @Mapping(target = "kind", source = "operationName")
    @Mapping(target = "status", expression = "java(Operation.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "resultFileId", expression =
            "java(\"file\".equals(entity.getResultEntityType()) ? entity.getResultEntityId() : null)")
    @Mapping(target = "result", ignore = true)
    @Mapping(target = "createTime", source = "startedAt")
    @Mapping(target = "doneTime", source = "finishedAt")
    Operation toDto(OperationEntity entity);
}
