package com.mysms.conformance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the tenant tables a SQL statement reads or changes without filtering them by {@code tenant_id = :tenantId} at
 * the statement's own level (JAVA-CODING-RULES §1.6, rule R1). A predicate counts only where it filters the table: in
 * the WHERE of the statement or sub-select that reads the base table, or in the ON of a joined tenant table — not
 * inside a deeper sub-select and not in one branch of an OR. Statements the check cannot place a predicate in
 * (compound statements, comma joins, {@code DELETE … USING}, {@code UPDATE … FROM}, a tenant table read without a
 * WHERE) are refused as {@link Unsupported}, so they are written differently instead of slipping through.
 *
 * <p>A single forward pass over the tokens with one frame per parenthesis level, which is all the SQL in MySMS's
 * repositories needs; it is not a general SQL parser.
 */
public final class TenantPredicates {

    private static final Pattern TOKEN = Pattern.compile(
            "'(?:[^']|'')*'|\\$\\$.*?\\$\\$|--[^\\n]*|[A-Za-z_][\\w.]*|:\\w+|::|\\S", Pattern.DOTALL);
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");
    private static final Set<String> STOP = Set.of("WHERE", "SET", "JOIN", "LEFT", "RIGHT", "INNER", "FULL", "CROSS",
            "ON", "ORDER", "GROUP", "LIMIT", "OFFSET", "FOR", "AS", "USING", "RETURNING", "UNION", "WINDOW", "HAVING",
            "EXCEPT", "INTERSECT", "VALUES", "SELECT", "NATURAL", "LATERAL", "TABLESAMPLE", "AND", "OR", "NOT");
    private static final Set<String> CLAUSE_END = Set.of("ORDER", "GROUP", "LIMIT", "OFFSET", "FOR", "RETURNING",
            "HAVING", "UNION", "WINDOW", "EXCEPT", "INTERSECT");
    private static final Set<String> CONDITION_END = Set.of("ORDER", "GROUP", "LIMIT", "OFFSET", "FOR", "RETURNING",
            "HAVING", "UNION", "WINDOW", "EXCEPT", "INTERSECT", "JOIN", "WHERE", "LEFT", "INNER");

    private final Set<String> tenantTables;

    /** @param tenantTables every {@code schema.table} with a tenant column, lower case */
    public TenantPredicates(Set<String> tenantTables) {
        this.tenantTables = Set.copyOf(tenantTables);
    }

    /** A statement the check cannot reason about; the message says how to write it instead. */
    public static final class Unsupported extends RuntimeException {

        private static final long serialVersionUID = 1L;

        Unsupported(String message) {
            super(message);
        }
    }

    /**
     * The tenant tables of the statement that miss their own predicate, in statement order; empty when every one is
     * filtered.
     *
     * @throws Unsupported when the statement's shape cannot be checked
     */
    public List<String> missing(String sql) {
        return new Pass(tokens(sql)).run();
    }

    private static List<String> tokens(String sql) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(sql);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    /** The table a FROM, UPDATE or JOIN names, with its alias and the index after them. */
    private record Named(String table, String alias, int next) {
    }

    /** One parenthesis level: its base table (FROM or UPDATE), how many joins came before its WHERE, its verb. */
    private static final class Frame {
        private Named base;
        private boolean baseFiltered;
        private int joins;
        private String verb;

        private boolean pending() {
            return base != null && !baseFiltered;
        }
    }

    /** The scan of one statement. */
    private final class Pass {
        private final List<String> tokens;
        private final Deque<Frame> frames = new ArrayDeque<>();
        private final List<String> missing = new ArrayList<>();

        private Pass(List<String> tokens) {
            this.tokens = tokens;
            frames.push(new Frame());
        }

        private List<String> run() {
            int i = 0;
            while (i < tokens.size()) {
                i = step(i);
            }
            while (!frames.isEmpty()) {
                close(frames.pop());
            }
            return List.copyOf(missing);
        }

        /** Reads token i and returns the index of the next token to read. */
        private int step(int i) {
            String token = tokens.get(i);
            String word = token.toUpperCase(Locale.ROOT);
            Frame frame = frames.peek();
            refuseUncheckable(word, frame);
            if (frame.verb == null && Set.of("DELETE", "UPDATE", "SELECT").contains(word)) {
                frame.verb = word;
            }
            if ("(".equals(token)) {
                frames.push(new Frame());
            } else if (")".equals(token)) {
                if (frames.size() == 1) {
                    throw new Unsupported("unbalanced \")\" in a SQL fragment; check the assembled statement instead");
                }
                close(frames.pop());
            } else if (("FROM".equals(word) || "UPDATE".equals(word)) && frame.base == null
                    && !("FROM".equals(word) && "DISTINCT".equals(word(i - 1)))) {
                Named named = tableAt(i + 1);
                if (named != null) {
                    frame.base = named;
                    if (named.next() < tokens.size() && ",".equals(tokens.get(named.next()))) {
                        throw new Unsupported("a comma join after " + named.table() + "; write it as JOIN ... ON");
                    }
                    return named.next();
                }
            } else if ("JOIN".equals(word)) {
                Named named = tableAt(i + 1);
                if (named != null) {
                    int on = named.next();
                    if ("ON".equals(word(on))) {
                        String reference = named.alias() != null ? named.alias() : unqualified(named.table());
                        if (!hasOwnPredicate(on + 1, conditionEnd(on + 1), reference)) {
                            missing.add(named.table());
                        }
                    }
                    frame.joins++;
                    return on;
                }
            } else if ("WHERE".equals(word) && frame.pending()) {
                frame.baseFiltered = true;
                if (!hasOwnPredicate(i + 1, conditionEnd(i + 1), reference(frame))) {
                    missing.add(frame.base.table());
                }
            } else if (CLAUSE_END.contains(word) && frame.pending()) {
                close(frame);
            }
            return i + 1;
        }

        private void refuseUncheckable(String word, Frame frame) {
            if (Set.of("UNION", "INTERSECT", "EXCEPT").contains(word)) {
                throw new Unsupported("a compound statement (" + word + "); check each part on its own");
            }
            if ("USING".equals(word) && frame.pending() && "DELETE".equals(frame.verb)) {
                throw new Unsupported("DELETE ... USING; filter every table in a sub-select instead");
            }
            if ("FROM".equals(word) && frame.pending() && "UPDATE".equals(frame.verb)) {
                throw new Unsupported("UPDATE ... FROM; filter every table in a sub-select instead");
            }
        }

        private void close(Frame frame) {
            if (frame.pending()) {
                frame.baseFiltered = true;
                throw new Unsupported("a statement over " + frame.base.table() + " has no WHERE in this text");
            }
        }

        private String word(int k) {
            return k >= 0 && k < tokens.size() ? tokens.get(k).toUpperCase(Locale.ROOT) : "";
        }

        private Named tableAt(int k) {
            if (k >= tokens.size() || !tenantTables.contains(tokens.get(k).toLowerCase(Locale.ROOT))) {
                return null;
            }
            String table = tokens.get(k).toLowerCase(Locale.ROOT);
            int j = k + 1;
            if ("AS".equals(word(j))) {
                j++;
            }
            String alias = null;
            if (j < tokens.size() && IDENTIFIER.matcher(tokens.get(j)).matches() && !STOP.contains(word(j))) {
                alias = tokens.get(j);
                j++;
            }
            return new Named(table, alias, j);
        }

        /** The index where the condition starting at k ends: a closing parenthesis or a clause at its own depth. */
        private int conditionEnd(int k) {
            int depth = 0;
            for (int j = k; j < tokens.size(); j++) {
                String word = word(j);
                if ("(".equals(word)) {
                    depth++;
                } else if (")".equals(word)) {
                    if (depth == 0) {
                        return j;
                    }
                    depth--;
                } else if (depth == 0 && CONDITION_END.contains(word) && j > k) {
                    return j;
                }
            }
            return tokens.size();
        }

        /**
         * Whether tokens k..end state {@code [reference.]tenant_id = :tenantId} at their own depth and outside any OR
         * (a predicate inside a sub-select or an OR branch does not filter this table).
         */
        private boolean hasOwnPredicate(int k, int end, String reference) {
            int depth = 0;
            StringBuilder level = new StringBuilder();
            for (int j = k; j < Math.min(end, tokens.size()); j++) {
                String token = tokens.get(j);
                if ("(".equals(token)) {
                    depth++;
                } else if (")".equals(token)) {
                    depth--;
                } else if (depth == 0) {
                    if ("OR".equals(token.toUpperCase(Locale.ROOT))) {
                        return false;
                    }
                    level.append(' ').append(token);
                }
            }
            String qualifier = reference.isEmpty() ? "" : Pattern.quote(reference) + "\\s*\\.\\s*";
            return Pattern.compile("(^|\\s)" + qualifier + "tenant_id\\s*=\\s*:\\s*tenantId").matcher(level).find();
        }

        /** How the base table is named in its WHERE: its alias, its bare name once joins exist, else unqualified. */
        private String reference(Frame frame) {
            if (frame.base.alias() != null) {
                return frame.base.alias();
            }
            return frame.joins > 0 ? unqualified(frame.base.table()) : "";
        }

        private String unqualified(String table) {
            return table.substring(table.indexOf('.') + 1);
        }
    }
}
