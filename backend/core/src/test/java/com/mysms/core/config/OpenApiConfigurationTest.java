package com.mysms.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.other.controller.OtherController;
import com.mysms.sample.controller.SampleController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

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

        new OpenApiConfiguration().mysmsContractSchemaCustomizer(new StaticListableBeanFactory()).customise(document);

        assertThat(result.getTypes()).isNull();
        assertThat(item.getTypes()).isNull();
        assertThat(value.getTypes()).isNull();
        assertThat(name.getTypes()).as("never sent as null").containsExactly("string");
        assertThat(operation.getTypes()).containsExactly("object");
    }

    @Test
    void aDocumentWithoutSchemasIsLeftAsItIs() {
        OpenAPI document = new OpenAPI();
        new OpenApiConfiguration().mysmsContractSchemaCustomizer(new StaticListableBeanFactory()).customise(document);
        assertThat(document.getComponents()).isNull();
        new OpenApiConfiguration().mysmsContractSchemaCustomizer(new StaticListableBeanFactory())
                .customise(document.components(new Components()));
        assertThat(document.getComponents().getSchemas()).isNull();
    }

    @Test
    void aContractFieldThatIsNotNullableIsRequired() {
        StaticListableBeanFactory controllers = new StaticListableBeanFactory(Map.of("sample", new SampleController()));
        Schema<?> sample = new JsonSchema().types(Set.of("object")).addProperty("name", new JsonSchema())
                .addProperty("note", new JsonSchema()).addProperty("computed", new JsonSchema());
        Schema<?> paging = new JsonSchema().types(Set.of("object")).addProperty("data", new JsonSchema());
        OpenAPI document = new OpenAPI().components(new Components().addSchemas("Sample", sample)
                .addSchemas("PagingInfoSample", paging));

        new OpenApiConfiguration().mysmsContractSchemaCustomizer(controllers).customise(document);

        assertThat(sample.getRequired()).as("name is not @Nullable; note is; computed has no field")
                .isEqualTo(List.of("name"));
        assertThat(paging.getRequired()).as("no contract class: left as springdoc wrote it").isNull();
    }

    @Test
    void noFieldOfAnUpdateIsRequiredAndAClearableOneMayBeNull() {
        StaticListableBeanFactory controllers = new StaticListableBeanFactory(Map.of("sample", new SampleController()));
        Schema<?> name = new JsonSchema().types(Set.of("string", "null"));
        JsonSchema note = new JsonSchema();
        note.setTypes(Set.of("string", "null"));
        note.setEnum(new java.util.ArrayList<>(List.of("a", "b")));
        Schema<?> parent = new JsonSchema().$ref("#/components/schemas/Sample");
        Schema<?> update = new JsonSchema().types(Set.of("object")).addProperty("name", name).addProperty("note", note)
                .addProperty("parent", parent);
        update.setRequired(List.of("name"));
        OpenAPI document = new OpenAPI().components(new Components().addSchemas("SampleUpdate", update));

        new OpenApiConfiguration().mysmsContractSchemaCustomizer(controllers).customise(document);

        assertThat(update.getRequired()).as("absent leaves a field unchanged: none is required").isNull();
        assertThat(name.getTypes()).as("@NotNull: may change, never be cleared").containsExactly("string");
        assertThat(note.getTypes()).as("JSON null clears it").containsExactly("string", "null");
        assertThat(note.getEnum()).as("an enum admits the null that clears it").containsExactly("a", "b", null);
        assertThat(parent.get$ref()).isNull();
        assertThat(parent.getAnyOf()).extracting(schema -> schema.get$ref()).containsExactly("#/components/schemas/Sample", null);
    }

    @Test
    void aSchemaNamedByClassesOfTwoAreasKeepsWhatSpringdocWrote() {
        StaticListableBeanFactory controllers = new StaticListableBeanFactory(
                Map.of("sample", new SampleController(), "other", new OtherController(), "plain", new Object()));
        Schema<?> sample = new JsonSchema().types(Set.of("object")).addProperty("name", new JsonSchema())
                .addProperty("note", new JsonSchema());
        sample.setRequired(List.of("name"));
        OpenAPI document = new OpenAPI().components(new Components().addSchemas("Sample", sample));

        new OpenApiConfiguration().mysmsContractSchemaCustomizer(controllers).customise(document);

        assertThat(sample.getRequired()).as("sample requires name, other requires note: neither decides")
                .isEqualTo(List.of("name"));
        assertThat(OpenApiConfiguration.contractRoots(controllers)).containsExactly("com.mysms.other", "com.mysms.sample");
        assertThat(OpenApiConfiguration.contractClass(List.of("com.mysms.sample", "com.mysms.other"), "Sample")).isEmpty();
        assertThat(OpenApiConfiguration.contractClass(List.of("com.mysms.other", "com.mysms.absent"), "Sample"))
                .contains(com.mysms.other.api.model.Sample.class);
        assertThat(OpenApiConfiguration.contractClass(List.of("com.mysms.sample"), "Sample"))
                .contains(com.mysms.sample.api.model.Sample.class);
    }

    @Test
    void anAreasDocumentFindsItsOwnAreasClassWhenASiblingAreaHasOneOfTheSameName() {
        List<String> roots = List.of("com.mysms.other", "com.mysms.sample");
        OpenAPI sampleOnly = new OpenAPI().paths(new io.swagger.v3.oas.models.Paths()
                .addPathItem("/v1/sample/samples", new io.swagger.v3.oas.models.PathItem()));
        assertThat(OpenApiConfiguration.documentedRoots(roots, sampleOnly)).containsExactly("com.mysms.sample");
        assertThat(OpenApiConfiguration.contractClass(OpenApiConfiguration.documentedRoots(roots, sampleOnly), "Sample"))
                .contains(com.mysms.sample.api.model.Sample.class);
        assertThat(OpenApiConfiguration.documentedRoots(roots, new OpenAPI())).isEqualTo(roots);
    }

    private static Schema<?> nullOnly() {
        return new JsonSchema().types(Set.of("null"));
    }
}
