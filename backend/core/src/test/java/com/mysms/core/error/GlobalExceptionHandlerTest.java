package com.mysms.core.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    record Body(@NotBlank String name) {
    }

    @RestController
    @Validated
    static class Probe {
        @GetMapping("/typed/{kind}")
        String typed(@org.springframework.web.bind.annotation.PathVariable String kind) {
            throw switch (kind) {
                case "missing" -> new ResourceNotFoundException("master.country", "c1");
                case "duplicate" -> new DuplicateResourceException("iso2 is taken");
                case "state" -> new InvalidStateException("not open");
                case "filter" -> new InvalidArgumentException(InvalidArgumentException.INVALID_FILTER, "bad");
                case "dup-key" -> new DuplicateKeyException("uq");
                case "not-null" -> new org.springframework.dao.DataIntegrityViolationException("Failing row contains (secret)",
                        new org.postgresql.util.PSQLException("null value", org.postgresql.util.PSQLState.NOT_NULL_VIOLATION));
                case "fk" -> new org.springframework.dao.DataIntegrityViolationException("Key (x)=(secret)",
                        new org.postgresql.util.PSQLException("fk", org.postgresql.util.PSQLState.FOREIGN_KEY_VIOLATION));
                case "violation" -> new ConstraintViolationException("bad", Set.of());
                case "unauthenticated" -> new UnauthenticatedException(UnauthenticatedException.INVALID_CREDENTIALS, "no");
                case "denied" -> new PermissionDeniedException("not yours");
                case "locked" -> new ConflictException("SETTING_LOCKED", "locked");
                case "unbuilt" -> new NotImplementedException();
                case "stub" -> io.grpc.Status.NOT_FOUND.withDescription("student 42 of tenant 7").asRuntimeException();
                default -> new IllegalStateException("boom with secrets");
            };
        }

        /** What the dispatcher throws when no controller serves a path; the edge filter marks contract operations. */
        @GetMapping("/unserved/{marked}")
        String unserved(@org.springframework.web.bind.annotation.PathVariable boolean marked,
                        jakarta.servlet.http.HttpServletRequest request)
                throws org.springframework.web.servlet.resource.NoResourceFoundException {
            if (marked) {
                request.setAttribute(Problems.OPERATION_ATTRIBUTE, "iam.ListRoles");
            }
            throw new org.springframework.web.servlet.resource.NoResourceFoundException(
                    org.springframework.http.HttpMethod.GET, "/unserved", "unserved");
        }

        @PostMapping("/body")
        String body(@Valid @RequestBody Body body) {
            return body.name();
        }

        @GetMapping("/params")
        String params(@RequestParam @Max(5) int size) {
            return "ok" + size;
        }

        @PatchMapping("/guarded")
        String guarded(@RequestHeader("If-Match") String ifMatch, @RequestHeader("X-Other") String other) {
            return ifMatch + other;
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator =
                new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new Probe())
                .setControllerAdvice(new GlobalExceptionHandler(new ConstraintMapper(MessageUtil.ofCatalogue())))
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        CallContextHolder.clear();
    }

    @Test
    void anUnservedContractOperationIs501AndAnUnknownPathIs404() throws Exception {
        mvc.perform(get("/unserved/true")).andExpect(status().isNotImplemented())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
        mvc.perform(get("/unserved/false")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void anotherServicesAnswerThatEscapedAStubCallIs503WithoutItsMessage() throws Exception {
        mvc.perform(get("/typed/stub")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value(com.mysms.core.grpc.GrpcStatuses.UNANSWERED));
    }

    @Test
    void typedExceptionsKeepStatusAndCode() throws Exception {
        mvc.perform(get("/typed/missing")).andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("master.country c1 not found"))
                .andExpect(jsonPath("$.traceId").value("4bf92f3577b34da6a3ce929d0e0e4736"));
        mvc.perform(get("/typed/duplicate")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE"));
        mvc.perform(get("/typed/state")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
        mvc.perform(get("/typed/filter")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"));
        mvc.perform(get("/typed/dup-key")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE"));
        mvc.perform(get("/typed/violation")).andExpect(status().isBadRequest());
        mvc.perform(get("/typed/unauthenticated")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(get("/typed/denied")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
        mvc.perform(get("/typed/locked")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SETTING_LOCKED"));
        mvc.perform(get("/typed/unbuilt")).andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
        mvc.perform(get("/typed/not-null")).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("The request violates a data rule."));
        mvc.perform(get("/typed/fk")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void unexpectedFailuresRevealNothing() throws Exception {
        mvc.perform(get("/typed/other")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL"))
                .andExpect(jsonPath("$.detail").doesNotExist());
    }

    @Test
    void frameworkFailuresMapToContractCodes() throws Exception {
        mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        mvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(post("/body").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/body")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/params")).andExpect(status().isBadRequest());
        mvc.perform(get("/params").param("size", "x")).andExpect(status().isBadRequest());
        mvc.perform(patch("/guarded").header("X-Other", "1")).andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
        mvc.perform(patch("/guarded").header("If-Match", "\"1\"")).andExpect(status().isBadRequest());
        mvc.perform(get("/body").accept(MediaType.IMAGE_PNG)).andExpect(status().is4xxClientError());
    }
}
