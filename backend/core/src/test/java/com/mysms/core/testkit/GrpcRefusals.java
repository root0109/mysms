package com.mysms.core.testkit;

import com.mysms.core.error.ConstraintMapper;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.MessageUtil;
import com.mysms.core.error.Problem;
import com.mysms.core.grpc.GrpcStatuses;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import io.grpc.Status;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * The {@link IntegrationGrpcClientException} a {@code <Callee>GrpcClient} throws, built through {@link GrpcStatuses}
 * and {@link IntegrationGrpcClientException#of} exactly as a real call builds it, so unit tests check production's
 * mapping.
 */
public final class GrpcRefusals {

    private static final GrpcStatuses STATUSES = new GrpcStatuses(new ConstraintMapper(MessageUtil.ofCatalogue()));

    private GrpcRefusals() {
    }

    /** The callee refused the call as REST would with this status and code (and these refused fields). */
    public static IntegrationGrpcClientException refused(String service, HttpStatus status, String code,
                                                         FieldError... errors) {
        return IntegrationGrpcClientException.of(service, STATUSES.toException(
                new Problem(status, code, "The " + service + " service's own sentence.", List.of(errors))));
    }

    /** The callee could not be reached (no server, a refused connection): UNAVAILABLE without a refusal of its own. */
    public static IntegrationGrpcClientException unreachable(String service) {
        return IntegrationGrpcClientException.of(service,
                Status.UNAVAILABLE.withDescription("io exception").asRuntimeException());
    }
}
