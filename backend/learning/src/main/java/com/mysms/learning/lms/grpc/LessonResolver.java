package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.api.model.LessonProgress;
import com.mysms.learning.lms.api.model.LessonContent;
import com.mysms.learning.lms.api.model.LessonNote;
import com.mysms.learning.lms.api.model.Lesson;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.LessonActivity;
import com.mysms.learning.lms.api.model.VideoWatch;
import com.mysms.learning.lms.business.LessonService;
import com.mysms.lms.v1.CreateLessonActivityRequest;
import com.mysms.lms.v1.CreateLessonActivityResponse;
import com.mysms.lms.v1.CreateLessonNoteRequest;
import com.mysms.lms.v1.CreateLessonNoteResponse;
import com.mysms.lms.v1.CreateLessonRequest;
import com.mysms.lms.v1.CreateLessonResponse;
import com.mysms.lms.v1.DeleteLessonActivityRequest;
import com.mysms.lms.v1.DeleteLessonActivityResponse;
import com.mysms.lms.v1.DeleteLessonNoteRequest;
import com.mysms.lms.v1.DeleteLessonNoteResponse;
import com.mysms.lms.v1.DeleteLessonRequest;
import com.mysms.lms.v1.DeleteLessonResponse;
import com.mysms.lms.v1.GetLessonActivityRequest;
import com.mysms.lms.v1.GetLessonActivityResponse;
import com.mysms.lms.v1.GetLessonContentRequest;
import com.mysms.lms.v1.GetLessonContentResponse;
import com.mysms.lms.v1.GetLessonNoteRequest;
import com.mysms.lms.v1.GetLessonNoteResponse;
import com.mysms.lms.v1.GetLessonRequest;
import com.mysms.lms.v1.GetLessonResponse;
import com.mysms.lms.v1.GetVideoWatchRequest;
import com.mysms.lms.v1.GetVideoWatchResponse;
import com.mysms.lms.v1.LessonActivityUpdate;
import com.mysms.lms.v1.LessonNoteUpdate;
import com.mysms.lms.v1.LessonServiceGrpc;
import com.mysms.lms.v1.LessonUpdate;
import com.mysms.lms.v1.ListLessonActivitiesRequest;
import com.mysms.lms.v1.ListLessonActivitiesResponse;
import com.mysms.lms.v1.ListLessonNotesRequest;
import com.mysms.lms.v1.ListLessonNotesResponse;
import com.mysms.lms.v1.ListLessonsRequest;
import com.mysms.lms.v1.ListLessonsResponse;
import com.mysms.lms.v1.ListVideoWatchesRequest;
import com.mysms.lms.v1.ListVideoWatchesResponse;
import com.mysms.lms.v1.MarkLessonCompleteRequest;
import com.mysms.lms.v1.MarkLessonCompleteResponse;
import com.mysms.lms.v1.MoveLessonRequest;
import com.mysms.lms.v1.MoveLessonResponse;
import com.mysms.lms.v1.TrackVideoWatchRequest;
import com.mysms.lms.v1.TrackVideoWatchResponse;
import com.mysms.lms.v1.UpdateLessonActivityResponse;
import com.mysms.lms.v1.UpdateLessonNoteResponse;
import com.mysms.lms.v1.UpdateLessonResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.LessonService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class LessonResolver extends LessonServiceGrpc.LessonServiceImplBase {

    private final LessonService service;
    private final ProtoBridge bridge;

    public LessonResolver(LessonService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLessons(ListLessonsRequest request, StreamObserver<ListLessonsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Lesson> rows = service.listLessons(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListLessonsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLesson(GetLessonRequest request, StreamObserver<GetLessonResponse> out) {
        Lesson found = service.getLesson(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLessonResponse.newBuilder(), "lesson", found).build());
    }

    @Override
    public void createLesson(CreateLessonRequest request, StreamObserver<CreateLessonResponse> out) {
        Lesson created = service.createLesson(bridge.body(request.getLesson(), Lesson.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLessonResponse.newBuilder(), "lesson", created).build());
    }

    @Override
    public void updateLesson(LessonUpdate request, StreamObserver<UpdateLessonResponse> out) {
        Lesson updated = service.updateLesson(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                LessonProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLessonResponse.newBuilder(), "lesson", updated).build());
    }

    @Override
    public void deleteLesson(DeleteLessonRequest request, StreamObserver<DeleteLessonResponse> out) {
        service.deleteLesson(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLessonResponse.getDefaultInstance());
    }

    @Override
    public void listLessonActivities(ListLessonActivitiesRequest request, StreamObserver<ListLessonActivitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LessonActivity> rows = service.listLessonActivitys(bridge.id(request.getLessonId(), "lessonId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLessonActivitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLessonActivity(GetLessonActivityRequest request, StreamObserver<GetLessonActivityResponse> out) {
        LessonActivity found = service.getLessonActivity(bridge.id(request.getLessonId(), "lessonId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLessonActivityResponse.newBuilder(), "lesson_activity", found).build());
    }

    @Override
    public void createLessonActivity(CreateLessonActivityRequest request, StreamObserver<CreateLessonActivityResponse> out) {
        LessonActivity created = service.createLessonActivity(bridge.id(request.getLessonId(), "lessonId"),
                bridge.body(request.getLessonActivity(), LessonActivity.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLessonActivityResponse.newBuilder(), "lesson_activity", created).build());
    }

    @Override
    public void updateLessonActivity(LessonActivityUpdate request, StreamObserver<UpdateLessonActivityResponse> out) {
        LessonActivity updated = service.updateLessonActivity(bridge.id(request.getLessonId(), "lessonId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                LessonActivityProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLessonActivityResponse.newBuilder(), "lesson_activity", updated).build());
    }

    @Override
    public void deleteLessonActivity(DeleteLessonActivityRequest request, StreamObserver<DeleteLessonActivityResponse> out) {
        service.deleteLessonActivity(bridge.id(request.getLessonId(), "lessonId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLessonActivityResponse.getDefaultInstance());
    }

    @Override
    public void listLessonNotes(ListLessonNotesRequest request, StreamObserver<ListLessonNotesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LessonNote> rows = service.listLessonNotes(bridge.id(request.getLessonId(), "lessonId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLessonNotesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLessonNote(GetLessonNoteRequest request, StreamObserver<GetLessonNoteResponse> out) {
        LessonNote found = service.getLessonNote(bridge.id(request.getLessonId(), "lessonId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLessonNoteResponse.newBuilder(), "lesson_note", found).build());
    }

    @Override
    public void createLessonNote(CreateLessonNoteRequest request, StreamObserver<CreateLessonNoteResponse> out) {
        LessonNote created = service.createLessonNote(bridge.id(request.getLessonId(), "lessonId"),
                bridge.body(request.getLessonNote(), LessonNote.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLessonNoteResponse.newBuilder(), "lesson_note", created).build());
    }

    @Override
    public void updateLessonNote(LessonNoteUpdate request, StreamObserver<UpdateLessonNoteResponse> out) {
        LessonNote updated = service.updateLessonNote(bridge.id(request.getLessonId(), "lessonId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                LessonNoteProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLessonNoteResponse.newBuilder(), "lesson_note", updated).build());
    }

    @Override
    public void deleteLessonNote(DeleteLessonNoteRequest request, StreamObserver<DeleteLessonNoteResponse> out) {
        service.deleteLessonNote(bridge.id(request.getLessonId(), "lessonId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLessonNoteResponse.getDefaultInstance());
    }

    @Override
    public void listVideoWatches(ListVideoWatchesRequest request, StreamObserver<ListVideoWatchesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<VideoWatch> rows = service.listVideoWatchs(bridge.id(request.getLessonId(), "lessonId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListVideoWatchesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getVideoWatch(GetVideoWatchRequest request, StreamObserver<GetVideoWatchResponse> out) {
        VideoWatch found = service.getVideoWatch(bridge.id(request.getLessonId(), "lessonId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetVideoWatchResponse.newBuilder(), "video_watch", found).build());
    }

    @Override
    public void getLessonContent(GetLessonContentRequest request, StreamObserver<GetLessonContentResponse> out) {
        LessonContent found = service.getLessonContent(bridge.id(request.getLessonId(), "lessonId"));
        ProtoBridge.send(out, bridge.reply(GetLessonContentResponse.newBuilder(), "result", found).build());
    }

    @Override
    public void moveLesson(MoveLessonRequest request, StreamObserver<MoveLessonResponse> out) {
        Lesson moved = service.moveLesson(bridge.id(request.getLessonId(), "lessonId"),
                bridge.body(request, com.mysms.learning.lms.api.model.MoveLessonRequest.class, "lesson_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(MoveLessonResponse.newBuilder(), "lesson", moved).build());
    }

    @Override
    public void markLessonComplete(MarkLessonCompleteRequest request, StreamObserver<MarkLessonCompleteResponse> out) {
        LessonProgress done = service.markLessonComplete(bridge.id(request.getLessonId(), "lessonId"));
        ProtoBridge.send(out, bridge.reply(MarkLessonCompleteResponse.newBuilder(), "result", done).build());
    }

    @Override
    public void trackVideoWatch(TrackVideoWatchRequest request, StreamObserver<TrackVideoWatchResponse> out) {
        VideoWatch watched = service.trackVideoWatch(bridge.id(request.getLessonId(), "lessonId"),
                bridge.body(request, com.mysms.learning.lms.api.model.TrackVideoWatchRequest.class, "lesson_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(TrackVideoWatchResponse.newBuilder(), "result", watched).build());
    }
}
