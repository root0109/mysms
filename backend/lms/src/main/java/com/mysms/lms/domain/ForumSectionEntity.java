package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.forum_section. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ForumSectionEntity extends BaseEntity {

    public static final String TABLE = "lms.forum_section";

    private UUID tenantId;
    private UUID forumId;
    private String title;
    private String description;
    private String sectionType;
    private UUID cohortGroupId;
    private Integer sortOrder;
}
