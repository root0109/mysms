package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.ContractOperations;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The web edge of the domain modules (JAVA-CODING-RULES §7, §9, §11, §16):
 * <ul>
 * <li>C3 — every controller implements a contract interface and declares no mapping of its own; controllers import
 * no repository, outbox, audit writer or event publisher.</li>
 * <li>D2 — no hand-written *DTO classes, no Optional fields in model/, no {@code @JsonView} or ResponseBodyAdvice; a
 * delete operation answers 204; no contract operation replaces with PUT (updates are PATCH + updateMask, §7.3) except
 * the one idempotent set the rules allow.</li>
 * <li>E2 — no {@code @ControllerAdvice}, own exception type or BusinessException in a domain module; no exception
 * message passed into a typed exception or the problem body.</li>
 * <li>X1 — no servlet filter in a domain module (the auth and idempotency filters live in auth-client and core).</li>
 * </ul>
 * The OIDC key set controller serves an infrastructure path, not an API operation, and is exempt from C3.
 */
public final class ApiLayerRule implements ConformanceRule {

    /** Controllers of infrastructure paths that are not API operations (§16 step 1): the OIDC key set. */
    private static final Set<String> INFRASTRUCTURE_CONTROLLERS =
            Set.of("iam/src/main/java/com/mysms/iam/controller/JwksController.java");
    /** The only PUT of the contracts (§7.3): setting one's own value of one setting key is an idempotent set. */
    private static final Set<String> PUT_ALLOWED = Set.of("iam.SetMySetting");

    private static final Pattern MAPPING = Pattern.compile("@(?:[\\w.]+\\.)?(Request|Get|Post|Put|Patch|Delete)Mapping\\b");
    private static final Pattern CONTROLLER_ADVICE = Pattern.compile("@(?:[\\w.]+\\.)?(Rest)?ControllerAdvice\\b");
    private static final Pattern OWN_EXCEPTION = Pattern.compile(
            "class \\w*Exception extends (RuntimeException|Exception|IllegalStateException)\\b|\\bBusinessException\\b");
    private static final Pattern MESSAGE_LEAK = Pattern.compile(
            "new (?:[\\w.]+\\.)?\\w+Exception\\([^;]*\\b(e|ex|exception|failure|cause)\\.getMessage\\(\\)");
    private static final Pattern HAND_WRITTEN_DTO = Pattern.compile("class \\w+(DTO|Dto)\\b");
    private static final Pattern RESPONSE_SHAPING = Pattern.compile("@(?:[\\w.]+\\.)?JsonView\\b|ResponseBodyAdvice");
    private static final Pattern OPTIONAL_FIELD = Pattern.compile("private (final )?(java\\.util\\.)?Optional<");
    private static final Pattern IMPLEMENTS_CONTRACT = Pattern.compile("class \\w+Controller implements \\w+Api\\b");
    /** A class directly in a module's controller package. */
    private static final Pattern CONTROLLER = Pattern.compile("/controller/[^/]+\\.java$");
    private static final Pattern DELETE_METHOD =
            Pattern.compile("public ResponseEntity<Void> (delete\\w*)\\([^)]*\\) \\{(.*?)\\n    \\}", Pattern.DOTALL);

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        for (String module : tree.domainModules()) {
            Pattern sideEffects = Pattern.compile("(?m)^import com\\.mysms\\." + module + "\\.repository\\.|"
                    + "^import com\\.mysms\\.core\\.outbox\\.|ApplicationEventPublisher");
            for (Path file : tree.java(module, "main")) {
                if (INFRASTRUCTURE_CONTROLLERS.contains(tree.relativeToBackend(file))) {
                    continue;
                }
                classRules(tree, file, found);
                if (CONTROLLER.matcher(tree.relative(file)).find()) {
                    controller(tree, file, sideEffects, found);
                }
            }
        }
        for (ContractOperations.Operation operation : ContractOperations.read(tree)) {
            if ("PUT".equals(operation.method()) && !PUT_ALLOWED.contains(operation.operationId())) {
                found.add(new Violation("D2", "api/openapi/" + operation.module() + ".yaml",
                        "PUT " + operation.operationId() + " (updates are PATCH + updateMask, §7.3)"));
            }
        }
        return found;
    }

    /** The module's own contract package: the API interfaces and DTOs (owned source, JAVA-CODING-RULES §7). */
    private static boolean isContract(SourceTree tree, Path file) {
        return tree.relative(file).matches(".*/src/main/java/com/mysms/\\w+/api/.*");
    }

    private static void classRules(SourceTree tree, Path file, List<Violation> found) {
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
        if (path.contains("/model/") && OPTIONAL_FIELD.matcher(source).find()) {
            found.add(new Violation("D2", path, "Optional field in a model class (partial updates use updateMask, §7.2)"));
        }
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
