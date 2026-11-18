package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.SubjectService;
import com.mysms.academics.api.SubjectApi;
import com.mysms.academics.api.model.Subject;
import com.mysms.academics.api.model.SubjectUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Subject operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class SubjectController implements SubjectApi {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @Override
    public ResponseEntity<Subject> createSubject(Subject subject, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Subject created = subjectService.createSubject(subject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/subjects/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSubject(UUID subjectId, String ifMatch) {
        subjectService.deleteSubject(subjectId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Subject> getSubject(UUID subjectId, String fields) {
        Subject found = subjectService.getSubject(subjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Subject>> listSubjects(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Subject> list = PagingUtil.pagingInfo(subjectService.listSubjects(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Subject> updateSubject(UUID subjectId, String ifMatch,
            SubjectUpdate subjectUpdate, String updateMask, Boolean validateOnly) {
        Subject updated = subjectService.updateSubject(subjectId, ifMatch, subjectUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
