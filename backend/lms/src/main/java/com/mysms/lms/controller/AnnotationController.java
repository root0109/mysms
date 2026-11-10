package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.AnnotationApi;
import com.mysms.lms.api.model.Annotation;
import com.mysms.lms.api.model.AnnotationShare;
import com.mysms.lms.api.model.AnnotationShareUpdate;
import com.mysms.lms.api.model.AnnotationUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.AnnotationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Annotation operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
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
            AnnotationUpdate annotationUpdate, String updateMask, Boolean validateOnly) {
        Annotation updated = annotationService.updateAnnotation(annotationId, ifMatch, annotationUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<AnnotationShare> updateAnnotationShare(UUID annotationId,
            UUID annotationShareId, String ifMatch, AnnotationShareUpdate annotationShareUpdate, String updateMask,
            Boolean validateOnly) {
        AnnotationShare updated =
                annotationService.updateAnnotationShare(annotationId, annotationShareId, ifMatch,
                        annotationShareUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
