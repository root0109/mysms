package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.settings_sidebar_item. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SettingsSidebarItemEntity extends BaseEntity {

    public static final String TABLE = "lms.settings_sidebar_item";

    private UUID tenantId;
    private UUID settingsId;
    private Integer sortOrder;
    private String itemType;
    private String itemKey;
    private String title;
    private String icon;
    private String webPageRoute;
    private String route;
    private String url;
    private Boolean isStandard;
    private Boolean isHidden;
    private Boolean openInNewWindow;
}
