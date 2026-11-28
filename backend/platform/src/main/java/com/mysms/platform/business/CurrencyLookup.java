package com.mysms.platform.business;

import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnavailableException;
import com.mysms.platform.master.api.model.Currency;
import com.mysms.platform.master.business.CurrencyService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The currency a plan is priced in, read in process from the sibling master area (whose reads are public, so no
 * permission is asked for). A plan without a currency is priced in the platform account's own currency
 * ({@code mysms.platform.razorpay.currency}, else MYSMS_PLATFORM_RAZORPAY_CURRENCY, INR by default). Never called
 * inside a database transaction.
 */
@Component
public class CurrencyLookup {

    /** A currency as the gateway needs it: the ISO code and how many minor units it has. */
    public record Money(String code, int exponent) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(CurrencyLookup.class);

    private final CurrencyService master;
    private final Money platformCurrency;

    public CurrencyLookup(CurrencyService master,
            @Value("${mysms.platform.razorpay.currency}") String platformCurrencyCode) {
        this.master = master;
        this.platformCurrency = new Money(platformCurrencyCode,
                java.util.Currency.getInstance(platformCurrencyCode).getDefaultFractionDigits());
    }

    /** The plan's currency, or the platform's when the plan names none; a currency master no longer has is 503. */
    public Money of(UUID currencyId) {
        if (currencyId == null) {
            return platformCurrency;
        }
        Currency currency;
        try {
            currency = master.getCurrency(currencyId);
        } catch (ResourceNotFoundException e) {
            LOG.warn("master has no currency {} for a plan", currencyId);
            throw new UnavailableException("The master service could not answer.");
        }
        if (currency.getCode() == null || currency.getCode().isEmpty()) {
            LOG.warn("master gave currency {} without its code", currencyId);
            throw new UnavailableException("The master service could not answer.");
        }
        Integer minorUnits = currency.getMinorUnits();
        return new Money(currency.getCode(), minorUnits == null ? 0 : minorUnits);
    }
}
