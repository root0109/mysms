package com.mysms.iam.mapper;

import com.mysms.contracts.iam.model.UserAccount;
import com.mysms.core.web.Etags;
import com.mysms.iam.domain.MeAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** The caller's own account as the contract shows it (the password hash is not part of the entity). */
@Mapper(config = IamMapperConfig.class, imports = Etags.class)
public interface MeAccountMapper {

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    UserAccount toDto(MeAccountEntity entity);

    default UserAccount.StatusEnum status(String value) {
        return value == null ? null : UserAccount.StatusEnum.fromValue(value);
    }

    default UserAccount.UserTypeEnum userType(String value) {
        return value == null ? null : UserAccount.UserTypeEnum.fromValue(value);
    }
}
