package com.mysms.core.testkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks the OpenAPI document a module's {@code <Module>OpenApiComponentTest} fetched from its running service before
 * the test writes it to {@code target/openapi/api-docs.yml}.
 */
public final class OpenApiDocuments {

    private OpenApiDocuments() {
    }

    /**
     * The fetched document, checked against the module's operation table: every documented operation must carry a
     * contract operationId and its {@code x-permission}, so a method that missed the table cannot ship mis-documented.
     */
    public static String checked(String module, String document) {
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
        return document;
    }
}
