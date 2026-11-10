package com.mysms.conformance;

/**
 * One broken rule: the rule's id (as listed in {@link Conformance}), the repository-relative place and what is wrong.
 *
 * @param rule    the rule id, for example {@code R1}
 * @param where   path relative to the repository root
 * @param problem what is wrong, in one line
 */
public record Violation(String rule, String where, String problem) {

    @Override
    public String toString() {
        return String.format("%-4s %s: %s", rule, where, problem);
    }
}
