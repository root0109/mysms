// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.Conversation;
import com.mysms.comms.api.model.ConversationMessage;
import com.mysms.comms.api.model.ConversationMessageUpdate;
import com.mysms.comms.api.model.ConversationParticipant;
import com.mysms.comms.api.model.ConversationParticipantUpdate;
import com.mysms.comms.api.model.ConversationUpdate;
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
public interface ConversationApi {

    String PATH_CREATE_CONVERSATION = "/v1/comms/conversations";
    /**
     * POST /v1/comms/conversations : Create Conversation
     * Create a Conversation. Supports Idempotency-Key and validateOnly.
     *
     * @param conversation  (required)
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
        value = ConversationApi.PATH_CREATE_CONVERSATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Conversation> createConversation(
         @Valid @RequestBody Conversation conversation,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CONVERSATION_MESSAGE = "/v1/comms/conversations/{conversationId}/messages";
    /**
     * POST /v1/comms/conversations/{conversationId}/messages : Create ConversationMessage
     * Create a ConversationMessage. Supports Idempotency-Key and validateOnly.
     *
     * @param conversationId Conversation id. (required)
     * @param conversationMessage  (required)
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
        value = ConversationApi.PATH_CREATE_CONVERSATION_MESSAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ConversationMessage> createConversationMessage(
         @PathVariable("conversationId") UUID conversationId,
         @Valid @RequestBody ConversationMessage conversationMessage,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CONVERSATION_PARTICIPANT = "/v1/comms/conversations/{conversationId}/participants";
    /**
     * POST /v1/comms/conversations/{conversationId}/participants : Create ConversationParticipant
     * Create a ConversationParticipant. Supports Idempotency-Key and validateOnly.
     *
     * @param conversationId Conversation id. (required)
     * @param conversationParticipant  (required)
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
        value = ConversationApi.PATH_CREATE_CONVERSATION_PARTICIPANT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ConversationParticipant> createConversationParticipant(
         @PathVariable("conversationId") UUID conversationId,
         @Valid @RequestBody ConversationParticipant conversationParticipant,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_CONVERSATION = "/v1/comms/conversations/{conversationId}";
    /**
     * DELETE /v1/comms/conversations/{conversationId} : Delete Conversation
     * Soft delete (sets deleted_at).
     *
     * @param conversationId Conversation id. (required)
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
        value = ConversationApi.PATH_DELETE_CONVERSATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteConversation(
         @PathVariable("conversationId") UUID conversationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CONVERSATION_MESSAGE = "/v1/comms/conversations/{conversationId}/messages/{conversationMessageId}";
    /**
     * DELETE /v1/comms/conversations/{conversationId}/messages/{conversationMessageId} : Delete ConversationMessage
     * Soft delete (sets deleted_at).
     *
     * @param conversationId Conversation id. (required)
     * @param conversationMessageId ConversationMessage id. (required)
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
        value = ConversationApi.PATH_DELETE_CONVERSATION_MESSAGE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteConversationMessage(
         @PathVariable("conversationId") UUID conversationId,
         @PathVariable("conversationMessageId") UUID conversationMessageId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CONVERSATION_PARTICIPANT = "/v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}";
    /**
     * DELETE /v1/comms/conversations/{conversationId}/participants/{conversationParticipantId} : Delete ConversationParticipant
     * Soft delete (sets deleted_at).
     *
     * @param conversationId Conversation id. (required)
     * @param conversationParticipantId ConversationParticipant id. (required)
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
        value = ConversationApi.PATH_DELETE_CONVERSATION_PARTICIPANT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteConversationParticipant(
         @PathVariable("conversationId") UUID conversationId,
         @PathVariable("conversationParticipantId") UUID conversationParticipantId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_CONVERSATION = "/v1/comms/conversations/{conversationId}";
    /**
     * GET /v1/comms/conversations/{conversationId} : Get Conversation
     * Get one Conversation by id. The ETag header carries the row version.
     *
     * @param conversationId Conversation id. (required)
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
        value = ConversationApi.PATH_GET_CONVERSATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Conversation> getConversation(
         @PathVariable("conversationId") UUID conversationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CONVERSATION_MESSAGE = "/v1/comms/conversations/{conversationId}/messages/{conversationMessageId}";
    /**
     * GET /v1/comms/conversations/{conversationId}/messages/{conversationMessageId} : Get ConversationMessage
     * Get one ConversationMessage by id. The ETag header carries the row version.
     *
     * @param conversationId Conversation id. (required)
     * @param conversationMessageId ConversationMessage id. (required)
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
        value = ConversationApi.PATH_GET_CONVERSATION_MESSAGE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ConversationMessage> getConversationMessage(
         @PathVariable("conversationId") UUID conversationId,
         @PathVariable("conversationMessageId") UUID conversationMessageId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CONVERSATION_PARTICIPANT = "/v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}";
    /**
     * GET /v1/comms/conversations/{conversationId}/participants/{conversationParticipantId} : Get ConversationParticipant
     * Get one ConversationParticipant by id. The ETag header carries the row version.
     *
     * @param conversationId Conversation id. (required)
     * @param conversationParticipantId ConversationParticipant id. (required)
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
        value = ConversationApi.PATH_GET_CONVERSATION_PARTICIPANT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ConversationParticipant> getConversationParticipant(
         @PathVariable("conversationId") UUID conversationId,
         @PathVariable("conversationParticipantId") UUID conversationParticipantId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_HIDE_MESSAGE_FOR_ME = "/v1/comms/messages/{messageId}:hideForMe";
    /**
     * POST /v1/comms/messages/{messageId}:hideForMe : Hide a message for the caller only
     * Hide a message for the caller only.
     *
     * @param messageId  (required)
     * @param body  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = ConversationApi.PATH_HIDE_MESSAGE_FOR_ME,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> hideMessageForMe(
         @PathVariable("messageId") UUID messageId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_CONVERSATION_MESSAGES = "/v1/comms/conversations/{conversationId}/messages";
    /**
     * GET /v1/comms/conversations/{conversationId}/messages : List ConversationMessages
     * List ConversationMessages visible to the caller, page by page, with filtering and sorting.
     *
     * @param conversationId Conversation id. (required)
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
        value = ConversationApi.PATH_LIST_CONVERSATION_MESSAGES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ConversationMessage>> listConversationMessages(
         @PathVariable("conversationId") UUID conversationId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CONVERSATION_PARTICIPANTS = "/v1/comms/conversations/{conversationId}/participants";
    /**
     * GET /v1/comms/conversations/{conversationId}/participants : List ConversationParticipants
     * List ConversationParticipants visible to the caller, page by page, with filtering and sorting.
     *
     * @param conversationId Conversation id. (required)
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
        value = ConversationApi.PATH_LIST_CONVERSATION_PARTICIPANTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ConversationParticipant>> listConversationParticipants(
         @PathVariable("conversationId") UUID conversationId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CONVERSATIONS = "/v1/comms/conversations";
    /**
     * GET /v1/comms/conversations : List Conversations
     * List Conversations visible to the caller, page by page, with filtering and sorting.
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
        value = ConversationApi.PATH_LIST_CONVERSATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Conversation>> listConversations(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_CONVERSATION_READ = "/v1/comms/conversations/{conversationId}:markRead";
    /**
     * POST /v1/comms/conversations/{conversationId}:markRead : Mark all messages read for the caller
     * Mark all messages read for the caller.
     *
     * @param conversationId  (required)
     * @param body  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = ConversationApi.PATH_MARK_CONVERSATION_READ,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> markConversationRead(
         @PathVariable("conversationId") UUID conversationId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_CONVERSATION = "/v1/comms/conversations/{conversationId}";
    /**
     * PATCH /v1/comms/conversations/{conversationId} : Update Conversation
     * Partially update a Conversation (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param conversationId Conversation id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param conversationUpdate  (required)
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
        value = ConversationApi.PATH_UPDATE_CONVERSATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Conversation> updateConversation(
         @PathVariable("conversationId") UUID conversationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ConversationUpdate conversationUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CONVERSATION_MESSAGE = "/v1/comms/conversations/{conversationId}/messages/{conversationMessageId}";
    /**
     * PATCH /v1/comms/conversations/{conversationId}/messages/{conversationMessageId} : Update ConversationMessage
     * Partially update a ConversationMessage (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param conversationId Conversation id. (required)
     * @param conversationMessageId ConversationMessage id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param conversationMessageUpdate  (required)
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
        value = ConversationApi.PATH_UPDATE_CONVERSATION_MESSAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ConversationMessage> updateConversationMessage(
         @PathVariable("conversationId") UUID conversationId,
         @PathVariable("conversationMessageId") UUID conversationMessageId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ConversationMessageUpdate conversationMessageUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CONVERSATION_PARTICIPANT = "/v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}";
    /**
     * PATCH /v1/comms/conversations/{conversationId}/participants/{conversationParticipantId} : Update ConversationParticipant
     * Partially update a ConversationParticipant (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param conversationId Conversation id. (required)
     * @param conversationParticipantId ConversationParticipant id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param conversationParticipantUpdate  (required)
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
        value = ConversationApi.PATH_UPDATE_CONVERSATION_PARTICIPANT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ConversationParticipant> updateConversationParticipant(
         @PathVariable("conversationId") UUID conversationId,
         @PathVariable("conversationParticipantId") UUID conversationParticipantId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ConversationParticipantUpdate conversationParticipantUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
