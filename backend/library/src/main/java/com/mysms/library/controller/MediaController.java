package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.MediaApi;
import com.mysms.library.api.model.Media;
import com.mysms.library.api.model.MediaAuthor;
import com.mysms.library.api.model.MediaAuthorUpdate;
import com.mysms.library.api.model.MediaCourse;
import com.mysms.library.api.model.MediaCourseUpdate;
import com.mysms.library.api.model.MediaPublisher;
import com.mysms.library.api.model.MediaPublisherUpdate;
import com.mysms.library.api.model.MediaSubject;
import com.mysms.library.api.model.MediaSubjectUpdate;
import com.mysms.library.api.model.MediaUnit;
import com.mysms.library.api.model.MediaUnitUpdate;
import com.mysms.library.api.model.MediaUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.MediaService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Media operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/library.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class MediaController implements MediaApi {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Override
    public ResponseEntity<Media> createMedia(Media media, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Media created = mediaService.createMedia(media, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/media/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<MediaAuthor> createMediaAuthor(UUID mediaId, MediaAuthor mediaAuthor,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        MediaAuthor created = mediaService.createMediaAuthor(mediaId, mediaAuthor, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/media/" + mediaId + "/authors/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<MediaCourse> createMediaCourse(UUID mediaId, MediaCourse mediaCourse,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        MediaCourse created = mediaService.createMediaCourse(mediaId, mediaCourse, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/media/" + mediaId + "/courses/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<MediaPublisher> createMediaPublisher(UUID mediaId,
            MediaPublisher mediaPublisher, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        MediaPublisher created = mediaService.createMediaPublisher(mediaId, mediaPublisher, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/media/" + mediaId + "/publishers/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<MediaSubject> createMediaSubject(UUID mediaId, MediaSubject mediaSubject,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        MediaSubject created = mediaService.createMediaSubject(mediaId, mediaSubject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/media/" + mediaId + "/subjects/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<MediaUnit> createMediaUnit(UUID mediaId, MediaUnit mediaUnit,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        MediaUnit created = mediaService.createMediaUnit(mediaId, mediaUnit, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/media/" + mediaId + "/units/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteMedia(UUID mediaId, String ifMatch) {
        mediaService.deleteMedia(mediaId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteMediaAuthor(UUID mediaId, UUID mediaAuthorId, String ifMatch) {
        mediaService.deleteMediaAuthor(mediaId, mediaAuthorId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteMediaCourse(UUID mediaId, UUID mediaCourseId, String ifMatch) {
        mediaService.deleteMediaCourse(mediaId, mediaCourseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteMediaPublisher(UUID mediaId, UUID mediaPublisherId,
            String ifMatch) {
        mediaService.deleteMediaPublisher(mediaId, mediaPublisherId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteMediaSubject(UUID mediaId, UUID mediaSubjectId, String ifMatch) {
        mediaService.deleteMediaSubject(mediaId, mediaSubjectId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteMediaUnit(UUID mediaId, UUID mediaUnitId, String ifMatch) {
        mediaService.deleteMediaUnit(mediaId, mediaUnitId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Media> getMedia(UUID mediaId, String fields) {
        Media found = mediaService.getMedia(mediaId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<MediaAuthor> getMediaAuthor(UUID mediaId, UUID mediaAuthorId, String fields) {
        MediaAuthor found = mediaService.getMediaAuthor(mediaId, mediaAuthorId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<MediaCourse> getMediaCourse(UUID mediaId, UUID mediaCourseId, String fields) {
        MediaCourse found = mediaService.getMediaCourse(mediaId, mediaCourseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<MediaPublisher> getMediaPublisher(UUID mediaId, UUID mediaPublisherId,
            String fields) {
        MediaPublisher found = mediaService.getMediaPublisher(mediaId, mediaPublisherId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<MediaSubject> getMediaSubject(UUID mediaId, UUID mediaSubjectId,
            String fields) {
        MediaSubject found = mediaService.getMediaSubject(mediaId, mediaSubjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<MediaUnit> getMediaUnit(UUID mediaId, UUID mediaUnitId, String fields) {
        MediaUnit found = mediaService.getMediaUnit(mediaId, mediaUnitId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Media>> listMedia(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Media> list = PagingUtil.pagingInfo(mediaService.listMedias(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<MediaAuthor>> listMediaAuthors(UUID mediaId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<MediaAuthor> list =
                PagingUtil.pagingInfo(mediaService.listMediaAuthors(mediaId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<MediaCourse>> listMediaCourses(UUID mediaId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<MediaCourse> list =
                PagingUtil.pagingInfo(mediaService.listMediaCourses(mediaId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<MediaPublisher>> listMediaPublishers(UUID mediaId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<MediaPublisher> list =
                PagingUtil.pagingInfo(mediaService.listMediaPublishers(mediaId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<MediaSubject>> listMediaSubjects(UUID mediaId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<MediaSubject> list =
                PagingUtil.pagingInfo(mediaService.listMediaSubjects(mediaId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<MediaUnit>> listMediaUnits(UUID mediaId, Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<MediaUnit> list =
                PagingUtil.pagingInfo(mediaService.listMediaUnits(mediaId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Media> updateMedia(UUID mediaId, String ifMatch, MediaUpdate mediaUpdate,
            String updateMask, Boolean validateOnly) {
        Media updated = mediaService.updateMedia(mediaId, ifMatch, mediaUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<MediaAuthor> updateMediaAuthor(UUID mediaId, UUID mediaAuthorId, String ifMatch,
            MediaAuthorUpdate mediaAuthorUpdate, String updateMask, Boolean validateOnly) {
        MediaAuthor updated =
                mediaService.updateMediaAuthor(mediaId, mediaAuthorId, ifMatch, mediaAuthorUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<MediaCourse> updateMediaCourse(UUID mediaId, UUID mediaCourseId, String ifMatch,
            MediaCourseUpdate mediaCourseUpdate, String updateMask, Boolean validateOnly) {
        MediaCourse updated =
                mediaService.updateMediaCourse(mediaId, mediaCourseId, ifMatch, mediaCourseUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<MediaPublisher> updateMediaPublisher(UUID mediaId, UUID mediaPublisherId,
            String ifMatch, MediaPublisherUpdate mediaPublisherUpdate, String updateMask, Boolean validateOnly) {
        MediaPublisher updated =
                mediaService.updateMediaPublisher(mediaId, mediaPublisherId, ifMatch, mediaPublisherUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<MediaSubject> updateMediaSubject(UUID mediaId, UUID mediaSubjectId,
            String ifMatch, MediaSubjectUpdate mediaSubjectUpdate, String updateMask, Boolean validateOnly) {
        MediaSubject updated =
                mediaService.updateMediaSubject(mediaId, mediaSubjectId, ifMatch, mediaSubjectUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<MediaUnit> updateMediaUnit(UUID mediaId, UUID mediaUnitId, String ifMatch,
            MediaUnitUpdate mediaUnitUpdate, String updateMask, Boolean validateOnly) {
        MediaUnit updated = mediaService.updateMediaUnit(mediaId, mediaUnitId, ifMatch, mediaUnitUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
