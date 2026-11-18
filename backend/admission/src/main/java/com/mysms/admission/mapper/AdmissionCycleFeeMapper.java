package com.mysms.admission.mapper;

import com.mysms.admission.domain.AdmissionCycleFeeEntity;
import com.mysms.admission.api.model.AdmissionCycleFee;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps admission.admission_cycle_fee to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AdmissionMapperConfig.class, imports = {Etags.class})
public interface AdmissionCycleFeeMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AdmissionCycleFee toDto(AdmissionCycleFeeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "admissionCycleId", ignore = true)
    AdmissionCycleFeeEntity toEntity(AdmissionCycleFee dto);
}
