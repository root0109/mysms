package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.annotation. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AnnotationEntity extends BaseEntity {

    public static final String TABLE = "lms.annotation";

    private UUID tenantId;
    private UUID userId;
    private UUID contentItemId;
    private UUID cohortId;
    private UUID ebookId;
    private UUID fileId;
    private String annotationType;
    private String annotationSubtype;
    private String pageNumber;
    private String startCharPos;
    private String charCount;
    private String location;
    private String annotatedText;
    private String title;
    private String description;
    private String embedCode;
    private String url;
    private UUID mediaFileId;
    private String visibility;
    private UUID forumTopicId;
}
