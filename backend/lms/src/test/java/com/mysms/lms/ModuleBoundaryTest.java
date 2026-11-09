package com.mysms.lms;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ModuleChecks;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The module boundary rules as plain unit tests (JAVA-CODING-RULES §2, review checks A1/R3). */
class ModuleBoundaryTest {

    private static final Path SOURCES = Path.of("src/main/java");

    @Test
    void sqlReferencesOnlyTheOwnSchema() {
        assertThat(ModuleChecks.foreignSchemaReferences("lms", SOURCES)).isEmpty();
    }

    @Test
    void sqlQualifiesEveryTable() {
        assertThat(ModuleChecks.unqualifiedTableReferences(SOURCES)).isEmpty();
    }

    @Test
    void sdkClientsStayWithinTheAllowedDependencies() {
        Path architecture = Path.of("..", "..", "ARCHITECTURE.md");
        assertThat(ModuleChecks.sdkImportsOutsideAllowlist("lms", SOURCES, architecture)).isEmpty();
    }
}
