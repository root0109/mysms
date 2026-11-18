// Owned API contract of sis.
package com.mysms.sis.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.model.ApplyPromotionsRequest;
import com.mysms.sis.api.model.ApprovePromotionRequest;
import org.springframework.lang.Nullable;
import com.mysms.sis.api.model.Operation;
import com.mysms.sis.api.model.Promotion;
import com.mysms.sis.api.model.PromotionList;
import com.mysms.sis.api.model.PromotionUpdate;
import com.mysms.sis.api.model.ProposePromotionsRequest;
import com.mysms.sis.api.model.RejectPromotionRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Validated
public interface PromotionApi {

    String PATH_APPLY_PROMOTIONS = "/v1/sis/promotions:apply";
    /**
     * POST /v1/sis/promotions:apply : Create next-year enrolments for approved promotions and close the current ones
     * Create next-year enrolments for approved promotions and close the current ones.
     *
     * @param applyPromotionsRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Accepted; poll the operation. (status code 202)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = PromotionApi.PATH_APPLY_PROMOTIONS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> applyPromotions(
         @Valid @RequestBody ApplyPromotionsRequest applyPromotionsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_APPROVE_PROMOTION = "/v1/sis/promotions/{promotionId}:approve";
    /**
     * POST /v1/sis/promotions/{promotionId}:approve : Approve
     * Approve.
     *
     * @param promotionId  (required)
     * @param approvePromotionRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = PromotionApi.PATH_APPROVE_PROMOTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Promotion> approvePromotion(
         @PathVariable("promotionId") UUID promotionId,
         @Valid @RequestBody ApprovePromotionRequest approvePromotionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_PROMOTION = "/v1/sis/promotions";
    /**
     * POST /v1/sis/promotions : Create Promotion
     * Create a Promotion. Supports Idempotency-Key and validateOnly.
     *
     * @param promotion  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = PromotionApi.PATH_CREATE_PROMOTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Promotion> createPromotion(
         @Valid @RequestBody Promotion promotion,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_PROMOTION = "/v1/sis/promotions/{promotionId}";
    /**
     * DELETE /v1/sis/promotions/{promotionId} : Delete Promotion
     * Soft delete (sets deleted_at).
     *
     * @param promotionId Promotion id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = PromotionApi.PATH_DELETE_PROMOTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePromotion(
         @PathVariable("promotionId") UUID promotionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_PROMOTION = "/v1/sis/promotions/{promotionId}";
    /**
     * GET /v1/sis/promotions/{promotionId} : Get Promotion
     * Get one Promotion by id. The ETag header carries the row version.
     *
     * @param promotionId Promotion id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = PromotionApi.PATH_GET_PROMOTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Promotion> getPromotion(
         @PathVariable("promotionId") UUID promotionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PROMOTIONS = "/v1/sis/promotions";
    /**
     * GET /v1/sis/promotions : List Promotions
     * List Promotions visible to the caller, page by page, with filtering and sorting.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = PromotionApi.PATH_LIST_PROMOTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Promotion>> listPromotions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PROPOSE_PROMOTIONS = "/v1/sis/promotions:propose";
    /**
     * POST /v1/sis/promotions:propose : Create proposed promotions for a class
     * Create proposed promotions for a class.
     *
     * @param proposePromotionsRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = PromotionApi.PATH_PROPOSE_PROMOTIONS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PromotionList> proposePromotions(
         @Valid @RequestBody ProposePromotionsRequest proposePromotionsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REJECT_PROMOTION = "/v1/sis/promotions/{promotionId}:reject";
    /**
     * POST /v1/sis/promotions/{promotionId}:reject : Reject (student detained)
     * Reject (student detained).
     *
     * @param promotionId  (required)
     * @param rejectPromotionRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = PromotionApi.PATH_REJECT_PROMOTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Promotion> rejectPromotion(
         @PathVariable("promotionId") UUID promotionId,
         @Valid @RequestBody RejectPromotionRequest rejectPromotionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_PROMOTION = "/v1/sis/promotions/{promotionId}";
    /**
     * PATCH /v1/sis/promotions/{promotionId} : Update Promotion
     * Partially update a Promotion (updateMask). Requires If-Match with the current ETag.
     *
     * @param promotionId Promotion id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param promotionUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = PromotionApi.PATH_UPDATE_PROMOTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Promotion> updatePromotion(
         @PathVariable("promotionId") UUID promotionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PromotionUpdate promotionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
