package com.mysms.demoseed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * What a seed run made: the tenant (so {@code remove} deletes exactly that one), the rows per module and resource
 * (found or created), the operations skipped because this release answers them 501, and the demo sign-ins. Written
 * to {@code <manifest directory>/<tenant code>.json}; it holds no password.
 */
public record Manifest(String tenantCode, UUID tenantId, UUID institutionId,
                       Map<String, Map<String, Integer>> rows, List<String> skippedOperations,
                       Map<String, String> personas) {

    public Manifest {
        Map<String, Map<String, Integer>> copy = new TreeMap<>();
        rows.forEach((module, counts) -> copy.put(module, Collections.unmodifiableMap(new TreeMap<>(counts))));
        rows = Collections.unmodifiableMap(copy);
        skippedOperations = List.copyOf(skippedOperations);
        personas = Collections.unmodifiableMap(new TreeMap<>(personas));
    }

    public static Path file(Path directory, String tenantCode) {
        return directory.resolve(tenantCode + ".json");
    }

    public void write(Path directory, JsonMapper json) {
        try {
            Files.createDirectories(directory);
            Files.writeString(file(directory, tenantCode), json.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the manifest in " + directory, e);
        }
    }

    public static Manifest read(Path directory, String tenantCode, JsonMapper json) {
        Path file = file(directory, tenantCode);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("no manifest " + file + ": remove deletes only a tenant this seeder made");
        }
        try {
            return json.readValue(Files.readString(file), Manifest.class);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
    }
}
