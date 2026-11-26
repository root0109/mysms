package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ModuleChecks;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The module boundary rules as plain unit tests, over every area of the module. */
class ModuleBoundaryTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Set<String> AREAS = ModuleChecks.moduleSchemas(Path.of("pom.xml"));

    @Test
    void sqlReferencesOnlyTheModulesOwnSchemas() {
        assertThat(ModuleChecks.foreignSchemaReferences(AREAS, SOURCES)).isEmpty();
    }

    @Test
    void sqlQualifiesEveryTable() {
        assertThat(ModuleChecks.unqualifiedTableReferences(SOURCES)).isEmpty();
    }

    @Test
    void grpcClientsStayWithinTheAllowedDependencies() {
        Path architecture = Path.of("..", "..", "ARCHITECTURE.md");
        assertThat(ModuleChecks.grpcImportsOutsideAllowlist(AREAS, SOURCES, architecture)).isEmpty();
    }
}
