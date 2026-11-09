package com.mysms.core.error;

import com.mysms.core.context.CallContextHolder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Builds RFC 9457 problem bodies exactly as the contract's `Problem` schema: type, title, status, detail, code,
 * traceId and errors[] (api/README "Errors"). The one place error bodies are shaped, for MVC and for filters alike.
 */
public final class Problems {

    public static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
    /** Request attribute the edge filter sets to the matched contract operation id. */
    public static final String OPERATION_ATTRIBUTE = "mysms.operationId";

    private Problems() {
    }

    public static Map<String, Object> body(HttpStatus status, String code, String detail, List<FieldError> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        if (detail != null && !detail.isBlank()) {
            body.put("detail", detail);
        }
        body.put("code", code);
        CallContextHolder.current().ifPresent(context -> body.put("traceId", context.traceId()));
        if (!errors.isEmpty()) {
            body.put("errors", errors.stream().map(Problems::error).toList());
        }
        return body;
    }

    public static ResponseEntity<Map<String, Object>> response(HttpStatus status, String code, String detail,
                                                               List<FieldError> errors) {
        return ResponseEntity.status(status).contentType(PROBLEM_JSON).body(body(status, code, detail, errors));
    }

    public static ResponseEntity<Map<String, Object>> response(ApiException exception) {
        return response(exception.status(), exception.code(), exception.getMessage(), exception.errors());
    }

    private static Map<String, Object> error(FieldError error) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("field", error.field());
        out.put("message", error.message());
        if (error.code() != null) {
            out.put("code", error.code());
        }
        return out;
    }
}
