package com.mysms.frontoffice.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.frontoffice.api.VisitorLogApi;
import com.mysms.frontoffice.api.model.CheckOutVisitorRequest;
import com.mysms.frontoffice.api.model.VisitorLog;
import com.mysms.frontoffice.api.model.VisitorLogUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.frontoffice.business.VisitorLogService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the VisitorLog operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/frontoffice.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class VisitorLogController implements VisitorLogApi {

    private final VisitorLogService visitorLogService;

    public VisitorLogController(VisitorLogService visitorLogService) {
        this.visitorLogService = visitorLogService;
    }

    @Override
    public ResponseEntity<VisitorLog> checkOutVisitor(UUID visitorLogId,
            CheckOutVisitorRequest checkOutVisitorRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<VisitorLog> createVisitorLog(VisitorLog visitorLog, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        VisitorLog created = visitorLogService.createVisitorLog(visitorLog, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/frontoffice/visitor-logs/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteVisitorLog(UUID visitorLogId, String ifMatch) {
        visitorLogService.deleteVisitorLog(visitorLogId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<VisitorLog> getVisitorLog(UUID visitorLogId, String fields) {
        VisitorLog found = visitorLogService.getVisitorLog(visitorLogId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<VisitorLog>> listVisitorLogs(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<VisitorLog> list =
                PagingUtil.pagingInfo(visitorLogService.listVisitorLogs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<VisitorLog> updateVisitorLog(UUID visitorLogId, String ifMatch,
            VisitorLogUpdate visitorLogUpdate, String updateMask, Boolean validateOnly) {
        VisitorLog updated = visitorLogService.updateVisitorLog(visitorLogId, ifMatch, visitorLogUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
