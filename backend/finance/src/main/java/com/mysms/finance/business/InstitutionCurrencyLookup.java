package com.mysms.finance.business;

import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.finance.integration.OrgGrpcClient;
import com.mysms.org.v1.GetInstitutionRequest;
import com.mysms.org.v1.Institution;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The currency an institution bills in, read from org with the caller's token (an Adapter over its gRPC client): an
 * institution the caller cannot see, or one without a currency, is 422 on {@code institutionId}; 403 names the
 * permission org asks for; anything else is 503.
 */
@Component
public class InstitutionCurrencyLookup {

    private static final String SERVICE = "org";

    private final OrgGrpcClient org;

    public InstitutionCurrencyLookup(OrgGrpcClient org) {
        this.org = org;
    }

    public UUID currencyOf(UUID institutionId) {
        Institution institution = Downstream.required(SERVICE, "org.institution.read",
                        "Billing in the institution's currency", () -> org.getInstitution(GetInstitutionRequest
                                .newBuilder().setId(institutionId.toString()).build()).getInstitution())
                .orElseThrow(() -> refused("institution does not exist"));
        if (!institution.hasCurrencyId() || institution.getCurrencyId().isEmpty()) {
            throw refused("the institution has no currency; name currencyId");
        }
        return UUID.fromString(institution.getCurrencyId());
    }

    private static ValidationFailedException refused(String message) {
        return new ValidationFailedException(List.of(new FieldError("institutionId", message, "NOT_FOUND")));
    }
}
