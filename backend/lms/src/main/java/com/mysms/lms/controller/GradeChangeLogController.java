package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.GradeChangeLogApi;
import com.mysms.lms.api.model.GradeChangeLog;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.GradeChangeLogService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the GradeChangeLog operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class GradeChangeLogController implements GradeChangeLogApi {

    private final GradeChangeLogService gradeChangeLogService;

    public GradeChangeLogController(GradeChangeLogService gradeChangeLogService) {
        this.gradeChangeLogService = gradeChangeLogService;
    }

    @Override
    public ResponseEntity<GradeChangeLog> getGradeChangeLog(UUID gradeChangeLogId, String fields) {
        GradeChangeLog found = gradeChangeLogService.getGradeChangeLog(gradeChangeLogId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<GradeChangeLog>> listGradeChangeLogs(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<GradeChangeLog> list =
                PagingUtil.pagingInfo(gradeChangeLogService.listGradeChangeLogs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
