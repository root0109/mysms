// Owned API contract of iam (JAVA-CODING-RULES §7); first materialized from api/openapi/iam.yaml on 2026-11-10.
package com.mysms.iam.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.iam.api.model.SettingDefinition;
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
public interface SettingDefinitionApi {

    String PATH_GET_SETTING_DEFINITION = "/v1/iam/setting-definitions/{settingDefinitionId}";
    /**
     * GET /v1/iam/setting-definitions/{settingDefinitionId} : Get SettingDefinition
     * Get one SettingDefinition by id. The ETag header carries the row version. A definition users may not change is 404 unless the caller administers settings at one of its allowed scopes.
     *
     * @param settingDefinitionId SettingDefinition id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = SettingDefinitionApi.PATH_GET_SETTING_DEFINITION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SettingDefinition> getSettingDefinition(
         @PathVariable("settingDefinitionId") UUID settingDefinitionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SETTING_DEFINITIONS = "/v1/iam/setting-definitions";
    /**
     * GET /v1/iam/setting-definitions : List SettingDefinitions
     * List SettingDefinitions visible to the caller, page by page, with filtering and sorting. Definitions users may not change are listed only to callers who administer settings at one of their allowed scopes.
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
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = SettingDefinitionApi.PATH_LIST_SETTING_DEFINITIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SettingDefinition>> listSettingDefinitions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );

}
