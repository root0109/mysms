package com.mysms.finance.mapper;

import com.mysms.finance.api.model.InvoiceLine;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.finance.domain.InvoiceLineEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for finance.invoice_line. The way in ignores the id, tenant, audit set and read-only fields, so
 * a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface InvoiceLineMapper {

    @Mapping(target = "taxRatePercent", expression = "java(WireValues.decimal(entity.getTaxRatePercent()))")
    @Mapping(target = "quantity", expression = "java(WireValues.decimal(entity.getQuantity()))")
    @Mapping(target = "unitPrice", expression = "java(WireValues.decimal(entity.getUnitPrice()))")
    @Mapping(target = "discountAmount", expression = "java(WireValues.decimal(entity.getDiscountAmount()))")
    @Mapping(target = "taxAmount", expression = "java(WireValues.decimal(entity.getTaxAmount()))")
    @Mapping(target = "lineTotal", expression = "java(WireValues.decimal(entity.getLineTotal()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    InvoiceLine toDto(InvoiceLineEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "taxRatePercent", ignore = true)
    @Mapping(target = "taxCode", ignore = true)
    @Mapping(target = "invoiceId", ignore = true)
    @Mapping(target = "quantity", expression = "java(WireValues.decimal(dto.getQuantity()))")
    @Mapping(target = "unitPrice", expression = "java(WireValues.decimal(dto.getUnitPrice()))")
    @Mapping(target = "discountAmount", expression = "java(WireValues.decimal(dto.getDiscountAmount()))")
    @Mapping(target = "taxAmount", expression = "java(WireValues.decimal(dto.getTaxAmount()))")
    @Mapping(target = "lineTotal", expression = "java(WireValues.decimal(dto.getLineTotal()))")
    InvoiceLineEntity toEntity(InvoiceLine dto);
}
