package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.RetentionRunApi;
import com.mysms.privacy.api.model.RetentionRun;
import com.mysms.privacy.business.RetentionRunService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the tenant's retention runs; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class RetentionRunController implements RetentionRunApi {

    private final RetentionRunService retentionRunService;

    public RetentionRunController(RetentionRunService retentionRunService) {
        this.retentionRunService = retentionRunService;
    }

    @Override
    public ResponseEntity<PagingInfo<RetentionRun>> listRetentionRuns(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<RetentionRun> list =
                PagingUtil.pagingInfo(retentionRunService.listRetentionRuns(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
