package com.mysms.core.filter;

import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The reviewed criteria builder for `filter` (api/README "Filtering and ordering"; JAVA-CODING-RULES §6): the AIP-160
 * subset `= != < <= > >=`, `:` (contains, strings), AND, OR, NOT, parentheses, quoted strings, numbers, true/false and
 * null, over an allowlist of fields. Produces SQL whose columns come only from the allowlist and whose values are
 * all bind parameters. Anything else is rejected with 400 INVALID_FILTER.
 */
public final class FilterParser {

    public static final int MAX_LENGTH = 2000;
    private static final int MAX_TERMS = 50;
    private static final int MAX_DEPTH = 10;

    private enum Kind { IDENT, STRING, NUMBER, OP, LPAREN, RPAREN, END }

    private record Token(Kind kind, String text) {
    }

    private final Map<String, FieldSpec> fields;
    private final List<Token> tokens;
    private final Map<String, Object> params = new LinkedHashMap<>();
    private int position;
    private int terms;
    private int depth;

    private FilterParser(Map<String, FieldSpec> fields, List<Token> tokens) {
        this.fields = fields;
        this.tokens = tokens;
    }

    /** SQL for the filter, or {@link SqlFragment#TRUE} for a blank filter. */
    public static SqlFragment parse(String filter, Map<String, FieldSpec> fields) {
        if (filter == null || filter.isBlank()) {
            return SqlFragment.TRUE;
        }
        if (filter.length() > MAX_LENGTH) {
            throw invalid("filter is longer than " + MAX_LENGTH + " characters.");
        }
        FilterParser parser = new FilterParser(fields, tokenize(filter));
        String sql = parser.orExpression();
        if (parser.peek().kind() != Kind.END) {
            throw invalid("unexpected '" + parser.peek().text() + "' in filter.");
        }
        return new SqlFragment(sql, parser.params);
    }

    /** The longest time range a list over a partitioned table may ask for (twelve monthly partitions and a bit). */
    public static final int MAX_RANGE_DAYS = 366;

    /**
     * Checks that a filter bounds a partitioned table's partition key on both sides, so the database reads only the
     * partitions of that range (database/README.md "Partitioned high-volume tables"). Accepted: top-level
     * {@code AND}-ed conditions {@code key >= a} (or {@code >}) and {@code key <= b} (or {@code <}), or {@code key = a},
     * spanning at most {@link #MAX_RANGE_DAYS} days. A bound inside {@code OR} or {@code NOT} does not count.
     * Call it before {@link #parse}; it answers 400 INVALID_FILTER, naming the field, when the range is missing.
     */
    public static void requireRange(String filter, Map<String, FieldSpec> fields, String key) {
        FieldSpec field = fields.get(key);
        if (field == null) {
            throw new IllegalArgumentException("no filter field " + key);
        }
        String example = "for example " + key + " >= \"2026-01-01\" AND " + key + " < \"2026-02-01\"";
        if (filter == null || filter.isBlank()) {
            throw invalid("this list needs a range on " + key + " (" + example + ").");
        }
        parse(filter, fields); // reports any other mistake first
        List<Token> tokens = tokenize(filter);
        Object lower = null;
        Object upper = null;
        boolean exclusive = false;
        int level = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.kind() == Kind.LPAREN || token.kind() == Kind.RPAREN) {
                level += token.kind() == Kind.LPAREN ? 1 : -1;
            } else if (level == 0 && token.kind() == Kind.IDENT && "OR".equals(token.text())) {
                throw invalid("the range on " + key + " cannot be combined with OR at the top level; put the "
                        + "alternatives in parentheses (" + example + ").");
            } else if (level == 0 && token.kind() == Kind.IDENT && key.equals(token.text())
                    && (i == 0 || !"NOT".equals(tokens.get(i - 1).text()))
                    && tokens.get(i + 2).kind() != Kind.IDENT) {
                Object value = field.type().parse(tokens.get(i + 2).text());
                switch (tokens.get(i + 1).text()) {
                    case ">=", ">" -> lower = later(lower, value);
                    case "<=", "<" -> {
                        Object before = upper;
                        upper = earlier(upper, value);
                        if (upper != before) {
                            exclusive = "<".equals(tokens.get(i + 1).text());
                        }
                    }
                    case "=" -> {
                        lower = later(lower, value);
                        Object before = upper;
                        upper = earlier(upper, value);
                        if (upper != before) {
                            exclusive = false;
                        }
                    }
                    default -> {
                        // != and : bound nothing
                    }
                }
            }
        }
        if (lower == null || upper == null) {
            throw invalid("this list needs a range on " + key + " (" + example + ").");
        }
        long days = ChronoUnit.DAYS.between(day(lower), day(upper));
        // `< next day` covers the same days as `<= last day`
        if (days > MAX_RANGE_DAYS + (exclusive ? 1 : 0)) {
            throw invalid("the range on " + key + " spans more than " + MAX_RANGE_DAYS + " days.");
        }
    }

    @SuppressWarnings("unchecked")
    private static Object later(Object current, Object value) {
        return current == null || ((Comparable<Object>) value).compareTo(current) > 0 ? value : current;
    }

    @SuppressWarnings("unchecked")
    private static Object earlier(Object current, Object value) {
        return current == null || ((Comparable<Object>) value).compareTo(current) < 0 ? value : current;
    }

    private static LocalDate day(Object value) {
        if (value instanceof OffsetDateTime time) {
            return time.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        throw invalid("the partition range must be a date or a timestamp.");
    }

    private String orExpression() {
        List<String> parts = new ArrayList<>(List.of(andExpression()));
        while (isKeyword("OR")) {
            position++;
            parts.add(andExpression());
        }
        return parts.size() == 1 ? parts.get(0) : "(" + String.join(" OR ", parts) + ")";
    }

    private String andExpression() {
        List<String> parts = new ArrayList<>(List.of(notExpression()));
        while (isKeyword("AND")) {
            position++;
            parts.add(notExpression());
        }
        return parts.size() == 1 ? parts.get(0) : "(" + String.join(" AND ", parts) + ")";
    }

    private String notExpression() {
        if (isKeyword("NOT")) {
            position++;
            return "(NOT " + notExpression() + ")";
        }
        return primary();
    }

    private String primary() {
        Token token = next();
        if (token.kind() == Kind.LPAREN) {
            if (++depth > MAX_DEPTH) {
                throw invalid("filter nests too deeply.");
            }
            String inner = orExpression();
            if (next().kind() != Kind.RPAREN) {
                throw invalid("missing ')' in filter.");
            }
            depth--;
            return inner;
        }
        if (token.kind() != Kind.IDENT) {
            throw invalid("expected a field name in filter, found '" + token.text() + "'.");
        }
        FieldSpec field = fields.get(token.text());
        if (field == null) {
            throw invalid("field '" + token.text() + "' cannot be filtered.");
        }
        Token op = next();
        if (op.kind() != Kind.OP) {
            throw invalid("expected an operator after '" + field.name() + "'.");
        }
        Token value = next();
        if (++terms > MAX_TERMS) {
            throw invalid("filter has more than " + MAX_TERMS + " conditions.");
        }
        return comparison(field, op.text(), value);
    }

    private String comparison(FieldSpec field, String op, Token value) {
        if (value.kind() == Kind.IDENT && "null".equals(value.text())) {
            return switch (op) {
                case "=" -> field.column() + " IS NULL";
                case "!=" -> field.column() + " IS NOT NULL";
                default -> throw invalid("null can only be compared with = or !=.");
            };
        }
        Object bound = literal(field, value);
        String name = "flt" + params.size();
        if (":".equals(op)) {
            if (field.type() != ValueType.STRING) {
                throw invalid("':' (contains) applies to text fields only.");
            }
            params.put(name, "%" + ((String) bound).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
            return field.column() + " ILIKE :" + name;
        }
        if (field.type() == ValueType.BOOLEAN && !"=".equals(op) && !"!=".equals(op)) {
            throw invalid("true/false fields can only be compared with = or !=.");
        }
        params.put(name, bound);
        return field.column() + " " + ("!=".equals(op) ? "<>" : op) + " :" + name;
    }

    private Object literal(FieldSpec field, Token value) {
        boolean bare = value.kind() == Kind.NUMBER
                || value.kind() == Kind.IDENT && ("true".equals(value.text()) || "false".equals(value.text()));
        if (value.kind() != Kind.STRING && !bare) {
            throw invalid("expected a value for '" + field.name() + "'.");
        }
        try {
            return field.type().parse(value.text());
        } catch (IllegalArgumentException e) {
            throw invalid("'" + field.name() + "' expects a " + field.type().name().toLowerCase(java.util.Locale.ROOT)
                    + " value.");
        }
    }

    private boolean isKeyword(String keyword) {
        Token token = peek();
        return token.kind() == Kind.IDENT && keyword.equals(token.text());
    }

    private Token peek() {
        return tokens.get(position);
    }

    private Token next() {
        Token token = tokens.get(position);
        if (token.kind() != Kind.END) {
            position++;
        }
        return token;
    }

    private static List<Token> tokenize(String text) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '(' || c == ')') {
                out.add(new Token(c == '(' ? Kind.LPAREN : Kind.RPAREN, String.valueOf(c)));
                i++;
            } else if (c == '"') {
                StringBuilder value = new StringBuilder();
                i++;
                while (i < text.length() && text.charAt(i) != '"') {
                    if (text.charAt(i) == '\\' && i + 1 < text.length()) {
                        i++;
                    }
                    value.append(text.charAt(i++));
                }
                if (i >= text.length()) {
                    throw invalid("unterminated string in filter.");
                }
                i++;
                out.add(new Token(Kind.STRING, value.toString()));
            } else if ("=!<>:".indexOf(c) >= 0) {
                String two = i + 1 < text.length() ? text.substring(i, i + 2) : "";
                if ("!=".equals(two) || "<=".equals(two) || ">=".equals(two)) {
                    out.add(new Token(Kind.OP, two));
                    i += 2;
                } else if (c == '!') {
                    throw invalid("unexpected '!' in filter.");
                } else {
                    out.add(new Token(Kind.OP, String.valueOf(c)));
                    i++;
                }
            } else if (c == '-' || Character.isDigit(c)) {
                int start = i++;
                while (i < text.length() && (Character.isDigit(text.charAt(i)) || text.charAt(i) == '.')) {
                    i++;
                }
                out.add(new Token(Kind.NUMBER, text.substring(start, i)));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_'
                        || text.charAt(i) == '.')) {
                    i++;
                }
                out.add(new Token(Kind.IDENT, text.substring(start, i)));
            } else {
                throw invalid("unexpected character '" + c + "' in filter.");
            }
        }
        out.add(new Token(Kind.END, "end of filter"));
        return out;
    }

    private static InvalidArgumentException invalid(String detail) {
        return new InvalidArgumentException(InvalidArgumentException.INVALID_FILTER, detail);
    }
}
