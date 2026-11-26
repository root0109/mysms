package com.mysms.learning.lms.controller;

import com.mysms.learning.lms.api.model.TimetableEntry;
import com.mysms.learning.lms.api.model.RosterEntry;
import com.mysms.learning.lms.api.model.CohortStudentProgress;
import com.mysms.learning.lms.api.model.CohortPerformance;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.CohortApi;
import com.mysms.learning.lms.api.model.ActivateCohortRequest;
import com.mysms.learning.lms.api.model.ApplyTimetableTemplateRequest;
import com.mysms.learning.lms.api.model.ArchiveCohortRequest;
import com.mysms.learning.lms.api.model.BatchEnrollInCohortRequest;
import com.mysms.learning.lms.api.model.BatchEnrollPreviewList;
import com.mysms.learning.lms.api.model.ChapterList;
import com.mysms.learning.lms.api.model.Cohort;
import com.mysms.learning.lms.api.model.CohortActivity;
import com.mysms.learning.lms.api.model.CohortActivityUpdate;
import com.mysms.learning.lms.api.model.CohortCourse;
import com.mysms.learning.lms.api.model.CohortCourseUpdate;
import com.mysms.learning.lms.api.model.CohortEnrollment;
import com.mysms.learning.lms.api.model.CohortEnrollmentList;
import com.mysms.learning.lms.api.model.CohortFeedback;
import com.mysms.learning.lms.api.model.CohortFeedbackUpdate;
import com.mysms.learning.lms.api.model.CohortGroup;
import com.mysms.learning.lms.api.model.CohortGroupUpdate;
import com.mysms.learning.lms.api.model.CohortHoliday;
import com.mysms.learning.lms.api.model.CohortHolidayUpdate;
import com.mysms.learning.lms.api.model.CohortScheduleSlot;
import com.mysms.learning.lms.api.model.CohortScheduleSlotUpdate;
import com.mysms.learning.lms.api.model.CohortSession;
import com.mysms.learning.lms.api.model.CohortSessionUpdate;
import com.mysms.learning.lms.api.model.CohortTeacher;
import com.mysms.learning.lms.api.model.CohortTeacherUpdate;
import com.mysms.learning.lms.api.model.CohortUpdate;
import com.mysms.learning.lms.api.model.CopyCourseOutlineToCohortRequest;
import com.mysms.learning.lms.api.model.DeactivateCohortRequest;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.api.model.EnrollInCohortRequest;
import com.mysms.learning.lms.api.model.EvaluateReleaseRulesRequest;
import com.mysms.learning.lms.api.model.ExportGradesRequest;
import com.mysms.learning.lms.api.model.Forum;
import com.mysms.learning.lms.api.model.ForumUpdate;
import com.mysms.learning.lms.api.model.GenerateCohortCertificatesRequest;
import com.mysms.learning.lms.api.model.Gradebook;
import com.mysms.learning.lms.api.model.ImportCohortMembersRequest;
import com.mysms.learning.lms.api.model.Operation;
import com.mysms.learning.lms.api.model.PlagiarismCohortLink;
import com.mysms.learning.lms.api.model.PlagiarismCohortLinkUpdate;
import com.mysms.learning.lms.api.model.PublishCohortRequest;
import com.mysms.learning.lms.api.model.PublishFinalGradesRequest;
import com.mysms.learning.lms.api.model.ReleaseGrantList;
import com.mysms.learning.lms.api.model.RosterEntryList;
import com.mysms.learning.lms.api.model.SendCohortAnnouncementRequest;
import com.mysms.learning.lms.api.model.SendResult;
import com.mysms.learning.lms.api.model.SetCohortMembersRequest;
import com.mysms.learning.lms.api.model.SetStudentAccessRequest;
import com.mysms.learning.lms.api.model.TimetableEntryList;
import com.mysms.learning.lms.api.model.UnpublishCohortRequest;
import com.mysms.learning.lms.api.model.UnreadCounts;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.CertificateService;
import com.mysms.learning.lms.business.CohortEnrollmentService;
import com.mysms.learning.lms.business.CohortGradebookService;
import com.mysms.learning.lms.business.CohortReleaseService;
import com.mysms.learning.lms.business.CohortService;
import com.mysms.learning.lms.business.TimetableEntryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Cohort operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class CohortController implements CohortApi {

    private final CohortService cohortService;
    private final CohortEnrollmentService cohortEnrollmentService;
    private final TimetableEntryService timetableEntryService;
    private final CohortGradebookService gradebookService;
    private final CohortReleaseService releaseService;
    private final CertificateService certificateService;

    public CohortController(CohortService cohortService, CohortEnrollmentService cohortEnrollmentService,
            TimetableEntryService timetableEntryService, CohortGradebookService gradebookService,
            CohortReleaseService releaseService, CertificateService certificateService) {
        this.certificateService = certificateService;
        this.releaseService = releaseService;
        this.cohortService = cohortService;
        this.cohortEnrollmentService = cohortEnrollmentService;
        this.timetableEntryService = timetableEntryService;
        this.gradebookService = gradebookService;
    }

    @Override
    public ResponseEntity<Cohort> activateCohort(UUID cohortId,
            ActivateCohortRequest activateCohortRequest, UUID idempotencyKey) {
        Cohort changed = cohortService.activateCohort(cohortId,
                activateCohortRequest == null ? null : activateCohortRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<TimetableEntryList> applyTimetableTemplate(UUID cohortId,
            ApplyTimetableTemplateRequest applyTimetableTemplateRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(timetableEntryService.applyTimetableTemplate(cohortId,
                applyTimetableTemplateRequest.getTimetableTemplateId()));
    }

    @Override
    public ResponseEntity<Cohort> archiveCohort(UUID cohortId, ArchiveCohortRequest archiveCohortRequest,
            UUID idempotencyKey) {
        Cohort changed = cohortService.archiveCohort(cohortId,
                archiveCohortRequest == null ? null : archiveCohortRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<BatchEnrollPreviewList> batchEnrollInCohort(UUID cohortId, UUID idempotencyKey,
            BatchEnrollInCohortRequest batchEnrollInCohortRequest) {
        return ResponseEntity.ok(cohortEnrollmentService.batchEnrollInCohort(cohortId,
                batchEnrollInCohortRequest.getUserIds(), batchEnrollInCohortRequest.getPaymentIds(),
                Boolean.TRUE.equals(batchEnrollInCohortRequest.getValidateOnly())));
    }

    @Override
    public ResponseEntity<ChapterList> copyCourseOutlineToCohort(UUID cohortId,
            CopyCourseOutlineToCohortRequest copyCourseOutlineToCohortRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(cohortService.copyCourseOutlineToCohort(cohortId,
                copyCourseOutlineToCohortRequest.getCourseId(), copyCourseOutlineToCohortRequest.getChapterIds(),
                copyCourseOutlineToCohortRequest.getShiftDatesFrom()));
    }

    @Override
    public ResponseEntity<Cohort> createCohort(Cohort cohort, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Cohort created = cohortService.createCohort(cohort, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CohortActivity> createCohortActivity(UUID cohortId,
            CohortActivity cohortActivity, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CohortActivity created = cohortService.createCohortActivity(cohortId, cohortActivity, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/activities/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CohortCourse> createCohortCourse(UUID cohortId, CohortCourse cohortCourse,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CohortCourse created = cohortService.createCohortCourse(cohortId, cohortCourse, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/courses/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CohortFeedback> createCohortFeedback(UUID cohortId,
            CohortFeedback cohortFeedback, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CohortFeedback created = cohortService.createCohortFeedback(cohortId, cohortFeedback, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/feedback/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CohortGroup> createCohortGroup(UUID cohortId, CohortGroup cohortGroup,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CohortGroup created = cohortService.createCohortGroup(cohortId, cohortGroup, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/groups/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CohortHoliday> createCohortHoliday(UUID cohortId, CohortHoliday cohortHoliday,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CohortHoliday created = cohortService.createCohortHoliday(cohortId, cohortHoliday, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/holidays/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CohortScheduleSlot> createCohortScheduleSlot(UUID cohortId,
            CohortScheduleSlot cohortScheduleSlot, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CohortScheduleSlot created =
                cohortService.createCohortScheduleSlot(cohortId, cohortScheduleSlot, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/schedule-slots/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CohortSession> createCohortSession(UUID cohortId, CohortSession cohortSession,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CohortSession created = cohortService.createCohortSession(cohortId, cohortSession, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/sessions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CohortTeacher> createCohortTeacher(UUID cohortId, CohortTeacher cohortTeacher,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CohortTeacher created = cohortService.createCohortTeacher(cohortId, cohortTeacher, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/teachers/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Forum> createForum(UUID cohortId, Forum forum, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Forum created = cohortService.createForum(cohortId, forum, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/cohorts/" + cohortId + "/forums/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PlagiarismCohortLink> createPlagiarismCohortLink(UUID cohortId,
            PlagiarismCohortLink plagiarismCohortLink, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PlagiarismCohortLink created =
                cohortService.createPlagiarismCohortLink(cohortId, plagiarismCohortLink, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/cohorts/" + cohortId + "/plagiarism-links/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Cohort> deactivateCohort(UUID cohortId,
            DeactivateCohortRequest deactivateCohortRequest, UUID idempotencyKey) {
        Cohort changed = cohortService.deactivateCohort(cohortId,
                deactivateCohortRequest == null ? null : deactivateCohortRequest.getEtag(),
                deactivateCohortRequest == null ? null : deactivateCohortRequest.getReason());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> deleteCohort(UUID cohortId, String ifMatch) {
        cohortService.deleteCohort(cohortId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCohortActivity(UUID cohortId, UUID cohortActivityId,
            String ifMatch) {
        cohortService.deleteCohortActivity(cohortId, cohortActivityId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCohortCourse(UUID cohortId, UUID cohortCourseId, String ifMatch) {
        cohortService.deleteCohortCourse(cohortId, cohortCourseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCohortFeedback(UUID cohortId, UUID cohortFeedbackId,
            String ifMatch) {
        cohortService.deleteCohortFeedback(cohortId, cohortFeedbackId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCohortGroup(UUID cohortId, UUID cohortGroupId, String ifMatch) {
        cohortService.deleteCohortGroup(cohortId, cohortGroupId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCohortHoliday(UUID cohortId, UUID cohortHolidayId, String ifMatch) {
        cohortService.deleteCohortHoliday(cohortId, cohortHolidayId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCohortScheduleSlot(UUID cohortId, UUID cohortScheduleSlotId,
            String ifMatch) {
        cohortService.deleteCohortScheduleSlot(cohortId, cohortScheduleSlotId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCohortSession(UUID cohortId, UUID cohortSessionId, String ifMatch) {
        cohortService.deleteCohortSession(cohortId, cohortSessionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCohortTeacher(UUID cohortId, UUID cohortTeacherId, String ifMatch) {
        cohortService.deleteCohortTeacher(cohortId, cohortTeacherId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteForum(UUID cohortId, UUID forumId, String ifMatch) {
        cohortService.deleteForum(cohortId, forumId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePlagiarismCohortLink(UUID cohortId, UUID plagiarismCohortLinkId,
            String ifMatch) {
        cohortService.deletePlagiarismCohortLink(cohortId, plagiarismCohortLinkId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CohortEnrollment> enrollInCohort(UUID cohortId,
            EnrollInCohortRequest enrollInCohortRequest, UUID idempotencyKey) {
        CohortEnrollment enrolled = cohortEnrollmentService.enrollInCohort(cohortId,
                enrollInCohortRequest == null ? null : enrollInCohortRequest.getPaymentId());
        return ResponseEntity.ok().eTag(enrolled.getEtag()).body(enrolled);
    }

    @Override
    public ResponseEntity<ReleaseGrantList> evaluateReleaseRules(UUID cohortId,
            EvaluateReleaseRulesRequest evaluateReleaseRulesRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(releaseService.evaluateReleaseRules(cohortId,
                evaluateReleaseRulesRequest == null ? null : evaluateReleaseRulesRequest.getUserId()));
    }

    @Override
    public ResponseEntity<Operation> exportGrades(UUID cohortId, ExportGradesRequest exportGradesRequest,
            UUID idempotencyKey) {
        return ResponseEntity.accepted().body(gradebookService.exportGrades(cohortId, exportGradesRequest));
    }

    @Override
    public ResponseEntity<Operation> generateCohortCertificates(UUID cohortId,
            GenerateCohortCertificatesRequest generateCohortCertificatesRequest, UUID idempotencyKey) {
        return ResponseEntity.accepted().body(certificateService.generateCohortCertificates(cohortId,
                generateCohortCertificatesRequest.getUserIds(), generateCohortCertificatesRequest.getTemplateCode(),
                generateCohortCertificatesRequest.getIssueDate(),
                Boolean.TRUE.equals(generateCohortCertificatesRequest.getPublish())));
    }

    @Override
    public ResponseEntity<Cohort> getCohort(UUID cohortId, String fields) {
        Cohort found = cohortService.getCohort(cohortId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CohortActivity> getCohortActivity(UUID cohortId, UUID cohortActivityId,
            String fields) {
        CohortActivity found = cohortService.getCohortActivity(cohortId, cohortActivityId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CohortCourse> getCohortCourse(UUID cohortId, UUID cohortCourseId,
            String fields) {
        CohortCourse found = cohortService.getCohortCourse(cohortId, cohortCourseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CohortFeedback> getCohortFeedback(UUID cohortId, UUID cohortFeedbackId,
            String fields) {
        CohortFeedback found = cohortService.getCohortFeedback(cohortId, cohortFeedbackId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CohortGroup> getCohortGroup(UUID cohortId, UUID cohortGroupId, String fields) {
        CohortGroup found = cohortService.getCohortGroup(cohortId, cohortGroupId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CohortHoliday> getCohortHoliday(UUID cohortId, UUID cohortHolidayId,
            String fields) {
        CohortHoliday found = cohortService.getCohortHoliday(cohortId, cohortHolidayId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CohortPerformance>> getCohortPerformance(UUID cohortId, Integer page, Integer size,
            String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(gradebookService.getCohortPerformance(cohortId, page, size,
                cursor)));
    }

    @Override
    public ResponseEntity<PagingInfo<CohortStudentProgress>> getCohortProgress(UUID cohortId, UUID userId, Integer page,
            Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(gradebookService.getCohortProgress(cohortId, userId, page, size,
                cursor)));
    }

    @Override
    public ResponseEntity<PagingInfo<RosterEntry>> getCohortRoster(UUID cohortId, String roleCode,
            Boolean includeInactive, Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(cohortEnrollmentService.getCohortRoster(cohortId, roleCode,
                includeInactive, page, size, cursor)));
    }

    @Override
    public ResponseEntity<CohortScheduleSlot> getCohortScheduleSlot(UUID cohortId,
            UUID cohortScheduleSlotId, String fields) {
        CohortScheduleSlot found = cohortService.getCohortScheduleSlot(cohortId, cohortScheduleSlotId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CohortSession> getCohortSession(UUID cohortId, UUID cohortSessionId,
            String fields) {
        CohortSession found = cohortService.getCohortSession(cohortId, cohortSessionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<DownloadLink> getCohortSyllabus(UUID cohortId, String format) {
        return ResponseEntity.ok(cohortService.getCohortSyllabus(cohortId, format));
    }

    @Override
    public ResponseEntity<CohortTeacher> getCohortTeacher(UUID cohortId, UUID cohortTeacherId,
            String fields) {
        CohortTeacher found = cohortService.getCohortTeacher(cohortId, cohortTeacherId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<TimetableEntry>> getCohortTimetable(UUID cohortId, Integer page, Integer size,
            String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(timetableEntryService.getCohortTimetable(cohortId, page, size,
                cursor)));
    }

    @Override
    public ResponseEntity<Forum> getForum(UUID cohortId, UUID forumId, String fields) {
        Forum found = cohortService.getForum(cohortId, forumId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Gradebook> getGradebook(UUID cohortId, UUID userId, UUID gradingCategoryId,
            Integer page, Integer size, String cursor) {
        CohortGradebookService.GradebookPage read = gradebookService.getGradebook(cohortId, userId, gradingCategoryId,
                page, size, cursor);
        Gradebook gradebook = read.gradebook();
        gradebook.setPaging(PagingUtil.pagingInfo(read.rows()).paging());
        return ResponseEntity.ok(gradebook);
    }

    @Override
    public ResponseEntity<PlagiarismCohortLink> getPlagiarismCohortLink(UUID cohortId,
            UUID plagiarismCohortLinkId, String fields) {
        PlagiarismCohortLink found =
                cohortService.getPlagiarismCohortLink(cohortId, plagiarismCohortLinkId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UnreadCounts> getUnreadCounts() {
        return ResponseEntity.ok(cohortService.getUnreadCounts());
    }

    @Override
    public ResponseEntity<Operation> importCohortMembers(UUID cohortId,
            ImportCohortMembersRequest importCohortMembersRequest, UUID idempotencyKey) {
        return ResponseEntity.accepted().body(cohortEnrollmentService.importCohortMembers(cohortId,
                importCohortMembersRequest));
    }

    @Override
    public ResponseEntity<PagingInfo<CohortActivity>> listCohortActivities(UUID cohortId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CohortActivity> list =
                PagingUtil.pagingInfo(cohortService.listCohortActivities(cohortId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CohortCourse>> listCohortCourses(UUID cohortId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CohortCourse> list =
                PagingUtil.pagingInfo(cohortService.listCohortCourses(cohortId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CohortFeedback>> listCohortFeedback(UUID cohortId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CohortFeedback> list =
                PagingUtil.pagingInfo(cohortService.listCohortFeedbacks(cohortId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CohortGroup>> listCohortGroups(UUID cohortId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CohortGroup> list =
                PagingUtil.pagingInfo(cohortService.listCohortGroups(cohortId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CohortHoliday>> listCohortHolidays(UUID cohortId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CohortHoliday> list =
                PagingUtil.pagingInfo(cohortService.listCohortHolidays(cohortId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CohortScheduleSlot>> listCohortScheduleSlots(UUID cohortId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CohortScheduleSlot> list =
                PagingUtil.pagingInfo(cohortService.listCohortScheduleSlots(cohortId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CohortSession>> listCohortSessions(UUID cohortId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CohortSession> list =
                PagingUtil.pagingInfo(cohortService.listCohortSessions(cohortId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CohortTeacher>> listCohortTeachers(UUID cohortId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CohortTeacher> list =
                PagingUtil.pagingInfo(cohortService.listCohortTeachers(cohortId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Cohort>> listCohorts(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Cohort> list = PagingUtil.pagingInfo(cohortService.listCohorts(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Forum>> listForums(UUID cohortId, Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Forum> list =
                PagingUtil.pagingInfo(cohortService.listForums(cohortId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PlagiarismCohortLink>> listPlagiarismCohortLinks(UUID cohortId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PlagiarismCohortLink> list =
                PagingUtil.pagingInfo(cohortService.listPlagiarismCohortLinks(cohortId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Cohort> publishCohort(UUID cohortId, PublishCohortRequest publishCohortRequest,
            UUID idempotencyKey) {
        Cohort changed = cohortService.publishCohort(cohortId,
                publishCohortRequest == null ? null : publishCohortRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<CohortEnrollmentList> publishFinalGrades(UUID cohortId,
            PublishFinalGradesRequest publishFinalGradesRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(gradebookService.publishFinalGrades(cohortId,
                publishFinalGradesRequest == null ? null : publishFinalGradesRequest.getUserIds()));
    }

    @Override
    public ResponseEntity<SendResult> sendCohortAnnouncement(UUID cohortId,
            SendCohortAnnouncementRequest sendCohortAnnouncementRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(cohortEnrollmentService.sendCohortAnnouncement(cohortId,
                sendCohortAnnouncementRequest));
    }

    @Override
    public ResponseEntity<RosterEntryList> setCohortMembers(UUID cohortId,
            SetCohortMembersRequest setCohortMembersRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(cohortEnrollmentService.setCohortMembers(cohortId, setCohortMembersRequest.getMembers(),
                setCohortMembersRequest.getRemoveUserIds()));
    }

    @Override
    public ResponseEntity<Cohort> setStudentAccess(UUID cohortId,
            SetStudentAccessRequest setStudentAccessRequest, UUID idempotencyKey) {
        Cohort changed = cohortService.setStudentAccess(cohortId, setStudentAccessRequest.getStudentAccessFrom(),
                setStudentAccessRequest.getStudentAccessUntil());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Cohort> unpublishCohort(UUID cohortId,
            UnpublishCohortRequest unpublishCohortRequest, UUID idempotencyKey) {
        Cohort changed = cohortService.unpublishCohort(cohortId,
                unpublishCohortRequest == null ? null : unpublishCohortRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Cohort> updateCohort(UUID cohortId, String ifMatch, CohortUpdate cohortUpdate,
            Boolean validateOnly) {
        Cohort updated = cohortService.updateCohort(cohortId, ifMatch, cohortUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CohortActivity> updateCohortActivity(UUID cohortId, UUID cohortActivityId,
            String ifMatch, CohortActivityUpdate cohortActivityUpdate, Boolean validateOnly) {
        CohortActivity updated = cohortService.updateCohortActivity(cohortId, cohortActivityId, ifMatch,
                cohortActivityUpdate, Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CohortCourse> updateCohortCourse(UUID cohortId, UUID cohortCourseId,
            String ifMatch, CohortCourseUpdate cohortCourseUpdate, Boolean validateOnly) {
        CohortCourse updated =
                cohortService.updateCohortCourse(cohortId, cohortCourseId, ifMatch, cohortCourseUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CohortFeedback> updateCohortFeedback(UUID cohortId, UUID cohortFeedbackId,
            String ifMatch, CohortFeedbackUpdate cohortFeedbackUpdate, Boolean validateOnly) {
        CohortFeedback updated =
                cohortService.updateCohortFeedback(cohortId, cohortFeedbackId, ifMatch, cohortFeedbackUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CohortGroup> updateCohortGroup(UUID cohortId, UUID cohortGroupId,
            String ifMatch, CohortGroupUpdate cohortGroupUpdate, Boolean validateOnly) {
        CohortGroup updated =
                cohortService.updateCohortGroup(cohortId, cohortGroupId, ifMatch, cohortGroupUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CohortHoliday> updateCohortHoliday(UUID cohortId, UUID cohortHolidayId,
            String ifMatch, CohortHolidayUpdate cohortHolidayUpdate, Boolean validateOnly) {
        CohortHoliday updated =
                cohortService.updateCohortHoliday(cohortId, cohortHolidayId, ifMatch, cohortHolidayUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CohortScheduleSlot> updateCohortScheduleSlot(UUID cohortId,
            UUID cohortScheduleSlotId, String ifMatch, CohortScheduleSlotUpdate cohortScheduleSlotUpdate,
            Boolean validateOnly) {
        CohortScheduleSlot updated =
                cohortService.updateCohortScheduleSlot(cohortId, cohortScheduleSlotId, ifMatch,
                        cohortScheduleSlotUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CohortSession> updateCohortSession(UUID cohortId, UUID cohortSessionId,
            String ifMatch, CohortSessionUpdate cohortSessionUpdate, Boolean validateOnly) {
        CohortSession updated =
                cohortService.updateCohortSession(cohortId, cohortSessionId, ifMatch, cohortSessionUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CohortTeacher> updateCohortTeacher(UUID cohortId, UUID cohortTeacherId,
            String ifMatch, CohortTeacherUpdate cohortTeacherUpdate, Boolean validateOnly) {
        CohortTeacher updated =
                cohortService.updateCohortTeacher(cohortId, cohortTeacherId, ifMatch, cohortTeacherUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Forum> updateForum(UUID cohortId, UUID forumId, String ifMatch,
            ForumUpdate forumUpdate, Boolean validateOnly) {
        Forum updated = cohortService.updateForum(cohortId, forumId, ifMatch, forumUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PlagiarismCohortLink> updatePlagiarismCohortLink(UUID cohortId,
            UUID plagiarismCohortLinkId, String ifMatch, PlagiarismCohortLinkUpdate plagiarismCohortLinkUpdate,
            Boolean validateOnly) {
        PlagiarismCohortLink updated =
                cohortService.updatePlagiarismCohortLink(cohortId, plagiarismCohortLinkId, ifMatch,
                        plagiarismCohortLinkUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
