package com.mysms.core.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/** Writes a problem+json body from a servlet filter, where no MVC exception handling is available. */
public final class ProblemResponseWriter {

    private final JsonMapper json;

    public ProblemResponseWriter(JsonMapper json) {
        this.json = json;
    }

    public void write(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(Problems.PROBLEM_JSON.toString());
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), Problems.body(status, code, detail, List.of()));
    }
}
