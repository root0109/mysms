package com.mysms.assessment.events;

import com.mysms.assessment.domain.HomeworkEntity;
import com.mysms.assessment.domain.HomeworkSubmissionEntity;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events assessment publishes through its outbox: {@code <schema>.<table>.created|updated|deleted} with the row
 * id.
 * Dependent modules keep their copies and logical references fresh from these (database/logical-references.md).
 */
public final class AssessmentEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private AssessmentEvents() {
    }

    /**
     * A step of the homework lifecycle (publish, sendForApproval, approve, reject): ids only — whoever notifies the
     * class or the reviewer reads the rest with their own access.
     */
    public static Map<String, Object> homework(HomeworkEntity homework) {
        return Map.of("id", homework.getId().toString(),
                "institutionId", homework.getInstitutionId().toString(),
                "classSectionId", homework.getClassSectionId().toString());
    }

    /** A step on a submission (check, reply): the submission, its homework and the student it belongs to. */
    public static Map<String, Object> submission(HomeworkSubmissionEntity submission) {
        return Map.of("id", submission.getId().toString(),
                "homeworkId", submission.getHomeworkId().toString(),
                "studentId", submission.getStudentId().toString());
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }
}
