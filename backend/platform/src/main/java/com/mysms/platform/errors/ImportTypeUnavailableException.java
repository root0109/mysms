package com.mysms.platform.errors;

import com.mysms.core.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * The contract lists the entity type, but no module imports it yet (a course package is a ZIP the lms module will
 * take, not CSV rows): 422 {@code IMPORT_TYPE_UNAVAILABLE}. Nothing was started.
 */
public final class ImportTypeUnavailableException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ImportTypeUnavailableException(String entityType) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "IMPORT_TYPE_UNAVAILABLE",
                "Importing " + entityType + " is not available yet.");
    }
}
