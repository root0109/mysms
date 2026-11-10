package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * What the code may build on, in every module and every source root:
 * <ul>
 * <li>B1 — no {@code Transactional} annotation of any package and no {@code TransactionTemplate} (transactions are
 * programmatic on {@code PlatformTransactionManager}); no ORM import or dependency (JPA, Hibernate, Spring Data,
 * MyBatis, jOOQ); no ArchUnit or Modulith.</li>
 * <li>T — mocks and stub servers (Mockito, {@code @MockBean}, {@code @MockitoBean}, spies, WireMock,
 * MockWebServer, MockRestServiceServer) only in unit tests under {@code src/test}; no Spring context
 * ({@code @SpringBootTest}) in a unit test — component tests live in {@code src/component-test}.</li>
 * </ul>
 */
public final class TechnologyRule implements ConformanceRule {

    private static final Pattern COMMENTS = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Pattern TRANSACTIONAL = Pattern.compile("\\bTransactional\\b|\\bTransactionTemplate\\b");
    private static final Pattern ORM_IMPORT = Pattern.compile(
            "import (jakarta\\.persistence|javax\\.persistence|org\\.hibernate|org\\.springframework\\.data)\\.");
    private static final Pattern ORM_ARTIFACT = Pattern.compile("<artifactId>(hibernate[\\w-]*|spring-boot-starter-"
            + "data-jpa|spring-data-[\\w-]+|jakarta\\.persistence-api|mybatis[\\w-]*|jooq[\\w-]*)</artifactId>");
    private static final Pattern ARCHITECTURE_TOOLS =
            Pattern.compile("import com\\.tngtech\\.archunit|import org\\.springframework\\.modulith");
    private static final Pattern MOCKS = Pattern.compile("org\\.mockito|@MockBean\\b|@MockitoBean\\b|@SpyBean\\b"
            + "|@MockitoSpyBean\\b|com\\.github\\.tomakehurst\\.wiremock|org\\.wiremock|okhttp3\\.mockwebserver"
            + "|MockRestServiceServer");
    private static final Pattern SPRING_CONTEXT = Pattern.compile("@(?:[\\w.]+\\.)?SpringBootTest\\b");

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        for (String module : tree.productModules()) {
            Path pom = tree.backend().resolve(module).resolve("pom.xml");
            if (ORM_ARTIFACT.matcher(tree.read(pom)).find()) {
                found.add(new Violation("B1", tree.relative(pom), "ORM dependency"));
            }
            for (String root : List.of("main", "test", "component-test")) {
                for (Path file : tree.java(module, root)) {
                    String code = COMMENTS.matcher(tree.read(file)).replaceAll("");
                    if (TRANSACTIONAL.matcher(code).find()) {
                        found.add(new Violation("B1", tree.relative(file), "@Transactional / TransactionTemplate"));
                    }
                    if (ORM_IMPORT.matcher(code).find()) {
                        found.add(new Violation("B1", tree.relative(file), "ORM import"));
                    }
                    if (ARCHITECTURE_TOOLS.matcher(code).find()) {
                        found.add(new Violation("B1", tree.relative(file), "ArchUnit / Modulith"));
                    }
                    if (!"test".equals(root) && MOCKS.matcher(code).find()) {
                        found.add(new Violation("T", tree.relative(file), "mock or stub outside a unit test"));
                    }
                    if ("test".equals(root) && SPRING_CONTEXT.matcher(code).find()) {
                        found.add(new Violation("T", tree.relative(file),
                                "Spring context in a unit test (component tests live in src/component-test)"));
                    }
                }
            }
        }
        return found;
    }
}
