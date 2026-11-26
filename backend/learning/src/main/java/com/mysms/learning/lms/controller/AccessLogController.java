package com.mysms.learning.lms.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.api.AccessLogApi;
import com.mysms.learning.lms.api.model.AccessLog;
import com.mysms.learning.lms.business.AccessLogService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the AccessLog operations; the log is immutable, so its records carry no ETag. */
@RestController
public class AccessLogController implements AccessLogApi {

    private final AccessLogService accessLogService;

    public AccessLogController(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }

    @Override
    public ResponseEntity<AccessLog> getAccessLog(UUID accessLogId, String fields) {
        return ResponseEntity.ok(ReadMask.apply(accessLogService.getAccessLog(accessLogId), fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AccessLog>> listAccessLogs(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AccessLog> list =
                PagingUtil.pagingInfo(accessLogService.listAccessLogs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
