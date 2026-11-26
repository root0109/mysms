package com.mysms.identity.iam.mapper;

import com.mysms.identity.iam.api.model.UserAccount;
import com.mysms.core.web.Etags;
import com.mysms.identity.iam.domain.UserAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps iam.user_account to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = IamMapperConfig.class, imports = {Etags.class})
public interface UserAccountMapper {

    @Mapping(target = "userType", expression =
            "java(entity.getUserType() == null ? null : UserAccount.UserTypeEnum.fromValue(entity.getUserType()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : UserAccount.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    UserAccount toDto(UserAccountEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "userType", expression = "java(dto.getUserType() == null ? null : dto.getUserType().getValue())")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "mobileVerifiedAt", ignore = true)
    @Mapping(target = "mustResetPassword", ignore = true)
    @Mapping(target = "termsAcceptedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginCount", ignore = true)
    @Mapping(target = "lastLoginRoleId", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    UserAccountEntity toEntity(UserAccount dto);
}
