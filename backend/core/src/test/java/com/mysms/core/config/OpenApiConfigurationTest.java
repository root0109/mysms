package com.mysms.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * The document keeps the contract's types: a property of any value keeps no type (a client generator does not invent a
 * {@code Null}), a nullable one keeps its own, and what the contract DTO requires is required.
 */
class OpenApiConfigurationTest {

    @Test
    void aNullOnlyPropertyBecomesAnyValueAndANullableOneKeepsItsOwnType() {
        Schema<?> result = nullOnly();
        Schema<?> item = nullOnly();
        Schema<?> value = nullOnly();
        Schema<?> name = new JsonSchema().types(Set.of("string", "null"));
        Schema<?> operation = new JsonSchema().types(Set.of("object")).addProperty("result", result)
                .addProperty("name", name).addProperty("history", new ArraySchema().items(item))
                .additionalProperties(value);
        OpenAPI document = new OpenAPI().components(new Components().addSchemas("Operation", operation));

        new OpenApiConfiguration().mysmsAnyValueCustomizer().customise(document);

        assertThat(result.getTypes()).isNull();
        assertThat(item.getTypes()).isNull();
        assertThat(value.getTypes()).isNull();
        assertThat(name.getTypes()).as("never sent as null").containsExactly("string");
        assertThat(operation.getTypes()).containsExactly("object");
    }

    @Test
    void aDocumentWithoutSchemasIsLeftAsItIs() {
        OpenAPI document = new OpenAPI();
        new OpenApiConfiguration().mysmsAnyValueCustomizer().customise(document);
        assertThat(document.getComponents()).isNull();
        new OpenApiConfiguration().mysmsAnyValueCustomizer().customise(document.components(new Components()));
        assertThat(document.getComponents().getSchemas()).isNull();
    }

    @Test
    void aContractFieldThatIsNotNullableIsRequired() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("mysms.auth.modules[0]", "sample")));
        Schema<?> sample = new JsonSchema().types(Set.of("object")).addProperty("name", new JsonSchema())
                .addProperty("note", new JsonSchema()).addProperty("computed", new JsonSchema());
        Schema<?> paging = new JsonSchema().types(Set.of("object")).addProperty("data", new JsonSchema());
        OpenAPI document = new OpenAPI().components(new Components().addSchemas("Sample", sample)
                .addSchemas("PagingInfoSample", paging));

        new OpenApiConfiguration().mysmsRequiredCustomizer(environment).customise(document);

        assertThat(sample.getRequired()).as("name is not @Nullable; note is; computed has no field")
                .isEqualTo(List.of("name"));
        assertThat(paging.getRequired()).as("no contract class: left as springdoc wrote it").isNull();
    }

    @Test
    void aSchemaNamedByClassesOfTwoModulesKeepsWhatSpringdocWrote() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("mysms.auth.modules[0]",
                "sample", "mysms.auth.modules[1]", "other", "mysms.auth.modules[2]", "absent")));
        Schema<?> sample = new JsonSchema().types(Set.of("object")).addProperty("name", new JsonSchema())
                .addProperty("note", new JsonSchema());
        sample.setRequired(List.of("name"));
        OpenAPI document = new OpenAPI().components(new Components().addSchemas("Sample", sample));

        new OpenApiConfiguration().mysmsRequiredCustomizer(environment).customise(document);

        assertThat(sample.getRequired()).as("sample requires name, other requires note: neither decides")
                .isEqualTo(List.of("name"));
        assertThat(OpenApiConfiguration.contractClass(List.of("sample", "other"), "Sample")).isEmpty();
        assertThat(OpenApiConfiguration.contractClass(List.of("other", "absent"), "Sample"))
                .contains(com.mysms.other.api.model.Sample.class);
        assertThat(OpenApiConfiguration.contractClass(List.of("sample"), "Sample"))
                .contains(com.mysms.sample.api.model.Sample.class);
    }

    private static Schema<?> nullOnly() {
        return new JsonSchema().types(Set.of("null"));
    }
}
