package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.saas_plan (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SaasPlanEntity extends BaseEntity {

    public static final String TABLE = "platform.saas_plan";
    private String code;
    private String displayName;
    private Integer billingCycleMonths;
    private BigDecimal price;
    private UUID currencyId;
    private Integer sortOrder;
    private Boolean isActive;
    private Integer maxMembers;
    private Integer maxEvents;
    private Integer maxFolders;
    private Integer maxFiles;
    private Integer maxVideos;
    private Integer maxAudios;
    private Integer maxBulletins;
    private Integer maxGroups;
}
