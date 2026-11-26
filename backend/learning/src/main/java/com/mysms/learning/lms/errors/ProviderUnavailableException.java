package com.mysms.learning.lms.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * An external provider an operation needs (plagiarism checks, LTI tools, e-book conversion) is not configured, or did
 * not answer: 503 with a code naming which. Nothing was changed; the detail never carries an address or a key.
 */
public final class ProviderUnavailableException extends ApiException {

    private static final long serialVersionUID = 1L;

    /** No account or tool is set up for the provider. */
    public static final String PLAGIARISM_NOT_CONFIGURED = "PLAGIARISM_NOT_CONFIGURED";
    public static final String PLAGIARISM_PROVIDER_FAILED = "PLAGIARISM_PROVIDER_FAILED";
    public static final String CONVERSION_NOT_CONFIGURED = "CONVERSION_NOT_CONFIGURED";
    public static final String CONVERSION_PROVIDER_FAILED = "CONVERSION_PROVIDER_FAILED";
    public static final String LTI_NOT_CONFIGURED = "LTI_NOT_CONFIGURED";

    public ProviderUnavailableException(String code, String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, code, detail);
    }
}
