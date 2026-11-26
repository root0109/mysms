package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.ContractOperations;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The web edge of the domain modules:
 * <ul>
 * <li>C3 — every controller implements a contract interface and declares no mapping of its own; controllers import no
 * repository, outbox, audit writer or event publisher.</li>
 * <li>D2 — no hand-written *DTO classes, no {@code @JsonView} or ResponseBodyAdvice; a delete answers 204; no
 * contract operation replaces with PUT (updates are PATCH) except the one idempotent set allowed; a PATCH takes no
 * {@code updateMask} and its body, the {@code <Resource>Update}, wraps every field in an uninitialised
 * {@code Optional} (§7.2), the only model class that may; {@code @Valid} sits on a type argument, never on a
 * container (§7.1).</li>
 * <li>E2 — no {@code @ControllerAdvice}, own exception type or BusinessException in a domain module; no exception
 * message passed into a typed exception or the problem body.</li>
 * <li>X1 — no servlet filter in a domain module (the auth and idempotency filters live in auth-client and core).</li>
 * </ul>
 * The OIDC key set and the service-token endpoint serve infrastructure paths, not API operations, and are exempt from
 * C3.
 */
public final class ApiLayerRule implements ConformanceRule {

    /**
     * Controllers of infrastructure paths that are not API operations: the OIDC key set and the service-principal token
     * endpoint.
     */
    private static final Set<String> INFRASTRUCTURE_CONTROLLERS = Set.of(
            "identity/src/main/java/com/mysms/identity/iam/controller/JwksController.java",
            "identity/src/main/java/com/mysms/identity/iam/controller/ServiceTokenController.java");
    /** The only PUT of the contracts: setting one's own value of one setting key is an idempotent set. */
    private static final Set<String> PUT_ALLOWED = Set.of("iam.SetMySetting");

    private static final Pattern MAPPING = Pattern.compile("@(?:[\\w.]+\\.)?(Request|Get|Post|Put|Patch|Delete)Mapping\\b");
    private static final Pattern CONTROLLER_ADVICE = Pattern.compile("@(?:[\\w.]+\\.)?(Rest)?ControllerAdvice\\b");
    private static final Pattern OWN_EXCEPTION = Pattern.compile(
            "class \\w*Exception extends (RuntimeException|Exception|IllegalStateException)\\b|\\bBusinessException\\b");
    private static final Pattern MESSAGE_LEAK = Pattern.compile(
            "new (?:[\\w.]+\\.)?\\w+Exception\\([^;]*\\b(e|ex|exception|failure|cause)\\.getMessage\\(\\)");
    private static final Pattern HAND_WRITTEN_DTO = Pattern.compile("class \\w+(DTO|Dto)\\b");
    private static final Pattern RESPONSE_SHAPING = Pattern.compile("@(?:[\\w.]+\\.)?JsonView\\b|ResponseBodyAdvice");
    private static final Pattern OPTIONAL_FIELD = Pattern.compile("private (final )?(@\\w+ )*(java\\.util\\.)?Optional<");
    /** A contract operation that patches: its body type and whether it still takes an update mask. */
    private static final Pattern PATCH_OPERATION = Pattern.compile(
            "method = RequestMethod\\.PATCH,.*?\\n    \\);", Pattern.DOTALL);
    private static final Pattern REQUEST_BODY = Pattern.compile("@RequestBody (\\w+) ");
    /** A field of a contract DTO (the generated shape indents members by two spaces). */
    private static final Pattern MEMBER_FIELD = Pattern.compile("(?m)^  private (?!static )(.+?) (\\w+)( = [^;>]*)?;$");
    /** {@code @Valid} on a container or an {@code Object} that may be one (Hibernate HV000271/HV000272). */
    private static final Pattern VALID_CONTAINER = Pattern.compile("@Valid\\s+(?:(?:@[\\w.]+(?:\\((?:[^()]|\\([^()]*\\))*"
            + "\\))?|public|protected|private|final)\\s+)*(?:(?:java\\.util\\.)?(?:List|Set|Map|Collection)<|Object\\s)");
    private static final Pattern IMPLEMENTS_CONTRACT = Pattern.compile("class \\w+Controller implements \\w+Api\\b");
    /** A class directly in a module's controller package. */
    private static final Pattern CONTROLLER = Pattern.compile("/controller/[^/]+\\.java$");
    private static final Pattern DELETE_METHOD =
            Pattern.compile("public ResponseEntity<Void> (delete\\w*)\\([^)]*\\) \\{(.*?)\\n    \\}", Pattern.DOTALL);

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        for (String module : tree.domainModules()) {
            Pattern sideEffects = Pattern.compile("(?m)^import com\\.mysms\\." + module + "\\.(?:\\w+\\.)?repository\\.|"
                    + "^import com\\.mysms\\.core\\.outbox\\.|ApplicationEventPublisher");
            List<Path> files = tree.java(module, "main");
            Set<Path> patchBodies = patchBodies(tree, files);
            for (Path file : files) {
                if (INFRASTRUCTURE_CONTROLLERS.contains(tree.relativeToBackend(file))) {
                    continue;
                }
                classRules(tree, file, patchBodies.contains(file), found);
                if (isContract(tree, file)) {
                    contractRules(tree, file, found);
                }
                if (CONTROLLER.matcher(tree.relative(file)).find()) {
                    controller(tree, file, sideEffects, found);
                }
            }
        }
        for (ContractOperations.Operation operation : ContractOperations.read(tree)) {
            if ("PUT".equals(operation.method()) && !PUT_ALLOWED.contains(operation.operationId())) {
                found.add(new Violation("D2", tree.relative(ContractOperations.table(tree, operation.area())),
                        "PUT " + operation.operationId() + " (updates are PATCH, §7.2/§7.3)"));
            }
        }
        return found;
    }

    /** An area's own contract package: the API interfaces and DTOs. */
    private static boolean isContract(SourceTree tree, Path file) {
        return tree.relative(file).matches(".*/src/main/java/com/mysms/\\w+/(?:\\w+/)?api/.*");
    }

    /** The request bodies of the module's PATCH operations: the only model classes that may hold Optional fields. */
    private static Set<Path> patchBodies(SourceTree tree, List<Path> files) {
        Set<Path> bodies = new java.util.HashSet<>();
        for (Path file : files) {
            if (!isContract(tree, file) || !tree.relative(file).endsWith("Api.java")) {
                continue;
            }
            Matcher patch = PATCH_OPERATION.matcher(tree.read(file));
            while (patch.find()) {
                Matcher body = REQUEST_BODY.matcher(patch.group());
                if (body.find()) {
                    bodies.add(file.resolveSibling("model").resolve(body.group(1) + ".java"));
                }
            }
        }
        return bodies;
    }

    private static void classRules(SourceTree tree, Path file, boolean patchBody, List<Violation> found) {
        String source = tree.read(file);
        String path = tree.relative(file);
        if (!isContract(tree, file) && MAPPING.matcher(source).find()) {
            found.add(new Violation("C3", path,
                    "declares a mapping (only the api package's contract interfaces declare them)"));
        }
        if (CONTROLLER_ADVICE.matcher(source).find()) {
            found.add(new Violation("E2", path, "own exception handler (core GlobalExceptionHandler is the only one)"));
        }
        if (OWN_EXCEPTION.matcher(source).find()) {
            found.add(new Violation("E2", path, "own exception type (use core's typed exceptions, §11.3)"));
        }
        if (MESSAGE_LEAK.matcher(source).find()) {
            found.add(new Violation("E2", path, "passes an exception message to the client (§11.2)"));
        }
        if (source.contains("extends OncePerRequestFilter")) {
            found.add(new Violation("X1", path,
                    "a servlet filter in a domain module (auth and idempotency filters live in auth-client/core)"));
        }
        if (HAND_WRITTEN_DTO.matcher(source).find()) {
            found.add(new Violation("D2", path, "hand-written transport DTO"));
        }
        if (RESPONSE_SHAPING.matcher(source).find()) {
            found.add(new Violation("D2", path, "response shaping outside the read mask (§7.5)"));
        }
        if (path.contains("/model/") && !patchBody && OPTIONAL_FIELD.matcher(source).find()) {
            found.add(new Violation("D2", path, "Optional field in a model class (only a PATCH body has them, §7.2)"));
        }
    }

    /** §7.2 and §7.1 on a contract: PATCH without updateMask, Optional update fields, no @Valid on a container. */
    private static void contractRules(SourceTree tree, Path file, List<Violation> found) {
        String source = tree.read(file);
        String path = tree.relative(file);
        if (validOnContainer(source)) {
            found.add(new Violation("D2", path, "@Valid on a container; put it on the type argument (§7.1)"));
        }
        Matcher patch = PATCH_OPERATION.matcher(source);
        while (patch.find()) {
            String operation = patch.group();
            if (operation.contains("\"updateMask\"")) {
                found.add(new Violation("D2", path, "a PATCH takes no updateMask (§7.2)"));
            }
            Matcher body = REQUEST_BODY.matcher(operation);
            if (body.find()) {
                updateBody(tree, file.resolveSibling("model").resolve(body.group(1) + ".java"), found);
            }
        }
    }

    private static void updateBody(SourceTree tree, Path file, List<Violation> found) {
        if (!Files.isRegularFile(file)) {
            found.add(new Violation("D2", tree.relative(file), "the PATCH body is not in the contract's model package"));
            return;
        }
        for (String field : notOptionalUpdateFields(tree.read(file))) {
            found.add(new Violation("D2", tree.relative(file),
                    field + " must be an uninitialised Optional<…> (PATCH body, §7.2)"));
        }
    }

    /**
     * The fields of a PATCH body that break §7.2: not an {@code Optional}, or initialised (absent and a default would
     * become one state).
     */
    public static List<String> notOptionalUpdateFields(String source) {
        List<String> broken = new ArrayList<>();
        Matcher field = MEMBER_FIELD.matcher(source);
        while (field.find()) {
            String type = field.group(1).replaceAll("@\\w+ ", "");
            if (!type.startsWith("Optional<") || field.group(3) != null) {
                broken.add(field.group(2));
            }
        }
        return broken;
    }

    /** Whether the source puts {@code @Valid} on a container rather than on its type argument (HV000271). */
    public static boolean validOnContainer(String source) {
        return VALID_CONTAINER.matcher(source).find();
    }

    private static void controller(SourceTree tree, Path file, Pattern sideEffects, List<Violation> found) {
        String source = tree.read(file);
        String path = tree.relative(file);
        if (!IMPLEMENTS_CONTRACT.matcher(source).find()) {
            found.add(new Violation("C3", path, "does not implement a contract interface"));
        }
        if (sideEffects.matcher(source).find()) {
            found.add(new Violation("C3", path, "side effects or data access in a controller (§9)"));
        }
        Matcher delete = DELETE_METHOD.matcher(source);
        while (delete.find()) {
            String body = delete.group(2);
            if (!body.contains("noContent()") && !body.contains("NotImplementedException")) {
                found.add(new Violation("D2", path, delete.group(1) + " must answer 204 (§7.4)"));
            }
        }
    }
}
