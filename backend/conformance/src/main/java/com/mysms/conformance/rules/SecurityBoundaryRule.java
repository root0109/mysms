package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * X1 (JAVA-CODING-RULES §16): tokens are verified in one place. Outside auth-client nothing imports a token library
 * (io.jsonwebtoken, com.nimbusds, or Spring Security beyond its crypto package) except iam's token issuer, nothing
 * reads the Authorization header, and only iam hashes passwords (Spring Security's crypto package, argon2id).
 */
public final class SecurityBoundaryRule implements ConformanceRule {

    /** The token issuer signs tokens with the token library. */
    private static final Set<String> TOKEN_ISSUERS = Set.of("iam/src/main/java/com/mysms/iam/business/TokenIssuer.java");
    private static final Pattern TOKEN_LIBRARY = Pattern.compile(
            "(?m)^import (io\\.jsonwebtoken|com\\.nimbusds|org\\.springframework\\.security\\.(?!crypto\\.))");
    private static final Pattern PASSWORD_HASHING = Pattern.compile("(?m)^import org\\.springframework\\.security\\.crypto\\.");
    private static final Pattern AUTHORIZATION_HEADER =
            Pattern.compile("getHeader\\(\\s*(\"Authorization\"|HttpHeaders\\.AUTHORIZATION)");

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        for (String module : tree.productModules()) {
            if ("auth-client".equals(module)) {
                continue;
            }
            for (Path file : tree.java(module, "main")) {
                String source = tree.read(file);
                String path = tree.relative(file);
                if (TOKEN_LIBRARY.matcher(source).find() && !TOKEN_ISSUERS.contains(tree.relativeToBackend(file))) {
                    found.add(new Violation("X1", path, "imports a token library (only auth-client verifies tokens)"));
                }
                if (PASSWORD_HASHING.matcher(source).find() && !"iam".equals(module)) {
                    found.add(new Violation("X1", path, "password hashing outside iam"));
                }
                if (AUTHORIZATION_HEADER.matcher(source).find()) {
                    found.add(new Violation("X1", path, "reads the Authorization header (only auth-client does)"));
                }
            }
        }
        return found;
    }
}
