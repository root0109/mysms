package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.theme. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ThemeEntity extends BaseEntity {

    public static final String TABLE = "platform.theme";

    private UUID tenantId;
    private String name;
    private String description;
    private UUID thumbnailFileId;
    private UUID thumbnailAltFileId;
    private UUID logoFileId;
    private Boolean isDefault;
}
