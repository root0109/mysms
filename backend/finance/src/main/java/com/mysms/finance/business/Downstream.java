package com.mysms.finance.business;

import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * How one call through another module's gRPC client becomes finance's answer (an Adapter shared by the lookups),
 * so that all of them fail the same way and the other module's status and message never reach the client.
 *
 * <p>A <em>required</em> read is one the operation cannot be served without: 404 is "that row does not exist for
 * this caller" (empty), 403 is this operation's 403 naming the permission the caller lacks, anything else is 503.
 * An <em>owner check</em> asks the other module whether the caller owns a record: 403 and 404 both mean "not shown
 * to this caller" (empty), anything else is 503.
 */
final class Downstream {

    private static final Logger LOG = LoggerFactory.getLogger(Downstream.class);

    private Downstream() {
    }

    /**
     * @param service    the module called, for the log and the 503 sentence
     * @param permission the permission the other module asks for, named in the 403
     * @param purpose    what the caller was doing, as the 403 sentence starts ("Generating fee dues")
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
            throw unavailable(service, e);
        }
    }

    /** The answer when the other module shows the record to this caller; empty when it refuses or does not know it. */
    static <T> Optional<T> ownerCheck(String service, Supplier<T> request) {
        try {
            return Optional.of(request.get());
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == HttpStatus.FORBIDDEN.value() || e.httpStatus() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw unavailable(service, e);
        }
    }

    private static UnavailableException unavailable(String service, IntegrationGrpcClientException e) {
        LOG.warn("the {} service answered {} ({})", service, e.httpStatus(), e.statusCode());
        return new UnavailableException("The " + service + " service could not answer.");
    }
}
