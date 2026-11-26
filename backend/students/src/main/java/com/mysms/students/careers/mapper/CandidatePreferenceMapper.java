package com.mysms.students.careers.mapper;

import com.mysms.students.careers.domain.CandidatePreferenceEntity;
import com.mysms.students.careers.api.model.CandidatePreference;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps careers.candidate_preference to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CareersMapperConfig.class, imports = {Etags.class})
public interface CandidatePreferenceMapper {

    @Mapping(target = "openTo", expression =
            "java(entity.getOpenTo() == null ? null : CandidatePreference.OpenToEnum.fromValue(entity.getOpenTo()))")
    @Mapping(target = "attire", expression =
            "java(entity.getAttire() == null ? null : CandidatePreference.AttireEnum.fromValue(entity.getAttire()))")
    @Mapping(target = "collaboration", expression =
            "java(entity.getCollaboration() == null ? null : "
                    + "CandidatePreference.CollaborationEnum.fromValue(entity.getCollaboration()))")
    @Mapping(target = "roleClarity", expression =
            "java(entity.getRoleClarity() == null ? null : "
                    + "CandidatePreference.RoleClarityEnum.fromValue(entity.getRoleClarity()))")
    @Mapping(target = "locationPreference", expression =
            "java(entity.getLocationPreference() == null ? null : "
                    + "CandidatePreference.LocationPreferenceEnum.fromValue(entity.getLocationPreference()))")
    @Mapping(target = "workHours", expression =
            "java(entity.getWorkHours() == null ? null : "
                    + "CandidatePreference.WorkHoursEnum.fromValue(entity.getWorkHours()))")
    @Mapping(target = "companyType", expression =
            "java(entity.getCompanyType() == null ? null : "
                    + "CandidatePreference.CompanyTypeEnum.fromValue(entity.getCompanyType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CandidatePreference toDto(CandidatePreferenceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "openTo", expression = "java(dto.getOpenTo() == null ? null : dto.getOpenTo().getValue())")
    @Mapping(target = "attire", expression = "java(dto.getAttire() == null ? null : dto.getAttire().getValue())")
    @Mapping(target = "collaboration", expression =
            "java(dto.getCollaboration() == null ? null : dto.getCollaboration().getValue())")
    @Mapping(target = "roleClarity", expression =
            "java(dto.getRoleClarity() == null ? null : dto.getRoleClarity().getValue())")
    @Mapping(target = "locationPreference", expression =
            "java(dto.getLocationPreference() == null ? null : dto.getLocationPreference().getValue())")
    @Mapping(target = "workHours", expression =
            "java(dto.getWorkHours() == null ? null : dto.getWorkHours().getValue())")
    @Mapping(target = "companyType", expression =
            "java(dto.getCompanyType() == null ? null : dto.getCompanyType().getValue())")
    CandidatePreferenceEntity toEntity(CandidatePreference dto);
}
