package com.mysms.core.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.UnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The client's wire format against a stand-in gateway on loopback (a unit test: no real gateway is called). */
class RazorpayClientTest {

    private static final GatewayCredentials KEYS = new GatewayCredentials("rzp_test_key", "s3cret");

    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private int status = 200;
    private String answer = "{}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " "
                    + exchange.getRequestHeaders().getFirst("Authorization") + " " + body);
            byte[] out = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private RazorpayClient client() {
        return new RazorpayClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1"),
                Duration.ofSeconds(5));
    }

    @Test
    void anOrderIsOpenedWithTheMerchantsKeysAndTheAmountInPaise() {
        answer = "{\"id\":\"order_1\",\"entity\":\"order\",\"amount\":123450,\"currency\":\"INR\",\"status\":\"created\"}";
        GatewayOrder order = client().createOrder(KEYS, RazorpayClient.minorUnits(new BigDecimal("1234.50"), 2), "INR",
                "INV-2026-000001-a-very-long-receipt-reference", Map.of("invoiceId", "abc"));
        assertThat(order).isEqualTo(new GatewayOrder("order_1", 123450, "INR", "created"));
        String basic = "Basic " + Base64.getEncoder().encodeToString("rzp_test_key:s3cret".getBytes(StandardCharsets.UTF_8));
        assertThat(requests).singleElement().satisfies(r -> assertThat(r)
                .startsWith("POST /v1/orders " + basic)
                .contains("\"amount\":123450", "\"currency\":\"INR\"", "\"invoiceId\":\"abc\"")
                .contains("\"receipt\":\"INV-2026-000001-a-very-long-receipt-refe\""));
    }

    @Test
    void aPaymentAndARefundAreReadInTheGatewaysFieldNames() {
        answer = "{\"id\":\"pay_1\",\"order_id\":\"order_1\",\"amount\":5000,\"currency\":\"INR\","
                + "\"status\":\"captured\",\"method\":\"upi\"}";
        GatewayPayment payment = client().fetchPayment(KEYS, "pay_1");
        assertThat(payment).isEqualTo(new GatewayPayment("pay_1", "order_1", 5000, "INR", "captured", "upi"));
        assertThat(payment.captured()).isTrue();
        answer = "{\"id\":\"rfnd_1\",\"payment_id\":\"pay_1\",\"amount\":2000,\"status\":\"processed\"}";
        assertThat(client().createRefund(KEYS, "pay_1", 2000, Map.of()))
                .isEqualTo(new GatewayRefund("rfnd_1", "pay_1", 2000, "processed"));
        assertThat(requests.get(1)).startsWith("POST /v1/payments/pay_1/refund ").contains("\"amount\":2000");
    }

    @Test
    void aRefusalIsTypedWithTheGatewaysCodeAndAFaultIsUnavailable() {
        status = 400;
        answer = "{\"error\":{\"code\":\"BAD_REQUEST_ERROR\",\"description\":\"The amount must be at least INR 1.00\"}}";
        assertThatThrownBy(() -> client().createRefund(KEYS, "pay_1", 1, Map.of()))
                .isInstanceOf(PaymentGatewayRejectedException.class)
                .hasMessageContaining("BAD_REQUEST_ERROR").hasMessageNotContaining("INR 1.00");
        status = 502;
        assertThatThrownBy(() -> client().fetchPayment(KEYS, "pay_1")).isInstanceOf(UnavailableException.class);
        status = 429;
        assertThatThrownBy(() -> client().fetchPayment(KEYS, "pay_1")).isInstanceOf(UnavailableException.class);
        RazorpayClient closed = client();
        server.stop(0);
        assertThatThrownBy(() -> closed.fetchPayment(KEYS, "pay_1"))
                .as("no connection: the request never reached the gateway")
                .isInstanceOf(PaymentGatewayUnreachableException.class)
                .satisfies(e -> assertThat(((PaymentGatewayUnreachableException) e).status().value()).isEqualTo(503));
    }

    @Test
    void aRefundIsFoundAmongThePaymentsRefundsByItsNote() {
        answer = "{\"entity\":\"collection\",\"count\":2,\"items\":["
                + "{\"id\":\"rfnd_2\",\"payment_id\":\"pay_1\",\"amount\":100,\"status\":\"pending\","
                + "\"notes\":{\"mysms_refund\":\"other\"}},"
                + "{\"id\":\"rfnd_1\",\"payment_id\":\"pay_1\",\"amount\":2000,\"status\":\"processed\","
                + "\"notes\":{\"mysms_refund\":\"r-1\"}}]}";
        assertThat(client().findRefund(KEYS, "pay_1", "mysms_refund", "r-1"))
                .contains(new GatewayRefund("rfnd_1", "pay_1", 2000, "processed"));
        assertThat(requests.get(0)).startsWith("GET /v1/payments/pay_1/refunds ");
        assertThat(client().findRefund(KEYS, "pay_1", "mysms_refund", "r-9")).isEmpty();
        answer = "{\"entity\":\"collection\",\"count\":0,\"items\":[]}";
        assertThat(client().findRefund(KEYS, "pay_1", "mysms_refund", "r-1")).isEmpty();
    }

    @Test
    void moneyIsNeverRoundedAndIdsCannotEscapeThePath() {
        assertThat(RazorpayClient.minorUnits(new BigDecimal("10"), 2)).isEqualTo(1000);
        assertThatThrownBy(() -> RazorpayClient.minorUnits(new BigDecimal("10.005"), 2))
                .isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> client().fetchPayment(KEYS, "../orders")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client().createOrder(KEYS, 0, "INR", "r", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(KEYS.toString()).doesNotContain("s3cret");
    }
}
