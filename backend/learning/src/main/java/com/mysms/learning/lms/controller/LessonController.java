package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.business.LessonService;
import com.mysms.learning.lms.api.LessonApi;
import com.mysms.learning.lms.api.model.Lesson;
import com.mysms.learning.lms.api.model.LessonActivity;
import com.mysms.learning.lms.api.model.LessonActivityUpdate;
import com.mysms.learning.lms.api.model.LessonContent;
import com.mysms.learning.lms.api.model.LessonNote;
import com.mysms.learning.lms.api.model.LessonNoteUpdate;
import com.mysms.learning.lms.api.model.LessonProgress;
import com.mysms.learning.lms.api.model.LessonUpdate;
import com.mysms.learning.lms.api.model.MoveLessonRequest;
import com.mysms.learning.lms.api.model.TrackVideoWatchRequest;
import com.mysms.learning.lms.api.model.VideoWatch;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Lesson operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class LessonController implements LessonApi {


    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Override
    public ResponseEntity<Lesson> createLesson(Lesson lesson, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Lesson created = lessonService.createLesson(lesson, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/lessons/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<LessonActivity> createLessonActivity(UUID lessonId,
            LessonActivity lessonActivity, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LessonActivity created = lessonService.createLessonActivity(lessonId, lessonActivity, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/lessons/" + lessonId + "/activities/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<LessonNote> createLessonNote(UUID lessonId, LessonNote lessonNote,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LessonNote created = lessonService.createLessonNote(lessonId, lessonNote, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/lessons/" + lessonId + "/notes/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLesson(UUID lessonId, String ifMatch) {
        lessonService.deleteLesson(lessonId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteLessonActivity(UUID lessonId, UUID lessonActivityId,
            String ifMatch) {
        lessonService.deleteLessonActivity(lessonId, lessonActivityId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteLessonNote(UUID lessonId, UUID lessonNoteId, String ifMatch) {
        lessonService.deleteLessonNote(lessonId, lessonNoteId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Lesson> getLesson(UUID lessonId, String fields) {
        Lesson found = lessonService.getLesson(lessonId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LessonActivity> getLessonActivity(UUID lessonId, UUID lessonActivityId,
            String fields) {
        LessonActivity found = lessonService.getLessonActivity(lessonId, lessonActivityId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LessonContent> getLessonContent(UUID lessonId) {
        return ResponseEntity.ok(lessonService.getLessonContent(lessonId));
    }

    @Override
    public ResponseEntity<LessonNote> getLessonNote(UUID lessonId, UUID lessonNoteId, String fields) {
        LessonNote found = lessonService.getLessonNote(lessonId, lessonNoteId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<VideoWatch> getVideoWatch(UUID lessonId, UUID videoWatchId, String fields) {
        VideoWatch found = lessonService.getVideoWatch(lessonId, videoWatchId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LessonActivity>> listLessonActivities(UUID lessonId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<LessonActivity> list =
                PagingUtil.pagingInfo(lessonService.listLessonActivitys(lessonId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<LessonNote>> listLessonNotes(UUID lessonId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<LessonNote> list =
                PagingUtil.pagingInfo(lessonService.listLessonNotes(lessonId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Lesson>> listLessons(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Lesson> list = PagingUtil.pagingInfo(lessonService.listLessons(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<VideoWatch>> listVideoWatches(UUID lessonId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<VideoWatch> list =
                PagingUtil.pagingInfo(lessonService.listVideoWatchs(lessonId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LessonProgress> markLessonComplete(UUID lessonId, Object body,
            UUID idempotencyKey) {
        return ResponseEntity.ok(lessonService.markLessonComplete(lessonId));
    }

    @Override
    public ResponseEntity<Lesson> moveLesson(UUID lessonId, MoveLessonRequest moveLessonRequest,
            UUID idempotencyKey) {
        Lesson moved = lessonService.moveLesson(lessonId, moveLessonRequest);
        return ResponseEntity.ok().eTag(moved.getEtag()).body(moved);
    }

    @Override
    public ResponseEntity<VideoWatch> trackVideoWatch(UUID lessonId,
            TrackVideoWatchRequest trackVideoWatchRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(lessonService.trackVideoWatch(lessonId, trackVideoWatchRequest));
    }

    @Override
    public ResponseEntity<Lesson> updateLesson(UUID lessonId, String ifMatch, LessonUpdate lessonUpdate,
            Boolean validateOnly) {
        Lesson updated = lessonService.updateLesson(lessonId, ifMatch, lessonUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<LessonActivity> updateLessonActivity(UUID lessonId, UUID lessonActivityId,
            String ifMatch, LessonActivityUpdate lessonActivityUpdate, Boolean validateOnly) {
        LessonActivity updated =
                lessonService.updateLessonActivity(lessonId, lessonActivityId, ifMatch, lessonActivityUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<LessonNote> updateLessonNote(UUID lessonId, UUID lessonNoteId, String ifMatch,
            LessonNoteUpdate lessonNoteUpdate, Boolean validateOnly) {
        LessonNote updated = lessonService.updateLessonNote(lessonId, lessonNoteId, ifMatch, lessonNoteUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
