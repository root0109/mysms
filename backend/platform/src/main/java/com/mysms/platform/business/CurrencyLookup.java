package com.mysms.platform.business;

import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.master.v1.Currency;
import com.mysms.master.v1.GetCurrencyRequest;
import com.mysms.platform.integration.MasterGrpcClient;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * The currency a plan is priced in, from master (the only module the platform may call), with the caller's own
 * identity. A plan without a currency is priced in the platform account's own currency
 * ({@code mysms.platform.razorpay.currency}, else MYSMS_PLATFORM_RAZORPAY_CURRENCY, INR by default). Never called
 * inside a database transaction.
 */
@Component
public class CurrencyLookup {

    /** A currency as the gateway needs it: the ISO code and how many minor units it has. */
    public record Money(String code, int exponent) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(CurrencyLookup.class);

    private final MasterGrpcClient master;
    private final Money platformCurrency;

    public CurrencyLookup(MasterGrpcClient master,
            @Value("${mysms.platform.razorpay.currency}") String platformCurrencyCode) {
        this.master = master;
        this.platformCurrency = new Money(platformCurrencyCode,
                java.util.Currency.getInstance(platformCurrencyCode).getDefaultFractionDigits());
    }

    /** The plan's currency, or the platform's when the plan names none. */
    public Money of(UUID currencyId) {
        if (currencyId == null) {
            return platformCurrency;
        }
        try {
            Currency currency = master.getCurrency(GetCurrencyRequest.newBuilder().setId(currencyId.toString()).build())
                    .getCurrency();
            if (currency.getCode().isEmpty()) {
                throw new UnavailableException("The master service could not answer.");
            }
            return new Money(currency.getCode(), currency.getMinorUnits());
        } catch (IntegrationGrpcClientException refused) {
            if (refused.httpStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new PermissionDeniedException("Reading the plan's currency is not permitted.");
            }
            LOG.warn("master answered {} ({}) to the currency lookup", refused.httpStatus(), refused.statusCode());
            throw new UnavailableException("The master service could not answer.");
        }
    }
}
