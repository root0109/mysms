// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.ProcessingActivity;
import com.mysms.privacy.api.model.ProcessingRegisterExport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
public interface ProcessingActivityApi {

    String PATH_GET_PROCESSING_REGISTER_EXPORT = "/v1/privacy/processing-activities:export";
    /**
     * GET /v1/privacy/processing-activities:export : Export the processing register
     * The whole processing register (RoPA) as CSV, one row per activity: code, module, purpose, legal basis, exemption clause, minors, audiences, data categories, recipients, cross-border and retention policy. The CSV travels as text inside the JSON answer, so REST and gRPC answer alike.
     *
     * @return OK (status code 200)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = ProcessingActivityApi.PATH_GET_PROCESSING_REGISTER_EXPORT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ProcessingRegisterExport> getProcessingRegisterExport();


    String PATH_LIST_PROCESSING_ACTIVITIES = "/v1/privacy/processing-activities";
    /**
     * GET /v1/privacy/processing-activities : List Processing Activities
     * The processing register (RoPA-lite, design §5.1 "GetProcessingRegister"): every activity of the platform
     * catalogue with its purpose, legal basis and data categories, page by page, with filtering and sorting.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;legalBasis &#x3D; &quot;consent&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = ProcessingActivityApi.PATH_LIST_PROCESSING_ACTIVITIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ProcessingActivity>> listProcessingActivities(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );
}
