package com.mysms.platform.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.api.FileObjectApi;
import com.mysms.platform.api.model.CopyFileRequest;
import com.mysms.platform.api.model.CreateUploadUrlRequest;
import com.mysms.platform.api.model.DownloadLink;
import com.mysms.platform.api.model.FileObject;
import com.mysms.platform.api.model.UploadTarget;
import com.mysms.platform.business.FileObjectService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the file operations. Signed URLs are never cached by intermediaries. */
@RestController
public class FileObjectController implements FileObjectApi {

    private final FileObjectService fileObjectService;

    public FileObjectController(FileObjectService fileObjectService) {
        this.fileObjectService = fileObjectService;
    }

    @Override
    public ResponseEntity<FileObject> completeUpload(UUID fileObjectId, Object body, UUID idempotencyKey) {
        FileObject completed = fileObjectService.completeUpload(fileObjectId);
        return ResponseEntity.ok().eTag(completed.getEtag()).body(completed);
    }

    @Override
    public ResponseEntity<FileObject> copyFile(UUID fileObjectId, CopyFileRequest copyFileRequest,
            UUID idempotencyKey) {
        FileObject copy = fileObjectService.copyFile(fileObjectId, copyFileRequest);
        return ResponseEntity.ok().eTag(copy.getEtag()).body(copy);
    }

    @Override
    public ResponseEntity<UploadTarget> createUploadUrl(CreateUploadUrlRequest createUploadUrlRequest,
            UUID idempotencyKey) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(fileObjectService.createUploadUrl(createUploadUrlRequest));
    }

    @Override
    public ResponseEntity<Void> deleteFileObject(UUID fileObjectId, String ifMatch) {
        fileObjectService.deleteFileObject(fileObjectId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DownloadLink> getDownloadUrl(UUID fileObjectId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(fileObjectService.getDownloadUrl(fileObjectId));
    }

    @Override
    public ResponseEntity<FileObject> getFileObject(UUID fileObjectId, String fields) {
        FileObject found = fileObjectService.getFileObject(fileObjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<FileObject>> listFileObjects(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<FileObject> list =
                PagingUtil.pagingInfo(fileObjectService.listFileObjects(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
