package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.external_tool_resource. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExternalToolResourceEntity extends BaseEntity {

    public static final String TABLE = "lms.external_tool_resource";

    private UUID tenantId;
    private UUID institutionId;
    private String title;
    private String description;
}
