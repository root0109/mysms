package com.mysms.finance.business;

import com.mysms.core.error.UnavailableException;
import com.mysms.finance.integration.MasterGrpcClient;
import com.mysms.master.v1.Currency;
import com.mysms.master.v1.GetCurrencyRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The ISO code and minor units of a currency, read from master (an Adapter over its gRPC client). A gateway order
 * needs both — the code names the currency, the minor units turn an amount into paise — so checkout without master is
 * 503; a statement only shows the code when master answers.
 */
@Component
public class CurrencyLookup {

    private static final String SERVICE = "master";

    /** A currency as the gateway needs it. */
    public record Money(String code, int minorUnits) {
    }

    private final MasterGrpcClient master;

    public CurrencyLookup(MasterGrpcClient master) {
        this.master = master;
    }

    /** The currency, or 503 when master cannot answer or does not know it. */
    public Money require(UUID currencyId) {
        Currency currency = Downstream.ownerCheck(SERVICE, () -> master.getCurrency(GetCurrencyRequest.newBuilder()
                        .setId(currencyId.toString()).build()).getCurrency())
                .orElseThrow(() -> new UnavailableException("The master service does not know the currency."));
        if (currency.getCode().isEmpty()) {
            throw new UnavailableException("The master service did not give the currency's code.");
        }
        return new Money(currency.getCode(), currency.getMinorUnits());
    }

    /** The ISO code when master answers; empty otherwise (a display nicety, never a guess). */
    public Optional<String> code(UUID currencyId) {
        if (currencyId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(require(currencyId).code());
        } catch (UnavailableException unavailable) {
            return Optional.empty();
        }
    }
}
