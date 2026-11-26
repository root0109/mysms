package com.mysms.core.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
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
        assertThat(ModuleChecks.foreignSchemaReferences(Set.of("finance"), root))
                .containsExactly("InvoiceRepository.java: sis.student");
        assertThat(ModuleChecks.unqualifiedTableReferences(root)).isEmpty();
        assertThat(ModuleChecks.foreignSchemaReferences(Set.of("finance"), root.resolve("missing"))).isEmpty();
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
        assertThat(ModuleChecks.foreignSchemaReferences(Set.of("finance"), sources))
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
        assertThat(ModuleChecks.grpcImportsOutsideAllowlist(Set.of("finance"), root.resolve("src"), architecture))
                .containsExactly("InvoiceService.java imports the gRPC contract of lms");
        assertThatThrownBy(() -> ModuleChecks.allowedDependencies("nope", architecture))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aModuleOfAreasMayReachItsSiblingsSchemasAndContracts(@TempDir Path root) throws Exception {
        Path architecture = Files.writeString(root.resolve("ARCHITECTURE.md"), """
                | Module | May depend on |
                |---|---|
                | `hr` | `iam` (2) |
                | `payroll` | `hr` (4) |
                """);
        Path pom = Files.writeString(root.resolve("pom.xml"), """
                <resource><directory>${mysms.migrations.dir}/hr</directory></resource>
                <resource><directory>${mysms.migrations.dir}/payroll</directory></resource>
                """);
        Path src = Files.createDirectories(root.resolve("src/com/mysms/hrms/payroll/repository"));
        Files.writeString(src.resolve("PayslipRepository.java"), """
                package com.mysms.hrms.payroll.repository;
                import com.mysms.iam.v1.UserAccountServiceGrpc;
                import com.mysms.sis.v1.StudentServiceGrpc;
                class PayslipRepository {
                    static final String SQL = "SELECT p.id FROM payroll.payslip p JOIN hr.staff s ON true"
                            + " JOIN sis.student t ON true";
                }
                """);
        Set<String> areas = ModuleChecks.moduleSchemas(pom);
        assertThat(areas).containsExactly("hr", "payroll");
        assertThat(ModuleChecks.foreignSchemaReferences(areas, root.resolve("src")))
                .containsExactly("PayslipRepository.java: sis.student");
        assertThat(ModuleChecks.grpcImportsOutsideAllowlist(areas, root.resolve("src"), architecture))
                .containsExactly("PayslipRepository.java imports the gRPC contract of sis");
        Path empty = Files.writeString(root.resolve("empty.xml"), "<project/>");
        assertThatThrownBy(() -> ModuleChecks.moduleSchemas(empty)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRealArchitectureTableCoversEveryArea() {
        Path architecture = Path.of("..", "..", "ARCHITECTURE.md");
        for (String area : ModuleChecks.SCHEMAS) {
            assertThat(ModuleChecks.allowedDependencies(area, architecture)).doesNotContain(area)
                    .isSubsetOf(ModuleChecks.SCHEMAS);
        }
    }
}
