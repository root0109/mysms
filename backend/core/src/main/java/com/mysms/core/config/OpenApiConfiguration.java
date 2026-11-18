package com.mysms.core.config;

import com.mysms.core.json.JsonMappers;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import tools.jackson.databind.JsonNode;

/**
 * The process's OpenAPI document, written by springdoc from the modules' contract interfaces. What the interfaces
 * cannot carry is restored from the module's operation table: the operationId, {@code x-permission}, security schemes,
 * idempotency, and the tag ({@code CountryApi} → {@code Country}) so generated clients name their API classes as the
 * contract does.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(OpenAPI.class)
public class OpenApiConfiguration {

    /** A handler of a module's controller package; group(1) is the module. */
    private static final Pattern CONTROLLER_PACKAGE = Pattern.compile("^com\\.mysms\\.([a-z]+)\\.controller\\.");

    @Bean
    public OpenAPI mysmsOpenApi(Environment environment) {
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
            contractTag(handler.getBeanType(), handler.getMethod(), controller.group(1))
                    .ifPresent(tag -> operation.setTags(List.of(tag)));
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

    /**
     * Drops the {@code "null"} springdoc adds to a {@code @Nullable} property's type: contract fields are
     * {@code NON_NULL}, so an absent value is left out, never sent as null. A {@code @Nullable Object} loses its
     * {@code type: "null"} altogether and becomes any JSON value.
     */
    @Bean
    public OpenApiCustomizer mysmsAnyValueCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().values().forEach(OpenApiConfiguration::anyValue);
            }
        };
    }

    private static void anyValue(Schema<?> schema) {
        if (schema == null) {
            return;
        }
        if (Set.of("null").equals(schema.getTypes())) {
            schema.setTypes(null);
        } else if (schema.getTypes() != null && schema.getTypes().contains("null")) {
            Set<String> types = new LinkedHashSet<>(schema.getTypes());
            types.remove("null");
            schema.setTypes(types);
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(OpenApiConfiguration::anyValue);
        }
        anyValue(schema.getItems());
        if (schema.getAdditionalProperties() instanceof Schema<?> values) {
            anyValue(values);
        }
    }

    /**
     * Marks every contract DTO field that is not {@code @Nullable} as required (springdoc reads only
     * {@code @NotNull}). A schema with no contract class (paging, problems, enums) keeps what springdoc wrote.
     */
    @Bean
    public OpenApiCustomizer mysmsRequiredCustomizer(Environment environment) {
        List<String> modules = Binder.get(environment).bind("mysms.auth.modules", Bindable.listOf(String.class))
                .orElse(List.of());
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().forEach((name, schema) -> contractClass(modules, name)
                    .ifPresent(type -> required(schema, type)));
        };
    }

    /**
     * The module class a schema was written from, when exactly one module of the process has a class of that name;
     * with several the merged schema keeps what springdoc wrote.
     */
    static Optional<Class<?>> contractClass(List<String> modules, String schemaName) {
        List<Class<?>> found = new ArrayList<>();
        for (String module : modules) {
            String name = "com.mysms." + module + ".api.model." + schemaName;
            if (ClassUtils.isPresent(name, OpenApiConfiguration.class.getClassLoader())) {
                found.add(ClassUtils.resolveClassName(name, OpenApiConfiguration.class.getClassLoader()));
            }
        }
        return found.size() == 1 ? Optional.of(found.get(0)) : Optional.empty();
    }

    private static void required(Schema<?> schema, Class<?> type) {
        if (schema.getProperties() == null) {
            return;
        }
        Set<String> required = new LinkedHashSet<>(schema.getRequired() == null ? List.of() : schema.getRequired());
        for (String property : schema.getProperties().keySet()) {
            Field field = ReflectionUtils.findField(type, property);
            if (field != null && !field.isAnnotationPresent(Nullable.class)) {
                required.add(property);
            }
        }
        schema.setRequired(required.isEmpty() ? null : List.copyOf(required));
    }

    /** The tag of an operation: the contract interface that declares it ({@code com.mysms.<module>.api.<Tag>Api}). */
    private static Optional<String> contractTag(Class<?> controller, Method method, String module) {
        String contract = "com.mysms." + module + ".api.";
        for (Class<?> implemented : controller.getInterfaces()) {
            String name = implemented.getName();
            if (name.startsWith(contract) && name.endsWith("Api") && name.indexOf('.', contract.length()) < 0
                    && ReflectionUtils.findMethod(implemented, method.getName(), method.getParameterTypes()) != null) {
                return Optional.of(implemented.getSimpleName().substring(0, implemented.getSimpleName().length() - 3));
            }
        }
        return Optional.empty();
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
