package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.TeacherSubjectService;
import com.mysms.academics.api.TeacherSubjectApi;
import com.mysms.academics.api.model.TeacherSubject;
import com.mysms.academics.api.model.TeacherSubjectUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the TeacherSubject operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class TeacherSubjectController implements TeacherSubjectApi {

    private final TeacherSubjectService teacherSubjectService;

    public TeacherSubjectController(TeacherSubjectService teacherSubjectService) {
        this.teacherSubjectService = teacherSubjectService;
    }

    @Override
    public ResponseEntity<TeacherSubject> createTeacherSubject(
            TeacherSubject teacherSubject, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        TeacherSubject created = teacherSubjectService.createTeacherSubject(teacherSubject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/teacher-subjects/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTeacherSubject(UUID teacherSubjectId, String ifMatch) {
        teacherSubjectService.deleteTeacherSubject(teacherSubjectId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TeacherSubject> getTeacherSubject(UUID teacherSubjectId,
            String fields) {
        TeacherSubject found = teacherSubjectService.getTeacherSubject(teacherSubjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<TeacherSubject>> listTeacherSubjects(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<TeacherSubject> list =
                PagingUtil.pagingInfo(teacherSubjectService.listTeacherSubjects(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<TeacherSubject> updateTeacherSubject(UUID teacherSubjectId,
            String ifMatch, TeacherSubjectUpdate teacherSubjectUpdate, Boolean validateOnly) {
        TeacherSubject updated =
                teacherSubjectService.updateTeacherSubject(teacherSubjectId, ifMatch,
                        teacherSubjectUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
