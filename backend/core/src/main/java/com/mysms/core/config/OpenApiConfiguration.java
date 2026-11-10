package com.mysms.core.config;

import com.mysms.core.json.JsonMappers;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import tools.jackson.databind.JsonNode;

/**
 * The OpenAPI document of a MySMS process, written by springdoc from the modules' own contract interfaces
 * (CLAUDE.md: OpenAPI documents are generated from the Spring code as part of the build — each module's service
 * component test fetches {@code /v3/api-docs.yaml} and writes {@code target/openapi/<module>.yaml}).
 *
 * <p>The interfaces carry the paths, parameters, bodies and validation; what they cannot carry is restored here by
 * the {@link OperationCustomizer} from the module's own operation table (META-INF/mysms/operations/&lt;module&gt;.json,
 * the same file the edge enforces): the contract operationId ({@code master.ListCountries}), {@code x-permission},
 * the security schemes, and the idempotency declaration.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(OpenAPI.class)
public class OpenApiConfiguration {

    /** A handler of a module's controller package; group(1) is the module. */
    private static final Pattern CONTROLLER_PACKAGE = Pattern.compile("^com\\.mysms\\.([a-z]+)\\.controller\\.");

    @Bean
    public OpenAPI mysmsOpenApi(Environment environment) {
        // the modules this process runs, as core configured them from the application class
        String names = String.join(", ", Binder.get(environment)
                .bind("mysms.auth.modules", Bindable.listOf(String.class)).orElse(List.of()));
        String version = OpenApiConfiguration.class.getPackage().getImplementationVersion();
        return new OpenAPI()
                .info(new Info().title("MySMS — " + names).version(version == null ? "dev" : version)
                        .description("Written from the modules' contract interfaces by springdoc."))
                .schemaRequirement("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                        .bearerFormat("JWT").description("OAuth 2.0 / OIDC access token. Claims carry tenant_id, "
                                + "user_id, roles and an optional act-as (impersonation) subject."))
                .schemaRequirement("platformAdminAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT").description("Access token of MySMS platform staff "
                                + "(audience mysms-platform-admin, MFA required). Only these operations may act "
                                + "across tenants; the target tenant is the resource being addressed."));
    }

    /** Restores per operation what the Java signature cannot state, from the module's operation table. */
    @Bean
    public OperationCustomizer mysmsOperationCustomizer() {
        Map<String, Map<String, JsonNode>> tables = new ConcurrentHashMap<>();
        return (operation, handler) -> {
            Matcher controller = CONTROLLER_PACKAGE.matcher(handler.getBeanType().getName());
            if (!controller.find()) {
                return operation;
            }
            JsonNode row = tables.computeIfAbsent(controller.group(1), OpenApiConfiguration::table)
                    .get(handler.getMethod().getName());
            if (row == null) {
                return operation;
            }
            operation.setOperationId(row.get("operationId").asString());
            operation.addExtension("x-permission", row.get("permission").asString());
            if (row.get("anonymous").asBoolean()) {
                operation.setSecurity(List.of());
            } else {
                row.get("schemes").forEach(scheme ->
                        operation.addSecurityItem(new SecurityRequirement().addList(scheme.asString())));
            }
            if (row.has("idempotency")) {
                operation.addExtension("x-idempotency", row.get("idempotency").asString());
            }
            return operation;
        };
    }

    /** The module's operation rows by controller method name ({@code master.ListCountries} → {@code listCountries}). */
    private static Map<String, JsonNode> table(String module) {
        String resource = "META-INF/mysms/operations/" + module + ".json";
        try (InputStream in = OpenApiConfiguration.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("operation table not found on the classpath: " + resource);
            }
            Map<String, JsonNode> byMethod = new ConcurrentHashMap<>();
            for (JsonNode row : JsonMappers.create().readTree(in)) {
                String name = row.get("operationId").asString();
                name = name.substring(name.indexOf('.') + 1);
                byMethod.put(name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1), row);
            }
            return byMethod;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("could not read " + resource, e);
        }
    }
}
