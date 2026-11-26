package com.mysms.learning.lms.domain;

import com.mysms.learning.lms.model.LessonDocument;
import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.lesson. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LessonEntity extends BaseEntity {

    public static final String TABLE = "lms.lesson";

    private UUID tenantId;
    private UUID chapterId;
    private String title;
    private Integer sortOrder;
    private Boolean includeInPreview;
    private String body;
    private LessonDocument content;
    private String teacherNotes;
    private LessonDocument teacherContent;
    private String youtubeVideoId;
    private String embeddedQuestion;
    private String fileType;
    private Boolean isScormPackage;
    private Integer version;
    private UUID supersededById;
    private OffsetDateTime publishedAt;
}
