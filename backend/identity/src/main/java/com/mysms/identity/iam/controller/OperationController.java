package com.mysms.identity.iam.controller;

import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.identity.iam.api.OperationApi;
import com.mysms.identity.iam.api.model.Operation;
import com.mysms.identity.iam.business.OperationService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of iam's long-running operations (tag Operation). */
@RestController
public class OperationController implements OperationApi {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    public ResponseEntity<Operation> cancelOperation(UUID operationId, UUID idempotencyKey) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(operationService.cancelOperation(operationId));
    }

    @Override
    public ResponseEntity<Operation> getOperation(UUID operationId) {
        return ResponseEntity.ok(operationService.getOperation(operationId));
    }

    @Override
    public ResponseEntity<PagingInfo<Operation>> listOperations(String kind, Integer page, Integer size,
            String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(operationService.listOperations(kind, page, size, cursor)));
    }
}
