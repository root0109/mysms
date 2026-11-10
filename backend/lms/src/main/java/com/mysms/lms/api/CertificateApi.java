// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.Certificate;
import com.mysms.lms.api.model.CertificateVerification;
import com.mysms.lms.api.model.DownloadLink;
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
public interface CertificateApi {

    String PATH_DOWNLOAD_CERTIFICATE = "/v1/lms/certificates/{certificateId}:download";
    /**
     * GET /v1/lms/certificates/{certificateId}:download : PDF download
     * PDF download.
     *
     * @param certificateId  (required)
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
        value = CertificateApi.PATH_DOWNLOAD_CERTIFICATE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<DownloadLink> downloadCertificate(
         @PathVariable("certificateId") UUID certificateId
    );


    String PATH_GET_CERTIFICATE = "/v1/lms/certificates/{certificateId}";
    /**
     * GET /v1/lms/certificates/{certificateId} : Get Certificate
     * Get one Certificate by id. The ETag header carries the row version.
     *
     * @param certificateId Certificate id. (required)
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
        value = CertificateApi.PATH_GET_CERTIFICATE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Certificate> getCertificate(
         @PathVariable("certificateId") UUID certificateId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CERTIFICATES = "/v1/lms/certificates";
    /**
     * GET /v1/lms/certificates : List Certificates
     * List Certificates visible to the caller, page by page, with filtering and sorting.
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
        value = CertificateApi.PATH_LIST_CERTIFICATES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Certificate>> listCertificates(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_VERIFY_CERTIFICATE = "/v1/lms/certificates/{certificateId}:verify";
    /**
     * GET /v1/lms/certificates/{certificateId}:verify : Public verification of a certificate
     * Public verification of a certificate. Public.
     *
     * @param certificateId  (required)
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
        value = CertificateApi.PATH_VERIFY_CERTIFICATE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CertificateVerification> verifyCertificate(
         @PathVariable("certificateId") UUID certificateId
    );

}
