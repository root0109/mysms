package com.mysms.iam.controller;

import com.mysms.authclient.AuthClientProperties;
import com.mysms.iam.business.TokenIssuer;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The issuer's public keys (RFC 7517 JSON Web Key Set) at /oauth2/jwks, which every module's verifier reads through
 * mysms.auth.jwk-set-uri. Infrastructure, not a contract operation: public keys only, served without a token.
 */
@RestController
public class JwksController {

    private static final MediaType JWK_SET = MediaType.parseMediaType("application/jwk-set+json");
    private static final Duration MAX_AGE = Duration.ofMinutes(5);

    private final TokenIssuer issuer;

    public JwksController(TokenIssuer issuer) {
        this.issuer = issuer;
    }

    @GetMapping(AuthClientProperties.JWKS_PATH)
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok().contentType(JWK_SET).cacheControl(CacheControl.maxAge(MAX_AGE).cachePublic())
                .body(issuer.jwks());
    }
}
