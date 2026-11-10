package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ReassignmentLogApi;
import com.mysms.lms.api.model.ReassignmentLog;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ReassignmentLogService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ReassignmentLog operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ReassignmentLogController implements ReassignmentLogApi {

    private final ReassignmentLogService reassignmentLogService;

    public ReassignmentLogController(ReassignmentLogService reassignmentLogService) {
        this.reassignmentLogService = reassignmentLogService;
    }

    @Override
    public ResponseEntity<ReassignmentLog> getReassignmentLog(UUID reassignmentLogId, String fields) {
        ReassignmentLog found = reassignmentLogService.getReassignmentLog(reassignmentLogId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ReassignmentLog>> listReassignmentLogs(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ReassignmentLog> list =
                PagingUtil.pagingInfo(reassignmentLogService.listReassignmentLogs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
