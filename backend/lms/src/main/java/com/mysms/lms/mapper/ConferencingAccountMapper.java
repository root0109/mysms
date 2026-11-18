package com.mysms.lms.mapper;

import com.mysms.lms.api.model.ConferencingAccount;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ConferencingAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.conferencing_account to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface ConferencingAccountMapper {

    @Mapping(target = "clientSecretMasked", ignore = true)
    @Mapping(target = "provider", expression =
            "java(entity.getProvider() == null ? null : "
                    + "ConferencingAccount.ProviderEnum.fromValue(entity.getProvider()))")
    @Mapping(target = "clientSecret", ignore = true)

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ConferencingAccount toDto(ConferencingAccountEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "provider", expression = "java(dto.getProvider() == null ? null : dto.getProvider().getValue())")
    @Mapping(target = "clientSecretEnc", ignore = true)
    ConferencingAccountEntity toEntity(ConferencingAccount dto);
}
