package com.mysms.core.error;

/** One field-level problem, reported in problem+json `errors[]` (api/README "Errors"). Never carries personal data. */
public record FieldError(String field, String message, String code) {
}
