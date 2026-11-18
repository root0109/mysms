package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.LearningProgramApi;
import com.mysms.lms.api.model.LearningProgram;
import com.mysms.lms.api.model.LearningProgramCourse;
import com.mysms.lms.api.model.LearningProgramCourseUpdate;
import com.mysms.lms.api.model.LearningProgramMember;
import com.mysms.lms.api.model.LearningProgramMemberUpdate;
import com.mysms.lms.api.model.LearningProgramUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.LearningProgramService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LearningProgram operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class LearningProgramController implements LearningProgramApi {

    private final LearningProgramService learningProgramService;

    public LearningProgramController(LearningProgramService learningProgramService) {
        this.learningProgramService = learningProgramService;
    }

    @Override
    public ResponseEntity<LearningProgram> createLearningProgram(LearningProgram learningProgram,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LearningProgram created = learningProgramService.createLearningProgram(learningProgram, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/learning-programs/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<LearningProgramCourse> createLearningProgramCourse(UUID learningProgramId,
            LearningProgramCourse learningProgramCourse, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LearningProgramCourse created =
                learningProgramService.createLearningProgramCourse(learningProgramId, learningProgramCourse,
                        dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/learning-programs/" + learningProgramId + "/courses/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<LearningProgramMember> createLearningProgramMember(UUID learningProgramId,
            LearningProgramMember learningProgramMember, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LearningProgramMember created =
                learningProgramService.createLearningProgramMember(learningProgramId, learningProgramMember,
                        dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/learning-programs/" + learningProgramId + "/members/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLearningProgram(UUID learningProgramId, String ifMatch) {
        learningProgramService.deleteLearningProgram(learningProgramId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteLearningProgramCourse(UUID learningProgramId,
            UUID learningProgramCourseId, String ifMatch) {
        learningProgramService.deleteLearningProgramCourse(learningProgramId, learningProgramCourseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteLearningProgramMember(UUID learningProgramId,
            UUID learningProgramMemberId, String ifMatch) {
        learningProgramService.deleteLearningProgramMember(learningProgramId, learningProgramMemberId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LearningProgramMember> enrollInLearningProgram(UUID learningProgramId,
            Object body, UUID idempotencyKey) {
        LearningProgramMember created = learningProgramService.enrollInLearningProgram(learningProgramId);
        return ResponseEntity.ok().eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<LearningProgram> getLearningProgram(UUID learningProgramId, String fields) {
        LearningProgram found = learningProgramService.getLearningProgram(learningProgramId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LearningProgramCourse> getLearningProgramCourse(UUID learningProgramId,
            UUID learningProgramCourseId, String fields) {
        LearningProgramCourse found =
                learningProgramService.getLearningProgramCourse(learningProgramId, learningProgramCourseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LearningProgramMember> getLearningProgramMember(UUID learningProgramId,
            UUID learningProgramMemberId, String fields) {
        LearningProgramMember found =
                learningProgramService.getLearningProgramMember(learningProgramId, learningProgramMemberId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LearningProgramCourse>> listLearningProgramCourses(UUID learningProgramId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<LearningProgramCourse> list =
                PagingUtil.pagingInfo(learningProgramService.listLearningProgramCourses(learningProgramId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<LearningProgramMember>> listLearningProgramMembers(UUID learningProgramId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<LearningProgramMember> list =
                PagingUtil.pagingInfo(learningProgramService.listLearningProgramMembers(learningProgramId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<LearningProgram>> listLearningPrograms(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LearningProgram> list =
                PagingUtil.pagingInfo(learningProgramService.listLearningPrograms(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LearningProgram> updateLearningProgram(UUID learningProgramId, String ifMatch,
            LearningProgramUpdate learningProgramUpdate, String updateMask, Boolean validateOnly) {
        LearningProgram updated =
                learningProgramService.updateLearningProgram(learningProgramId, ifMatch, learningProgramUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<LearningProgramCourse> updateLearningProgramCourse(UUID learningProgramId,
            UUID learningProgramCourseId, String ifMatch, LearningProgramCourseUpdate learningProgramCourseUpdate,
            String updateMask, Boolean validateOnly) {
        LearningProgramCourse updated =
                learningProgramService.updateLearningProgramCourse(learningProgramId, learningProgramCourseId,
                        ifMatch, learningProgramCourseUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<LearningProgramMember> updateLearningProgramMember(UUID learningProgramId,
            UUID learningProgramMemberId, String ifMatch, LearningProgramMemberUpdate learningProgramMemberUpdate,
            String updateMask, Boolean validateOnly) {
        LearningProgramMember updated =
                learningProgramService.updateLearningProgramMember(learningProgramId, learningProgramMemberId,
                        ifMatch, learningProgramMemberUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
