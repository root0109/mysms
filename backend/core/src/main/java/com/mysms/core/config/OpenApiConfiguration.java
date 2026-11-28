package com.mysms.core.config;

import com.mysms.core.json.JsonMappers;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.lang.reflect.AnnotatedParameterizedType;
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
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.beans.factory.ListableBeanFactory;
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
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * The process's OpenAPI documents, written by springdoc from the areas' contract interfaces: one group per area the
 * process serves ({@code /v3/api-docs/<area>}, {@link OpenApiGroupsEnvironmentPostProcessor}). What the
 * interfaces cannot carry is restored from the area's operation table: the operationId, {@code x-permission}, security
 * schemes, idempotency, and the tag ({@code CountryApi} → {@code Country}) so generated clients name their API classes
 * as the contract does. An area's packages are {@code com.mysms.<module>} or {@code com.mysms.<module>.<area>}.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(OpenAPI.class)
public class OpenApiConfiguration {

    /** The area of a contract path ({@code /v1/<area>/…}; an underscore in the area name is a hyphen in the URL). */
    private static final Pattern DOCUMENTED_AREA = Pattern.compile("^/v1/([a-z-]+)/");
    /**
     * A handler of an area's controller package; group(1) is the area's root package, group(2) the area's package
     * segment (the area name without underscores: {@code time_attendance} → {@code timeattendance}).
     */
    private static final Pattern CONTROLLER_PACKAGE =
            Pattern.compile("^(com\\.mysms\\.(?:[a-z]+\\.)?([a-z]+))\\.controller\\.");

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

    /** Restores per operation what the Java signature cannot state, from the area's operation table. */
    @Bean
    public GlobalOperationCustomizer mysmsOperationCustomizer(Environment environment) {
        Map<String, Map<String, JsonNode>> tables = new ConcurrentHashMap<>();
        return (operation, handler) -> {
            Matcher controller = CONTROLLER_PACKAGE.matcher(handler.getBeanType().getName());
            if (!controller.find()) {
                return operation;
            }
            JsonNode row = tables.computeIfAbsent(areaOf(environment, controller.group(2)), OpenApiConfiguration::table)
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
     * The contract's types and required lists, in one pass so their order is fixed. First drops the {@code "null"}
     * springdoc adds to a {@code @Nullable} property's type: contract fields are {@code NON_NULL}, so an absent value
     * is left out, never sent as null (a {@code @Nullable Object} loses its {@code type: "null"} altogether and becomes
     * any JSON value). Then marks every contract DTO field that is not {@code @Nullable} as required (springdoc reads
     * only {@code @NotNull}) — except the {@code Optional} fields of a PATCH's {@code <Resource>Update} (§7.2): none of
     * them is required, and one whose type argument is neither {@code @NotNull} nor {@code @NotBlank} may be sent as
     * null to clear it, so its type keeps {@code "null"}. A schema with no contract class (paging, problems, enums)
     * keeps what springdoc wrote.
     */
    @Bean
    public GlobalOpenApiCustomizer mysmsContractSchemaCustomizer(ListableBeanFactory beans) {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().values().forEach(OpenApiConfiguration::anyValue);
            List<String> roots = documentedRoots(contractRoots(beans), openApi);
            openApi.getComponents().getSchemas().forEach((name, schema) -> contractClass(roots, name)
                    .ifPresent(type -> required(schema, type)));
        };
    }

    /**
     * The roots of the areas a document shows (an area's group shows only its {@code /v1/<area>} paths), so a schema
     * name a sibling area also uses still finds its own area's class; every root when the document has no paths.
     */
    static List<String> documentedRoots(List<String> roots, OpenAPI openApi) {
        Set<String> areas = new java.util.TreeSet<>();
        if (openApi.getPaths() != null) {
            openApi.getPaths().keySet().forEach(path -> {
                Matcher area = DOCUMENTED_AREA.matcher(path);
                if (area.find()) {
                    // the package segment of the path's area: hyphens out, like the underscores they stand for
                    areas.add(area.group(1).replace("-", ""));
                }
            });
        }
        if (areas.isEmpty()) {
            return roots;
        }
        return roots.stream().filter(root -> areas.contains(root.substring(root.lastIndexOf('.') + 1))).toList();
    }

    /** The root packages of the areas the process serves, read from their controllers. */
    static List<String> contractRoots(ListableBeanFactory beans) {
        Set<String> roots = new java.util.TreeSet<>();
        for (String bean : beans.getBeanNamesForAnnotation(RestController.class)) {
            Class<?> type = beans.getType(bean);
            Matcher controller = type == null ? null : CONTROLLER_PACKAGE.matcher(ClassUtils.getUserClass(type).getName());
            if (controller != null && controller.find()) {
                roots.add(controller.group(1));
            }
        }
        return List.copyOf(roots);
    }

    /**
     * The contract class a schema was written from, when exactly one area of the process has a class of that name;
     * with several the merged schema keeps what springdoc wrote.
     */
    static Optional<Class<?>> contractClass(List<String> roots, String schemaName) {
        List<Class<?>> found = new ArrayList<>();
        for (String root : roots) {
            String name = root + ".api.model." + schemaName;
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
        schema.getProperties().forEach((property, propertySchema) -> {
            Field field = ReflectionUtils.findField(type, property);
            if (field == null) {
                return;
            }
            if (field.getType() == Optional.class) {
                required.remove(property);
                if (clearable(field)) {
                    nullable(propertySchema);
                }
            } else if (!field.isAnnotationPresent(Nullable.class)) {
                required.add(property);
            }
        });
        schema.setRequired(required.isEmpty() ? null : List.copyOf(required));
    }

    /** An update field that JSON null may clear: its {@code Optional}'s type argument is not required. */
    private static boolean clearable(Field field) {
        return !(field.getAnnotatedType() instanceof AnnotatedParameterizedType optional)
                || !(optional.getAnnotatedActualTypeArguments()[0].isAnnotationPresent(NotNull.class)
                || optional.getAnnotatedActualTypeArguments()[0].isAnnotationPresent(NotBlank.class));
    }

    /**
     * Adds {@code "null"} to a property's types (and to its enum values, which would otherwise refuse it); a
     * {@code $ref} property becomes {@code anyOf: [$ref, null]}.
     */
    @SuppressWarnings("unchecked")
    private static void nullable(Schema<?> property) {
        if (property.getEnum() != null && !property.getEnum().contains(null)) {
            List<Object> values = new ArrayList<>(property.getEnum());
            values.add(null);
            ((Schema<Object>) property).setEnum(values);
        }
        if (property.get$ref() != null) {
            Schema<?> ref = new Schema<>().$ref(property.get$ref());
            property.set$ref(null);
            property.setAnyOf(new ArrayList<>(List.of(ref, new Schema<>().types(Set.of("null")))));
        } else if (property.getTypes() != null) {
            Set<String> types = new LinkedHashSet<>(property.getTypes());
            types.add("null");
            property.setTypes(types);
        }
    }

    /** The tag of an operation: the contract interface that declares it ({@code <area root>.api.<Tag>Api}). */
    private static Optional<String> contractTag(Class<?> controller, Method method, String root) {
        String contract = root + ".api.";
        for (Class<?> implemented : controller.getInterfaces()) {
            String name = implemented.getName();
            if (name.startsWith(contract) && name.endsWith("Api") && name.indexOf('.', contract.length()) < 0
                    && ReflectionUtils.findMethod(implemented, method.getName(), method.getParameterTypes()) != null) {
                return Optional.of(implemented.getSimpleName().substring(0, implemented.getSimpleName().length() - 3));
            }
        }
        return Optional.empty();
    }

    /** The area's operation rows by controller method name ({@code master.ListCountries} → {@code listCountries}). */
    /**
     * The area of a controller's package segment: the served area ({@code mysms.auth.modules}) whose name without
     * underscores is the segment; the segment itself when none matches (an area named like its package).
     */
    private static String areaOf(Environment environment, String segment) {
        for (int i = 0; environment.containsProperty("mysms.auth.modules[" + i + "]"); i++) {
            String area = environment.getProperty("mysms.auth.modules[" + i + "]");
            if (area != null && area.replace("_", "").equals(segment)) {
                return area;
            }
        }
        return segment;
    }

    private static Map<String, JsonNode> table(String area) {
        String resource = "META-INF/mysms/operations/" + area + ".json";
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
