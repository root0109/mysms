package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.tag. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TagEntity extends BaseEntity {

    public static final String TABLE = "platform.tag";

    private UUID tenantId;
    private String tagType;
    private String name;
    private String slug;
    private Integer sortOrder;
    private String code;
}
