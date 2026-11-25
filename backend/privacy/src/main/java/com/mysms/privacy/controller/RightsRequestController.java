package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.RightsRequestApi;
import com.mysms.privacy.api.model.AccessSummary;
import com.mysms.privacy.api.model.AssignRightsRequestRequest;
import com.mysms.privacy.api.model.CloseRightsRequestRequest;
import com.mysms.privacy.api.model.ExtendRightsRequestRequest;
import com.mysms.privacy.api.model.FulfilAccessRequestRequest;
import com.mysms.privacy.api.model.FulfilRightsRequestRequest;
import com.mysms.privacy.api.model.RejectRightsRequestRequest;
import com.mysms.privacy.api.model.RetryRightsRequestTaskRequest;
import com.mysms.privacy.api.model.RightsRequest;
import com.mysms.privacy.api.model.VerifyRightsRequestIdentityRequest;
import com.mysms.privacy.business.RightsRequestService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the staff's rights requests (the tenant's queue and the steps on a request); authentication and the
 * permission check already ran in the auth-client filter.
 */
@RestController
public class RightsRequestController implements RightsRequestApi {

    private final RightsRequestService rightsRequestService;

    public RightsRequestController(RightsRequestService rightsRequestService) {
        this.rightsRequestService = rightsRequestService;
    }

    @Override
    public ResponseEntity<RightsRequest> assignRightsRequest(UUID rightsRequestId,
            AssignRightsRequestRequest assignRightsRequestRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.assignRightsRequest(rightsRequestId, assignRightsRequestRequest));
    }

    @Override
    public ResponseEntity<RightsRequest> closeRightsRequest(UUID rightsRequestId,
            CloseRightsRequestRequest closeRightsRequestRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.closeRightsRequest(rightsRequestId, closeRightsRequestRequest));
    }

    @Override
    public ResponseEntity<RightsRequest> extendRightsRequest(UUID rightsRequestId,
            ExtendRightsRequestRequest extendRightsRequestRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.extendRightsRequest(rightsRequestId, extendRightsRequestRequest));
    }

    @Override
    public ResponseEntity<RightsRequest> fulfilAccessRequest(UUID rightsRequestId,
            FulfilAccessRequestRequest fulfilAccessRequestRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.fulfilAccessRequest(rightsRequestId, fulfilAccessRequestRequest));
    }

    @Override
    public ResponseEntity<RightsRequest> fulfilWithdrawAllRequest(UUID rightsRequestId,
            FulfilRightsRequestRequest fulfilRightsRequestRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.fulfilWithdrawAllRequest(rightsRequestId, fulfilRightsRequestRequest));
    }

    @Override
    public ResponseEntity<AccessSummary> getAccessSummary(UUID rightsRequestId) {
        return ResponseEntity.ok(rightsRequestService.getAccessSummary(rightsRequestId));
    }

    @Override
    public ResponseEntity<RightsRequest> getRightsRequest(UUID rightsRequestId, String fields) {
        RightsRequest found = rightsRequestService.getRightsRequest(rightsRequestId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RightsRequest>> listRightsRequests(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<RightsRequest> list =
                PagingUtil.pagingInfo(rightsRequestService.listRightsRequests(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<RightsRequest> rejectRightsRequest(UUID rightsRequestId,
            RejectRightsRequestRequest rejectRightsRequestRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.rejectRightsRequest(rightsRequestId, rejectRightsRequestRequest));
    }

    @Override
    public ResponseEntity<RightsRequest> retryRightsRequestTask(UUID rightsRequestId, UUID taskId,
            RetryRightsRequestTaskRequest retryRightsRequestTaskRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.retryRightsRequestTask(rightsRequestId, taskId,
                retryRightsRequestTaskRequest));
    }

    @Override
    public ResponseEntity<RightsRequest> startRightsRequestFulfilment(UUID rightsRequestId,
            FulfilRightsRequestRequest fulfilRightsRequestRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.startRightsRequestFulfilment(rightsRequestId, fulfilRightsRequestRequest));
    }

    @Override
    public ResponseEntity<RightsRequest> verifyRightsRequestIdentity(UUID rightsRequestId,
            VerifyRightsRequestIdentityRequest verifyRightsRequestIdentityRequest, UUID idempotencyKey) {
        return stepped(rightsRequestService.verifyRightsRequestIdentity(rightsRequestId,
                verifyRightsRequestIdentityRequest));
    }

    private static ResponseEntity<RightsRequest> stepped(RightsRequest changed) {
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }
}
