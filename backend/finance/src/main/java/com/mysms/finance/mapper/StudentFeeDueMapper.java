package com.mysms.finance.mapper;

import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.finance.api.model.StudentFeeDue;
import com.mysms.finance.domain.StudentFeeDueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity → contract DTO for finance.student_fee_due. Dues are written only by fee runs and the fee workflows, never
 * built from a request, so there is no way in; the ETag always comes from row_version.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface StudentFeeDueMapper {

    @Mapping(target = "amount", expression = "java(WireValues.decimal(entity.getAmount()))")
    @Mapping(target = "discountPercent", expression = "java(WireValues.decimal(entity.getDiscountPercent()))")
    @Mapping(target = "feeFactor", expression = "java(WireValues.decimal(entity.getFeeFactor()))")
    @Mapping(target = "discountAmount", expression = "java(WireValues.decimal(entity.getDiscountAmount()))")
    @Mapping(target = "netAmount", expression = "java(WireValues.decimal(entity.getNetAmount()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : StudentFeeDue.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StudentFeeDue toDto(StudentFeeDueEntity entity);
}
