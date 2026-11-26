package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.content_author. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ContentAuthorEntity extends BaseEntity {

    public static final String TABLE = "lms.content_author";

    private UUID tenantId;
    private UUID userId;
    private UUID quizId;
    private UUID questionId;
    private UUID assignmentId;
    private UUID exerciseId;
    private UUID learningProgramId;
}
