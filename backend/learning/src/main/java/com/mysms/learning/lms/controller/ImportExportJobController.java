package com.mysms.learning.lms.controller;

import com.mysms.learning.lms.business.CoursePackageService;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ImportExportJobApi;
import com.mysms.learning.lms.api.model.CancelImportExportJobRequest;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.api.model.ImportExportJob;
import com.mysms.learning.lms.api.model.RestoreCohortArchiveRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ImportExportJobService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ImportExportJob operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ImportExportJobController implements ImportExportJobApi {

    private final ImportExportJobService importExportJobService;
    private final CoursePackageService coursePackageService;

    public ImportExportJobController(ImportExportJobService importExportJobService,
            CoursePackageService coursePackageService) {
        this.coursePackageService = coursePackageService;
        this.importExportJobService = importExportJobService;
    }

    @Override
    public ResponseEntity<ImportExportJob> cancelImportExportJob(UUID importExportJobId,
            CancelImportExportJobRequest cancelImportExportJobRequest, UUID idempotencyKey) {
        ImportExportJob changed =
                importExportJobService.cancelImportExportJob(importExportJobId, cancelImportExportJobRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ImportExportJob> createImportExportJob(ImportExportJob importExportJob,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ImportExportJob created = importExportJobService.createImportExportJob(importExportJob, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/import-export-jobs/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ImportExportJob> getImportExportJob(UUID importExportJobId, String fields) {
        ImportExportJob found = importExportJobService.getImportExportJob(importExportJobId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<DownloadLink> getJobPackage(UUID importExportJobId) {
        return ResponseEntity.ok(coursePackageService.getJobPackage(importExportJobId));
    }

    @Override
    public ResponseEntity<PagingInfo<ImportExportJob>> listImportExportJobs(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ImportExportJob> list =
                PagingUtil.pagingInfo(importExportJobService.listImportExportJobs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ImportExportJob> restoreCohortArchive(UUID importExportJobId,
            RestoreCohortArchiveRequest restoreCohortArchiveRequest, UUID idempotencyKey) {
        ImportExportJob restored = coursePackageService.restoreCohortArchive(importExportJobId,
                restoreCohortArchiveRequest);
        return ResponseEntity.ok().eTag(restored.getEtag()).body(restored);
    }
}
