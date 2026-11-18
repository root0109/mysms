// Owned API contract of master.
package com.mysms.master.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.master.api.model.Currency;
import com.mysms.master.api.model.CurrencyUpdate;
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
public interface CurrencyApi {

    String PATH_CREATE_CURRENCY = "/v1/master/currencies";
    /**
     * POST /v1/master/currencies : Create Currency
     * Create a Currency. Supports validateOnly. Master data is global and has no idempotency store; a retried create is detected by the unique natural key and answered with 409.
     *
     * @param currency  (required)
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
        value = CurrencyApi.PATH_CREATE_CURRENCY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Currency> createCurrency(
         @Valid @RequestBody Currency currency,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_CURRENCY = "/v1/master/currencies/{currencyId}";
    /**
     * DELETE /v1/master/currencies/{currencyId} : Delete Currency
     * Soft delete (sets deleted_at).
     *
     * @param currencyId Currency id. (required)
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
        value = CurrencyApi.PATH_DELETE_CURRENCY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCurrency(
         @PathVariable("currencyId") UUID currencyId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_CURRENCY = "/v1/master/currencies/{currencyId}";
    /**
     * GET /v1/master/currencies/{currencyId} : Get Currency
     * Get one Currency by id. The ETag header carries the row version. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param currencyId Currency id. (required)
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
        value = CurrencyApi.PATH_GET_CURRENCY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Currency> getCurrency(
         @PathVariable("currencyId") UUID currencyId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CURRENCIES = "/v1/master/currencies";
    /**
     * GET /v1/master/currencies : List Currencies
     * List Currencies visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
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
        value = CurrencyApi.PATH_LIST_CURRENCIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Currency>> listCurrencies(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_CURRENCY = "/v1/master/currencies/{currencyId}";
    /**
     * PATCH /v1/master/currencies/{currencyId} : Update Currency
     * Partially update a Currency (updateMask). Requires If-Match with the current ETag.
     *
     * @param currencyId Currency id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param currencyUpdate  (required)
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
        value = CurrencyApi.PATH_UPDATE_CURRENCY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Currency> updateCurrency(
         @PathVariable("currencyId") UUID currencyId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CurrencyUpdate currencyUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
