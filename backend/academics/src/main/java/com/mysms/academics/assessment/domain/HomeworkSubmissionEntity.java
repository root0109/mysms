package com.mysms.academics.assessment.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of assessment.homework_submission. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class HomeworkSubmissionEntity extends BaseEntity {

    public static final String TABLE = "assessment.homework_submission";

    private UUID tenantId;
    private UUID homeworkId;
    private UUID studentId;
    private LocalDate submittedOn;
    private String status;
    private UUID checkedByUserId;
    private LocalDate checkedOn;
    private String teacherComments;
    private String studentReply;
}
