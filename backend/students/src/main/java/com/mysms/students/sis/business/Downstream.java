package com.mysms.students.sis.business;

import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * How one call through another module's gRPC client becomes this module's answer — shared by sis's gateways so that
 * all of them fail the same way, and so that the other module's status and message never reach the client.
 *
 * <p>A <em>required</em> read is one the operation cannot be served without: 404 is "that row does not exist for
 * this caller" (empty), 403 is this operation's 403 naming the permission the caller lacks, anything else is 503.
 * An <em>optional</em> read fills a part of an answer the contract does not require: whatever goes wrong, the part
 * is left out.
 */
final class Downstream {

    private static final Logger LOG = LoggerFactory.getLogger(Downstream.class);

    private Downstream() {
    }

    /**
     * @param service    the module called, for the log and the 503 sentence
     * @param permission the permission the other module asks for, named in the 403
     * @param purpose    what the caller was doing, as the 403 sentence starts ("Creating a portal account")
     */
    static <T> Optional<T> required(String service, String permission, String purpose, Supplier<T> request) {
        try {
            return Optional.of(request.get());
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            if (e.httpStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new PermissionDeniedException(purpose + " also needs " + permission + ".");
            }
            LOG.warn("the {} service answered {} ({})", service, e.httpStatus(), e.statusCode());
            throw new UnavailableException("The " + service + " service could not answer.");
        }
    }

    /** The answer, or empty when the other module refuses, does not know the row, fails or cannot be reached. */
    static <T> Optional<T> optional(String service, Supplier<T> request) {
        try {
            return Optional.of(request.get());
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() != HttpStatus.FORBIDDEN.value() && e.httpStatus() != HttpStatus.NOT_FOUND.value()) {
                LOG.warn("the {} service answered {} ({}); the optional part is left out", service, e.httpStatus(),
                        e.statusCode());
            }
            return Optional.empty();
        }
    }
}
