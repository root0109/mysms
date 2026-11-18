package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.theme_token: one design token of a theme (column for column; audit set from BaseEntity). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ThemeTokenEntity extends BaseEntity {

    public static final String TABLE = "platform.theme_token";

    private UUID tenantId;
    private UUID themeId;
    private String tokenKey;
    private String value;
}
