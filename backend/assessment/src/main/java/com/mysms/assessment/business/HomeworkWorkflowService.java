package com.mysms.assessment.business;

import com.mysms.assessment.api.model.ApproveHomeworkRequest;
import com.mysms.assessment.api.model.CheckHomeworkSubmissionRequest;
import com.mysms.assessment.api.model.Homework;
import com.mysms.assessment.api.model.HomeworkSubmission;
import com.mysms.assessment.api.model.PublishHomeworkRequest;
import com.mysms.assessment.api.model.RejectHomeworkRequest;
import com.mysms.assessment.api.model.ReplyToHomeworkFeedbackRequest;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.HomeworkApprovalEntity;
import com.mysms.assessment.domain.HomeworkEntity;
import com.mysms.assessment.domain.HomeworkSubmissionEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.HomeworkMapper;
import com.mysms.assessment.mapper.HomeworkSubmissionMapper;
import com.mysms.assessment.repository.HomeworkApprovalRepository;
import com.mysms.assessment.repository.HomeworkRepository;
import com.mysms.assessment.repository.HomeworkSubmissionRepository;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The homework lifecycle (ui/03 flow F5, api/state-machines.md "Homework"): a teacher publishes homework — at once
 * when they may also approve homework for its institution, otherwise it waits for a reviewer, who approves it
 * (published) or rejects it (back to draft, with the reason kept on the approval record) — and, once students have
 * handed in, the teacher checks each submission and the student may answer the teacher's comments.
 *
 * <p>Who needs a review is decided by the permissions a role holds, not by a setting: a school that wants no review
 * gives its teachers the approving permission. "For its institution" is as exact as the token is: it carries one set
 * of institutions for all of a caller's grants (STATUS, known limit 1), so someone who approves in one school and
 * teaches in another passes as an approver in both. The student who may reply is the account that created the
 * submission; a submission entered for a student by someone else has no one who can reply until the student and
 * guardian operations exist. Every step locks the row it changes, checks the caller's institution
 * scope the way the standard operations do (a row outside it does not exist: 404), honours the optional body etag
 * (412) and writes its audit record and event in the transaction of the change (JAVA-CODING-RULES §4).
 */
@Service
public class HomeworkWorkflowService extends AppTransactionManager {

    static final String PERMISSION_PUBLISH = "assessment.homework.publish_homework";
    static final String PERMISSION_APPROVE = "assessment.homework.approve_homework";
    static final String PERMISSION_REJECT = "assessment.homework.reject_homework";
    static final String PERMISSION_CHECK = "assessment.homework_submission.check_homework_submission";
    static final String PERMISSION_REPLY = "assessment.homework_submission.reply_to_homework_feedback";
    /** The longest comment, reply or reason a step keeps; the columns are text, the limit is the workflow's. */
    static final int MAX_TEXT = 4000;
    private static final String HOMEWORK = HomeworkEntity.TABLE;
    private static final String SUBMISSION = HomeworkSubmissionEntity.TABLE;
    private static final String DRAFT = "draft";
    private static final String PENDING = "pending";
    private static final String PUBLISHED = "published";

    private final HomeworkRepository homeworks;
    private final HomeworkApprovalRepository approvals;
    private final HomeworkSubmissionRepository submissions;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public HomeworkWorkflowService(HomeworkRepository homeworks, HomeworkApprovalRepository approvals,
            HomeworkSubmissionRepository submissions, HomeworkMapper homeworkMapper,
            HomeworkSubmissionMapper submissionMapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.homeworks = homeworks;
        this.approvals = approvals;
        this.submissions = submissions;
        this.homeworkMapper = homeworkMapper;
        this.submissionMapper = submissionMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * assessment.PublishHomework: a draft is published — or, when the caller may not approve homework for its
     * institution, sent for review (status pending, with a pending approval record). Publishing stamps the day the
     * homework was set when the teacher gave none. Any other state answers 409.
     */
    public Homework publishHomework(UUID id, PublishHomeworkRequest request) {
        DataScope scope = DataScope.current(PERMISSION_PUBLISH);
        boolean reviewer = AuthContext.require().hasPermission(PERMISSION_APPROVE);
        Optional<UUID> actor = Actor.current();
        return doInWriteTransaction(() -> {
            HomeworkEntity homework = lockWritable(scope, id, request == null ? null : request.getEtag());
            requireStatus(homework, DRAFT);
            OffsetDateTime now = now();
            boolean direct = reviewer
                    && DataScope.current(PERMISSION_APPROVE).canRead(Optional.of(homework.getInstitutionId()));
            if (direct) {
                move(homework, PUBLISHED, now, now, actor);
                audit.recordTransition(HOMEWORK, id, "publish", List.of("status", "assignedOn"), null);
                outbox.publish(HOMEWORK + ".publish", HOMEWORK, id, AssessmentEvents.homework(homework));
            } else {
                move(homework, PENDING, null, now, actor);
                HomeworkApprovalEntity approval = HomeworkApprovalEntity.builder()
                        .homeworkId(id)
                        .decision(PENDING)
                        .build();
                approval.onCreatingData(actor, clock);
                approval.setTenantId(homework.getTenantId());
                UUID approvalId = approvals.insert(approval).getId();
                audit.recordCreate(HomeworkApprovalEntity.TABLE, approvalId);
                audit.recordTransition(HOMEWORK, id, "sendForApproval", List.of("status"), null);
                outbox.publish(HOMEWORK + ".sendForApproval", HOMEWORK, id, AssessmentEvents.homework(homework));
            }
            return homeworkMapper.toDto(reread(id));
        });
    }

    /**
     * assessment.ApproveHomework: the reviewer approves homework that waits for review; it is published and its
     * pending approval record carries the decision, the reviewer and any comment. Any other state answers 409.
     */
    public Homework approveHomework(UUID id, ApproveHomeworkRequest request) {
        String etag = request == null ? null : request.getEtag();
        String comments = text("reason", request == null ? null : request.getReason());
        return decide(id, etag, PERMISSION_APPROVE, "approve", "approved", PUBLISHED, comments);
    }

    /**
     * assessment.RejectHomework: the reviewer sends homework that waits for review back to its teacher: it is a draft
     * again, and its approval record carries the decision, the reviewer and the reason. Any other state answers 409.
     */
    public Homework rejectHomework(UUID id, RejectHomeworkRequest request) {
        String etag = request == null ? null : request.getEtag();
        String comments = text("reason", request == null ? null : request.getReason());
        return decide(id, etag, PERMISSION_REJECT, "reject", "rejected", DRAFT, comments);
    }

    /** The reviewer's decision on pending homework: the homework moves on and the pending approval is decided. */
    private Homework decide(UUID id, String etag, String permission, String step, String decision, String status,
            String comments) {
        DataScope scope = DataScope.current(permission);
        Optional<UUID> actor = Actor.current();
        return doInWriteTransaction(() -> {
            HomeworkEntity homework = lockWritable(scope, id, etag);
            requireStatus(homework, PENDING);
            OffsetDateTime now = now();
            move(homework, status, PUBLISHED.equals(status) ? now : null, now, actor);
            // homework sent for review by this workflow has exactly one pending approval; one made pending by hand
            // through the standard operations is decided the same way, and none is not an obstacle
            for (HomeworkApprovalEntity pending : approvals.findPending(id)) {
                if (approvals.decide(pending.getId(), pending.getRowVersion(), decision, comments,
                        actor.orElse(null), now) == 0) {
                    throw new PreconditionFailedException("The row changed since it was read.");
                }
                audit.recordTransition(HomeworkApprovalEntity.TABLE, pending.getId(), step,
                        List.of("decision", "comments", "decidedByUserId", "decidedAt"), comments);
            }
            audit.recordTransition(HOMEWORK, id, step, List.of("status"), comments);
            outbox.publish(HOMEWORK + "." + step, HOMEWORK, id, AssessmentEvents.homework(homework));
            return homeworkMapper.toDto(reread(id));
        });
    }

    /**
     * assessment.CheckHomeworkSubmission: the teacher has looked at a submission — it is checked, by whom and when,
     * with the teacher's comments (a call without comments keeps the ones already given). Checking again is allowed:
     * it is how a teacher corrects a comment.
     */
    public HomeworkSubmission checkHomeworkSubmission(UUID id, CheckHomeworkSubmissionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CHECK);
        String comments = text("teacherComments", request == null ? null : request.getTeacherComments());
        Optional<UUID> actor = Actor.current();
        return doInWriteTransaction(() -> {
            HomeworkSubmissionEntity submission = lockWritable(scope, id, request == null ? null : request.getEtag(),
                    submissions);
            OffsetDateTime now = now();
            if (submissions.check(id, submission.getRowVersion(), comments, actor.orElse(null),
                    LocalDate.now(clock.withZone(ZoneOffset.UTC)), now) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(SUBMISSION, id, "check",
                    List.of("status", "checkedByUserId", "checkedOn", "teacherComments"), null);
            outbox.publish(SUBMISSION + ".check", SUBMISSION, id, AssessmentEvents.submission(submission));
            return submissionMapper.toDto(submissions.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(SUBMISSION, id)));
        });
    }

    /**
     * assessment.ReplyToHomeworkFeedback: the student answers the teacher's comments on their own submission. Only
     * the account that handed the submission in may reply (403 for anyone else who can see it), and only once the
     * teacher has commented (409 before).
     */
    public HomeworkSubmission replyToHomeworkFeedback(UUID id, ReplyToHomeworkFeedbackRequest request) {
        validator.validate(request);
        String reply = text("studentReply", request.getStudentReply());
        if (reply == null) {
            throw new ValidationFailedException(List.of(new FieldError("studentReply", "must not be blank", "NotBlank")));
        }
        DataScope scope = DataScope.current(PERMISSION_REPLY);
        Optional<UUID> actor = Actor.current();
        return doInWriteTransaction(() -> {
            HomeworkSubmissionEntity submission = lockWritable(scope, id, null, submissions);
            if (actor.isEmpty() || !actor.get().equals(submission.getCreatedBy())) {
                throw new PermissionDeniedException("Only the student who handed in the submission can reply.");
            }
            requireVersion(request.getEtag(), submission.getRowVersion());
            if (submission.getTeacherComments() == null) {
                throw new InvalidStateException("The teacher has not commented on the submission yet.");
            }
            if (submissions.reply(id, submission.getRowVersion(), reply, actor.get(), now()) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(SUBMISSION, id, "reply", List.of("studentReply"), null);
            outbox.publish(SUBMISSION + ".reply", SUBMISSION, id, AssessmentEvents.submission(submission));
            return submissionMapper.toDto(submissions.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(SUBMISSION, id)));
        });
    }

    /** The homework, locked, if the caller may see it (else 404) and change it (else 403) under the etag sent. */
    private HomeworkEntity lockWritable(DataScope scope, UUID id, String etag) {
        HomeworkEntity homework = homeworks.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(HOMEWORK, id));
        Optional<UUID> institution = Optional.of(homework.getInstitutionId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(HOMEWORK, id);
        }
        scope.requireWritable(institution);
        requireVersion(etag, homework.getRowVersion());
        return homework;
    }

    /** The submission, locked, under the same rules; its institution is its homework's. */
    private static HomeworkSubmissionEntity lockWritable(DataScope scope, UUID id, String etag,
            HomeworkSubmissionRepository submissions) {
        HomeworkSubmissionEntity submission = submissions.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(SUBMISSION, id));
        Optional<UUID> institution = submissions.institutionOf(submission.getHomeworkId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(SUBMISSION, id);
        }
        scope.requireWritable(institution);
        requireVersion(etag, submission.getRowVersion());
        return submission;
    }

    private static void requireVersion(String etag, int version) {
        OptionalInt expected = Etags.bodyVersion(etag);
        if (expected.isPresent() && expected.getAsInt() != version) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
    }

    private static void requireStatus(HomeworkEntity homework, String status) {
        if (!status.equals(homework.getStatus())) {
            throw new InvalidStateException("The homework cannot be changed this way in its current state.");
        }
    }

    /** Moves the locked homework to a state; {@code assignedOn} is kept when the homework already has one. */
    private void move(HomeworkEntity homework, String status, OffsetDateTime assignedOn, OffsetDateTime now,
            Optional<UUID> actor) {
        if (homeworks.transition(homework.getId(), homework.getRowVersion(), status, assignedOn, now,
                actor.orElse(null)) == 0) {
            throw new PreconditionFailedException("The row changed since it was read.");
        }
    }

    private HomeworkEntity reread(UUID id) {
        return homeworks.findById(id).orElseThrow(() -> new ResourceNotFoundException(HOMEWORK, id));
    }

    /** A comment, reply or reason as it is kept: trimmed, null when blank, refused when longer than the limit. */
    private static String text(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > MAX_TEXT) {
            throw new ValidationFailedException(List.of(
                    new FieldError(field, "must be at most " + MAX_TEXT + " characters", "Size")));
        }
        return trimmed;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
