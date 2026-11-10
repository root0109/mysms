package com.mysms.payroll.mapper;

import com.mysms.payroll.api.model.StaffBankAccount;
import com.mysms.core.web.Etags;
import com.mysms.payroll.domain.StaffBankAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for payroll.staff_bank_account. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = PayrollMapperConfig.class, imports = {Etags.class})
public interface StaffBankAccountMapper {

    @Mapping(target = "accountNumber", ignore = true)

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StaffBankAccount toDto(StaffBankAccountEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "accountNumberEnc", ignore = true)
    @Mapping(target = "accountNumberMasked", ignore = true)
    StaffBankAccountEntity toEntity(StaffBankAccount dto);
}
