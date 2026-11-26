package com.mysms.conformance;

import com.mysms.conformance.rules.ApiLayerRule;
import com.mysms.conformance.rules.ConfigurationRule;
import com.mysms.conformance.rules.GrpcGuidelineRule;
import com.mysms.conformance.rules.MapperRule;
import com.mysms.conformance.rules.MavenLayoutRule;
import com.mysms.conformance.rules.ModuleBoundaryRule;
import com.mysms.conformance.rules.PermissionCatalogueRule;
import com.mysms.conformance.rules.PersistenceRule;
import com.mysms.conformance.rules.SecurityBoundaryRule;
import com.mysms.conformance.rules.SqlRule;
import com.mysms.conformance.rules.TechnologyRule;
import com.mysms.conformance.rules.TestKitRule;
import java.util.List;

/**
 * The mechanical coding rules that the compiler, Checkstyle and SpotBugs do not already enforce. The rule ids:
 * <pre>
 * M0   backend/ holds only the Maven project                           MavenLayoutRule
 * R1   tenant predicate on every tenant table a statement touches        SqlRule
 * R2   SQL lives in repositories, as constants                           SqlRule
 * §6   no hard DELETE except pruning module-local infrastructure          SqlRule
 * B1   no Transactional/TransactionTemplate, ORM, ArchUnit, Modulith     TechnologyRule
 * T    mocks only in unit tests; test kit of every module                TechnologyRule, TestKitRule
 * A1   module dependencies and messaging imports                         ModuleBoundaryRule
 * §3   packages are the layer roles                                       ModuleBoundaryRule
 * §14  migrations touch only their own schema                             ModuleBoundaryRule
 * §5   entities extend BaseEntity and name their TABLE                   PersistenceRule
 * N1   entity shape, audit set, lifecycle hooks in business/ only        PersistenceRule
 * R5   plain repositories, constant SQL, keyset paging, no DB functions  PersistenceRule
 * C3   controllers implement the contract, no side effects               ApiLayerRule
 * D2   no hand-written DTOs or response shaping; delete answers 204       ApiLayerRule
 * E2   core's exceptions and handler only, no message leaks              ApiLayerRule
 * M1   one MapperConfig, stateless mappers, audit set ignored            MapperRule
 * X1   tokens verified only in auth-client; passwords hashed only in iam ApiLayerRule, SecurityBoundaryRule
 * P1   every tenant permission is seeded in iam's catalogue               PermissionCatalogueRule
 * G1   protos owned by their area; callee copies byte-identical           GrpcGuidelineRule
 * G2   field numbers match conformance/src/main/resources/field-numbers.lock.json
 * G3   operation table = the area's RPCs
 * G4   other modules' gRPC stubs only in an integration package
 * G5   every gRPC server implementation is a @GrpcService
 * G6   no REST client between modules
 * G7   no Transactions helper (AppTransactionManager only)
 * G8   every area's OpenApiComponentTest writes target/openapi/<area>/api-docs.yml
 * G9   every module's application.properties; the monolith's combines them ConfigurationRule
 * </pre>
 */
public final class Conformance {

    private final List<ConformanceRule> rules = List.of(new MavenLayoutRule(), new SqlRule(), new TechnologyRule(),
            new ModuleBoundaryRule(), new PersistenceRule(), new ApiLayerRule(), new MapperRule(),
            new SecurityBoundaryRule(), new TestKitRule(), new PermissionCatalogueRule(), new GrpcGuidelineRule(),
            new ConfigurationRule());

    /** Every violation in the tree, rule by rule, in a stable order. */
    public List<Violation> check(SourceTree tree) {
        return rules.stream().flatMap(rule -> rule.check(tree).stream()).toList();
    }
}
