package com.mysms.campus.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.library.api.model.Media;
import com.mysms.campus.library.api.model.MediaAuthor;
import com.mysms.campus.library.api.model.MediaAuthorUpdate;
import com.mysms.campus.library.api.model.MediaCourse;
import com.mysms.campus.library.api.model.MediaCourseUpdate;
import com.mysms.campus.library.api.model.MediaPublisher;
import com.mysms.campus.library.api.model.MediaPublisherUpdate;
import com.mysms.campus.library.api.model.MediaSubject;
import com.mysms.campus.library.api.model.MediaSubjectUpdate;
import com.mysms.campus.library.api.model.MediaUnit;
import com.mysms.campus.library.api.model.MediaUnitUpdate;
import com.mysms.campus.library.api.model.MediaUpdate;
import com.mysms.campus.library.business.MediaService;
import com.mysms.library.v1.CreateMediaAuthorRequest;
import com.mysms.library.v1.CreateMediaAuthorResponse;
import com.mysms.library.v1.CreateMediaCourseRequest;
import com.mysms.library.v1.CreateMediaCourseResponse;
import com.mysms.library.v1.CreateMediaPublisherRequest;
import com.mysms.library.v1.CreateMediaPublisherResponse;
import com.mysms.library.v1.CreateMediaRequest;
import com.mysms.library.v1.CreateMediaResponse;
import com.mysms.library.v1.CreateMediaSubjectRequest;
import com.mysms.library.v1.CreateMediaSubjectResponse;
import com.mysms.library.v1.CreateMediaUnitRequest;
import com.mysms.library.v1.CreateMediaUnitResponse;
import com.mysms.library.v1.DeleteMediaAuthorRequest;
import com.mysms.library.v1.DeleteMediaAuthorResponse;
import com.mysms.library.v1.DeleteMediaCourseRequest;
import com.mysms.library.v1.DeleteMediaCourseResponse;
import com.mysms.library.v1.DeleteMediaPublisherRequest;
import com.mysms.library.v1.DeleteMediaPublisherResponse;
import com.mysms.library.v1.DeleteMediaRequest;
import com.mysms.library.v1.DeleteMediaResponse;
import com.mysms.library.v1.DeleteMediaSubjectRequest;
import com.mysms.library.v1.DeleteMediaSubjectResponse;
import com.mysms.library.v1.DeleteMediaUnitRequest;
import com.mysms.library.v1.DeleteMediaUnitResponse;
import com.mysms.library.v1.GetMediaAuthorRequest;
import com.mysms.library.v1.GetMediaAuthorResponse;
import com.mysms.library.v1.GetMediaCourseRequest;
import com.mysms.library.v1.GetMediaCourseResponse;
import com.mysms.library.v1.GetMediaPublisherRequest;
import com.mysms.library.v1.GetMediaPublisherResponse;
import com.mysms.library.v1.GetMediaRequest;
import com.mysms.library.v1.GetMediaResponse;
import com.mysms.library.v1.GetMediaSubjectRequest;
import com.mysms.library.v1.GetMediaSubjectResponse;
import com.mysms.library.v1.GetMediaUnitRequest;
import com.mysms.library.v1.GetMediaUnitResponse;
import com.mysms.library.v1.ListMediaAuthorsRequest;
import com.mysms.library.v1.ListMediaAuthorsResponse;
import com.mysms.library.v1.ListMediaCoursesRequest;
import com.mysms.library.v1.ListMediaCoursesResponse;
import com.mysms.library.v1.ListMediaPublishersRequest;
import com.mysms.library.v1.ListMediaPublishersResponse;
import com.mysms.library.v1.ListMediaRequest;
import com.mysms.library.v1.ListMediaResponse;
import com.mysms.library.v1.ListMediaSubjectsRequest;
import com.mysms.library.v1.ListMediaSubjectsResponse;
import com.mysms.library.v1.ListMediaUnitsRequest;
import com.mysms.library.v1.ListMediaUnitsResponse;
import com.mysms.library.v1.MediaServiceGrpc;
import com.mysms.library.v1.UpdateMediaAuthorRequest;
import com.mysms.library.v1.UpdateMediaAuthorResponse;
import com.mysms.library.v1.UpdateMediaCourseRequest;
import com.mysms.library.v1.UpdateMediaCourseResponse;
import com.mysms.library.v1.UpdateMediaPublisherRequest;
import com.mysms.library.v1.UpdateMediaPublisherResponse;
import com.mysms.library.v1.UpdateMediaRequest;
import com.mysms.library.v1.UpdateMediaResponse;
import com.mysms.library.v1.UpdateMediaSubjectRequest;
import com.mysms.library.v1.UpdateMediaSubjectResponse;
import com.mysms.library.v1.UpdateMediaUnitRequest;
import com.mysms.library.v1.UpdateMediaUnitResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.MediaService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MediaResolver extends MediaServiceGrpc.MediaServiceImplBase {

    private final MediaService service;
    private final ProtoBridge bridge;

    public MediaResolver(MediaService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listMedia(ListMediaRequest request, StreamObserver<ListMediaResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Media> rows = service.listMedias(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMediaResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMedia(GetMediaRequest request, StreamObserver<GetMediaResponse> out) {
        Media found = service.getMedia(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMediaResponse.newBuilder(), "media", found).build());
    }

    @Override
    public void createMedia(CreateMediaRequest request, StreamObserver<CreateMediaResponse> out) {
        Media created = service.createMedia(
                bridge.body(request.getMedia(), Media.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMediaResponse.newBuilder(), "media", created).build());
    }

    @Override
    public void updateMedia(UpdateMediaRequest request, StreamObserver<UpdateMediaResponse> out) {
        Media updated = service.updateMedia(
                bridge.id(request.getMedia().getId(), "id"),
                bridge.requiredIfMatch(request.getMedia().getEtag()),
                bridge.changes(request.getMedia(), request.getUpdateMask(), MediaUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMediaResponse.newBuilder(), "media", updated).build());
    }

    @Override
    public void deleteMedia(DeleteMediaRequest request, StreamObserver<DeleteMediaResponse> out) {
        service.deleteMedia(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMediaResponse.getDefaultInstance());
    }

    @Override
    public void listMediaAuthors(ListMediaAuthorsRequest request, StreamObserver<ListMediaAuthorsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MediaAuthor> rows = service.listMediaAuthors(
                bridge.id(request.getMediaId(), "mediaId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMediaAuthorsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMediaAuthor(GetMediaAuthorRequest request, StreamObserver<GetMediaAuthorResponse> out) {
        MediaAuthor found = service.getMediaAuthor(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMediaAuthorResponse.newBuilder(), "media_author", found).build());
    }

    @Override
    public void createMediaAuthor(CreateMediaAuthorRequest request, StreamObserver<CreateMediaAuthorResponse> out) {
        MediaAuthor created = service.createMediaAuthor(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.body(request.getMediaAuthor(), MediaAuthor.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMediaAuthorResponse.newBuilder(), "media_author", created).build());
    }

    @Override
    public void updateMediaAuthor(UpdateMediaAuthorRequest request, StreamObserver<UpdateMediaAuthorResponse> out) {
        MediaAuthor updated = service.updateMediaAuthor(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getMediaAuthor().getId(), "id"),
                bridge.requiredIfMatch(request.getMediaAuthor().getEtag()),
                bridge.changes(request.getMediaAuthor(), request.getUpdateMask(), MediaAuthorUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMediaAuthorResponse.newBuilder(), "media_author", updated).build());
    }

    @Override
    public void deleteMediaAuthor(DeleteMediaAuthorRequest request, StreamObserver<DeleteMediaAuthorResponse> out) {
        service.deleteMediaAuthor(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMediaAuthorResponse.getDefaultInstance());
    }

    @Override
    public void listMediaPublishers(ListMediaPublishersRequest request, StreamObserver<ListMediaPublishersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MediaPublisher> rows = service.listMediaPublishers(
                bridge.id(request.getMediaId(), "mediaId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMediaPublishersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMediaPublisher(GetMediaPublisherRequest request, StreamObserver<GetMediaPublisherResponse> out) {
        MediaPublisher found = service.getMediaPublisher(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMediaPublisherResponse.newBuilder(), "media_publisher", found).build());
    }

    @Override
    public void createMediaPublisher(CreateMediaPublisherRequest request, StreamObserver<CreateMediaPublisherResponse> out) {
        MediaPublisher created = service.createMediaPublisher(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.body(request.getMediaPublisher(), MediaPublisher.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMediaPublisherResponse.newBuilder(), "media_publisher", created).build());
    }

    @Override
    public void updateMediaPublisher(UpdateMediaPublisherRequest request, StreamObserver<UpdateMediaPublisherResponse> out) {
        MediaPublisher updated = service.updateMediaPublisher(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getMediaPublisher().getId(), "id"),
                bridge.requiredIfMatch(request.getMediaPublisher().getEtag()),
                bridge.changes(request.getMediaPublisher(), request.getUpdateMask(), MediaPublisherUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMediaPublisherResponse.newBuilder(), "media_publisher", updated).build());
    }

    @Override
    public void deleteMediaPublisher(DeleteMediaPublisherRequest request, StreamObserver<DeleteMediaPublisherResponse> out) {
        service.deleteMediaPublisher(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMediaPublisherResponse.getDefaultInstance());
    }

    @Override
    public void listMediaCourses(ListMediaCoursesRequest request, StreamObserver<ListMediaCoursesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MediaCourse> rows = service.listMediaCourses(
                bridge.id(request.getMediaId(), "mediaId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMediaCoursesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMediaCourse(GetMediaCourseRequest request, StreamObserver<GetMediaCourseResponse> out) {
        MediaCourse found = service.getMediaCourse(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMediaCourseResponse.newBuilder(), "media_course", found).build());
    }

    @Override
    public void createMediaCourse(CreateMediaCourseRequest request, StreamObserver<CreateMediaCourseResponse> out) {
        MediaCourse created = service.createMediaCourse(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.body(request.getMediaCourse(), MediaCourse.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMediaCourseResponse.newBuilder(), "media_course", created).build());
    }

    @Override
    public void updateMediaCourse(UpdateMediaCourseRequest request, StreamObserver<UpdateMediaCourseResponse> out) {
        MediaCourse updated = service.updateMediaCourse(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getMediaCourse().getId(), "id"),
                bridge.requiredIfMatch(request.getMediaCourse().getEtag()),
                bridge.changes(request.getMediaCourse(), request.getUpdateMask(), MediaCourseUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMediaCourseResponse.newBuilder(), "media_course", updated).build());
    }

    @Override
    public void deleteMediaCourse(DeleteMediaCourseRequest request, StreamObserver<DeleteMediaCourseResponse> out) {
        service.deleteMediaCourse(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMediaCourseResponse.getDefaultInstance());
    }

    @Override
    public void listMediaSubjects(ListMediaSubjectsRequest request, StreamObserver<ListMediaSubjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MediaSubject> rows = service.listMediaSubjects(
                bridge.id(request.getMediaId(), "mediaId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMediaSubjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMediaSubject(GetMediaSubjectRequest request, StreamObserver<GetMediaSubjectResponse> out) {
        MediaSubject found = service.getMediaSubject(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMediaSubjectResponse.newBuilder(), "media_subject", found).build());
    }

    @Override
    public void createMediaSubject(CreateMediaSubjectRequest request, StreamObserver<CreateMediaSubjectResponse> out) {
        MediaSubject created = service.createMediaSubject(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.body(request.getMediaSubject(), MediaSubject.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMediaSubjectResponse.newBuilder(), "media_subject", created).build());
    }

    @Override
    public void updateMediaSubject(UpdateMediaSubjectRequest request, StreamObserver<UpdateMediaSubjectResponse> out) {
        MediaSubject updated = service.updateMediaSubject(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getMediaSubject().getId(), "id"),
                bridge.requiredIfMatch(request.getMediaSubject().getEtag()),
                bridge.changes(request.getMediaSubject(), request.getUpdateMask(), MediaSubjectUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMediaSubjectResponse.newBuilder(), "media_subject", updated).build());
    }

    @Override
    public void deleteMediaSubject(DeleteMediaSubjectRequest request, StreamObserver<DeleteMediaSubjectResponse> out) {
        service.deleteMediaSubject(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMediaSubjectResponse.getDefaultInstance());
    }

    @Override
    public void listMediaUnits(ListMediaUnitsRequest request, StreamObserver<ListMediaUnitsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MediaUnit> rows = service.listMediaUnits(
                bridge.id(request.getMediaId(), "mediaId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMediaUnitsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMediaUnit(GetMediaUnitRequest request, StreamObserver<GetMediaUnitResponse> out) {
        MediaUnit found = service.getMediaUnit(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMediaUnitResponse.newBuilder(), "media_unit", found).build());
    }

    @Override
    public void createMediaUnit(CreateMediaUnitRequest request, StreamObserver<CreateMediaUnitResponse> out) {
        MediaUnit created = service.createMediaUnit(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.body(request.getMediaUnit(), MediaUnit.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMediaUnitResponse.newBuilder(), "media_unit", created).build());
    }

    @Override
    public void updateMediaUnit(UpdateMediaUnitRequest request, StreamObserver<UpdateMediaUnitResponse> out) {
        MediaUnit updated = service.updateMediaUnit(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getMediaUnit().getId(), "id"),
                bridge.requiredIfMatch(request.getMediaUnit().getEtag()),
                bridge.changes(request.getMediaUnit(), request.getUpdateMask(), MediaUnitUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMediaUnitResponse.newBuilder(), "media_unit", updated).build());
    }

    @Override
    public void deleteMediaUnit(DeleteMediaUnitRequest request, StreamObserver<DeleteMediaUnitResponse> out) {
        service.deleteMediaUnit(
                bridge.id(request.getMediaId(), "mediaId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMediaUnitResponse.getDefaultInstance());
    }
}
