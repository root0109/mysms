package com.mysms.platform.business;

import com.mysms.core.error.ApiException;
import com.mysms.core.payment.GatewayCredentials;
import com.mysms.core.payment.GatewayOrder;
import com.mysms.core.payment.GatewayPayment;
import com.mysms.core.payment.RazorpayClient;
import com.mysms.core.payment.RazorpaySignatures;
import com.mysms.platform.errors.NotConfiguredException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The platform's own merchant account at the payment gateway, through which tenants pay for their subscription (core's
 * {@link RazorpayClient} speaks the protocol; this class holds the account and the two proofs of payment). Without key
 * credentials checkout and its confirmation answer 503 NOT_CONFIGURED; without a webhook secret the webhook does.
 * Opening an order is a remote call: never inside a database transaction.
 */
public final class SubscriptionGateway {

    /** The name stored as tenant_subscription.gateway_name. */
    public static final String NAME = "razorpay";
    /** The order notes that bring the tenant and the subscription back with the gateway's (signed) webhook. */
    public static final String NOTE_TENANT = "tenantId";
    public static final String NOTE_SUBSCRIPTION = "subscriptionId";
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionGateway.class);

    private final RazorpayClient client;
    private final GatewayCredentials credentials;
    private final String webhookSecret;

    /**
     * @param credentials   the platform account's key pair, or null when checkout is not configured
     * @param webhookSecret the account's webhook secret, or null when the webhook is not configured
     */
    public SubscriptionGateway(RazorpayClient client, GatewayCredentials credentials, String webhookSecret) {
        this.client = client;
        this.credentials = credentials;
        this.webhookSecret = webhookSecret;
    }

    /** The publishable key the client's checkout opens the order with. */
    public String publicKey() {
        return checkoutAccount().keyId();
    }

    /** Fails with 503 when checkout is not configured (before anything is written). */
    public void requireCheckout() {
        checkoutAccount();
    }

    /**
     * Opens an order for a subscription: the subscription id is the receipt, and the tenant and subscription ride in
     * the notes, which the gateway returns inside the signed webhook body.
     */
    public GatewayOrder openOrder(long amountMinor, String currency, UUID tenant, UUID subscription) {
        Map<String, String> notes = new LinkedHashMap<>();
        notes.put(NOTE_TENANT, tenant.toString());
        notes.put(NOTE_SUBSCRIPTION, subscription.toString());
        return client.createOrder(checkoutAccount(), amountMinor, currency, subscription.toString(), notes);
    }

    /** Whether the checkout signature proves the order was paid with this payment. */
    public boolean checkoutProves(String orderId, String paymentId, String signature) {
        return RazorpaySignatures.checkoutValid(orderId, paymentId, signature, checkoutAccount().keySecret());
    }

    /** The payment, when the gateway reports it captured for this order; empty otherwise or when it cannot answer. */
    public Optional<GatewayPayment> capturedPayment(String orderId, String paymentId) {
        GatewayPayment payment;
        try {
            payment = client.fetchPayment(checkoutAccount(), paymentId);
        } catch (ApiException unanswered) {
            LOG.info("the gateway could not confirm a subscription payment now ({}); the webhook settles it",
                    unanswered.code());
            return Optional.empty();
        }
        return payment.captured() && orderId.equals(payment.orderId()) ? Optional.of(payment) : Optional.empty();
    }

    /** Whether the webhook signature covers exactly these body bytes. */
    public boolean webhookProves(byte[] body, String signature) {
        if (webhookSecret == null) {
            throw new NotConfiguredException("Subscription payment callbacks are not configured for this deployment.");
        }
        return RazorpaySignatures.webhookValid(body, signature, webhookSecret);
    }

    private GatewayCredentials checkoutAccount() {
        if (credentials == null) {
            throw new NotConfiguredException("Subscription payments are not configured for this deployment.");
        }
        return credentials;
    }
}
