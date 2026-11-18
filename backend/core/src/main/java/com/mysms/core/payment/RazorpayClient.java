package com.mysms.core.payment;

import com.mysms.core.error.UnavailableException;
import com.mysms.core.json.JsonMappers;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adapter to Razorpay's REST API (orders, payments, refunds), authenticated per call with the merchant's
 * {@link GatewayCredentials}. A 4xx is {@link PaymentGatewayRejectedException}, no connection
 * {@link PaymentGatewayUnreachableException}, and a timeout or 5xx {@link UnavailableException} (the gateway may have
 * acted). Never call it inside a database transaction.
 */
public final class RazorpayClient {

    /** The production endpoint; a deployment may point elsewhere (Razorpay's test mode uses the same host). */
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.razorpay.com/v1/");

    private static final String DEPENDENCY = "The payment gateway is not reachable.";
    private static final int RECEIPT_MAX = 40;

    private final URI baseUri;
    private final Duration timeout;
    private final HttpClient http;
    private final JsonMapper json = JsonMappers.create();

    public RazorpayClient(URI baseUri, Duration timeout) {
        this.baseUri = baseUri.toString().endsWith("/") ? baseUri : URI.create(baseUri + "/");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /**
     * Opens an order for {@code amountMinor} (paise for INR). {@code receipt} is the merchant's own reference (cut to the
     * gateway's 40 characters); {@code notes} are echoed back on the payment and its webhooks.
     */
    public GatewayOrder createOrder(GatewayCredentials credentials, long amountMinor, String currency, String receipt,
            Map<String, String> notes) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("An order amount must be positive.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountMinor);
        body.put("currency", currency);
        body.put("receipt", receipt.length() > RECEIPT_MAX ? receipt.substring(0, RECEIPT_MAX) : receipt);
        body.put("notes", notes);
        JsonNode order = send(credentials, "POST", "orders", body);
        return new GatewayOrder(order.path("id").asString(), order.path("amount").asLong(),
                order.path("currency").asString(), order.path("status").asString());
    }

    /** The payment as the gateway holds it now (a webhook or a checkout callback is confirmed against this). */
    public GatewayPayment fetchPayment(GatewayCredentials credentials, String paymentId) {
        JsonNode payment = send(credentials, "GET", "payments/" + pathSegment(paymentId), null);
        return payment(payment);
    }

    /** Refunds {@code amountMinor} of a captured payment; {@code notes} identify the refund on the gateway's side. */
    public GatewayRefund createRefund(GatewayCredentials credentials, String paymentId, long amountMinor,
            Map<String, String> notes) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("A refund amount must be positive.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountMinor);
        body.put("notes", notes);
        return refund(send(credentials, "POST", "payments/" + pathSegment(paymentId) + "/refund", body));
    }

    /** The payment's refund whose {@code note} is {@code value}, as the gateway holds it; empty when there is none. */
    public Optional<GatewayRefund> findRefund(GatewayCredentials credentials, String paymentId, String note,
            String value) {
        JsonNode list = send(credentials, "GET", "payments/" + pathSegment(paymentId) + "/refunds?count=100", null);
        for (JsonNode refund : list.path("items")) {
            if (value.equals(refund.path("notes").path(note).asString(null))) {
                return Optional.of(refund(refund));
            }
        }
        return Optional.empty();
    }

    /**
     * An amount in minor units: {@code 1234.50} INR (exponent 2) is {@code 123450} paise. A value with more decimals than
     * the currency has is refused rather than rounded — money is never rounded silently.
     */
    public static long minorUnits(BigDecimal amount, int exponent) {
        return amount.movePointRight(exponent).longValueExact();
    }

    /** A payment of a webhook's {@code payload.payment.entity} or of a fetch, in the gateway's field names. */
    public static GatewayPayment payment(JsonNode entity) {
        return new GatewayPayment(entity.path("id").asString(), entity.path("order_id").asString(null),
                entity.path("amount").asLong(), entity.path("currency").asString(), entity.path("status").asString(),
                entity.path("method").asString(null));
    }

    private static GatewayRefund refund(JsonNode refund) {
        return new GatewayRefund(refund.path("id").asString(), refund.path("payment_id").asString(),
                refund.path("amount").asLong(), refund.path("status").asString());
    }

    private JsonNode send(GatewayCredentials credentials, String method, String path, Object body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(timeout)
                .header("Authorization", basic(credentials))
                .header("Accept", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        }
        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (ConnectException | HttpConnectTimeoutException notConnected) {
            throw new PaymentGatewayUnreachableException();
        } catch (IOException unreachable) {
            throw new UnavailableException(DEPENDENCY);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new UnavailableException(DEPENDENCY);
        }
        int status = response.statusCode();
        if (status >= 500 || status == 429) {
            throw new UnavailableException(DEPENDENCY);
        }
        JsonNode answer = read(response.body());
        if (status >= 400) {
            throw new PaymentGatewayRejectedException(answer.path("error").path("code").asString("UNKNOWN"));
        }
        return answer;
    }

    private JsonNode read(String body) {
        try {
            return json.readTree(body == null || body.isBlank() ? "{}" : body);
        } catch (JacksonException notJson) {
            throw new UnavailableException(DEPENDENCY);
        }
    }

    private static String basic(GatewayCredentials credentials) {
        String pair = credentials.keyId() + ":" + credentials.keySecret();
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    private static String pathSegment(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_]{1,40}")) {
            throw new IllegalArgumentException("Not a gateway id.");
        }
        return id;
    }
}
