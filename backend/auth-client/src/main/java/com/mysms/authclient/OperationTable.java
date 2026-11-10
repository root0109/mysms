package com.mysms.authclient;

import com.mysms.core.json.JsonMappers;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;

/**
 * Every operation this process serves, from the contracts (META-INF/mysms/operations/&lt;module&gt;.json), matched by
 * method and path. A request that matches no contract operation is refused at the edge: nothing undeclared is
 * reachable.
 */
public final class OperationTable {

    private record Entry(PathPattern pattern, OperationRule rule) {
    }

    private static final Comparator<Entry> MOST_SPECIFIC_FIRST =
            Comparator.comparing(Entry::pattern, PathPattern.SPECIFICITY_COMPARATOR);

    private final List<Entry> entries;

    private OperationTable(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    /** Loads the operation tables of the given modules from the classpath. */
    public static OperationTable load(Collection<String> modules) {
        if (modules.isEmpty()) {
            throw new IllegalArgumentException("mysms.auth.modules must name the modules this process serves");
        }
        JsonMapper json = JsonMappers.create();
        PathPatternParser parser = new PathPatternParser();
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String module : modules) {
            String resource = "META-INF/mysms/operations/" + module + ".json";
            try (InputStream in = OperationTable.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("operation table not found on the classpath: " + resource);
                }
                for (JsonNode row : json.readTree(in)) {
                    Set<String> schemes = new HashSet<>();
                    row.get("schemes").forEach(s -> schemes.add(s.asString()));
                    OperationRule rule = new OperationRule(row.get("operationId").asString(), row.get("method").asString(),
                            row.get("path").asString(), row.get("permission").asString(), schemes,
                            row.get("anonymous").asBoolean(),
                            row.has("idempotency") ? row.get("idempotency").asString() : null);
                    if (!seen.add(rule.method() + " " + rule.path())) {
                        throw new IllegalStateException("duplicate operation " + rule.method() + " " + rule.path());
                    }
                    entries.add(new Entry(parser.parse(rule.path()), rule));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return new OperationTable(entries);
    }

    /** The operation a request addresses; the most specific template wins (`/x/{id}:cancel` over `/x/{id}`). */
    public Optional<OperationRule> match(String method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        return entries.stream()
                .filter(e -> e.rule().method().equals(method) && e.pattern().matches(container))
                .min(MOST_SPECIFIC_FIRST)
                .map(Entry::rule);
    }

    public int size() {
        return entries.size();
    }
}
