package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.PurchaseRequestApi;
import com.mysms.library.api.model.AcceptPurchaseRequestRequest;
import com.mysms.library.api.model.PurchaseRequest;
import com.mysms.library.api.model.PurchaseRequestCourse;
import com.mysms.library.api.model.PurchaseRequestCourseUpdate;
import com.mysms.library.api.model.PurchaseRequestSubject;
import com.mysms.library.api.model.PurchaseRequestSubjectUpdate;
import com.mysms.library.api.model.PurchaseRequestUpdate;
import com.mysms.library.api.model.RejectPurchaseRequestRequest;
import com.mysms.library.api.model.SubmitPurchaseRequestRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.PurchaseRequestService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PurchaseRequest operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class PurchaseRequestController implements PurchaseRequestApi {

    private final PurchaseRequestService purchaseRequestService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @Override
    public ResponseEntity<PurchaseRequest> acceptPurchaseRequest(UUID purchaseRequestId,
            AcceptPurchaseRequestRequest acceptPurchaseRequestRequest, UUID idempotencyKey) {
        PurchaseRequest changed =
                purchaseRequestService.acceptPurchaseRequest(purchaseRequestId, acceptPurchaseRequestRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<PurchaseRequest> createPurchaseRequest(PurchaseRequest purchaseRequest,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PurchaseRequest created = purchaseRequestService.createPurchaseRequest(purchaseRequest, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/purchase-requests/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PurchaseRequestCourse> createPurchaseRequestCourse(UUID purchaseRequestId,
            PurchaseRequestCourse purchaseRequestCourse, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PurchaseRequestCourse created =
                purchaseRequestService.createPurchaseRequestCourse(purchaseRequestId, purchaseRequestCourse,
                        dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/library/purchase-requests/" + purchaseRequestId + "/courses/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PurchaseRequestSubject> createPurchaseRequestSubject(UUID purchaseRequestId,
            PurchaseRequestSubject purchaseRequestSubject, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PurchaseRequestSubject created =
                purchaseRequestService.createPurchaseRequestSubject(purchaseRequestId, purchaseRequestSubject,
                        dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/library/purchase-requests/" + purchaseRequestId + "/subjects/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deletePurchaseRequest(UUID purchaseRequestId, String ifMatch) {
        purchaseRequestService.deletePurchaseRequest(purchaseRequestId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePurchaseRequestCourse(UUID purchaseRequestId,
            UUID purchaseRequestCourseId, String ifMatch) {
        purchaseRequestService.deletePurchaseRequestCourse(purchaseRequestId, purchaseRequestCourseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePurchaseRequestSubject(UUID purchaseRequestId,
            UUID purchaseRequestSubjectId, String ifMatch) {
        purchaseRequestService.deletePurchaseRequestSubject(purchaseRequestId, purchaseRequestSubjectId,
                ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PurchaseRequest> getPurchaseRequest(UUID purchaseRequestId, String fields) {
        PurchaseRequest found = purchaseRequestService.getPurchaseRequest(purchaseRequestId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PurchaseRequestCourse> getPurchaseRequestCourse(UUID purchaseRequestId,
            UUID purchaseRequestCourseId, String fields) {
        PurchaseRequestCourse found =
                purchaseRequestService.getPurchaseRequestCourse(purchaseRequestId, purchaseRequestCourseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PurchaseRequestSubject> getPurchaseRequestSubject(UUID purchaseRequestId,
            UUID purchaseRequestSubjectId, String fields) {
        PurchaseRequestSubject found =
                purchaseRequestService.getPurchaseRequestSubject(purchaseRequestId, purchaseRequestSubjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PurchaseRequestCourse>> listPurchaseRequestCourses(UUID purchaseRequestId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PurchaseRequestCourse> list =
                PagingUtil.pagingInfo(purchaseRequestService.listPurchaseRequestCourses(purchaseRequestId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PurchaseRequestSubject>> listPurchaseRequestSubjects(UUID purchaseRequestId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PurchaseRequestSubject> list =
                PagingUtil.pagingInfo(purchaseRequestService.listPurchaseRequestSubjects(purchaseRequestId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PurchaseRequest>> listPurchaseRequests(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PurchaseRequest> list =
                PagingUtil.pagingInfo(purchaseRequestService.listPurchaseRequests(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PurchaseRequest> rejectPurchaseRequest(UUID purchaseRequestId,
            RejectPurchaseRequestRequest rejectPurchaseRequestRequest, UUID idempotencyKey) {
        PurchaseRequest changed =
                purchaseRequestService.rejectPurchaseRequest(purchaseRequestId, rejectPurchaseRequestRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<PurchaseRequest> submitPurchaseRequest(UUID purchaseRequestId,
            SubmitPurchaseRequestRequest submitPurchaseRequestRequest, UUID idempotencyKey) {
        PurchaseRequest changed =
                purchaseRequestService.submitPurchaseRequest(purchaseRequestId, submitPurchaseRequestRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<PurchaseRequest> updatePurchaseRequest(UUID purchaseRequestId, String ifMatch,
            PurchaseRequestUpdate purchaseRequestUpdate, String updateMask, Boolean validateOnly) {
        PurchaseRequest updated =
                purchaseRequestService.updatePurchaseRequest(purchaseRequestId, ifMatch, purchaseRequestUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PurchaseRequestCourse> updatePurchaseRequestCourse(UUID purchaseRequestId,
            UUID purchaseRequestCourseId, String ifMatch, PurchaseRequestCourseUpdate purchaseRequestCourseUpdate,
            String updateMask, Boolean validateOnly) {
        PurchaseRequestCourse updated =
                purchaseRequestService.updatePurchaseRequestCourse(purchaseRequestId, purchaseRequestCourseId,
                        ifMatch, purchaseRequestCourseUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PurchaseRequestSubject> updatePurchaseRequestSubject(UUID purchaseRequestId,
            UUID purchaseRequestSubjectId, String ifMatch, PurchaseRequestSubjectUpdate purchaseRequestSubjectUpdate,
            String updateMask, Boolean validateOnly) {
        PurchaseRequestSubject updated =
                purchaseRequestService.updatePurchaseRequestSubject(purchaseRequestId, purchaseRequestSubjectId,
                        ifMatch, purchaseRequestSubjectUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
