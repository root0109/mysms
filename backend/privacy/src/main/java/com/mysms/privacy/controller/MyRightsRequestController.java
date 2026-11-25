package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.privacy.api.MyRightsRequestApi;
import com.mysms.privacy.api.model.AccessSummary;
import com.mysms.privacy.api.model.MyRightsRequest;
import com.mysms.privacy.api.model.SubmitMyRequestRequest;
import com.mysms.privacy.business.RightsRequestService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the requester's own rights requests; authentication and the permission check already ran in the
 * auth-client filter.
 */
@RestController
public class MyRightsRequestController implements MyRightsRequestApi {

    private static final String MY_REQUESTS = "/v1/privacy/my/requests/";

    private final RightsRequestService rightsRequestService;

    public MyRightsRequestController(RightsRequestService rightsRequestService) {
        this.rightsRequestService = rightsRequestService;
    }

    @Override
    public ResponseEntity<AccessSummary> getMyAccessSummary(UUID rightsRequestId) {
        return ResponseEntity.ok(rightsRequestService.getMyAccessSummary(rightsRequestId));
    }

    @Override
    public ResponseEntity<MyRightsRequest> getMyRequest(UUID rightsRequestId) {
        return ResponseEntity.ok(rightsRequestService.getMyRequest(rightsRequestId));
    }

    @Override
    public ResponseEntity<PagingInfo<MyRightsRequest>> listMyRequests(Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(rightsRequestService.listMyRequests(page, size, cursor)));
    }

    @Override
    public ResponseEntity<MyRightsRequest> submitMyRequest(SubmitMyRequestRequest submitMyRequestRequest,
            UUID idempotencyKey) {
        MyRightsRequest submitted = rightsRequestService.submitMyRequest(submitMyRequestRequest);
        return ResponseEntity.created(URI.create(MY_REQUESTS + submitted.getId())).body(submitted);
    }
}
