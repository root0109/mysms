// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.ComposeMailRequest;
import com.mysms.comms.api.model.MailboxMessage;
import org.springframework.lang.Nullable;
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
public interface MailboxMessageApi {

    String PATH_COMPOSE_MAIL = "/v1/comms/mailbox-messages:compose";
    /**
     * POST /v1/comms/mailbox-messages:compose : Send internal mail; copies land in recipients&#39; inboxes
     * Send internal mail; copies land in recipients&#39; inboxes.
     *
     * @param composeMailRequest  (required)
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
        value = MailboxMessageApi.PATH_COMPOSE_MAIL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MailboxMessage> composeMail(
         @Valid @RequestBody ComposeMailRequest composeMailRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DELETE_MAILBOX_MESSAGE = "/v1/comms/mailbox-messages/{mailboxMessageId}";
    /**
     * DELETE /v1/comms/mailbox-messages/{mailboxMessageId} : Delete MailboxMessage
     * Soft delete (sets deleted_at).
     *
     * @param mailboxMessageId MailboxMessage id. (required)
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
        value = MailboxMessageApi.PATH_DELETE_MAILBOX_MESSAGE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteMailboxMessage(
         @PathVariable("mailboxMessageId") UUID mailboxMessageId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_MAILBOX_MESSAGE = "/v1/comms/mailbox-messages/{mailboxMessageId}";
    /**
     * GET /v1/comms/mailbox-messages/{mailboxMessageId} : Get MailboxMessage
     * Get one MailboxMessage by id. The ETag header carries the row version.
     *
     * @param mailboxMessageId MailboxMessage id. (required)
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
        value = MailboxMessageApi.PATH_GET_MAILBOX_MESSAGE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MailboxMessage> getMailboxMessage(
         @PathVariable("mailboxMessageId") UUID mailboxMessageId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MAILBOX_MESSAGES = "/v1/comms/mailbox-messages";
    /**
     * GET /v1/comms/mailbox-messages : List MailboxMessages
     * List MailboxMessages visible to the caller, page by page, with filtering and sorting.
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
        value = MailboxMessageApi.PATH_LIST_MAILBOX_MESSAGES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MailboxMessage>> listMailboxMessages(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_MAIL_READ = "/v1/comms/mailbox-messages/{mailboxMessageId}:markRead";
    /**
     * POST /v1/comms/mailbox-messages/{mailboxMessageId}:markRead : Mark read
     * Mark read.
     *
     * @param mailboxMessageId  (required)
     * @param body  (required)
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
        value = MailboxMessageApi.PATH_MARK_MAIL_READ,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MailboxMessage> markMailRead(
         @PathVariable("mailboxMessageId") UUID mailboxMessageId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
