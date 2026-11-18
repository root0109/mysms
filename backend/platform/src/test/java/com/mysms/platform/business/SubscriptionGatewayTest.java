package com.mysms.platform.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.ApiException;
import com.mysms.core.payment.GatewayCredentials;
import com.mysms.core.payment.GatewayOrder;
import com.mysms.core.payment.RazorpayClient;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** The platform's merchant account: the order it opens carries the tenant and the subscription; the two proofs. */
class SubscriptionGatewayTest {

    @Test
    void anOrderCarriesTheSubscriptionAsReceiptAndTheTenantInItsNotes() throws IOException {
        AtomicReference<String> sent = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/orders", exchange -> {
            sent.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"id\":\"order_9\",\"amount\":120000,\"currency\":\"INR\",\"status\":\"created\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            SubscriptionGateway gateway = new SubscriptionGateway(new RazorpayClient(URI.create("http://127.0.0.1:"
                    + server.getAddress().getPort() + "/v1"), Duration.ofSeconds(5)),
                    new GatewayCredentials("rzp_test_k", "secret"), null);
            UUID tenant = UUID.randomUUID();
            UUID subscription = UUID.randomUUID();
            GatewayOrder order = gateway.openOrder(120000, "INR", tenant, subscription);
            assertThat(order.id()).isEqualTo("order_9");
            assertThat(sent.get()).contains("\"amount\":120000").contains("\"currency\":\"INR\"")
                    .contains("\"receipt\":\"" + subscription.toString().substring(0, 36) + "\"")
                    .contains("\"tenantId\":\"" + tenant + "\"").contains("\"subscriptionId\":\"" + subscription + "\"");
            assertThat(gateway.publicKey()).isEqualTo("rzp_test_k");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void theProofsAreTheGatewaysSignatures() throws Exception {
        SubscriptionGateway gateway = new SubscriptionGateway(new RazorpayClient(RazorpayClient.DEFAULT_BASE_URI,
                Duration.ofSeconds(1)), new GatewayCredentials("k", "key-secret"), "hook-secret");
        assertThat(gateway.checkoutProves("order_1", "pay_1", hmac("key-secret", "order_1|pay_1"))).isTrue();
        assertThat(gateway.checkoutProves("order_1", "pay_2", hmac("key-secret", "order_1|pay_1"))).isFalse();
        assertThat(gateway.webhookProves("{}".getBytes(StandardCharsets.UTF_8), hmac("hook-secret", "{}"))).isTrue();
        assertThat(gateway.webhookProves("{ }".getBytes(StandardCharsets.UTF_8), hmac("hook-secret", "{}"))).isFalse();
        SubscriptionGateway unconfigured = new SubscriptionGateway(new RazorpayClient(RazorpayClient.DEFAULT_BASE_URI,
                Duration.ofSeconds(1)), null, null);
        assertThatThrownBy(unconfigured::requireCheckout).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("NOT_CONFIGURED"));
        assertThatThrownBy(() -> unconfigured.webhookProves(new byte[0], "x")).isInstanceOf(ApiException.class);
    }

    @Test
    void aPaymentCountsOnlyWhenTheGatewayReportsItCapturedForTheOrder() throws IOException {
        AtomicReference<String> answer = new AtomicReference<>();
        AtomicReference<Integer> status = new AtomicReference<>(200);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/payments/", exchange -> {
            byte[] body = answer.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            SubscriptionGateway gateway = new SubscriptionGateway(new RazorpayClient(URI.create("http://127.0.0.1:"
                    + server.getAddress().getPort() + "/v1"), Duration.ofSeconds(5)),
                    new GatewayCredentials("rzp_test_k", "secret"), null);
            answer.set(payment("captured", "order_1"));
            assertThat(gateway.capturedPayment("order_1", "pay_1")).hasValueSatisfying(paid -> {
                assertThat(paid.id()).isEqualTo("pay_1");
                assertThat(paid.method()).isEqualTo("upi");
            });
            answer.set(payment("authorized", "order_1"));
            assertThat(gateway.capturedPayment("order_1", "pay_1")).as("authorised, not settled").isEmpty();
            answer.set(payment("captured", "order_2"));
            assertThat(gateway.capturedPayment("order_1", "pay_1")).as("another order's money").isEmpty();
            status.set(502);
            answer.set("{}");
            assertThat(gateway.capturedPayment("order_1", "pay_1")).as("no answer: the webhook decides").isEmpty();
        } finally {
            server.stop(0);
        }
    }

    private static String payment(String status, String order) {
        return "{\"id\":\"pay_1\",\"order_id\":\"" + order + "\",\"amount\":120000,\"currency\":\"INR\","
                + "\"status\":\"" + status + "\",\"method\":\"upi\"}";
    }

    private static String hmac(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
