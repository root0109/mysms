package com.mysms.payroll;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ModuleChecks;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** The module boundary rules as plain unit tests. */
class ModuleBoundaryTest {

    private static final Path SOURCES = Path.of("src/main/java");

    @Test
    void sqlReferencesOnlyTheOwnSchema() {
        assertThat(ModuleChecks.foreignSchemaReferences("payroll", SOURCES)).isEmpty();
    }

    @Test
    void sqlQualifiesEveryTable() {
        assertThat(ModuleChecks.unqualifiedTableReferences(SOURCES)).isEmpty();
    }

    @Test
    void grpcClientsStayWithinTheAllowedDependencies() {
        Path architecture = Path.of("..", "..", "ARCHITECTURE.md");
        assertThat(ModuleChecks.grpcImportsOutsideAllowlist("payroll", SOURCES, architecture)).isEmpty();
    }
}
