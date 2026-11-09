package com.mysms.core.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class ProblemResponseWriterTest {

    @Test
    void writesProblemJsonFromFilters() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ProblemResponseWriter(JsonMapper.builder().build())
                .write(response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "The access token has expired.");
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"TOKEN_EXPIRED\"", "\"status\":401", "\"title\":\"Unauthorized\"");
    }

    @Test
    void fieldErrorsCarryOptionalCodes() {
        var body = Problems.body(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", "",
                List.of(new FieldError("name", "must not be blank", null)));
        assertThat(body).doesNotContainKey("detail").doesNotContainKey("traceId");
        assertThat(body.get("errors").toString()).isEqualTo("[{field=name, message=must not be blank}]");
    }
}
