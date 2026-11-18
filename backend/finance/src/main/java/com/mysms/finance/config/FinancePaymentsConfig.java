package com.mysms.finance.config;

import com.mysms.core.payment.RazorpayClient;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The payment gateway finance talks to. Its address comes from {@code mysms.finance.razorpay-url}, then
 * {@code MYSMS_RAZORPAY_URL}, then Razorpay's production endpoint; the merchant credentials are each tenant's own
 * gateway account. (The other modules finance reads are called through its {@code integration} gRPC clients.)
 */
@Configuration(proxyBeanMethods = false)
public class FinancePaymentsConfig {

    /** Bean name of finance's gateway client (the monolith may hold another module's client of the same type). */
    public static final String GATEWAY = "financeRazorpayClient";

    @Bean(name = GATEWAY)
    public RazorpayClient financeRazorpayClient(
            @Value("${mysms.finance.razorpay-url}") String baseUrl,
            @Value("${mysms.finance.razorpay-timeout}") Duration timeout) {
        URI base = baseUrl == null || baseUrl.isBlank() ? RazorpayClient.DEFAULT_BASE_URI : URI.create(baseUrl);
        return new RazorpayClient(base, timeout);
    }
}
