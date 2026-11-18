package com.mysms.frontoffice.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.frontoffice.api.CallLogApi;
import com.mysms.frontoffice.api.model.CallLog;
import com.mysms.frontoffice.api.model.CallLogUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.frontoffice.business.CallLogService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CallLog operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class CallLogController implements CallLogApi {

    private final CallLogService callLogService;

    public CallLogController(CallLogService callLogService) {
        this.callLogService = callLogService;
    }

    @Override
    public ResponseEntity<CallLog> createCallLog(CallLog callLog, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CallLog created = callLogService.createCallLog(callLog, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/frontoffice/call-logs/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCallLog(UUID callLogId, String ifMatch) {
        callLogService.deleteCallLog(callLogId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CallLog> getCallLog(UUID callLogId, String fields) {
        CallLog found = callLogService.getCallLog(callLogId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CallLog>> listCallLogs(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<CallLog> list = PagingUtil.pagingInfo(callLogService.listCallLogs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CallLog> updateCallLog(UUID callLogId, String ifMatch,
            CallLogUpdate callLogUpdate, String updateMask, Boolean validateOnly) {
        CallLog updated = callLogService.updateCallLog(callLogId, ifMatch, callLogUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
