package com.mysms.students.sis.mapper;

import com.mysms.students.sis.api.model.DisciplineIncident;
import com.mysms.core.web.Etags;
import com.mysms.students.sis.domain.DisciplineIncidentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for sis.discipline_incident. The way in ignores the id, tenant, audit set and read-only fields,
 * so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = SisMapperConfig.class, imports = {Etags.class})
public interface DisciplineIncidentMapper {

    @Mapping(target = "incidentType", expression =
            "java(entity.getIncidentType() == null ? null : "
                    + "DisciplineIncident.IncidentTypeEnum.fromValue(entity.getIncidentType()))")
    @Mapping(target = "evidenceMediaType", expression =
            "java(entity.getEvidenceMediaType() == null ? null : "
                    + "DisciplineIncident.EvidenceMediaTypeEnum.fromValue(entity.getEvidenceMediaType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    DisciplineIncident toDto(DisciplineIncidentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "incidentType", expression =
            "java(dto.getIncidentType() == null ? null : dto.getIncidentType().getValue())")
    @Mapping(target = "evidenceMediaType", expression =
            "java(dto.getEvidenceMediaType() == null ? null : dto.getEvidenceMediaType().getValue())")
    DisciplineIncidentEntity toEntity(DisciplineIncident dto);
}
