package com.mysms.finance.mapper;

import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.finance.api.model.Invoice;
import com.mysms.finance.domain.InvoiceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for finance.invoice. The way in takes only the bill-to and date fields a draft is created
 * with: the id, the tenant, the audit set, the number, the totals, the status and every posting stamp are the
 * service's, so a request can never set a value only the workflow changes; the ETag always comes from row_version.
 */
@Mapper(config = FinanceMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface InvoiceMapper {

    @Mapping(target = "subtotal", expression = "java(WireValues.decimal(entity.getSubtotal()))")
    @Mapping(target = "discountTotal", expression = "java(WireValues.decimal(entity.getDiscountTotal()))")
    @Mapping(target = "taxTotal", expression = "java(WireValues.decimal(entity.getTaxTotal()))")
    @Mapping(target = "total", expression = "java(WireValues.decimal(entity.getTotal()))")
    @Mapping(target = "amountPaid", expression = "java(WireValues.decimal(entity.getAmountPaid()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Invoice.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "invoiceType", expression =
            "java(entity.getInvoiceType() == null ? null : Invoice.InvoiceTypeEnum.fromValue(entity.getInvoiceType()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Invoice toDto(InvoiceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discountTotal", ignore = true)
    @Mapping(target = "taxTotal", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "amountPaid", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "invoiceType", ignore = true)
    @Mapping(target = "creditForInvoiceId", ignore = true)
    @Mapping(target = "postedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "documentFileId", ignore = true)
    InvoiceEntity toEntity(Invoice dto);
}
