package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.common.v1.Pagination;
import com.mysms.core.paging.PageRequest;
import com.mysms.learning.lms.api.model.BatchEnrollPreviewList;
import com.mysms.learning.lms.api.model.CohortEnrollmentList;
import com.mysms.learning.lms.api.model.CohortPerformance;
import com.mysms.learning.lms.api.model.CohortStudentProgress;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.api.model.Operation;
import com.mysms.learning.lms.api.model.SendResult;
import com.mysms.learning.lms.api.model.ReleaseGrantList;
import com.mysms.learning.lms.api.model.ChapterList;
import com.mysms.learning.lms.api.model.Cohort;
import com.mysms.learning.lms.api.model.CohortEnrollment;
import com.mysms.learning.lms.api.model.RosterEntry;
import com.mysms.learning.lms.api.model.RosterEntryList;
import com.mysms.learning.lms.api.model.TimetableEntry;
import com.mysms.learning.lms.api.model.TimetableEntryList;
import com.mysms.learning.lms.api.model.CohortActivity;
import com.mysms.learning.lms.api.model.CohortActivityUpdate;
import com.mysms.learning.lms.api.model.CohortCourse;
import com.mysms.learning.lms.api.model.CohortCourseUpdate;
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
import com.mysms.learning.lms.api.model.Forum;
import com.mysms.learning.lms.api.model.ForumUpdate;
import com.mysms.learning.lms.api.model.PlagiarismCohortLink;
import com.mysms.learning.lms.api.model.PlagiarismCohortLinkUpdate;
import com.mysms.learning.lms.business.CertificateService;
import com.mysms.learning.lms.business.CohortEnrollmentService;
import com.mysms.learning.lms.business.CohortGradebookService;
import com.mysms.learning.lms.business.CohortReleaseService;
import com.mysms.learning.lms.business.CohortService;
import com.mysms.learning.lms.business.TimetableEntryService;
import com.mysms.lms.v1.ActivateCohortRequest;
import com.mysms.lms.v1.ActivateCohortResponse;
import com.mysms.lms.v1.ApplyTimetableTemplateRequest;
import com.mysms.lms.v1.ApplyTimetableTemplateResponse;
import com.mysms.lms.v1.ArchiveCohortRequest;
import com.mysms.lms.v1.ArchiveCohortResponse;
import com.mysms.lms.v1.BatchEnrollInCohortRequest;
import com.mysms.lms.v1.BatchEnrollInCohortResponse;
import com.mysms.lms.v1.CohortServiceGrpc;
import com.mysms.lms.v1.CopyCourseOutlineToCohortRequest;
import com.mysms.lms.v1.CopyCourseOutlineToCohortResponse;
import com.mysms.lms.v1.CreateCohortActivityRequest;
import com.mysms.lms.v1.CreateCohortActivityResponse;
import com.mysms.lms.v1.CreateCohortCourseRequest;
import com.mysms.lms.v1.CreateCohortCourseResponse;
import com.mysms.lms.v1.CreateCohortFeedbackRequest;
import com.mysms.lms.v1.CreateCohortFeedbackResponse;
import com.mysms.lms.v1.CreateCohortGroupRequest;
import com.mysms.lms.v1.CreateCohortGroupResponse;
import com.mysms.lms.v1.CreateCohortHolidayRequest;
import com.mysms.lms.v1.CreateCohortHolidayResponse;
import com.mysms.lms.v1.CreateCohortRequest;
import com.mysms.lms.v1.CreateCohortResponse;
import com.mysms.lms.v1.CreateCohortScheduleSlotRequest;
import com.mysms.lms.v1.CreateCohortScheduleSlotResponse;
import com.mysms.lms.v1.CreateCohortSessionRequest;
import com.mysms.lms.v1.CreateCohortSessionResponse;
import com.mysms.lms.v1.CreateCohortTeacherRequest;
import com.mysms.lms.v1.CreateCohortTeacherResponse;
import com.mysms.lms.v1.CreateForumRequest;
import com.mysms.lms.v1.CreateForumResponse;
import com.mysms.lms.v1.CreatePlagiarismCohortLinkRequest;
import com.mysms.lms.v1.CreatePlagiarismCohortLinkResponse;
import com.mysms.lms.v1.DeactivateCohortRequest;
import com.mysms.lms.v1.DeactivateCohortResponse;
import com.mysms.lms.v1.DeleteCohortActivityRequest;
import com.mysms.lms.v1.DeleteCohortActivityResponse;
import com.mysms.lms.v1.DeleteCohortCourseRequest;
import com.mysms.lms.v1.DeleteCohortCourseResponse;
import com.mysms.lms.v1.DeleteCohortFeedbackRequest;
import com.mysms.lms.v1.DeleteCohortFeedbackResponse;
import com.mysms.lms.v1.DeleteCohortGroupRequest;
import com.mysms.lms.v1.DeleteCohortGroupResponse;
import com.mysms.lms.v1.DeleteCohortHolidayRequest;
import com.mysms.lms.v1.DeleteCohortHolidayResponse;
import com.mysms.lms.v1.DeleteCohortRequest;
import com.mysms.lms.v1.DeleteCohortResponse;
import com.mysms.lms.v1.DeleteCohortScheduleSlotRequest;
import com.mysms.lms.v1.DeleteCohortScheduleSlotResponse;
import com.mysms.lms.v1.DeleteCohortSessionRequest;
import com.mysms.lms.v1.DeleteCohortSessionResponse;
import com.mysms.lms.v1.DeleteCohortTeacherRequest;
import com.mysms.lms.v1.DeleteCohortTeacherResponse;
import com.mysms.lms.v1.DeleteForumRequest;
import com.mysms.lms.v1.DeleteForumResponse;
import com.mysms.lms.v1.DeletePlagiarismCohortLinkRequest;
import com.mysms.lms.v1.DeletePlagiarismCohortLinkResponse;
import com.mysms.lms.v1.EnrollInCohortRequest;
import com.mysms.lms.v1.EnrollInCohortResponse;
import com.mysms.lms.v1.EvaluateReleaseRulesRequest;
import com.mysms.lms.v1.EvaluateReleaseRulesResponse;
import com.mysms.lms.v1.ExportGradesRequest;
import com.mysms.lms.v1.ExportGradesResponse;
import com.mysms.lms.v1.GenerateCohortCertificatesRequest;
import com.mysms.lms.v1.GenerateCohortCertificatesResponse;
import com.mysms.lms.v1.GetCohortActivityRequest;
import com.mysms.lms.v1.GetCohortActivityResponse;
import com.mysms.lms.v1.GetCohortCourseRequest;
import com.mysms.lms.v1.GetCohortCourseResponse;
import com.mysms.lms.v1.GetCohortFeedbackRequest;
import com.mysms.lms.v1.GetCohortFeedbackResponse;
import com.mysms.lms.v1.GetCohortGroupRequest;
import com.mysms.lms.v1.GetCohortGroupResponse;
import com.mysms.lms.v1.GetCohortHolidayRequest;
import com.mysms.lms.v1.GetCohortHolidayResponse;
import com.mysms.lms.v1.GetCohortPerformanceRequest;
import com.mysms.lms.v1.GetCohortPerformanceResponse;
import com.mysms.lms.v1.GetCohortProgressRequest;
import com.mysms.lms.v1.GetCohortProgressResponse;
import com.mysms.lms.v1.GetCohortRequest;
import com.mysms.lms.v1.GetCohortResponse;
import com.mysms.lms.v1.GetCohortRosterRequest;
import com.mysms.lms.v1.GetCohortRosterResponse;
import com.mysms.lms.v1.GetCohortScheduleSlotRequest;
import com.mysms.lms.v1.GetCohortScheduleSlotResponse;
import com.mysms.lms.v1.GetCohortSessionRequest;
import com.mysms.lms.v1.GetCohortSessionResponse;
import com.mysms.lms.v1.GetCohortSyllabusRequest;
import com.mysms.lms.v1.GetCohortSyllabusResponse;
import com.mysms.lms.v1.GetCohortTeacherRequest;
import com.mysms.lms.v1.GetCohortTeacherResponse;
import com.mysms.lms.v1.GetCohortTimetableRequest;
import com.mysms.lms.v1.GetCohortTimetableResponse;
import com.mysms.lms.v1.GetForumRequest;
import com.mysms.lms.v1.GetForumResponse;
import com.mysms.lms.v1.GetGradebookRequest;
import com.mysms.lms.v1.GetGradebookResponse;
import com.mysms.lms.v1.GetPlagiarismCohortLinkRequest;
import com.mysms.lms.v1.GetPlagiarismCohortLinkResponse;
import com.mysms.lms.v1.GetUnreadCountsRequest;
import com.mysms.lms.v1.GetUnreadCountsResponse;
import com.mysms.lms.v1.ImportCohortMembersRequest;
import com.mysms.lms.v1.ImportCohortMembersResponse;
import com.mysms.lms.v1.ListCohortActivitiesRequest;
import com.mysms.lms.v1.ListCohortActivitiesResponse;
import com.mysms.lms.v1.ListCohortCoursesRequest;
import com.mysms.lms.v1.ListCohortCoursesResponse;
import com.mysms.lms.v1.ListCohortFeedbackRequest;
import com.mysms.lms.v1.ListCohortFeedbackResponse;
import com.mysms.lms.v1.ListCohortGroupsRequest;
import com.mysms.lms.v1.ListCohortGroupsResponse;
import com.mysms.lms.v1.ListCohortHolidaysRequest;
import com.mysms.lms.v1.ListCohortHolidaysResponse;
import com.mysms.lms.v1.ListCohortScheduleSlotsRequest;
import com.mysms.lms.v1.ListCohortScheduleSlotsResponse;
import com.mysms.lms.v1.ListCohortSessionsRequest;
import com.mysms.lms.v1.ListCohortSessionsResponse;
import com.mysms.lms.v1.ListCohortTeachersRequest;
import com.mysms.lms.v1.ListCohortTeachersResponse;
import com.mysms.lms.v1.ListCohortsRequest;
import com.mysms.lms.v1.ListCohortsResponse;
import com.mysms.lms.v1.ListForumsRequest;
import com.mysms.lms.v1.ListForumsResponse;
import com.mysms.lms.v1.ListPlagiarismCohortLinksRequest;
import com.mysms.lms.v1.ListPlagiarismCohortLinksResponse;
import com.mysms.lms.v1.PublishCohortRequest;
import com.mysms.lms.v1.PublishCohortResponse;
import com.mysms.lms.v1.PublishFinalGradesRequest;
import com.mysms.lms.v1.PublishFinalGradesResponse;
import com.mysms.lms.v1.SendCohortAnnouncementRequest;
import com.mysms.lms.v1.SendCohortAnnouncementResponse;
import com.mysms.lms.v1.SetCohortMembersRequest;
import com.mysms.lms.v1.SetCohortMembersResponse;
import com.mysms.lms.v1.SetStudentAccessRequest;
import com.mysms.lms.v1.SetStudentAccessResponse;
import com.mysms.lms.v1.UnpublishCohortRequest;
import com.mysms.lms.v1.UnpublishCohortResponse;
import com.mysms.lms.v1.UpdateCohortActivityRequest;
import com.mysms.lms.v1.UpdateCohortActivityResponse;
import com.mysms.lms.v1.UpdateCohortCourseRequest;
import com.mysms.lms.v1.UpdateCohortCourseResponse;
import com.mysms.lms.v1.UpdateCohortFeedbackRequest;
import com.mysms.lms.v1.UpdateCohortFeedbackResponse;
import com.mysms.lms.v1.UpdateCohortGroupRequest;
import com.mysms.lms.v1.UpdateCohortGroupResponse;
import com.mysms.lms.v1.UpdateCohortHolidayRequest;
import com.mysms.lms.v1.UpdateCohortHolidayResponse;
import com.mysms.lms.v1.UpdateCohortRequest;
import com.mysms.lms.v1.UpdateCohortResponse;
import com.mysms.lms.v1.UpdateCohortScheduleSlotRequest;
import com.mysms.lms.v1.UpdateCohortScheduleSlotResponse;
import com.mysms.lms.v1.UpdateCohortSessionRequest;
import com.mysms.lms.v1.UpdateCohortSessionResponse;
import com.mysms.lms.v1.UpdateCohortTeacherRequest;
import com.mysms.lms.v1.UpdateCohortTeacherResponse;
import com.mysms.lms.v1.UpdateForumRequest;
import com.mysms.lms.v1.UpdateForumResponse;
import com.mysms.lms.v1.UpdatePlagiarismCohortLinkRequest;
import com.mysms.lms.v1.UpdatePlagiarismCohortLinkResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.CohortService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class CohortResolver extends CohortServiceGrpc.CohortServiceImplBase {

    private final CohortService service;
    private final CohortEnrollmentService enrollments;
    private final TimetableEntryService timetables;
    private final CohortGradebookService gradebook;
    private final CohortReleaseService releases;
    private final CertificateService certificates;
    private final ProtoBridge bridge;

    public CohortResolver(CohortService service, CohortEnrollmentService enrollments, TimetableEntryService timetables,
            CohortGradebookService gradebook, CohortReleaseService releases, CertificateService certificates,
            ProtoBridge bridge) {
        this.certificates = certificates;
        this.releases = releases;
        this.service = service;
        this.enrollments = enrollments;
        this.timetables = timetables;
        this.gradebook = gradebook;
        this.bridge = bridge;
    }

    /** A page's navigation as the proto carries it (what {@link ProtoBridge#page} writes for a list response). */
    private static Pagination pagination(Page<?> rows) {
        int number = rows.page();
        return Pagination.newBuilder().setSize(rows.items().size()).setFirst(1)
                .setPrevious(number > 1 && PageRequest.reachableByOffset(number - 1, rows.size()) ? number - 1 : 0)
                .setNext(rows.hasNext() ? number + 1 : 0).setPage(number).setNextCursor(rows.nextCursor()).build();
    }

    @Override
    public void listCohorts(ListCohortsRequest request, StreamObserver<ListCohortsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Cohort> rows = service.listCohorts(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohort(GetCohortRequest request, StreamObserver<GetCohortResponse> out) {
        Cohort found = service.getCohort(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortResponse.newBuilder(), "cohort", found).build());
    }

    @Override
    public void createCohort(CreateCohortRequest request, StreamObserver<CreateCohortResponse> out) {
        Cohort created = service.createCohort(bridge.body(request.getCohort(), Cohort.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCohortResponse.newBuilder(), "cohort", created).build());
    }

    @Override
    public void updateCohort(UpdateCohortRequest request, StreamObserver<UpdateCohortResponse> out) {
        Cohort updated = service.updateCohort(bridge.id(request.getCohort().getId(), "id"),
                bridge.requiredIfMatch(request.getCohort().getEtag()),
                bridge.changes(request.getCohort(), request.getUpdateMask(), CohortUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortResponse.newBuilder(), "cohort", updated).build());
    }

    @Override
    public void deleteCohort(DeleteCohortRequest request, StreamObserver<DeleteCohortResponse> out) {
        service.deleteCohort(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortResponse.getDefaultInstance());
    }

    @Override
    public void listCohortCourses(ListCohortCoursesRequest request, StreamObserver<ListCohortCoursesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortCourse> rows = service.listCohortCourses(bridge.id(request.getCohortId(), "cohortId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortCoursesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortCourse(GetCohortCourseRequest request, StreamObserver<GetCohortCourseResponse> out) {
        CohortCourse found = service.getCohortCourse(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortCourseResponse.newBuilder(), "cohort_course", found).build());
    }

    @Override
    public void createCohortCourse(CreateCohortCourseRequest request, StreamObserver<CreateCohortCourseResponse> out) {
        CohortCourse created = service.createCohortCourse(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getCohortCourse(), CohortCourse.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCohortCourseResponse.newBuilder(), "cohort_course", created).build());
    }

    @Override
    public void updateCohortCourse(UpdateCohortCourseRequest request, StreamObserver<UpdateCohortCourseResponse> out) {
        CohortCourse updated = service.updateCohortCourse(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getCohortCourse().getId(), "id"), bridge.requiredIfMatch(request.getCohortCourse().getEtag()),
                bridge.changes(request.getCohortCourse(), request.getUpdateMask(), CohortCourseUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortCourseResponse.newBuilder(), "cohort_course", updated).build());
    }

    @Override
    public void deleteCohortCourse(DeleteCohortCourseRequest request, StreamObserver<DeleteCohortCourseResponse> out) {
        service.deleteCohortCourse(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortCourseResponse.getDefaultInstance());
    }

    @Override
    public void listCohortTeachers(ListCohortTeachersRequest request, StreamObserver<ListCohortTeachersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortTeacher> rows = service.listCohortTeachers(bridge.id(request.getCohortId(), "cohortId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortTeachersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortTeacher(GetCohortTeacherRequest request, StreamObserver<GetCohortTeacherResponse> out) {
        CohortTeacher found = service.getCohortTeacher(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortTeacherResponse.newBuilder(), "cohort_teacher", found).build());
    }

    @Override
    public void createCohortTeacher(CreateCohortTeacherRequest request, StreamObserver<CreateCohortTeacherResponse> out) {
        CohortTeacher created = service.createCohortTeacher(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getCohortTeacher(), CohortTeacher.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCohortTeacherResponse.newBuilder(), "cohort_teacher", created).build());
    }

    @Override
    public void updateCohortTeacher(UpdateCohortTeacherRequest request, StreamObserver<UpdateCohortTeacherResponse> out) {
        CohortTeacher updated = service.updateCohortTeacher(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getCohortTeacher().getId(), "id"), bridge.requiredIfMatch(request.getCohortTeacher().getEtag()),
                bridge.changes(request.getCohortTeacher(), request.getUpdateMask(), CohortTeacherUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortTeacherResponse.newBuilder(), "cohort_teacher", updated).build());
    }

    @Override
    public void deleteCohortTeacher(DeleteCohortTeacherRequest request, StreamObserver<DeleteCohortTeacherResponse> out) {
        service.deleteCohortTeacher(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortTeacherResponse.getDefaultInstance());
    }

    @Override
    public void listCohortActivities(ListCohortActivitiesRequest request, StreamObserver<ListCohortActivitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortActivity> rows = service.listCohortActivities(bridge.id(request.getCohortId(), "cohortId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortActivitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortActivity(GetCohortActivityRequest request, StreamObserver<GetCohortActivityResponse> out) {
        CohortActivity found = service.getCohortActivity(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortActivityResponse.newBuilder(), "cohort_activity", found).build());
    }

    @Override
    public void createCohortActivity(CreateCohortActivityRequest request, StreamObserver<CreateCohortActivityResponse> out) {
        CohortActivity created = service.createCohortActivity(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getCohortActivity(), CohortActivity.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCohortActivityResponse.newBuilder(), "cohort_activity", created).build());
    }

    @Override
    public void updateCohortActivity(UpdateCohortActivityRequest request, StreamObserver<UpdateCohortActivityResponse> out) {
        CohortActivity updated = service.updateCohortActivity(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getCohortActivity().getId(), "id"),
                bridge.requiredIfMatch(request.getCohortActivity().getEtag()),
                bridge.changes(request.getCohortActivity(), request.getUpdateMask(), CohortActivityUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortActivityResponse.newBuilder(), "cohort_activity", updated).build());
    }

    @Override
    public void deleteCohortActivity(DeleteCohortActivityRequest request, StreamObserver<DeleteCohortActivityResponse> out) {
        service.deleteCohortActivity(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortActivityResponse.getDefaultInstance());
    }

    @Override
    public void listCohortFeedback(ListCohortFeedbackRequest request, StreamObserver<ListCohortFeedbackResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortFeedback> rows = service.listCohortFeedbacks(bridge.id(request.getCohortId(), "cohortId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortFeedbackResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortFeedback(GetCohortFeedbackRequest request, StreamObserver<GetCohortFeedbackResponse> out) {
        CohortFeedback found = service.getCohortFeedback(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortFeedbackResponse.newBuilder(), "cohort_feedback", found).build());
    }

    @Override
    public void createCohortFeedback(CreateCohortFeedbackRequest request, StreamObserver<CreateCohortFeedbackResponse> out) {
        CohortFeedback created = service.createCohortFeedback(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getCohortFeedback(), CohortFeedback.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCohortFeedbackResponse.newBuilder(), "cohort_feedback", created).build());
    }

    @Override
    public void updateCohortFeedback(UpdateCohortFeedbackRequest request, StreamObserver<UpdateCohortFeedbackResponse> out) {
        CohortFeedback updated = service.updateCohortFeedback(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getCohortFeedback().getId(), "id"),
                bridge.requiredIfMatch(request.getCohortFeedback().getEtag()),
                bridge.changes(request.getCohortFeedback(), request.getUpdateMask(), CohortFeedbackUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortFeedbackResponse.newBuilder(), "cohort_feedback", updated).build());
    }

    @Override
    public void deleteCohortFeedback(DeleteCohortFeedbackRequest request, StreamObserver<DeleteCohortFeedbackResponse> out) {
        service.deleteCohortFeedback(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortFeedbackResponse.getDefaultInstance());
    }

    @Override
    public void listForums(ListForumsRequest request, StreamObserver<ListForumsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Forum> rows = service.listForums(bridge.id(request.getCohortId(), "cohortId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListForumsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getForum(GetForumRequest request, StreamObserver<GetForumResponse> out) {
        Forum found = service.getForum(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetForumResponse.newBuilder(), "forum", found).build());
    }

    @Override
    public void createForum(CreateForumRequest request, StreamObserver<CreateForumResponse> out) {
        Forum created = service.createForum(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getForum(), Forum.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateForumResponse.newBuilder(), "forum", created).build());
    }

    @Override
    public void updateForum(UpdateForumRequest request, StreamObserver<UpdateForumResponse> out) {
        Forum updated = service.updateForum(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getForum().getId(), "id"), bridge.requiredIfMatch(request.getForum().getEtag()),
                bridge.changes(request.getForum(), request.getUpdateMask(), ForumUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateForumResponse.newBuilder(), "forum", updated).build());
    }

    @Override
    public void deleteForum(DeleteForumRequest request, StreamObserver<DeleteForumResponse> out) {
        service.deleteForum(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteForumResponse.getDefaultInstance());
    }

    @Override
    public void listCohortGroups(ListCohortGroupsRequest request, StreamObserver<ListCohortGroupsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortGroup> rows = service.listCohortGroups(bridge.id(request.getCohortId(), "cohortId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortGroupsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortGroup(GetCohortGroupRequest request, StreamObserver<GetCohortGroupResponse> out) {
        CohortGroup found = service.getCohortGroup(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortGroupResponse.newBuilder(), "cohort_group", found).build());
    }

    @Override
    public void createCohortGroup(CreateCohortGroupRequest request, StreamObserver<CreateCohortGroupResponse> out) {
        CohortGroup created = service.createCohortGroup(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getCohortGroup(), CohortGroup.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCohortGroupResponse.newBuilder(), "cohort_group", created).build());
    }

    @Override
    public void updateCohortGroup(UpdateCohortGroupRequest request, StreamObserver<UpdateCohortGroupResponse> out) {
        CohortGroup updated = service.updateCohortGroup(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getCohortGroup().getId(), "id"), bridge.requiredIfMatch(request.getCohortGroup().getEtag()),
                bridge.changes(request.getCohortGroup(), request.getUpdateMask(), CohortGroupUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortGroupResponse.newBuilder(), "cohort_group", updated).build());
    }

    @Override
    public void deleteCohortGroup(DeleteCohortGroupRequest request, StreamObserver<DeleteCohortGroupResponse> out) {
        service.deleteCohortGroup(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortGroupResponse.getDefaultInstance());
    }

    @Override
    public void listCohortScheduleSlots(ListCohortScheduleSlotsRequest request,
            StreamObserver<ListCohortScheduleSlotsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortScheduleSlot> rows = service.listCohortScheduleSlots(bridge.id(request.getCohortId(), "cohortId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortScheduleSlotsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortScheduleSlot(GetCohortScheduleSlotRequest request, StreamObserver<GetCohortScheduleSlotResponse> out) {
        CohortScheduleSlot found = service.getCohortScheduleSlot(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortScheduleSlotResponse.newBuilder(), "cohort_schedule_slot", found).build());
    }

    @Override
    public void createCohortScheduleSlot(CreateCohortScheduleSlotRequest request,
            StreamObserver<CreateCohortScheduleSlotResponse> out) {
        CohortScheduleSlot created = service.createCohortScheduleSlot(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getCohortScheduleSlot(), CohortScheduleSlot.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateCohortScheduleSlotResponse.newBuilder(), "cohort_schedule_slot", created).build());
    }

    @Override
    public void updateCohortScheduleSlot(UpdateCohortScheduleSlotRequest request,
            StreamObserver<UpdateCohortScheduleSlotResponse> out) {
        CohortScheduleSlot updated = service.updateCohortScheduleSlot(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getCohortScheduleSlot().getId(), "id"),
                bridge.requiredIfMatch(request.getCohortScheduleSlot().getEtag()),
                bridge.changes(request.getCohortScheduleSlot(), request.getUpdateMask(), CohortScheduleSlotUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateCohortScheduleSlotResponse.newBuilder(), "cohort_schedule_slot", updated).build());
    }

    @Override
    public void deleteCohortScheduleSlot(DeleteCohortScheduleSlotRequest request,
            StreamObserver<DeleteCohortScheduleSlotResponse> out) {
        service.deleteCohortScheduleSlot(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortScheduleSlotResponse.getDefaultInstance());
    }

    @Override
    public void listCohortSessions(ListCohortSessionsRequest request, StreamObserver<ListCohortSessionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortSession> rows = service.listCohortSessions(bridge.id(request.getCohortId(), "cohortId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortSessionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortSession(GetCohortSessionRequest request, StreamObserver<GetCohortSessionResponse> out) {
        CohortSession found = service.getCohortSession(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortSessionResponse.newBuilder(), "cohort_session", found).build());
    }

    @Override
    public void createCohortSession(CreateCohortSessionRequest request, StreamObserver<CreateCohortSessionResponse> out) {
        CohortSession created = service.createCohortSession(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getCohortSession(), CohortSession.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCohortSessionResponse.newBuilder(), "cohort_session", created).build());
    }

    @Override
    public void updateCohortSession(UpdateCohortSessionRequest request, StreamObserver<UpdateCohortSessionResponse> out) {
        CohortSession updated = service.updateCohortSession(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getCohortSession().getId(), "id"), bridge.requiredIfMatch(request.getCohortSession().getEtag()),
                bridge.changes(request.getCohortSession(), request.getUpdateMask(), CohortSessionUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortSessionResponse.newBuilder(), "cohort_session", updated).build());
    }

    @Override
    public void deleteCohortSession(DeleteCohortSessionRequest request, StreamObserver<DeleteCohortSessionResponse> out) {
        service.deleteCohortSession(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortSessionResponse.getDefaultInstance());
    }

    @Override
    public void listCohortHolidays(ListCohortHolidaysRequest request, StreamObserver<ListCohortHolidaysResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortHoliday> rows = service.listCohortHolidays(bridge.id(request.getCohortId(), "cohortId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortHolidaysResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortHoliday(GetCohortHolidayRequest request, StreamObserver<GetCohortHolidayResponse> out) {
        CohortHoliday found = service.getCohortHoliday(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortHolidayResponse.newBuilder(), "cohort_holiday", found).build());
    }

    @Override
    public void createCohortHoliday(CreateCohortHolidayRequest request, StreamObserver<CreateCohortHolidayResponse> out) {
        CohortHoliday created = service.createCohortHoliday(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getCohortHoliday(), CohortHoliday.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCohortHolidayResponse.newBuilder(), "cohort_holiday", created).build());
    }

    @Override
    public void updateCohortHoliday(UpdateCohortHolidayRequest request, StreamObserver<UpdateCohortHolidayResponse> out) {
        CohortHoliday updated = service.updateCohortHoliday(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getCohortHoliday().getId(), "id"), bridge.requiredIfMatch(request.getCohortHoliday().getEtag()),
                bridge.changes(request.getCohortHoliday(), request.getUpdateMask(), CohortHolidayUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortHolidayResponse.newBuilder(), "cohort_holiday", updated).build());
    }

    @Override
    public void deleteCohortHoliday(DeleteCohortHolidayRequest request, StreamObserver<DeleteCohortHolidayResponse> out) {
        service.deleteCohortHoliday(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortHolidayResponse.getDefaultInstance());
    }

    @Override
    public void listPlagiarismCohortLinks(ListPlagiarismCohortLinksRequest request,
            StreamObserver<ListPlagiarismCohortLinksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PlagiarismCohortLink> rows = service.listPlagiarismCohortLinks(bridge.id(request.getCohortId(), "cohortId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPlagiarismCohortLinksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPlagiarismCohortLink(GetPlagiarismCohortLinkRequest request,
            StreamObserver<GetPlagiarismCohortLinkResponse> out) {
        PlagiarismCohortLink found = service.getPlagiarismCohortLink(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetPlagiarismCohortLinkResponse.newBuilder(), "plagiarism_cohort_link", found).build());
    }

    @Override
    public void createPlagiarismCohortLink(CreatePlagiarismCohortLinkRequest request,
            StreamObserver<CreatePlagiarismCohortLinkResponse> out) {
        PlagiarismCohortLink created = service.createPlagiarismCohortLink(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request.getPlagiarismCohortLink(), PlagiarismCohortLink.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreatePlagiarismCohortLinkResponse.newBuilder(), "plagiarism_cohort_link", created).build());
    }

    @Override
    public void updatePlagiarismCohortLink(UpdatePlagiarismCohortLinkRequest request,
            StreamObserver<UpdatePlagiarismCohortLinkResponse> out) {
        PlagiarismCohortLink updated = service.updatePlagiarismCohortLink(bridge.id(request.getCohortId(), "cohortId"),
                bridge.id(request.getPlagiarismCohortLink().getId(), "id"),
                bridge.requiredIfMatch(request.getPlagiarismCohortLink().getEtag()),
                bridge.changes(request.getPlagiarismCohortLink(), request.getUpdateMask(),
                PlagiarismCohortLinkUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdatePlagiarismCohortLinkResponse.newBuilder(), "plagiarism_cohort_link", updated).build());
    }

    @Override
    public void deletePlagiarismCohortLink(DeletePlagiarismCohortLinkRequest request,
            StreamObserver<DeletePlagiarismCohortLinkResponse> out) {
        service.deletePlagiarismCohortLink(bridge.id(request.getCohortId(), "cohortId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePlagiarismCohortLinkResponse.getDefaultInstance());
    }

    @Override
    public void publishCohort(PublishCohortRequest request, StreamObserver<PublishCohortResponse> out) {
        Cohort changed = service.publishCohort(bridge.id(request.getCohortId(), "cohortId"),
                request.hasEtag() ? request.getEtag() : null);
        ProtoBridge.send(out, bridge.reply(PublishCohortResponse.newBuilder(), "cohort", changed).build());
    }

    @Override
    public void unpublishCohort(UnpublishCohortRequest request, StreamObserver<UnpublishCohortResponse> out) {
        Cohort changed = service.unpublishCohort(bridge.id(request.getCohortId(), "cohortId"),
                request.hasEtag() ? request.getEtag() : null);
        ProtoBridge.send(out, bridge.reply(UnpublishCohortResponse.newBuilder(), "cohort", changed).build());
    }

    @Override
    public void enrollInCohort(EnrollInCohortRequest request, StreamObserver<EnrollInCohortResponse> out) {
        CohortEnrollment enrolled = enrollments.enrollInCohort(bridge.id(request.getCohortId(), "cohortId"),
                request.hasPaymentId() ? bridge.id(request.getPaymentId(), "paymentId") : null);
        ProtoBridge.send(out, bridge.reply(EnrollInCohortResponse.newBuilder(), "result", enrolled).build());
    }

    @Override
    public void batchEnrollInCohort(BatchEnrollInCohortRequest request, StreamObserver<BatchEnrollInCohortResponse> out) {
        com.mysms.learning.lms.api.model.BatchEnrollInCohortRequest body = bridge.body(request,
                com.mysms.learning.lms.api.model.BatchEnrollInCohortRequest.class, "cohort_id", "request_id");
        BatchEnrollPreviewList preview = enrollments.batchEnrollInCohort(bridge.id(request.getCohortId(), "cohortId"),
                body.getUserIds(), body.getPaymentIds(), Boolean.TRUE.equals(body.getValidateOnly()));
        ProtoBridge.send(out, bridge.reply(BatchEnrollInCohortResponse.newBuilder(), "items", preview.getItems()).build());
    }

    @Override
    public void getCohortTimetable(GetCohortTimetableRequest request, StreamObserver<GetCohortTimetableResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TimetableEntry> rows = timetables.getCohortTimetable(bridge.id(request.getCohortId(), "cohortId"), page.page(),
                page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(GetCohortTimetableResponse.newBuilder(), rows).build());
    }

    @Override
    public void applyTimetableTemplate(ApplyTimetableTemplateRequest request,
            StreamObserver<ApplyTimetableTemplateResponse> out) {
        TimetableEntryList applied = timetables.applyTimetableTemplate(bridge.id(request.getCohortId(), "cohortId"),
                request.hasTimetableTemplateId() ? bridge.id(request.getTimetableTemplateId(),
                        "timetableTemplateId") : null);
        ProtoBridge.send(out, bridge.reply(ApplyTimetableTemplateResponse.newBuilder(), "items", applied.getItems()).build());
    }

    @Override
    public void sendCohortAnnouncement(SendCohortAnnouncementRequest request,
            StreamObserver<SendCohortAnnouncementResponse> out) {
        SendResult sent = enrollments.sendCohortAnnouncement(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request, com.mysms.learning.lms.api.model.SendCohortAnnouncementRequest.class, "cohort_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(SendCohortAnnouncementResponse.newBuilder(), "result", sent).build());
    }

    @Override
    public void getCohortProgress(GetCohortProgressRequest request, StreamObserver<GetCohortProgressResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortStudentProgress> rows = gradebook.getCohortProgress(bridge.id(request.getCohortId(), "cohortId"),
                request.hasUserId() ? bridge.id(request.getUserId(), "userId") : null, page.page(), page.size(),
                page.cursor());
        ProtoBridge.send(out, bridge.page(GetCohortProgressResponse.newBuilder(), rows).build());
    }

    @Override
    public void generateCohortCertificates(GenerateCohortCertificatesRequest request,
            StreamObserver<GenerateCohortCertificatesResponse> out) {
        com.mysms.learning.lms.api.model.GenerateCohortCertificatesRequest body = bridge.body(request,
                com.mysms.learning.lms.api.model.GenerateCohortCertificatesRequest.class, "cohort_id", "request_id");
        Operation done = certificates.generateCohortCertificates(bridge.id(request.getCohortId(), "cohortId"),
                body.getUserIds(), body.getTemplateCode(), body.getIssueDate(), Boolean.TRUE.equals(body.getPublish()));
        ProtoBridge.send(out, bridge.reply(GenerateCohortCertificatesResponse.newBuilder(), "result", done).build());
    }

    @Override
    public void activateCohort(ActivateCohortRequest request, StreamObserver<ActivateCohortResponse> out) {
        Cohort changed = service.activateCohort(bridge.id(request.getCohortId(), "cohortId"),
                request.hasEtag() ? request.getEtag() : null);
        ProtoBridge.send(out, bridge.reply(ActivateCohortResponse.newBuilder(), "cohort", changed).build());
    }

    @Override
    public void deactivateCohort(DeactivateCohortRequest request, StreamObserver<DeactivateCohortResponse> out) {
        Cohort changed = service.deactivateCohort(bridge.id(request.getCohortId(), "cohortId"),
                request.hasEtag() ? request.getEtag() : null, request.hasReason() ? request.getReason() : null);
        ProtoBridge.send(out, bridge.reply(DeactivateCohortResponse.newBuilder(), "cohort", changed).build());
    }

    @Override
    public void archiveCohort(ArchiveCohortRequest request, StreamObserver<ArchiveCohortResponse> out) {
        Cohort changed = service.archiveCohort(bridge.id(request.getCohortId(), "cohortId"),
                request.hasEtag() ? request.getEtag() : null);
        ProtoBridge.send(out, bridge.reply(ArchiveCohortResponse.newBuilder(), "cohort", changed).build());
    }

    @Override
    public void copyCourseOutlineToCohort(CopyCourseOutlineToCohortRequest request,
            StreamObserver<CopyCourseOutlineToCohortResponse> out) {
        com.mysms.learning.lms.api.model.CopyCourseOutlineToCohortRequest body = bridge.body(request,
                com.mysms.learning.lms.api.model.CopyCourseOutlineToCohortRequest.class, "cohort_id", "request_id");
        ChapterList copied = service.copyCourseOutlineToCohort(bridge.id(request.getCohortId(), "cohortId"),
                body.getCourseId(), body.getChapterIds(), body.getShiftDatesFrom());
        ProtoBridge.send(out, bridge.reply(CopyCourseOutlineToCohortResponse.newBuilder(), "items", copied.getItems()).build());
    }

    @Override
    public void getCohortRoster(GetCohortRosterRequest request, StreamObserver<GetCohortRosterResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RosterEntry> rows = enrollments.getCohortRoster(bridge.id(request.getCohortId(), "cohortId"),
                request.hasRoleCode() ? request.getRoleCode() : null,
                request.hasIncludeInactive() ? request.getIncludeInactive() : null, page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(GetCohortRosterResponse.newBuilder(), rows).build());
    }

    @Override
    public void setCohortMembers(SetCohortMembersRequest request, StreamObserver<SetCohortMembersResponse> out) {
        com.mysms.learning.lms.api.model.SetCohortMembersRequest body = bridge.body(request,
                com.mysms.learning.lms.api.model.SetCohortMembersRequest.class, "cohort_id", "request_id");
        RosterEntryList touched = enrollments.setCohortMembers(bridge.id(request.getCohortId(), "cohortId"),
                body.getMembers(), body.getRemoveUserIds());
        ProtoBridge.send(out, bridge.reply(SetCohortMembersResponse.newBuilder(), "items", touched.getItems()).build());
    }

    @Override
    public void importCohortMembers(ImportCohortMembersRequest request,
            StreamObserver<ImportCohortMembersResponse> out) {
        Operation accepted = enrollments.importCohortMembers(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ImportCohortMembersRequest.class, "cohort_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ImportCohortMembersResponse.newBuilder(), "result", accepted).build());
    }

    @Override
    public void setStudentAccess(SetStudentAccessRequest request, StreamObserver<SetStudentAccessResponse> out) {
        com.mysms.learning.lms.api.model.SetStudentAccessRequest body = bridge.body(request,
                com.mysms.learning.lms.api.model.SetStudentAccessRequest.class, "cohort_id", "request_id");
        Cohort changed = service.setStudentAccess(bridge.id(request.getCohortId(), "cohortId"), body.getStudentAccessFrom(),
                body.getStudentAccessUntil());
        ProtoBridge.send(out, bridge.reply(SetStudentAccessResponse.newBuilder(), "cohort", changed).build());
    }

    @Override
    public void getCohortSyllabus(GetCohortSyllabusRequest request, StreamObserver<GetCohortSyllabusResponse> out) {
        String format = !request.hasFormat()
                || request.getFormat() == GetCohortSyllabusRequest.Format.FORMAT_UNSPECIFIED ? null
                        : request.getFormat().name().substring("FORMAT_".length())
                                .toLowerCase(java.util.Locale.ROOT);
        DownloadLink link = service.getCohortSyllabus(bridge.id(request.getCohortId(), "cohortId"), format);
        ProtoBridge.send(out, bridge.reply(GetCohortSyllabusResponse.newBuilder(), "result", link).build());
    }

    @Override
    public void getGradebook(GetGradebookRequest request, StreamObserver<GetGradebookResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        CohortGradebookService.GradebookPage read = gradebook.getGradebook(bridge.id(request.getCohortId(), "cohortId"),
                request.hasUserId() ? bridge.id(request.getUserId(), "userId") : null,
                request.hasGradingCategoryId() ? bridge.id(request.getGradingCategoryId(), "gradingCategoryId") : null,
                page.page(), page.size(), page.cursor());
        GetGradebookResponse.Builder reply = bridge.reply(GetGradebookResponse.newBuilder(), "result", read.gradebook());
        reply.getResultBuilder().setPagination(pagination(read.rows()));
        ProtoBridge.send(out, reply.build());
    }

    @Override
    public void getCohortPerformance(GetCohortPerformanceRequest request, StreamObserver<GetCohortPerformanceResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortPerformance> rows = gradebook.getCohortPerformance(bridge.id(request.getCohortId(), "cohortId"),
                page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(GetCohortPerformanceResponse.newBuilder(), rows).build());
    }

    @Override
    public void exportGrades(ExportGradesRequest request, StreamObserver<ExportGradesResponse> out) {
        Operation accepted = gradebook.exportGrades(bridge.id(request.getCohortId(), "cohortId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ExportGradesRequest.class, "cohort_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ExportGradesResponse.newBuilder(), "result", accepted).build());
    }

    @Override
    public void publishFinalGrades(PublishFinalGradesRequest request, StreamObserver<PublishFinalGradesResponse> out) {
        com.mysms.learning.lms.api.model.PublishFinalGradesRequest body = bridge.body(request,
                com.mysms.learning.lms.api.model.PublishFinalGradesRequest.class, "cohort_id", "request_id");
        CohortEnrollmentList published = gradebook.publishFinalGrades(bridge.id(request.getCohortId(), "cohortId"),
                body.getUserIds());
        ProtoBridge.send(out, bridge.reply(PublishFinalGradesResponse.newBuilder(), "items", published.getItems()).build());
    }

    @Override
    public void evaluateReleaseRules(EvaluateReleaseRulesRequest request, StreamObserver<EvaluateReleaseRulesResponse> out) {
        ReleaseGrantList grants = releases.evaluateReleaseRules(bridge.id(request.getCohortId(), "cohortId"),
                request.hasUserId() ? bridge.id(request.getUserId(), "userId") : null);
        ProtoBridge.send(out, bridge.reply(EvaluateReleaseRulesResponse.newBuilder(), "items", grants.getItems()).build());
    }

    @Override
    public void getUnreadCounts(GetUnreadCountsRequest request, StreamObserver<GetUnreadCountsResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetUnreadCountsResponse.newBuilder(), "result",
                service.getUnreadCounts()).build());
    }
}
