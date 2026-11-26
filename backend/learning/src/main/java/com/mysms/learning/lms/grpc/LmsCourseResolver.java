package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.CoursePackageService;
import com.mysms.learning.lms.api.model.ProgressBucket;
import com.mysms.learning.lms.model.CatalogPage;
import com.mysms.learning.lms.api.model.Enrollment;
import com.mysms.learning.lms.business.CatalogService;
import com.mysms.learning.lms.business.EnrollmentService;
import com.mysms.learning.lms.api.model.LessonProgress;
import com.mysms.learning.lms.api.model.ChapterList;
import com.mysms.learning.lms.api.model.ScormLaunch;
import com.mysms.learning.lms.api.model.CourseOutline;
import com.mysms.learning.lms.api.model.Dependencies;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.CatalogBanner;
import com.mysms.learning.lms.api.model.CatalogBannerUpdate;
import com.mysms.learning.lms.api.model.Chapter;
import com.mysms.learning.lms.api.model.ChapterUpdate;
import com.mysms.learning.lms.api.model.CourseAuthorProfile;
import com.mysms.learning.lms.api.model.CourseAuthorProfileUpdate;
import com.mysms.learning.lms.api.model.CourseInterest;
import com.mysms.learning.lms.api.model.CourseInterestUpdate;
import com.mysms.learning.lms.api.model.CourseMentor;
import com.mysms.learning.lms.api.model.CourseMentorUpdate;
import com.mysms.learning.lms.api.model.CoursePreview;
import com.mysms.learning.lms.api.model.CoursePreviewUpdate;
import com.mysms.learning.lms.api.model.CourseReview;
import com.mysms.learning.lms.api.model.CourseReviewUpdate;
import com.mysms.learning.lms.api.model.CourseTeacher;
import com.mysms.learning.lms.api.model.CourseTeacherUpdate;
import com.mysms.learning.lms.api.model.CourseTestimonial;
import com.mysms.learning.lms.api.model.CourseTestimonialUpdate;
import com.mysms.learning.lms.api.model.EnrollmentRequest;
import com.mysms.learning.lms.api.model.EnrollmentRequestUpdate;
import com.mysms.learning.lms.api.model.LmsCourse;
import com.mysms.learning.lms.api.model.LmsCourseUpdate;
import com.mysms.learning.lms.api.model.RelatedCourse;
import com.mysms.learning.lms.api.model.RelatedCourseUpdate;
import com.mysms.learning.lms.business.LmsCourseService;
import com.mysms.lms.v1.ApproveCourseRequest;
import com.mysms.lms.v1.ApproveCourseResponse;
import com.mysms.lms.v1.ApproveEnrollmentRequestRequest;
import com.mysms.lms.v1.ApproveEnrollmentRequestResponse;
import com.mysms.lms.v1.BatchEnrollInCourseRequest;
import com.mysms.lms.v1.BatchEnrollInCourseResponse;
import com.mysms.lms.v1.CheckCourseDependenciesRequest;
import com.mysms.lms.v1.CheckCourseDependenciesResponse;
import com.mysms.lms.v1.CompareCoursesRequest;
import com.mysms.lms.v1.CompareCoursesResponse;
import com.mysms.lms.v1.CreateCatalogBannerRequest;
import com.mysms.lms.v1.CreateCatalogBannerResponse;
import com.mysms.lms.v1.CreateChapterRequest;
import com.mysms.lms.v1.CreateChapterResponse;
import com.mysms.lms.v1.CreateCourseAuthorProfileRequest;
import com.mysms.lms.v1.CreateCourseAuthorProfileResponse;
import com.mysms.lms.v1.CreateCourseInterestRequest;
import com.mysms.lms.v1.CreateCourseInterestResponse;
import com.mysms.lms.v1.CreateCourseMentorRequest;
import com.mysms.lms.v1.CreateCourseMentorResponse;
import com.mysms.lms.v1.CreateCoursePreviewRequest;
import com.mysms.lms.v1.CreateCoursePreviewResponse;
import com.mysms.lms.v1.CreateCourseReviewRequest;
import com.mysms.lms.v1.CreateCourseReviewResponse;
import com.mysms.lms.v1.CreateCourseTeacherRequest;
import com.mysms.lms.v1.CreateCourseTeacherResponse;
import com.mysms.lms.v1.CreateCourseTestimonialRequest;
import com.mysms.lms.v1.CreateCourseTestimonialResponse;
import com.mysms.lms.v1.CreateCourseVersionRequest;
import com.mysms.lms.v1.CreateCourseVersionResponse;
import com.mysms.lms.v1.CreateEnrollmentRequestRequest;
import com.mysms.lms.v1.CreateEnrollmentRequestResponse;
import com.mysms.lms.v1.CreateLmsCourseRequest;
import com.mysms.lms.v1.CreateLmsCourseResponse;
import com.mysms.lms.v1.CreateRelatedCourseRequest;
import com.mysms.lms.v1.CreateRelatedCourseResponse;
import com.mysms.lms.v1.DeactivateCourseRequest;
import com.mysms.lms.v1.DeactivateCourseResponse;
import com.mysms.lms.v1.DeleteCatalogBannerRequest;
import com.mysms.lms.v1.DeleteCatalogBannerResponse;
import com.mysms.lms.v1.DeleteChapterRequest;
import com.mysms.lms.v1.DeleteChapterResponse;
import com.mysms.lms.v1.DeleteCourseAuthorProfileRequest;
import com.mysms.lms.v1.DeleteCourseAuthorProfileResponse;
import com.mysms.lms.v1.DeleteCourseInterestRequest;
import com.mysms.lms.v1.DeleteCourseInterestResponse;
import com.mysms.lms.v1.DeleteCourseMentorRequest;
import com.mysms.lms.v1.DeleteCourseMentorResponse;
import com.mysms.lms.v1.DeleteCoursePreviewRequest;
import com.mysms.lms.v1.DeleteCoursePreviewResponse;
import com.mysms.lms.v1.DeleteCourseReviewRequest;
import com.mysms.lms.v1.DeleteCourseReviewResponse;
import com.mysms.lms.v1.DeleteCourseTeacherRequest;
import com.mysms.lms.v1.DeleteCourseTeacherResponse;
import com.mysms.lms.v1.DeleteCourseTestimonialRequest;
import com.mysms.lms.v1.DeleteCourseTestimonialResponse;
import com.mysms.lms.v1.DeleteEnrollmentRequestRequest;
import com.mysms.lms.v1.DeleteEnrollmentRequestResponse;
import com.mysms.lms.v1.DeleteLmsCourseRequest;
import com.mysms.lms.v1.DeleteLmsCourseResponse;
import com.mysms.lms.v1.DeleteRelatedCourseRequest;
import com.mysms.lms.v1.DeleteRelatedCourseResponse;
import com.mysms.lms.v1.EnrollInCourseRequest;
import com.mysms.lms.v1.EnrollInCourseResponse;
import com.mysms.lms.v1.ExportCourseRequest;
import com.mysms.lms.v1.ExportCourseResponse;
import com.mysms.lms.v1.ImportCoursePackageRequest;
import com.mysms.lms.v1.ImportCoursePackageResponse;
import com.mysms.lms.v1.GetCatalogBannerRequest;
import com.mysms.lms.v1.GetCatalogBannerResponse;
import com.mysms.lms.v1.GetCatalogHomeRequest;
import com.mysms.lms.v1.GetCatalogHomeResponse;
import com.mysms.lms.v1.GetChapterRequest;
import com.mysms.lms.v1.GetChapterResponse;
import com.mysms.lms.v1.GetCourseAuthorProfileRequest;
import com.mysms.lms.v1.GetCourseAuthorProfileResponse;
import com.mysms.lms.v1.GetCourseInterestRequest;
import com.mysms.lms.v1.GetCourseInterestResponse;
import com.mysms.lms.v1.GetCourseMentorRequest;
import com.mysms.lms.v1.GetCourseMentorResponse;
import com.mysms.lms.v1.GetCourseOutlineRequest;
import com.mysms.lms.v1.GetCourseOutlineResponse;
import com.mysms.lms.v1.GetCoursePreviewRequest;
import com.mysms.lms.v1.GetCoursePreviewResponse;
import com.mysms.lms.v1.GetCourseProgressDistributionRequest;
import com.mysms.lms.v1.GetCourseProgressDistributionResponse;
import com.mysms.lms.v1.GetCourseReviewRequest;
import com.mysms.lms.v1.GetCourseReviewResponse;
import com.mysms.lms.v1.GetCourseSyllabusRequest;
import com.mysms.lms.v1.GetCourseSyllabusResponse;
import com.mysms.lms.v1.GetCourseTeacherRequest;
import com.mysms.lms.v1.GetCourseTeacherResponse;
import com.mysms.lms.v1.GetCourseTestimonialRequest;
import com.mysms.lms.v1.GetCourseTestimonialResponse;
import com.mysms.lms.v1.GetEnrollmentRequestRequest;
import com.mysms.lms.v1.GetEnrollmentRequestResponse;
import com.mysms.lms.v1.GetEvaluatorSlotsRequest;
import com.mysms.lms.v1.GetEvaluatorSlotsResponse;
import com.mysms.lms.v1.GetLmsCourseRequest;
import com.mysms.lms.v1.GetLmsCourseResponse;
import com.mysms.lms.v1.GetRelatedCourseRequest;
import com.mysms.lms.v1.GetRelatedCourseResponse;
import com.mysms.lms.v1.GetScormLaunchRequest;
import com.mysms.lms.v1.GetScormLaunchResponse;
import com.mysms.lms.v1.GetTeacherDashboardRequest;
import com.mysms.lms.v1.GetTeacherDashboardResponse;
import com.mysms.lms.v1.HideCatalogListingRequest;
import com.mysms.lms.v1.HideCatalogListingResponse;
import com.mysms.lms.v1.IssueMyCertificateRequest;
import com.mysms.lms.v1.IssueMyCertificateResponse;
import com.mysms.lms.v1.ListCatalogBannersRequest;
import com.mysms.lms.v1.ListCatalogBannersResponse;
import com.mysms.lms.v1.ListChaptersRequest;
import com.mysms.lms.v1.ListChaptersResponse;
import com.mysms.lms.v1.ListCourseAuthorProfilesRequest;
import com.mysms.lms.v1.ListCourseAuthorProfilesResponse;
import com.mysms.lms.v1.ListCourseInterestsRequest;
import com.mysms.lms.v1.ListCourseInterestsResponse;
import com.mysms.lms.v1.ListCourseMentorsRequest;
import com.mysms.lms.v1.ListCourseMentorsResponse;
import com.mysms.lms.v1.ListCoursePreviewsRequest;
import com.mysms.lms.v1.ListCoursePreviewsResponse;
import com.mysms.lms.v1.ListCourseReviewsRequest;
import com.mysms.lms.v1.ListCourseReviewsResponse;
import com.mysms.lms.v1.ListCourseTeachersRequest;
import com.mysms.lms.v1.ListCourseTeachersResponse;
import com.mysms.lms.v1.ListCourseTestimonialsRequest;
import com.mysms.lms.v1.ListCourseTestimonialsResponse;
import com.mysms.lms.v1.ListEnrollmentRequestsRequest;
import com.mysms.lms.v1.ListEnrollmentRequestsResponse;
import com.mysms.lms.v1.ListLmsCoursesRequest;
import com.mysms.lms.v1.ListLmsCoursesResponse;
import com.mysms.lms.v1.ListRelatedCoursesRequest;
import com.mysms.lms.v1.ListRelatedCoursesResponse;
import com.mysms.lms.v1.LmsCourseServiceGrpc;
import com.mysms.lms.v1.MoveChapterRequest;
import com.mysms.lms.v1.MoveChapterResponse;
import com.mysms.lms.v1.PublishCatalogListingRequest;
import com.mysms.lms.v1.PublishCatalogListingResponse;
import com.mysms.lms.v1.PublishCourseRequest;
import com.mysms.lms.v1.PublishCourseResponse;
import com.mysms.lms.v1.ReactivateCourseRequest;
import com.mysms.lms.v1.ReactivateCourseResponse;
import com.mysms.lms.v1.RejectEnrollmentRequestRequest;
import com.mysms.lms.v1.RejectEnrollmentRequestResponse;
import com.mysms.lms.v1.ReorderChaptersRequest;
import com.mysms.lms.v1.ReorderChaptersResponse;
import com.mysms.lms.v1.SaveScormProgressRequest;
import com.mysms.lms.v1.SaveScormProgressResponse;
import com.mysms.lms.v1.SubmitCourseForReviewRequest;
import com.mysms.lms.v1.SubmitCourseForReviewResponse;
import com.mysms.lms.v1.UnpublishCourseRequest;
import com.mysms.lms.v1.UnpublishCourseResponse;
import com.mysms.lms.v1.UpdateCatalogBannerRequest;
import com.mysms.lms.v1.UpdateCatalogBannerResponse;
import com.mysms.lms.v1.UpdateChapterRequest;
import com.mysms.lms.v1.UpdateChapterResponse;
import com.mysms.lms.v1.UpdateCourseAuthorProfileRequest;
import com.mysms.lms.v1.UpdateCourseAuthorProfileResponse;
import com.mysms.lms.v1.UpdateCourseInterestRequest;
import com.mysms.lms.v1.UpdateCourseInterestResponse;
import com.mysms.lms.v1.UpdateCourseMentorRequest;
import com.mysms.lms.v1.UpdateCourseMentorResponse;
import com.mysms.lms.v1.UpdateCoursePreviewRequest;
import com.mysms.lms.v1.UpdateCoursePreviewResponse;
import com.mysms.lms.v1.UpdateCourseReviewRequest;
import com.mysms.lms.v1.UpdateCourseReviewResponse;
import com.mysms.lms.v1.UpdateCourseTeacherRequest;
import com.mysms.lms.v1.UpdateCourseTeacherResponse;
import com.mysms.lms.v1.UpdateCourseTestimonialRequest;
import com.mysms.lms.v1.UpdateCourseTestimonialResponse;
import com.mysms.lms.v1.UpdateEnrollmentRequestRequest;
import com.mysms.lms.v1.UpdateEnrollmentRequestResponse;
import com.mysms.lms.v1.UpdateLmsCourseRequest;
import com.mysms.lms.v1.UpdateLmsCourseResponse;
import com.mysms.lms.v1.UpdateRelatedCourseRequest;
import com.mysms.lms.v1.UpdateRelatedCourseResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.LmsCourseService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class LmsCourseResolver extends LmsCourseServiceGrpc.LmsCourseServiceImplBase {

    private final LmsCourseService service;
    private final EnrollmentService enrollments;
    private final CatalogService catalog;
    private final ProtoBridge bridge;
    private final CoursePackageService packages;

    public LmsCourseResolver(LmsCourseService service, EnrollmentService enrollments, CatalogService catalog,
            ProtoBridge bridge, CoursePackageService packages) {
        this.packages = packages;
        this.service = service;
        this.enrollments = enrollments;
        this.catalog = catalog;
        this.bridge = bridge;
    }

    @Override
    public void listLmsCourses(ListLmsCoursesRequest request, StreamObserver<ListLmsCoursesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LmsCourse> rows = service.listLmsCourses(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLmsCoursesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLmsCourse(GetLmsCourseRequest request, StreamObserver<GetLmsCourseResponse> out) {
        LmsCourse found = service.getLmsCourse(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLmsCourseResponse.newBuilder(), "course", found).build());
    }

    @Override
    public void createLmsCourse(CreateLmsCourseRequest request, StreamObserver<CreateLmsCourseResponse> out) {
        LmsCourse created = service.createLmsCourse(bridge.body(request.getCourse(), LmsCourse.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLmsCourseResponse.newBuilder(), "course", created).build());
    }

    @Override
    public void updateLmsCourse(UpdateLmsCourseRequest request, StreamObserver<UpdateLmsCourseResponse> out) {
        LmsCourse updated = service.updateLmsCourse(bridge.id(request.getCourse().getId(), "id"),
                bridge.requiredIfMatch(request.getCourse().getEtag()),
                bridge.changes(request.getCourse(), request.getUpdateMask(), LmsCourseUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLmsCourseResponse.newBuilder(), "course", updated).build());
    }

    @Override
    public void deleteLmsCourse(DeleteLmsCourseRequest request, StreamObserver<DeleteLmsCourseResponse> out) {
        service.deleteLmsCourse(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLmsCourseResponse.getDefaultInstance());
    }

    @Override
    public void listCourseTeachers(ListCourseTeachersRequest request, StreamObserver<ListCourseTeachersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CourseTeacher> rows = service.listCourseTeachers(bridge.id(request.getCourseId(), "courseId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCourseTeachersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCourseTeacher(GetCourseTeacherRequest request, StreamObserver<GetCourseTeacherResponse> out) {
        CourseTeacher found = service.getCourseTeacher(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCourseTeacherResponse.newBuilder(), "course_teacher", found).build());
    }

    @Override
    public void createCourseTeacher(CreateCourseTeacherRequest request, StreamObserver<CreateCourseTeacherResponse> out) {
        CourseTeacher created = service.createCourseTeacher(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCourseTeacher(), CourseTeacher.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCourseTeacherResponse.newBuilder(), "course_teacher", created).build());
    }

    @Override
    public void updateCourseTeacher(UpdateCourseTeacherRequest request, StreamObserver<UpdateCourseTeacherResponse> out) {
        CourseTeacher updated = service.updateCourseTeacher(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCourseTeacher().getId(), "id"), bridge.requiredIfMatch(request.getCourseTeacher().getEtag()),
                bridge.changes(request.getCourseTeacher(), request.getUpdateMask(), CourseTeacherUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCourseTeacherResponse.newBuilder(), "course_teacher", updated).build());
    }

    @Override
    public void deleteCourseTeacher(DeleteCourseTeacherRequest request, StreamObserver<DeleteCourseTeacherResponse> out) {
        service.deleteCourseTeacher(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCourseTeacherResponse.getDefaultInstance());
    }

    @Override
    public void listRelatedCourses(ListRelatedCoursesRequest request, StreamObserver<ListRelatedCoursesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RelatedCourse> rows = service.listRelatedCourses(bridge.id(request.getCourseId(), "courseId"),
                page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRelatedCoursesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRelatedCourse(GetRelatedCourseRequest request, StreamObserver<GetRelatedCourseResponse> out) {
        RelatedCourse found = service.getRelatedCourse(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRelatedCourseResponse.newBuilder(), "related_course", found).build());
    }

    @Override
    public void createRelatedCourse(CreateRelatedCourseRequest request, StreamObserver<CreateRelatedCourseResponse> out) {
        RelatedCourse created = service.createRelatedCourse(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getRelatedCourse(), RelatedCourse.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRelatedCourseResponse.newBuilder(), "related_course", created).build());
    }

    @Override
    public void updateRelatedCourse(UpdateRelatedCourseRequest request, StreamObserver<UpdateRelatedCourseResponse> out) {
        RelatedCourse updated = service.updateRelatedCourse(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getRelatedCourse().getId(), "id"), bridge.requiredIfMatch(request.getRelatedCourse().getEtag()),
                bridge.changes(request.getRelatedCourse(), request.getUpdateMask(), RelatedCourseUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRelatedCourseResponse.newBuilder(), "related_course", updated).build());
    }

    @Override
    public void deleteRelatedCourse(DeleteRelatedCourseRequest request, StreamObserver<DeleteRelatedCourseResponse> out) {
        service.deleteRelatedCourse(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRelatedCourseResponse.getDefaultInstance());
    }

    @Override
    public void listCourseMentors(ListCourseMentorsRequest request, StreamObserver<ListCourseMentorsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CourseMentor> rows = service.listCourseMentors(bridge.id(request.getCourseId(), "courseId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCourseMentorsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCourseMentor(GetCourseMentorRequest request, StreamObserver<GetCourseMentorResponse> out) {
        CourseMentor found = service.getCourseMentor(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCourseMentorResponse.newBuilder(), "course_mentor", found).build());
    }

    @Override
    public void createCourseMentor(CreateCourseMentorRequest request, StreamObserver<CreateCourseMentorResponse> out) {
        CourseMentor created = service.createCourseMentor(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCourseMentor(), CourseMentor.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCourseMentorResponse.newBuilder(), "course_mentor", created).build());
    }

    @Override
    public void updateCourseMentor(UpdateCourseMentorRequest request, StreamObserver<UpdateCourseMentorResponse> out) {
        CourseMentor updated = service.updateCourseMentor(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCourseMentor().getId(), "id"), bridge.requiredIfMatch(request.getCourseMentor().getEtag()),
                bridge.changes(request.getCourseMentor(), request.getUpdateMask(), CourseMentorUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCourseMentorResponse.newBuilder(), "course_mentor", updated).build());
    }

    @Override
    public void deleteCourseMentor(DeleteCourseMentorRequest request, StreamObserver<DeleteCourseMentorResponse> out) {
        service.deleteCourseMentor(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCourseMentorResponse.getDefaultInstance());
    }

    @Override
    public void listChapters(ListChaptersRequest request, StreamObserver<ListChaptersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Chapter> rows = service.listChapters(bridge.id(request.getCourseId(), "courseId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListChaptersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getChapter(GetChapterRequest request, StreamObserver<GetChapterResponse> out) {
        Chapter found = service.getChapter(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetChapterResponse.newBuilder(), "chapter", found).build());
    }

    @Override
    public void createChapter(CreateChapterRequest request, StreamObserver<CreateChapterResponse> out) {
        Chapter created = service.createChapter(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getChapter(), Chapter.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateChapterResponse.newBuilder(), "chapter", created).build());
    }

    @Override
    public void updateChapter(UpdateChapterRequest request, StreamObserver<UpdateChapterResponse> out) {
        Chapter updated = service.updateChapter(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getChapter().getId(), "id"), bridge.requiredIfMatch(request.getChapter().getEtag()),
                bridge.changes(request.getChapter(), request.getUpdateMask(), ChapterUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateChapterResponse.newBuilder(), "chapter", updated).build());
    }

    @Override
    public void deleteChapter(DeleteChapterRequest request, StreamObserver<DeleteChapterResponse> out) {
        service.deleteChapter(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteChapterResponse.getDefaultInstance());
    }

    @Override
    public void listCourseReviews(ListCourseReviewsRequest request, StreamObserver<ListCourseReviewsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CourseReview> rows = service.listCourseReviews(bridge.id(request.getCourseId(), "courseId"),
                page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCourseReviewsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCourseReview(GetCourseReviewRequest request, StreamObserver<GetCourseReviewResponse> out) {
        CourseReview found = service.getCourseReview(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCourseReviewResponse.newBuilder(), "course_review", found).build());
    }

    @Override
    public void createCourseReview(CreateCourseReviewRequest request, StreamObserver<CreateCourseReviewResponse> out) {
        CourseReview created = service.createCourseReview(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCourseReview(), CourseReview.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCourseReviewResponse.newBuilder(), "course_review", created).build());
    }

    @Override
    public void updateCourseReview(UpdateCourseReviewRequest request, StreamObserver<UpdateCourseReviewResponse> out) {
        CourseReview updated = service.updateCourseReview(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCourseReview().getId(), "id"), bridge.requiredIfMatch(request.getCourseReview().getEtag()),
                bridge.changes(request.getCourseReview(), request.getUpdateMask(), CourseReviewUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCourseReviewResponse.newBuilder(), "course_review", updated).build());
    }

    @Override
    public void deleteCourseReview(DeleteCourseReviewRequest request, StreamObserver<DeleteCourseReviewResponse> out) {
        service.deleteCourseReview(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCourseReviewResponse.getDefaultInstance());
    }

    @Override
    public void listCourseInterests(ListCourseInterestsRequest request, StreamObserver<ListCourseInterestsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CourseInterest> rows = service.listCourseInterests(bridge.id(request.getCourseId(), "courseId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCourseInterestsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCourseInterest(GetCourseInterestRequest request, StreamObserver<GetCourseInterestResponse> out) {
        CourseInterest found = service.getCourseInterest(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCourseInterestResponse.newBuilder(), "course_interest", found).build());
    }

    @Override
    public void createCourseInterest(CreateCourseInterestRequest request, StreamObserver<CreateCourseInterestResponse> out) {
        CourseInterest created = service.createCourseInterest(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCourseInterest(), CourseInterest.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCourseInterestResponse.newBuilder(), "course_interest", created).build());
    }

    @Override
    public void updateCourseInterest(UpdateCourseInterestRequest request, StreamObserver<UpdateCourseInterestResponse> out) {
        CourseInterest updated = service.updateCourseInterest(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCourseInterest().getId(), "id"),
                bridge.requiredIfMatch(request.getCourseInterest().getEtag()),
                bridge.changes(request.getCourseInterest(), request.getUpdateMask(), CourseInterestUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCourseInterestResponse.newBuilder(), "course_interest", updated).build());
    }

    @Override
    public void deleteCourseInterest(DeleteCourseInterestRequest request, StreamObserver<DeleteCourseInterestResponse> out) {
        service.deleteCourseInterest(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCourseInterestResponse.getDefaultInstance());
    }

    @Override
    public void listCourseAuthorProfiles(ListCourseAuthorProfilesRequest request,
            StreamObserver<ListCourseAuthorProfilesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CourseAuthorProfile> rows = service.listCourseAuthorProfiles(bridge.id(request.getCourseId(), "courseId"),
                page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCourseAuthorProfilesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCourseAuthorProfile(GetCourseAuthorProfileRequest request,
            StreamObserver<GetCourseAuthorProfileResponse> out) {
        CourseAuthorProfile found = service.getCourseAuthorProfile(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCourseAuthorProfileResponse.newBuilder(), "course_author_profile", found).build());
    }

    @Override
    public void createCourseAuthorProfile(CreateCourseAuthorProfileRequest request,
            StreamObserver<CreateCourseAuthorProfileResponse> out) {
        CourseAuthorProfile created = service.createCourseAuthorProfile(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCourseAuthorProfile(), CourseAuthorProfile.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateCourseAuthorProfileResponse.newBuilder(), "course_author_profile", created).build());
    }

    @Override
    public void updateCourseAuthorProfile(UpdateCourseAuthorProfileRequest request,
            StreamObserver<UpdateCourseAuthorProfileResponse> out) {
        CourseAuthorProfile updated = service.updateCourseAuthorProfile(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCourseAuthorProfile().getId(), "id"),
                bridge.requiredIfMatch(request.getCourseAuthorProfile().getEtag()),
                bridge.changes(request.getCourseAuthorProfile(), request.getUpdateMask(), CourseAuthorProfileUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateCourseAuthorProfileResponse.newBuilder(), "course_author_profile", updated).build());
    }

    @Override
    public void deleteCourseAuthorProfile(DeleteCourseAuthorProfileRequest request,
            StreamObserver<DeleteCourseAuthorProfileResponse> out) {
        service.deleteCourseAuthorProfile(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCourseAuthorProfileResponse.getDefaultInstance());
    }

    @Override
    public void listCoursePreviews(ListCoursePreviewsRequest request, StreamObserver<ListCoursePreviewsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CoursePreview> rows = service.listCoursePreviews(bridge.id(request.getCourseId(), "courseId"),
                page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCoursePreviewsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCoursePreview(GetCoursePreviewRequest request, StreamObserver<GetCoursePreviewResponse> out) {
        CoursePreview found = service.getCoursePreview(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCoursePreviewResponse.newBuilder(), "course_preview", found).build());
    }

    @Override
    public void createCoursePreview(CreateCoursePreviewRequest request, StreamObserver<CreateCoursePreviewResponse> out) {
        CoursePreview created = service.createCoursePreview(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCoursePreview(), CoursePreview.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCoursePreviewResponse.newBuilder(), "course_preview", created).build());
    }

    @Override
    public void updateCoursePreview(UpdateCoursePreviewRequest request, StreamObserver<UpdateCoursePreviewResponse> out) {
        CoursePreview updated = service.updateCoursePreview(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCoursePreview().getId(), "id"), bridge.requiredIfMatch(request.getCoursePreview().getEtag()),
                bridge.changes(request.getCoursePreview(), request.getUpdateMask(), CoursePreviewUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCoursePreviewResponse.newBuilder(), "course_preview", updated).build());
    }

    @Override
    public void deleteCoursePreview(DeleteCoursePreviewRequest request, StreamObserver<DeleteCoursePreviewResponse> out) {
        service.deleteCoursePreview(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCoursePreviewResponse.getDefaultInstance());
    }

    @Override
    public void listCourseTestimonials(ListCourseTestimonialsRequest request,
            StreamObserver<ListCourseTestimonialsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CourseTestimonial> rows = service.listCourseTestimonials(bridge.id(request.getCourseId(), "courseId"),
                page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCourseTestimonialsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCourseTestimonial(GetCourseTestimonialRequest request, StreamObserver<GetCourseTestimonialResponse> out) {
        CourseTestimonial found = service.getCourseTestimonial(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCourseTestimonialResponse.newBuilder(), "course_testimonial", found).build());
    }

    @Override
    public void createCourseTestimonial(CreateCourseTestimonialRequest request,
            StreamObserver<CreateCourseTestimonialResponse> out) {
        CourseTestimonial created = service.createCourseTestimonial(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCourseTestimonial(), CourseTestimonial.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCourseTestimonialResponse.newBuilder(), "course_testimonial", created).build());
    }

    @Override
    public void updateCourseTestimonial(UpdateCourseTestimonialRequest request,
            StreamObserver<UpdateCourseTestimonialResponse> out) {
        CourseTestimonial updated = service.updateCourseTestimonial(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCourseTestimonial().getId(), "id"),
                bridge.requiredIfMatch(request.getCourseTestimonial().getEtag()),
                bridge.changes(request.getCourseTestimonial(), request.getUpdateMask(), CourseTestimonialUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCourseTestimonialResponse.newBuilder(), "course_testimonial", updated).build());
    }

    @Override
    public void deleteCourseTestimonial(DeleteCourseTestimonialRequest request,
            StreamObserver<DeleteCourseTestimonialResponse> out) {
        service.deleteCourseTestimonial(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCourseTestimonialResponse.getDefaultInstance());
    }

    @Override
    public void listCatalogBanners(ListCatalogBannersRequest request, StreamObserver<ListCatalogBannersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CatalogBanner> rows = service.listCatalogBanners(bridge.id(request.getCourseId(), "courseId"),
                page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCatalogBannersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCatalogBanner(GetCatalogBannerRequest request, StreamObserver<GetCatalogBannerResponse> out) {
        CatalogBanner found = service.getCatalogBanner(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCatalogBannerResponse.newBuilder(), "catalog_banner", found).build());
    }

    @Override
    public void createCatalogBanner(CreateCatalogBannerRequest request, StreamObserver<CreateCatalogBannerResponse> out) {
        CatalogBanner created = service.createCatalogBanner(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCatalogBanner(), CatalogBanner.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCatalogBannerResponse.newBuilder(), "catalog_banner", created).build());
    }

    @Override
    public void updateCatalogBanner(UpdateCatalogBannerRequest request, StreamObserver<UpdateCatalogBannerResponse> out) {
        CatalogBanner updated = service.updateCatalogBanner(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCatalogBanner().getId(), "id"), bridge.requiredIfMatch(request.getCatalogBanner().getEtag()),
                bridge.changes(request.getCatalogBanner(), request.getUpdateMask(), CatalogBannerUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCatalogBannerResponse.newBuilder(), "catalog_banner", updated).build());
    }

    @Override
    public void deleteCatalogBanner(DeleteCatalogBannerRequest request, StreamObserver<DeleteCatalogBannerResponse> out) {
        service.deleteCatalogBanner(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCatalogBannerResponse.getDefaultInstance());
    }

    @Override
    public void listEnrollmentRequests(ListEnrollmentRequestsRequest request,
            StreamObserver<ListEnrollmentRequestsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EnrollmentRequest> rows = service.listEnrollmentRequests(bridge.id(request.getCourseId(), "courseId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEnrollmentRequestsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEnrollmentRequest(GetEnrollmentRequestRequest request, StreamObserver<GetEnrollmentRequestResponse> out) {
        EnrollmentRequest found = service.getEnrollmentRequest(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEnrollmentRequestResponse.newBuilder(), "enrollment_request", found).build());
    }

    @Override
    public void createEnrollmentRequest(CreateEnrollmentRequestRequest request,
            StreamObserver<CreateEnrollmentRequestResponse> out) {
        EnrollmentRequest created = service.createEnrollmentRequest(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getEnrollmentRequest(), EnrollmentRequest.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEnrollmentRequestResponse.newBuilder(), "enrollment_request", created).build());
    }

    @Override
    public void updateEnrollmentRequest(UpdateEnrollmentRequestRequest request,
            StreamObserver<UpdateEnrollmentRequestResponse> out) {
        EnrollmentRequest updated = service.updateEnrollmentRequest(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getEnrollmentRequest().getId(), "id"),
                bridge.requiredIfMatch(request.getEnrollmentRequest().getEtag()),
                bridge.changes(request.getEnrollmentRequest(), request.getUpdateMask(), EnrollmentRequestUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEnrollmentRequestResponse.newBuilder(), "enrollment_request", updated).build());
    }

    @Override
    public void deleteEnrollmentRequest(DeleteEnrollmentRequestRequest request,
            StreamObserver<DeleteEnrollmentRequestResponse> out) {
        service.deleteEnrollmentRequest(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEnrollmentRequestResponse.getDefaultInstance());
    }

    @Override
    public void publishCourse(PublishCourseRequest request, StreamObserver<PublishCourseResponse> out) {
        LmsCourse changed = service.publishCourse(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.PublishCourseRequest.class, "course_id",
                        "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(PublishCourseResponse.newBuilder(), "course", changed).build());
    }

    @Override
    public void unpublishCourse(UnpublishCourseRequest request, StreamObserver<UnpublishCourseResponse> out) {
        LmsCourse changed = service.unpublishCourse(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.UnpublishCourseRequest.class, "course_id",
                        "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(UnpublishCourseResponse.newBuilder(), "course", changed).build());
    }

    @Override
    public void submitCourseForReview(SubmitCourseForReviewRequest request, StreamObserver<SubmitCourseForReviewResponse> out) {
        LmsCourse changed = service.submitCourseForReview(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.SubmitCourseForReviewRequest.class, "course_id",
                        "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(SubmitCourseForReviewResponse.newBuilder(), "course", changed).build());
    }

    @Override
    public void approveCourse(ApproveCourseRequest request, StreamObserver<ApproveCourseResponse> out) {
        LmsCourse changed = service.approveCourse(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ApproveCourseRequest.class, "course_id",
                        "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(ApproveCourseResponse.newBuilder(), "course", changed).build());
    }

    @Override
    public void getCourseOutline(GetCourseOutlineRequest request, StreamObserver<GetCourseOutlineResponse> out) {
        CourseOutline outline = service.getCourseOutline(bridge.id(request.getCourseId(), "courseId"));
        ProtoBridge.send(out, bridge.reply(GetCourseOutlineResponse.newBuilder(), "result", outline).build());
    }

    @Override
    public void reorderChapters(ReorderChaptersRequest request, StreamObserver<ReorderChaptersResponse> out) {
        ChapterList reordered = service.reorderChapters(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ReorderChaptersRequest.class, "course_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ReorderChaptersResponse.newBuilder(), "items", reordered.getItems()).build());
    }

    @Override
    public void enrollInCourse(EnrollInCourseRequest request, StreamObserver<EnrollInCourseResponse> out) {
        Enrollment enrolled = enrollments.enrollInCourse(bridge.id(request.getCourseId(), "courseId"));
        ProtoBridge.send(out, bridge.reply(EnrollInCourseResponse.newBuilder(), "result", enrolled).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void batchEnrollInCourse(BatchEnrollInCourseRequest request, StreamObserver<BatchEnrollInCourseResponse> out) {
        com.mysms.learning.lms.api.model.BatchEnrollPreviewList lines = service.batchEnrollInCourse(
                bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.BatchEnrollInCourseRequest.class, "course_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(BatchEnrollInCourseResponse.newBuilder(), "items", lines.getItems())
                .build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void issueMyCertificate(IssueMyCertificateRequest request, StreamObserver<IssueMyCertificateResponse> out) {
        com.mysms.learning.lms.api.model.Certificate issued = service.issueMyCertificate(
                bridge.id(request.getCourseId(), "courseId"));
        ProtoBridge.send(out, bridge.reply(IssueMyCertificateResponse.newBuilder(), "result", issued).build());
    }

    @Override
    public void getCourseProgressDistribution(GetCourseProgressDistributionRequest request,
            StreamObserver<GetCourseProgressDistributionResponse> out) {
        Page<ProgressBucket> buckets = service.getCourseProgressDistribution(bridge.id(request.getCourseId(), "courseId"));
        ProtoBridge.send(out, bridge.page(GetCourseProgressDistributionResponse.newBuilder(), buckets).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void getEvaluatorSlots(GetEvaluatorSlotsRequest request, StreamObserver<GetEvaluatorSlotsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<com.mysms.learning.lms.api.model.EvaluatorSlot> slots = service.getEvaluatorSlots(
                bridge.id(request.getCourseId(), "courseId"),
                date(request.getFromDate(), "fromDate"), date(request.getToDate(), "toDate"),
                page.page(), page.size());
        ProtoBridge.send(out, bridge.page(GetEvaluatorSlotsResponse.newBuilder(), slots).build());
    }

    @Override
    public void exportCourse(ExportCourseRequest request, StreamObserver<ExportCourseResponse> out) {
        ProtoBridge.send(out, bridge.reply(ExportCourseResponse.newBuilder(), "result",
                packages.exportCourse(bridge.id(request.getCourseId(), "courseId"))).build());
    }

    @Override
    public void importCoursePackage(ImportCoursePackageRequest request,
            StreamObserver<ImportCoursePackageResponse> out) {
        ProtoBridge.send(out, bridge.reply(ImportCoursePackageResponse.newBuilder(), "result",
                packages.importCoursePackage(bridge.id(request.getCourseId(), "courseId"),
                        bridge.body(request, com.mysms.learning.lms.api.model.ImportCoursePackageRequest.class, "course_id",
                                "request_id"))).build());
    }

    @Override
    public void getScormLaunch(GetScormLaunchRequest request, StreamObserver<GetScormLaunchResponse> out) {
        ScormLaunch launch = service.getScormLaunch(bridge.id(request.getChapterId(), "chapterId"));
        ProtoBridge.send(out, bridge.reply(GetScormLaunchResponse.newBuilder(), "result", launch).build());
    }

    @Override
    public void saveScormProgress(SaveScormProgressRequest request, StreamObserver<SaveScormProgressResponse> out) {
        LessonProgress saved = service.saveScormProgress(bridge.id(request.getChapterId(), "chapterId"),
                bridge.body(request, com.mysms.learning.lms.api.model.SaveScormProgressRequest.class, "chapter_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(SaveScormProgressResponse.newBuilder(), "result", saved).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void createCourseVersion(CreateCourseVersionRequest request, StreamObserver<CreateCourseVersionResponse> out) {
        LmsCourse created = service.createCourseVersion(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.CreateCourseVersionRequest.class, "course_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CreateCourseVersionResponse.newBuilder(), "result", created).build());
    }

    @Override
    public void deactivateCourse(DeactivateCourseRequest request, StreamObserver<DeactivateCourseResponse> out) {
        com.mysms.learning.lms.api.model.DeactivateCourseRequest body =
                bridge.body(request, com.mysms.learning.lms.api.model.DeactivateCourseRequest.class, "course_id", "request_id");
        LmsCourse changed = service.deactivateCourse(bridge.id(request.getCourseId(), "courseId"), body.getEtag(),
                body.getReason());
        ProtoBridge.send(out, bridge.reply(DeactivateCourseResponse.newBuilder(), "course", changed).build());
    }

    @Override
    public void reactivateCourse(ReactivateCourseRequest request, StreamObserver<ReactivateCourseResponse> out) {
        LmsCourse changed = service.reactivateCourse(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ReactivateCourseRequest.class, "course_id",
                        "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(ReactivateCourseResponse.newBuilder(), "course", changed).build());
    }

    @Override
    public void publishCatalogListing(PublishCatalogListingRequest request, StreamObserver<PublishCatalogListingResponse> out) {
        LmsCourse changed = service.publishCatalogListing(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.PublishCatalogListingRequest.class, "course_id",
                        "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(PublishCatalogListingResponse.newBuilder(), "course", changed).build());
    }

    @Override
    public void hideCatalogListing(HideCatalogListingRequest request, StreamObserver<HideCatalogListingResponse> out) {
        LmsCourse changed = service.hideCatalogListing(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request, com.mysms.learning.lms.api.model.HideCatalogListingRequest.class, "course_id",
                        "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(HideCatalogListingResponse.newBuilder(), "course", changed).build());
    }

    @Override
    public void checkCourseDependencies(CheckCourseDependenciesRequest request,
            StreamObserver<CheckCourseDependenciesResponse> out) {
        Dependencies found = service.checkCourseDependencies(bridge.id(request.getCourseId(), "courseId"));
        ProtoBridge.send(out, bridge.reply(CheckCourseDependenciesResponse.newBuilder(), "result", found).build());
    }

    @Override
    public void getCourseSyllabus(GetCourseSyllabusRequest request, StreamObserver<GetCourseSyllabusResponse> out) {
        String format = switch (request.getFormat()) {
            case FORMAT_PDF -> "pdf";
            case FORMAT_DOCX -> "docx";
            case FORMAT_HTML -> "html";
            default -> null;
        };
        ProtoBridge.send(out, bridge.reply(GetCourseSyllabusResponse.newBuilder(), "result",
                service.getCourseSyllabus(bridge.id(request.getCourseId(), "courseId"), format)).build());
    }

    @Override
    public void getCatalogHome(GetCatalogHomeRequest request, StreamObserver<GetCatalogHomeResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        CatalogPage found = catalog.getCatalogHome(request.hasCategoryId() ? bridge.id(request.getCategoryId(), "categoryId")
                : null, bridge.orNull(request.getQuery()), page.page(), page.size(), page.cursor());
        GetCatalogHomeResponse.Builder reply = bridge.reply(GetCatalogHomeResponse.newBuilder(), "result",
                found.home().courses(found.courses().items()));
        // the home's courses travel with the same pagination a list of them would
        reply.getResultBuilder().setPagination(bridge.page(ListLmsCoursesResponse.newBuilder(), found.courses())
                .getPagination());
        ProtoBridge.send(out, reply.build());
    }

    @Override
    public void compareCourses(CompareCoursesRequest request, StreamObserver<CompareCoursesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LmsCourse> rows = catalog.compareCourses(request.getCourseIdsList().stream().map(id -> bridge.id(id, "courseIds"))
                .toList(), page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(CompareCoursesResponse.newBuilder(), rows).build());
    }

    @Override
    public void moveChapter(MoveChapterRequest request, StreamObserver<MoveChapterResponse> out) {
        Chapter moved = service.moveChapter(bridge.id(request.getChapterId(), "chapterId"),
                bridge.body(request, com.mysms.learning.lms.api.model.MoveChapterRequest.class, "chapter_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(MoveChapterResponse.newBuilder(), "result", moved).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void approveEnrollmentRequest(ApproveEnrollmentRequestRequest request,
            StreamObserver<ApproveEnrollmentRequestResponse> out) {
        com.mysms.learning.lms.api.model.EnrollmentRequest approved = service.approveEnrollmentRequest(
                bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getEnrollmentRequestId(), "enrollmentRequestId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ApproveEnrollmentRequestRequest.class, "course_id",
                        "enrollment_request_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ApproveEnrollmentRequestResponse.newBuilder(), "enrollment_request",
                approved).build());
    }

    /** The request is the REST body; the path ids and the idempotency key travel apart. */
    @Override
    public void rejectEnrollmentRequest(RejectEnrollmentRequestRequest request,
            StreamObserver<RejectEnrollmentRequestResponse> out) {
        EnrollmentRequest changed = service.rejectEnrollmentRequest(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getEnrollmentRequestId(), "enrollmentRequestId"),
                bridge.body(request, com.mysms.learning.lms.api.model.RejectEnrollmentRequestRequest.class,
                        "course_id", "enrollment_request_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectEnrollmentRequestResponse.newBuilder(), "enrollment_request", changed).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void getTeacherDashboard(GetTeacherDashboardRequest request, StreamObserver<GetTeacherDashboardResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetTeacherDashboardResponse.newBuilder(), "result",
                service.getTeacherDashboard()).build());
    }

    /** A required ISO date of REST's query string: 400 when absent or malformed, like REST's parameter binding. */
    private static LocalDate date(String value, String name) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidArgumentException("Invalid value for " + name + ".");
        }
    }
}
