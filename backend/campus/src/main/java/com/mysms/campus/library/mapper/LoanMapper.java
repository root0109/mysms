package com.mysms.campus.library.mapper;

import com.mysms.campus.library.api.model.Loan;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.campus.library.domain.LoanEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps library.loan to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LibraryMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface LoanMapper {

    @Mapping(target = "fineAmount", expression = "java(WireValues.decimal(entity.getFineAmount()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Loan.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Loan toDto(LoanEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "fineAmount", expression = "java(WireValues.decimal(dto.getFineAmount()))")
    @Mapping(target = "status", ignore = true)
    LoanEntity toEntity(Loan dto);
}
