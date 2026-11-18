package com.mysms.conformance;

import java.util.List;

/**
 * One family of rules over the repository (Strategy): {@link Conformance} runs every rule over the same
 * {@link SourceTree}, so adding a rule never touches the others.
 */
public interface ConformanceRule {

    /** Every violation of this rule family in the tree; empty when the tree conforms. */
    List<Violation> check(SourceTree tree);
}
