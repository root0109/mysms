package com.mysms.platform.config;

import com.mysms.core.payment.GatewayCredentials;
import com.mysms.core.payment.RazorpayClient;
import com.mysms.platform.business.SubscriptionGateway;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * How tenants pay for their subscription: the platform's own merchant account at the gateway (property first, then
 * environment: {@code mysms.platform.razorpay.key-id} / {@code MYSMS_PLATFORM_RAZORPAY_KEY_ID}, {@code key-secret} /
 * {@code MYSMS_PLATFORM_RAZORPAY_KEY_SECRET}, {@code webhook-secret} / {@code MYSMS_PLATFORM_RAZORPAY_WEBHOOK_SECRET},
 * optionally {@code url} / {@code MYSMS_PLATFORM_RAZORPAY_URL}), the account's currency for plans that name none
 * ({@code currency} / {@code MYSMS_PLATFORM_RAZORPAY_CURRENCY}, INR by default; read by
 * {@link com.mysms.platform.business.CurrencyLookup}). Whatever is missing answers 503 where it is needed and nothing
 * else.
 */
@Configuration(proxyBeanMethods = false)
public class PlatformPaymentsConfig {

    @Bean
    public SubscriptionGateway subscriptionGateway(
            @Value("${mysms.platform.razorpay.url}") String url,
            @Value("${mysms.platform.razorpay.key-id}") String keyId,
            @Value("${mysms.platform.razorpay.key-secret}") String keySecret,
            @Value("${mysms.platform.razorpay.webhook-secret}")
            String webhookSecret) {
        RazorpayClient client = new RazorpayClient(url.isBlank() ? RazorpayClient.DEFAULT_BASE_URI : URI.create(url),
                Duration.ofSeconds(10));
        GatewayCredentials credentials = keyId.isBlank() || keySecret.isBlank()
                ? null : new GatewayCredentials(keyId, keySecret);
        return new SubscriptionGateway(client, credentials, webhookSecret.isBlank() ? null : webhookSecret);
    }
}
