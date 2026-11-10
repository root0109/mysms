package com.mysms.admission.mapper;

import com.mysms.admission.domain.ApplicationEntity;
import com.mysms.admission.api.model.Application;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for admission.application. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AdmissionMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface ApplicationMapper {

    @Mapping(target = "nationalIdMasked", ignore = true)
    @Mapping(target = "gender", expression =
            "java(entity.getGender() == null ? null : Application.GenderEnum.fromValue(entity.getGender()))")
    @Mapping(target = "nationalId", ignore = true)
    @Mapping(target = "heightCm", expression = "java(WireValues.decimal(entity.getHeightCm()))")
    @Mapping(target = "weightKg", expression = "java(WireValues.decimal(entity.getWeightKg()))")
    @Mapping(target = "previousBoard", expression =
            "java(entity.getPreviousBoard() == null ? null : "
                    + "Application.PreviousBoardEnum.fromValue(entity.getPreviousBoard()))")
    @Mapping(target = "transportMode", expression =
            "java(entity.getTransportMode() == null ? null : "
                    + "Application.TransportModeEnum.fromValue(entity.getTransportMode()))")
    @Mapping(target = "familyIncome", expression = "java(WireValues.decimal(entity.getFamilyIncome()))")
    @Mapping(target = "applicationFee", expression = "java(WireValues.decimal(entity.getApplicationFee()))")
    @Mapping(target = "discountPercent", expression = "java(WireValues.decimal(entity.getDiscountPercent()))")
    @Mapping(target = "paymentStatus", expression =
            "java(entity.getPaymentStatus() == null ? null : "
                    + "Application.PaymentStatusEnum.fromValue(entity.getPaymentStatus()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Application.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Application toDto(ApplicationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "gender", expression = "java(dto.getGender() == null ? null : dto.getGender().getValue())")
    @Mapping(target = "nationalIdEnc", ignore = true)
    @Mapping(target = "heightCm", expression = "java(WireValues.decimal(dto.getHeightCm()))")
    @Mapping(target = "weightKg", expression = "java(WireValues.decimal(dto.getWeightKg()))")
    @Mapping(target = "previousBoard", expression =
            "java(dto.getPreviousBoard() == null ? null : dto.getPreviousBoard().getValue())")
    @Mapping(target = "transportMode", expression =
            "java(dto.getTransportMode() == null ? null : dto.getTransportMode().getValue())")
    @Mapping(target = "familyIncome", expression = "java(WireValues.decimal(dto.getFamilyIncome()))")
    @Mapping(target = "applicationFee", expression = "java(WireValues.decimal(dto.getApplicationFee()))")
    @Mapping(target = "discountPercent", expression = "java(WireValues.decimal(dto.getDiscountPercent()))")
    @Mapping(target = "paymentStatus", expression =
            "java(dto.getPaymentStatus() == null ? null : dto.getPaymentStatus().getValue())")
    @Mapping(target = "status", ignore = true)
    ApplicationEntity toEntity(Application dto);
}
