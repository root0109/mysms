package com.mysms.core.error;

import java.util.List;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

/**
 * What a database integrity violation a service did not pre-check means to the client, for both transports: a unique
 * key → 409 DUPLICATE, a foreign key → 409 INVALID_STATE_TRANSITION, anything else (NOT NULL, CHECK) → 422. The
 * database's message contains row values, so only the SQLState and the constraint name are logged — never the message.
 */
public final class IntegrityViolations {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrityViolations.class);

    private IntegrityViolations() {
    }

    public static Problem problem(DataIntegrityViolationException exception) {
        if (exception instanceof DuplicateKeyException) {
            return Problem.of(new DuplicateResourceException("A resource with the same unique key already exists."));
        }
        String sqlState = null;
        String constraint = null;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException psql) {
                sqlState = psql.getSQLState();
                ServerErrorMessage server = psql.getServerErrorMessage();
                constraint = server == null ? null : server.getConstraint();
                break;
            }
        }
        LOG.warn("integrity violation sqlState={} constraint={}", sqlState, constraint);
        if ("23503".equals(sqlState)) {
            return Problem.of(new InvalidStateException("A referenced or referencing row prevents this change."));
        }
        return new Problem(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", "The request violates a data rule.",
                List.of());
    }
}
