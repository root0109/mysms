package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.ProcessingActivityApi;
import com.mysms.privacy.api.model.ProcessingActivity;
import com.mysms.privacy.api.model.ProcessingRegisterExport;
import com.mysms.privacy.business.ProcessingActivityService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessingActivityController implements ProcessingActivityApi {

    private final ProcessingActivityService processingActivityService;

    public ProcessingActivityController(ProcessingActivityService processingActivityService) {
        this.processingActivityService = processingActivityService;
    }

    @Override
    public ResponseEntity<PagingInfo<ProcessingActivity>> listProcessingActivities(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ProcessingActivity> list = PagingUtil.pagingInfo(
                processingActivityService.listProcessingActivities(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ProcessingRegisterExport> getProcessingRegisterExport() {
        return ResponseEntity.ok(processingActivityService.getProcessingRegisterExport());
    }
}
