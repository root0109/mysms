package com.mysms.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** The repository conforms: this test is how the rules run in every build (Maven runs it from this module's folder). */
class ConformanceTest {

    @Test
    void theRepositoryConformsToTheWrittenRules() {
        SourceTree tree = new SourceTree(Path.of("").toAbsolutePath().getParent().getParent());

        List<Violation> violations = new Conformance().check(tree);

        assertThat(tree.domainModules()).hasSize(22);
        assertThat(tree.tenantTables()).contains("sis.student", "iam.user_account").doesNotContain("master.country");
        assertThat(violations)
                .as("%d violation(s):%n%s", violations.size(),
                        violations.stream().map(Violation::toString).collect(Collectors.joining("\n")))
                .isEmpty();
    }
}
