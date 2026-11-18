package com.mysms.iam.controller;

import com.mysms.authclient.AuthClientProperties;
import com.mysms.core.error.Problems;
import com.mysms.iam.business.ServiceClients;
import com.mysms.iam.business.TokenIssuer;
import io.swagger.v3.oas.annotations.Hidden;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The OAuth 2.0 token endpoint for service principals (RFC 6749 §4.4, client credentials) at
 * /oauth2/token: a module authenticates with its client id (the module name) and secret in the form body
 * ({@code client_secret_post}) and receives a short-lived service token ({@code sub = service:<module>}, audience
 * {@code mysms-internal}, the client's configured permissions, no tenant). Infrastructure, not a contract operation,
 * like the JWKS. Refusals are problem+json carrying the RFC 6749 {@code error} as well; every authentication failure
 * gets the same answer, so clients cannot be probed. The secret is never logged.
 */
@Hidden // infrastructure, not an API operation: keeps the OpenAPI document to the contract
@RestController
public class ServiceTokenController {

    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final Logger LOG = LoggerFactory.getLogger(ServiceTokenController.class);

    private final ServiceClients clients;
    private final TokenIssuer issuer;

    public ServiceTokenController(ServiceClients clients, TokenIssuer issuer) {
        this.clients = clients;
        this.issuer = issuer;
    }

    @PostMapping(path = AuthClientProperties.TOKEN_PATH, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> token(@RequestParam MultiValueMap<String, String> form) {
        if (!CLIENT_CREDENTIALS.equals(single(form, "grant_type"))) {
            return refuse(HttpStatus.BAD_REQUEST, "UNSUPPORTED_GRANT_TYPE", "unsupported_grant_type",
                    "Only the client_credentials grant is served here.");
        }
        String clientId = single(form, "client_id");
        Optional<ServiceClients.Client> client = clients.authenticate(clientId, single(form, "client_secret"));
        if (client.isEmpty()) {
            LOG.warn("service token refused: client authentication failed");
            return refuse(HttpStatus.UNAUTHORIZED, "INVALID_CLIENT", "invalid_client",
                    "Client authentication failed.");
        }
        Duration ttl = clients.tokenTtl();
        TokenIssuer.IssuedToken token = issuer.issueService(clientId, client.get().permissions(), ttl);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", token.token());
        body.put("token_type", "Bearer");
        body.put("expires_in", ttl.toSeconds());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /** A form value sent exactly once (RFC 6749 §3.2: a repeated parameter makes the request invalid). */
    private static String single(MultiValueMap<String, String> form, String name) {
        List<String> values = form.get(name);
        return values == null || values.size() != 1 ? null : values.get(0);
    }

    private static ResponseEntity<Map<String, Object>> refuse(HttpStatus status, String code, String error,
                                                              String detail) {
        Map<String, Object> body = new LinkedHashMap<>(Problems.body(status, code, detail, List.of()));
        body.put("error", error);
        ResponseEntity.BodyBuilder answer = ResponseEntity.status(status).cacheControl(CacheControl.noStore())
                .contentType(Problems.PROBLEM_JSON);
        return answer.body(body);
    }
}
