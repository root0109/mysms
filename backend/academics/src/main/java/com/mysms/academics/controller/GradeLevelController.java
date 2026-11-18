package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.GradeLevelService;
import com.mysms.academics.api.GradeLevelApi;
import com.mysms.academics.api.model.GradeLevel;
import com.mysms.academics.api.model.GradeLevelUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the GradeLevel operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class GradeLevelController implements GradeLevelApi {

    private final GradeLevelService gradeLevelService;

    public GradeLevelController(GradeLevelService gradeLevelService) {
        this.gradeLevelService = gradeLevelService;
    }

    @Override
    public ResponseEntity<GradeLevel> createGradeLevel(GradeLevel gradeLevel, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        GradeLevel created = gradeLevelService.createGradeLevel(gradeLevel, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/grade-levels/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteGradeLevel(UUID gradeLevelId, String ifMatch) {
        gradeLevelService.deleteGradeLevel(gradeLevelId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GradeLevel> getGradeLevel(UUID gradeLevelId, String fields) {
        GradeLevel found = gradeLevelService.getGradeLevel(gradeLevelId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<GradeLevel>> listGradeLevels(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<GradeLevel> list =
                PagingUtil.pagingInfo(gradeLevelService.listGradeLevels(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<GradeLevel> updateGradeLevel(UUID gradeLevelId, String ifMatch,
            GradeLevelUpdate gradeLevelUpdate, String updateMask, Boolean validateOnly) {
        GradeLevel updated = gradeLevelService.updateGradeLevel(gradeLevelId, ifMatch, gradeLevelUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
