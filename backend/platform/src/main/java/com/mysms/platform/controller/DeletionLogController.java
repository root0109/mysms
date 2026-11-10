package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.DeletionLogApi;
import com.mysms.platform.api.model.DeletionLog;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.DeletionLogService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the DeletionLog operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/platform.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class DeletionLogController implements DeletionLogApi {

    private final DeletionLogService deletionLogService;

    public DeletionLogController(DeletionLogService deletionLogService) {
        this.deletionLogService = deletionLogService;
    }

    @Override
    public ResponseEntity<DeletionLog> getDeletionLog(UUID deletionLogId, String fields) {
        DeletionLog found = deletionLogService.getDeletionLog(deletionLogId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<DeletionLog>> listDeletionLogs(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<DeletionLog> list =
                PagingUtil.pagingInfo(deletionLogService.listDeletionLogs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
