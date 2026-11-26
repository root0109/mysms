// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.learning.lms.api.model.PublicLmsSettings;
import com.mysms.learning.lms.api.model.SendContactMessageRequest;
import com.mysms.learning.lms.api.model.Settings;
import com.mysms.learning.lms.api.model.SettingsPrimaryCountry;
import com.mysms.learning.lms.api.model.SettingsPrimaryCountryUpdate;
import com.mysms.learning.lms.api.model.SettingsSidebarItem;
import com.mysms.learning.lms.api.model.SettingsSidebarItemUpdate;
import com.mysms.learning.lms.api.model.SettingsUpdate;
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
public interface SettingsApi {

    String PATH_CREATE_SETTINGS_PRIMARY_COUNTRY = "/v1/lms/settings/{settingsId}/primary-countries";
    /**
     * POST /v1/lms/settings/{settingsId}/primary-countries : Create SettingsPrimaryCountry
     * Create a SettingsPrimaryCountry. Supports Idempotency-Key and validateOnly.
     *
     * @param settingsId Settings id. (required)
     * @param settingsPrimaryCountry  (required)
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
        value = SettingsApi.PATH_CREATE_SETTINGS_PRIMARY_COUNTRY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SettingsPrimaryCountry> createSettingsPrimaryCountry(
         @PathVariable("settingsId") UUID settingsId,
         @Valid @RequestBody SettingsPrimaryCountry settingsPrimaryCountry,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_SETTINGS_SIDEBAR_ITEM = "/v1/lms/settings/{settingsId}/sidebar-items";
    /**
     * POST /v1/lms/settings/{settingsId}/sidebar-items : Create SettingsSidebarItem
     * Create a SettingsSidebarItem. Supports Idempotency-Key and validateOnly.
     *
     * @param settingsId Settings id. (required)
     * @param settingsSidebarItem  (required)
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
        value = SettingsApi.PATH_CREATE_SETTINGS_SIDEBAR_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SettingsSidebarItem> createSettingsSidebarItem(
         @PathVariable("settingsId") UUID settingsId,
         @Valid @RequestBody SettingsSidebarItem settingsSidebarItem,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_SETTINGS_PRIMARY_COUNTRY = "/v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}";
    /**
     * DELETE /v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId} : Delete SettingsPrimaryCountry
     * Soft delete (sets deleted_at).
     *
     * @param settingsId Settings id. (required)
     * @param settingsPrimaryCountryId SettingsPrimaryCountry id. (required)
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
        value = SettingsApi.PATH_DELETE_SETTINGS_PRIMARY_COUNTRY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSettingsPrimaryCountry(
         @PathVariable("settingsId") UUID settingsId,
         @PathVariable("settingsPrimaryCountryId") UUID settingsPrimaryCountryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_SETTINGS_SIDEBAR_ITEM = "/v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}";
    /**
     * DELETE /v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId} : Delete SettingsSidebarItem
     * Soft delete (sets deleted_at).
     *
     * @param settingsId Settings id. (required)
     * @param settingsSidebarItemId SettingsSidebarItem id. (required)
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
        value = SettingsApi.PATH_DELETE_SETTINGS_SIDEBAR_ITEM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSettingsSidebarItem(
         @PathVariable("settingsId") UUID settingsId,
         @PathVariable("settingsSidebarItemId") UUID settingsSidebarItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_PUBLIC_SETTINGS = "/v1/lms/public-settings";
    /**
     * GET /v1/lms/public-settings : Settings needed by the public catalogue (navigation, feature switches, contact, SEO)
     * Settings needed by the public catalogue (navigation, feature switches, contact, SEO). Public.
     *
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
        method = RequestMethod.GET,
        value = SettingsApi.PATH_GET_PUBLIC_SETTINGS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PublicLmsSettings> getPublicSettings(
        
    );


    String PATH_GET_SETTINGS = "/v1/lms/settings";
    /**
     * GET /v1/lms/settings : Get Settings
     * Get one Settings by id. The ETag header carries the row version.
     *
     * @return OK (status code 200)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = SettingsApi.PATH_GET_SETTINGS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Settings> getSettings(
        
    );


    String PATH_GET_SETTINGS_PRIMARY_COUNTRY = "/v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}";
    /**
     * GET /v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId} : Get SettingsPrimaryCountry
     * Get one SettingsPrimaryCountry by id. The ETag header carries the row version.
     *
     * @param settingsId Settings id. (required)
     * @param settingsPrimaryCountryId SettingsPrimaryCountry id. (required)
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
        value = SettingsApi.PATH_GET_SETTINGS_PRIMARY_COUNTRY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SettingsPrimaryCountry> getSettingsPrimaryCountry(
         @PathVariable("settingsId") UUID settingsId,
         @PathVariable("settingsPrimaryCountryId") UUID settingsPrimaryCountryId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_SETTINGS_SIDEBAR_ITEM = "/v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}";
    /**
     * GET /v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId} : Get SettingsSidebarItem
     * Get one SettingsSidebarItem by id. The ETag header carries the row version.
     *
     * @param settingsId Settings id. (required)
     * @param settingsSidebarItemId SettingsSidebarItem id. (required)
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
        value = SettingsApi.PATH_GET_SETTINGS_SIDEBAR_ITEM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SettingsSidebarItem> getSettingsSidebarItem(
         @PathVariable("settingsId") UUID settingsId,
         @PathVariable("settingsSidebarItemId") UUID settingsSidebarItemId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SETTINGS_PRIMARY_COUNTRIES = "/v1/lms/settings/{settingsId}/primary-countries";
    /**
     * GET /v1/lms/settings/{settingsId}/primary-countries : List SettingsPrimaryCountries
     * List SettingsPrimaryCountries visible to the caller, page by page, with filtering and sorting.
     *
     * @param settingsId Settings id. (required)
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
        value = SettingsApi.PATH_LIST_SETTINGS_PRIMARY_COUNTRIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SettingsPrimaryCountry>> listSettingsPrimaryCountries(
         @PathVariable("settingsId") UUID settingsId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SETTINGS_SIDEBAR_ITEMS = "/v1/lms/settings/{settingsId}/sidebar-items";
    /**
     * GET /v1/lms/settings/{settingsId}/sidebar-items : List SettingsSidebarItems
     * List SettingsSidebarItems visible to the caller, page by page, with filtering and sorting.
     *
     * @param settingsId Settings id. (required)
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
        value = SettingsApi.PATH_LIST_SETTINGS_SIDEBAR_ITEMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SettingsSidebarItem>> listSettingsSidebarItems(
         @PathVariable("settingsId") UUID settingsId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_SEND_CONTACT_MESSAGE = "/v1/lms/contact";
    /**
     * POST /v1/lms/contact : Send a message to the LMS contact address configured in settings
     * Send a message to the LMS contact address configured in settings.
     *
     * @param sendContactMessageRequest  (required)
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
        value = SettingsApi.PATH_SEND_CONTACT_MESSAGE,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> sendContactMessage(
         @Valid @RequestBody SendContactMessageRequest sendContactMessageRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_SETTINGS = "/v1/lms/settings";
    /**
     * PATCH /v1/lms/settings : Update Settings
     * Partially update a Settings (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param settingsUpdate  (required)
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
        value = SettingsApi.PATH_UPDATE_SETTINGS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Settings> updateSettings(
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SettingsUpdate settingsUpdate
    );


    String PATH_UPDATE_SETTINGS_PRIMARY_COUNTRY = "/v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}";
    /**
     * PATCH /v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId} : Update SettingsPrimaryCountry
     * Partially update a SettingsPrimaryCountry (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param settingsId Settings id. (required)
     * @param settingsPrimaryCountryId SettingsPrimaryCountry id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param settingsPrimaryCountryUpdate  (required)
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
        value = SettingsApi.PATH_UPDATE_SETTINGS_PRIMARY_COUNTRY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SettingsPrimaryCountry> updateSettingsPrimaryCountry(
         @PathVariable("settingsId") UUID settingsId,
         @PathVariable("settingsPrimaryCountryId") UUID settingsPrimaryCountryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SettingsPrimaryCountryUpdate settingsPrimaryCountryUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_SETTINGS_SIDEBAR_ITEM = "/v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}";
    /**
     * PATCH /v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId} : Update SettingsSidebarItem
     * Partially update a SettingsSidebarItem (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param settingsId Settings id. (required)
     * @param settingsSidebarItemId SettingsSidebarItem id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param settingsSidebarItemUpdate  (required)
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
        value = SettingsApi.PATH_UPDATE_SETTINGS_SIDEBAR_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SettingsSidebarItem> updateSettingsSidebarItem(
         @PathVariable("settingsId") UUID settingsId,
         @PathVariable("settingsSidebarItemId") UUID settingsSidebarItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SettingsSidebarItemUpdate settingsSidebarItemUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
