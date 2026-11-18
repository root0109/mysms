package com.mysms.core.error;

import org.springframework.http.HttpStatus;

/** The resource does not exist or is not visible to the caller: invisible rows are 404, never 403. */
public final class ResourceNotFoundException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " " + id + " not found");
    }
}
