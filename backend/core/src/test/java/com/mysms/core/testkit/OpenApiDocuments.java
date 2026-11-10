package com.mysms.core.testkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test kit: keeps the OpenAPI document a module's service component test fetched from its own running service
 * ({@code /v3/api-docs.yaml}, written by springdoc from the module's contract interfaces — core's
 * OpenApiConfiguration). The document lands in {@code target/openapi/<module>.yaml} as a build artifact of every
 * {@code mvn verify}, the way CLAUDE.md requires: generated from the Spring code, never hand-written.
 */
public final class OpenApiDocuments {

    private OpenApiDocuments() {
    }

    /**
     * Writes the fetched document to {@code target/openapi/<module>.yaml} and returns it for assertions — after
     * checking it against the module's own operation table: every documented operation must carry a contract
     * operationId and its {@code x-permission}, so a controller method that silently missed the table cannot ship
     * mis-documented. (Operations whose resource has no controller yet answer 501 at the edge and have nothing for
     * springdoc to document; the operation-status report accounts for those.)
     */
    public static String write(String module, String document) {
        if (document == null || document.isBlank()) {
            throw new IllegalStateException("empty OpenAPI document for " + module);
        }
        Set<String> contract = new HashSet<>();
        String resource = "META-INF/mysms/operations/" + module + ".json";
        try (InputStream in = OpenApiDocuments.class.getClassLoader().getResourceAsStream(resource)) {
            for (JsonNode row : JsonMapper.builder().build().readTree(in)) {
                contract.add(row.get("operationId").asString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + resource, e);
        }
        List<String> documented = Pattern.compile("operationId: (\\S+)").matcher(document).results()
                .map(match -> match.group(1)).toList();
        // every documented operation is a contract operation (infrastructure controllers are @Hidden), so a
        // controller method the customizer could not match to its table row cannot ship under a default id
        List<String> unknown = documented.stream().filter(id -> !contract.contains(id)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalStateException("documented outside the contract of " + module + ": " + unknown);
        }
        if (documented.isEmpty()) {
            throw new IllegalStateException("no contract operation is documented for " + module);
        }
        int permissions = document.split("x-permission:", -1).length - 1;
        if (permissions != documented.size()) {
            throw new IllegalStateException("the OpenAPI document of " + module + " carries " + permissions
                    + " x-permission entries for " + documented.size() + " documented contract operations");
        }
        try {
            Path target = Path.of("target", "openapi", module + ".yaml");
            Files.createDirectories(target.getParent());
            Files.writeString(target, document, StandardCharsets.UTF_8);
            return document;
        } catch (IOException e) {
            throw new UncheckedIOException("could not write the OpenAPI document of " + module, e);
        }
    }
}
