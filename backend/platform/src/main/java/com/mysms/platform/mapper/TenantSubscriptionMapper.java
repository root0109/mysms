package com.mysms.platform.mapper;

import com.mysms.platform.api.model.TenantSubscription;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.platform.domain.TenantSubscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for platform.tenant_subscription. The way in ignores the id, the tenant, the audit set and
 * every read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = PlatformMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface TenantSubscriptionMapper {

    @Mapping(target = "price", expression = "java(WireValues.decimal(entity.getPrice()))")
    @Mapping(target = "amountPaid", expression = "java(WireValues.decimal(entity.getAmountPaid()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : TenantSubscription.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    TenantSubscription toDto(TenantSubscriptionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "maxMembers", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "currencyId", ignore = true)
    @Mapping(target = "billingCycleMonths", ignore = true)
    @Mapping(target = "gatewayName", ignore = true)
    @Mapping(target = "gatewayTxnRef", ignore = true)
    @Mapping(target = "amountPaid", ignore = true)
    @Mapping(target = "gatewayStatus", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "payerName", ignore = true)
    @Mapping(target = "status", ignore = true)
    TenantSubscriptionEntity toEntity(TenantSubscription dto);
}
