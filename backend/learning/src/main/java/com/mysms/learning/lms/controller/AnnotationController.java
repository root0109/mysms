package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.AnnotationApi;
import com.mysms.learning.lms.api.model.Annotation;
import com.mysms.learning.lms.api.model.AnnotationShare;
import com.mysms.learning.lms.api.model.AnnotationShareUpdate;
import com.mysms.learning.lms.api.model.AnnotationUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.AnnotationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Annotation operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class AnnotationController implements AnnotationApi {

    private final AnnotationService annotationService;

    public AnnotationController(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    @Override
    public ResponseEntity<Annotation> createAnnotation(Annotation annotation, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Annotation created = annotationService.createAnnotation(annotation, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/annotations/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<AnnotationShare> createAnnotationShare(UUID annotationId,
            AnnotationShare annotationShare, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AnnotationShare created = annotationService.createAnnotationShare(annotationId, annotationShare, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/annotations/" + annotationId + "/shares/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAnnotation(UUID annotationId, String ifMatch) {
        annotationService.deleteAnnotation(annotationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAnnotationShare(UUID annotationId, UUID annotationShareId,
            String ifMatch) {
        annotationService.deleteAnnotationShare(annotationId, annotationShareId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Annotation> getAnnotation(UUID annotationId, String fields) {
        Annotation found = annotationService.getAnnotation(annotationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AnnotationShare> getAnnotationShare(UUID annotationId, UUID annotationShareId,
            String fields) {
        AnnotationShare found = annotationService.getAnnotationShare(annotationId, annotationShareId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AnnotationShare>> listAnnotationShares(UUID annotationId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AnnotationShare> list =
                PagingUtil.pagingInfo(annotationService.listAnnotationShares(annotationId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Annotation>> listAnnotations(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Annotation> list =
                PagingUtil.pagingInfo(annotationService.listAnnotations(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Annotation> updateAnnotation(UUID annotationId, String ifMatch,
            AnnotationUpdate annotationUpdate, Boolean validateOnly) {
        Annotation updated = annotationService.updateAnnotation(annotationId, ifMatch, annotationUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<AnnotationShare> updateAnnotationShare(UUID annotationId,
            UUID annotationShareId, String ifMatch, AnnotationShareUpdate annotationShareUpdate, Boolean validateOnly) {
        AnnotationShare updated =
                annotationService.updateAnnotationShare(annotationId, annotationShareId, ifMatch,
                        annotationShareUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
