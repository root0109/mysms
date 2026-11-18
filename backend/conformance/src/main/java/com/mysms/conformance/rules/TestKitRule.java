package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * T: every domain module checks its own boundaries (ModuleBoundaryTest), has a service component test, and its
 * component tests sign real tokens (TestTokens) instead of bypassing authentication.
 */
public final class TestKitRule implements ConformanceRule {

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        for (String module : tree.domainModules()) {
            String where = tree.relative(tree.backend().resolve(module));
            List<Path> unit = tree.java(module, "test");
            List<Path> component = tree.java(module, "component-test");
            if (unit.stream().noneMatch(f -> SourceTree.fileName(f).equals("ModuleBoundaryTest.java"))) {
                found.add(new Violation("T", where, "no ModuleBoundaryTest"));
            }
            if (component.stream().noneMatch(f -> SourceTree.fileName(f).endsWith("ServiceComponentTest.java"))) {
                found.add(new Violation("T", where, "no service component test"));
            }
            if (component.stream().noneMatch(f -> tree.read(f).contains("TestTokens"))) {
                found.add(new Violation("T", where, "component tests sign no real tokens (TestTokens)"));
            }
        }
        return found;
    }
}
