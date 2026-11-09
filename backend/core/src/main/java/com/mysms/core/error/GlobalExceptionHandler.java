package com.mysms.core.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The single place REST error bodies are built (JAVA-CODING-RULES §9, §11): typed exceptions keep their status and
 * code, framework exceptions map onto the contract's codes, anything unexpected becomes a 500 without details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String IF_MATCH = "If-Match";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException exception) {
        return Problems.response(exception);
    }

    /** Bean validation of a request body: 422 with one entry per field (contract: VALIDATION_FAILED). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException exception) {
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage(), e.getCode()))
                .toList();
        return Problems.response(new ValidationFailedException(errors));
    }

    /** Constraint violations on parameters (pageSize bounds, lengths...) are malformed requests: 400. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> invalidParameters(HandlerMethodValidationException exception) {
        List<FieldError> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error -> new FieldError(
                        result.getMethodParameter().getParameterName(), error.getDefaultMessage(), "INVALID_ARGUMENT")))
                .toList();
        return Problems.response(HttpStatus.BAD_REQUEST, InvalidArgumentException.INVALID_ARGUMENT,
                "The request has invalid parameters.", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> constraintViolation(ConstraintViolationException exception) {
        List<FieldError> errors = exception.getConstraintViolations().stream()
                .map(v -> new FieldError(v.getPropertyPath().toString(), v.getMessage(), "INVALID_ARGUMENT"))
                .toList();
        return Problems.response(HttpStatus.BAD_REQUEST, InvalidArgumentException.INVALID_ARGUMENT,
                "The request has invalid parameters.", errors);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> missingHeader(MissingRequestHeaderException exception) {
        if (IF_MATCH.equalsIgnoreCase(exception.getHeaderName())) {
            return Problems.response(new PreconditionRequiredException(
                    "If-Match with the ETag of the last read is required."));
        }
        return badRequest("Missing header " + exception.getHeaderName() + ".");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> missingParameter(MissingServletRequestParameterException exception) {
        return badRequest("Missing parameter " + exception.getParameterName() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return badRequest("Invalid value for " + exception.getName() + ".");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException exception) {
        return badRequest("The request body is not valid JSON for this operation.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> method(HttpRequestMethodNotSupportedException exception) {
        return Problems.response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", null, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> mediaType(HttpMediaTypeNotSupportedException exception) {
        return Problems.response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", null, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> notAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return Problems.response(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", null, List.of());
    }

    /**
     * No handler for the path. The edge filter has already answered 404 for anything that is not a contract
     * operation and marked contract operations on the request, so a marked request here is an operation this release
     * does not serve yet: 501, never a misleading 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noResource(NoResourceFoundException exception, HttpServletRequest request) {
        if (request.getAttribute(Problems.OPERATION_ATTRIBUTE) != null) {
            return Problems.response(new NotImplementedException());
        }
        return Problems.response(HttpStatus.NOT_FOUND, "NOT_FOUND", null, List.of());
    }

    /** A unique index caught a duplicate the service did not pre-check (race): still a clean 409. */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> duplicate(DuplicateKeyException exception) {
        return Problems.response(new DuplicateResourceException("A resource with the same unique key already exists."));
    }

    /**
     * Any other integrity violation (NOT NULL, CHECK, foreign key) that a service did not pre-validate. The database's
     * message contains row values, so only the SQLState and constraint name are logged — never the message.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException exception) {
        String sqlState = null;
        String constraint = null;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException psql) {
                sqlState = psql.getSQLState();
                org.postgresql.util.ServerErrorMessage server = psql.getServerErrorMessage();
                constraint = server == null ? null : server.getConstraint();
                break;
            }
        }
        LOG.warn("integrity violation sqlState={} constraint={}", sqlState, constraint);
        if ("23503".equals(sqlState)) {
            return Problems.response(new InvalidStateException("A referenced or referencing row prevents this change."));
        }
        return Problems.response(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED",
                "The request violates a data rule.", List.of());
    }

    /** Anything unexpected: logged with the trace id, answered without internals. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception exception) {
        LOG.error("unhandled exception", exception);
        return Problems.response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", null, List.of());
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String detail) {
        return Problems.response(HttpStatus.BAD_REQUEST, InvalidArgumentException.INVALID_ARGUMENT, detail, List.of());
    }
}
