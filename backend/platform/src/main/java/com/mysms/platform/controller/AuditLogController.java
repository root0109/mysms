package com.mysms.platform.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.api.AuditLogApi;
import com.mysms.platform.api.model.AuditLog;
import com.mysms.platform.business.AuditLogService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the audit trail (read-only). */
@RestController
public class AuditLogController implements AuditLogApi {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public ResponseEntity<AuditLog> getAuditLog(UUID auditLogId, String fields) {
        return ResponseEntity.ok(ReadMask.apply(auditLogService.getAuditLog(auditLogId), fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AuditLog>> listAuditLogs(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AuditLog> list =
                PagingUtil.pagingInfo(auditLogService.listAuditLogs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
