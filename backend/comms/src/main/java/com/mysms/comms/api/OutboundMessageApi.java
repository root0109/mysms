// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.CancelOutboundMessageRequest;
import org.springframework.lang.Nullable;
import com.mysms.comms.api.model.OutboundMessage;
import com.mysms.comms.api.model.RetryOutboundMessageRequest;
import com.mysms.comms.api.model.SendMessageRequest;
import com.mysms.comms.api.model.SendResult;
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
public interface OutboundMessageApi {

    String PATH_CANCEL_OUTBOUND_MESSAGE = "/v1/comms/outbound-messages/{outboundMessageId}:cancel";
    /**
     * POST /v1/comms/outbound-messages/{outboundMessageId}:cancel : Cancel a queued message
     * Cancel a queued message.
     *
     * @param outboundMessageId  (required)
     * @param cancelOutboundMessageRequest  (required)
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
        value = OutboundMessageApi.PATH_CANCEL_OUTBOUND_MESSAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<OutboundMessage> cancelOutboundMessage(
         @PathVariable("outboundMessageId") UUID outboundMessageId,
         @Valid @RequestBody CancelOutboundMessageRequest cancelOutboundMessageRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_OUTBOUND_MESSAGE = "/v1/comms/outbound-messages/{outboundMessageId}";
    /**
     * GET /v1/comms/outbound-messages/{outboundMessageId} : Get OutboundMessage
     * Get one OutboundMessage by id. The ETag header carries the row version.
     *
     * @param outboundMessageId OutboundMessage id. (required)
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
        value = OutboundMessageApi.PATH_GET_OUTBOUND_MESSAGE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<OutboundMessage> getOutboundMessage(
         @PathVariable("outboundMessageId") UUID outboundMessageId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_OUTBOUND_MESSAGES = "/v1/comms/outbound-messages";
    /**
     * GET /v1/comms/outbound-messages : List OutboundMessages
     * List OutboundMessages visible to the caller, page by page, with filtering and sorting.
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
        value = OutboundMessageApi.PATH_LIST_OUTBOUND_MESSAGES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<OutboundMessage>> listOutboundMessages(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RETRY_OUTBOUND_MESSAGE = "/v1/comms/outbound-messages/{outboundMessageId}:retry";
    /**
     * POST /v1/comms/outbound-messages/{outboundMessageId}:retry : Retry a failed message
     * Retry a failed message.
     *
     * @param outboundMessageId  (required)
     * @param retryOutboundMessageRequest  (required)
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
        value = OutboundMessageApi.PATH_RETRY_OUTBOUND_MESSAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<OutboundMessage> retryOutboundMessage(
         @PathVariable("outboundMessageId") UUID outboundMessageId,
         @Valid @RequestBody RetryOutboundMessageRequest retryOutboundMessageRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SEND_MESSAGE = "/v1/comms/outbound-messages:send";
    /**
     * POST /v1/comms/outbound-messages:send : Queue SMS / e-mail / push to people, classes, groups or guardians
     * Queue SMS / e-mail / push to people, classes, groups or guardians.
     *
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param sendMessageRequest  (required)
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
        value = OutboundMessageApi.PATH_SEND_MESSAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SendResult> sendMessage(
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody SendMessageRequest sendMessageRequest
    );

}
