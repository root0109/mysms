/*
 * Bundles each module's OpenAPI file into one self-contained document for the code generators.
 *
 * Every external reference (./common.yaml#/components/schemas/Operation, ./people.yaml#/components/schemas/Person
 * ...) is copied into the module document's own components under its original name and the $ref is rewritten
 * to the local pointer. Without this, OpenAPI Generator's 3.1 parser inlines external schemas and names them
 * after the first operation that uses them (Operation would become PlatformClearDemoData202Response).
 *
 * It also writes, per module, the operation table the auth filter enforces at the edge
 * (method, path template, x-permission, security schemes, whether a token is optional; JAVA-CODING-RULES §16).
 *
 * Single-file program (JEP 330), run on the classpath of the OpenAPI Generator CLI jar, which bundles Jackson:
 *   java -cp openapi-generator-cli.jar api/tools/OpenApiBundler.java <api/openapi dir> <out dir> [<permissions dir>]
 * Used by the TypeScript SDK (clients/packages/sdk) and by the Java contracts/sdk modules (backend/), so both
 * generate from identical inputs.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.yaml.snakeyaml.LoaderOptions;

public final class OpenApiBundler {

    private static final Set<String> NOT_MODULES = Set.of("common.yaml", "redocly.yaml");
    private static final Set<String> VERBS = Set.of("get", "put", "post", "delete", "patch");
    private static final Pattern EXTERNAL =
            Pattern.compile("^(?:\\./)?([a-z]+\\.yaml)#/components/([A-Za-z]+)/([A-Za-z0-9_.-]+)$");
    private static final Pattern LOCAL = Pattern.compile("^#/components/([A-Za-z]+)/([A-Za-z0-9_.-]+)$");

    private final Path dir;
    private final ObjectMapper yaml;
    private final Map<String, JsonNode> cache = new HashMap<>();

    private OpenApiBundler(Path dir) {
        this.dir = dir;
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(64 * 1024 * 1024);
        this.yaml = new ObjectMapper(YAMLFactory.builder().loaderOptions(options).build());
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("usage: OpenApiBundler <api/openapi dir> <out dir> [<permissions dir>]");
        }
        Path dir = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Path permissions = args.length == 3 ? Path.of(args[2]) : null;
        Files.createDirectories(out);
        if (permissions != null) {
            Files.createDirectories(permissions);
        }
        OpenApiBundler bundler = new OpenApiBundler(dir);
        ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        List<String> modules;
        try (Stream<Path> files = Files.list(dir)) {
            modules = files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".yaml") && !NOT_MODULES.contains(n))
                    .sorted()
                    .toList();
        }
        if (modules.size() != 21) {
            throw new IllegalStateException("expected 21 module contracts, found " + modules.size());
        }
        for (String file : modules) {
            String module = file.substring(0, file.length() - ".yaml".length());
            ObjectNode bundled = bundler.bundle(file);
            json.writeValue(out.resolve(module + ".json").toFile(), bundled);
            if (permissions != null) {
                json.writeValue(permissions.resolve(module + ".json").toFile(), operations(module, bundled));
            }
        }
        System.err.println("OpenApiBundler: bundled " + modules.size() + " module contracts into " + out);
    }

    private JsonNode load(String file) {
        return cache.computeIfAbsent(file, f -> {
            try {
                return yaml.readTree(dir.resolve(f).toFile());
            } catch (IOException e) {
                throw new IllegalStateException("cannot read " + f, e);
            }
        });
    }

    private ObjectNode bundle(String self) {
        ObjectNode root = (ObjectNode) load(self).deepCopy();
        ObjectNode components = root.has("components") ? (ObjectNode) root.get("components") : root.putObject("components");
        // where each local component name came from, so two different sources can never share a name
        Map<String, String> origin = new HashMap<>();
        components.fieldNames().forEachRemaining(kind -> components.get(kind).fieldNames()
                .forEachRemaining(name -> origin.put(kind + "/" + name, self + "#" + kind + "/" + name)));
        Deque<String[]> pending = new ArrayDeque<>();
        rewrite(root, self, self, origin, pending);
        while (!pending.isEmpty()) {
            String[] next = pending.removeFirst();
            String from = next[0];
            String kind = next[1];
            String name = next[2];
            JsonNode value = load(from).path("components").path(kind).path(name).deepCopy();
            rewrite(value, from, self, origin, pending);
            ObjectNode bucket = components.has(kind) ? (ObjectNode) components.get(kind) : components.putObject(kind);
            bucket.set(name, value);
        }
        return root;
    }

    private void rewrite(JsonNode node, String from, String self, Map<String, String> origin, Deque<String[]> pending) {
        if (node.isArray()) {
            node.forEach(child -> rewrite(child, from, self, origin, pending));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        ObjectNode object = (ObjectNode) node;
        JsonNode ref = object.get("$ref");
        if (ref != null && ref.isTextual()) {
            object.put("$ref", resolve(ref.asText(), from, self, origin, pending));
        }
        for (Map.Entry<String, JsonNode> field : object.properties()) {
            if (!"$ref".equals(field.getKey())) {
                rewrite(field.getValue(), from, self, origin, pending);
            }
        }
    }

    private String resolve(String ref, String from, String self, Map<String, String> origin, Deque<String[]> pending) {
        Matcher local = LOCAL.matcher(ref);
        if (local.matches()) {
            // a local pointer inside an imported file points into that file
            return from.equals(self) ? ref : importComponent(from, local.group(1), local.group(2), origin, pending);
        }
        Matcher external = EXTERNAL.matcher(ref);
        if (!external.matches()) {
            throw new IllegalStateException("unsupported $ref " + ref + " in " + from);
        }
        if (external.group(1).equals(self)) {
            return "#/components/" + external.group(2) + "/" + external.group(3);
        }
        return importComponent(external.group(1), external.group(2), external.group(3), origin, pending);
    }

    private String importComponent(String file, String kind, String name, Map<String, String> origin,
                                   Deque<String[]> pending) {
        String key = kind + "/" + name;
        String source = file + "#" + key;
        String known = origin.get(key);
        if (known != null && !known.equals(source)) {
            throw new IllegalStateException("component name clash: " + known + " vs " + source);
        }
        if (known == null) {
            if (load(file).path("components").path(kind).path(name).isMissingNode()) {
                throw new IllegalStateException("unresolved $ref " + source);
            }
            origin.put(key, source); // recorded before rewriting, so cycles terminate
            pending.addLast(new String[] {file, kind, name});
        }
        return "#/components/" + key;
    }

    /**
     * "required" or "optional" when the operation declares the Idempotency-Key header (api/README "Idempotency"),
     * else null: the edge stores and replays responses only for operations that declare it.
     */
    private static String idempotency(JsonNode spec, JsonNode pathItem, JsonNode operation) {
        String found = null;
        for (JsonNode list : new JsonNode[] {pathItem.path("parameters"), operation.path("parameters")}) {
            for (JsonNode parameter : list) {
                JsonNode resolved = parameter;
                if (parameter.has("$ref")) {
                    String ref = parameter.get("$ref").asText();
                    resolved = spec.path("components").path("parameters").path(ref.substring(ref.lastIndexOf('/') + 1));
                }
                if ("header".equals(resolved.path("in").asText())
                        && "Idempotency-Key".equalsIgnoreCase(resolved.path("name").asText())) {
                    found = resolved.path("required").asBoolean() ? "required" : "optional";
                }
            }
        }
        return found;
    }

    /** method, path template, x-permission, schemes and token-optional flag of every operation, by operationId. */
    private static ArrayNode operations(String module, JsonNode spec) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, ObjectNode> byId = new TreeMap<>();
        JsonNode defaultSecurity = spec.path("security");
        spec.path("paths").properties().forEach(path -> path.getValue().properties().forEach(op -> {
            if (!VERBS.contains(op.getKey())) {
                return;
            }
            JsonNode operation = op.getValue();
            String id = operation.path("operationId").asText();
            String permission = operation.path("x-permission").asText();
            if (id.isEmpty() || permission.isEmpty()) {
                throw new IllegalStateException(module + " " + op.getKey() + " " + path.getKey()
                        + ": operationId and x-permission are required");
            }
            JsonNode security = operation.has("security") ? operation.get("security") : defaultSecurity;
            ArrayNode schemes = mapper.createArrayNode();
            security.forEach(requirement -> requirement.properties().forEach(scheme -> schemes.add(scheme.getKey())));
            // `security: []` or an empty requirement `{}` means the operation may be called without a token
            boolean anonymous = security.isEmpty() || java.util.stream.StreamSupport.stream(security.spliterator(), false)
                    .anyMatch(JsonNode::isEmpty);
            ObjectNode row = mapper.createObjectNode();
            row.put("operationId", id);
            row.put("method", op.getKey().toUpperCase(java.util.Locale.ROOT));
            row.put("path", path.getKey());
            row.put("permission", permission);
            row.set("schemes", schemes);
            row.put("anonymous", anonymous);
            String idempotency = idempotency(spec, path.getValue(), operation);
            if (idempotency != null) {
                row.put("idempotency", idempotency);
            }
            if (byId.put(id, row) != null) {
                throw new IllegalStateException("duplicate operationId " + id);
            }
        }));
        ArrayNode rows = mapper.createArrayNode();
        byId.values().forEach(rows::add);
        return rows;
    }
}
