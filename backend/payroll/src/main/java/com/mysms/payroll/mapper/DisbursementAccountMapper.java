package com.mysms.payroll.mapper;

import com.mysms.payroll.api.model.DisbursementAccount;
import com.mysms.core.web.Etags;
import com.mysms.payroll.domain.DisbursementAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps payroll.disbursement_account to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = PayrollMapperConfig.class, imports = {Etags.class})
public interface DisbursementAccountMapper {

    @Mapping(target = "accountNumberMasked", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    DisbursementAccount toDto(DisbursementAccountEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "accountNumberEnc", ignore = true)
    DisbursementAccountEntity toEntity(DisbursementAccount dto);
}
