package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.class_page_member. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ClassPageMemberEntity extends BaseEntity {

    public static final String TABLE = "comms.class_page_member";

    private UUID tenantId;
    private UUID classPageId;
    private UUID userId;
    private Boolean isFollowing;
    private String reaction;
    private Boolean isActive;
}
