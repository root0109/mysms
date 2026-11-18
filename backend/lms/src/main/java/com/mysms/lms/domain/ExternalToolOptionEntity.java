package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.external_tool_option. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExternalToolOptionEntity extends BaseEntity {

    public static final String TABLE = "lms.external_tool_option";

    private UUID tenantId;
    private UUID externalToolId;
    private String optionKind;
    private String optionValue;
}
