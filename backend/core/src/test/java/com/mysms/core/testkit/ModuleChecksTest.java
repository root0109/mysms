package com.mysms.core.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleChecksTest {

    private static final String ARCHITECTURE = """
            | Module | May depend on |
            |---|---|
            | `master` | — |
            | `finance` | `academics` (3), `master` (6), `sis` (6) |
            """;

    @Test
    void findsSqlThatReachesAnotherSchema(@TempDir Path root) throws Exception {
        Path repo = Files.createDirectories(root.resolve("com/mysms/finance/repository"));
        Files.writeString(repo.resolve("InvoiceRepository.java"), """
                class InvoiceRepository {
                    static final String OWN = "SELECT i.id FROM finance.invoice i WHERE i.tenant_id = :t";
                    static final String JOIN = \"""
                            SELECT i.id FROM finance.invoice i
                            JOIN sis.student s ON s.id = i.student_id
                            \""";
                    static final String NOT_SQL = "org.springframework.jdbc";
                    static final String PROSE = "see sis.student for details";
                }
                """);
        assertThat(ModuleChecks.foreignSchemaReferences("finance", root)).containsExactly("InvoiceRepository.java: sis.student");
        assertThat(ModuleChecks.unqualifiedTableReferences(root)).isEmpty();
        assertThat(ModuleChecks.foreignSchemaReferences("finance", root.resolve("missing"))).isEmpty();
    }

    @Test
    void scansSqlResourcesAndLowercaseSqlAndFlagsUnqualifiedTables(@TempDir Path root) throws Exception {
        Path java = Files.createDirectories(root.resolve("src/main/java/com/mysms/finance"));
        Path sql = Files.createDirectories(root.resolve("src/main/resources/sql"));
        Files.writeString(java.resolve("Repo.java"), """
                class Repo {
                    static final String LOWER = "select i.id from finance.invoice i join sis.student s on true";
                    static final String BARE = "SELECT id FROM invoice WHERE x IN (SELECT 1 FROM unnest(:ids))";
                    static final String SUBQUERY = "SELECT count(*) FROM (SELECT 1) t";
                }
                """);
        Files.writeString(sql.resolve("report.sql"), "SELECT * FROM finance.invoice JOIN people.person p ON true");
        Path sources = root.resolve("src/main/java");
        assertThat(ModuleChecks.foreignSchemaReferences("finance", sources))
                .containsExactly("Repo.java: sis.student", "report.sql: people.person");
        assertThat(ModuleChecks.unqualifiedTableReferences(sources)).containsExactly("Repo.java: FROM invoice");
    }

    @Test
    void enforcesTheGrpcAllowlistFromArchitectureMd(@TempDir Path root) throws Exception {
        Path architecture = Files.writeString(root.resolve("ARCHITECTURE.md"), ARCHITECTURE);
        Path src = Files.createDirectories(root.resolve("src/com/mysms/finance/business"));
        Files.writeString(src.resolve("InvoiceService.java"), """
                package com.mysms.finance.business;
                import com.mysms.finance.v1.Invoice;
                import com.mysms.common.v1.PageableRequest;
                import com.mysms.sis.v1.StudentServiceGrpc;
                import com.mysms.lms.v1.CourseServiceGrpc;
                class InvoiceService {}
                """);
        assertThat(ModuleChecks.allowedDependencies("finance", architecture)).containsExactly("academics", "master", "sis");
        assertThat(ModuleChecks.allowedDependencies("master", architecture)).isEmpty();
        assertThat(ModuleChecks.grpcImportsOutsideAllowlist("finance", root.resolve("src"), architecture))
                .containsExactly("InvoiceService.java imports the gRPC contract of lms");
        assertThatThrownBy(() -> ModuleChecks.allowedDependencies("nope", architecture))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRealArchitectureTableCoversEveryModule() {
        Path architecture = Path.of("..", "..", "ARCHITECTURE.md");
        for (String module : ModuleChecks.MODULES) {
            assertThat(ModuleChecks.allowedDependencies(module, architecture)).doesNotContain(module)
                    .isSubsetOf(ModuleChecks.MODULES);
        }
    }
}
