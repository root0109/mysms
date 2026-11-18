package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.lesson_note. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class LessonNoteEntity extends BaseEntity {

    public static final String TABLE = "lms.lesson_note";

    private UUID tenantId;
    private UUID lessonId;
    private UUID userId;
    private String color;
    private String highlightedText;
    private String note;
}
