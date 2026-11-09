package com.mysms.core.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FilterParserTest {

    private static final Map<String, FieldSpec> FIELDS = Map.of(
            "name", new FieldSpec("name", "c.name", ValueType.STRING),
            "isActive", new FieldSpec("isActive", "c.is_active", ValueType.BOOLEAN),
            "sortOrder", new FieldSpec("sortOrder", "c.sort_order", ValueType.INTEGER),
            "countryId", new FieldSpec("countryId", "c.country_id", ValueType.UUID));

    private static SqlFragment parse(String filter) {
        return FilterParser.parse(filter, FIELDS);
    }

    @Test
    void blankFilterMatchesEverything() {
        assertThat(parse(null)).isSameAs(SqlFragment.TRUE);
        assertThat(parse("  ")).isSameAs(SqlFragment.TRUE);
    }

    @Test
    void buildsBoundSqlFromTheAllowlist() {
        UUID country = UUID.randomUUID();
        SqlFragment sql = parse("isActive = true AND (sortOrder >= 3 OR NOT name != \"India\") AND countryId = \""
                + country + "\"");
        assertThat(sql.sql()).isEqualTo(
                "(c.is_active = :flt0 AND (c.sort_order >= :flt1 OR (NOT c.name <> :flt2)) AND c.country_id = :flt3)");
        assertThat(sql.params()).containsEntry("flt0", true).containsEntry("flt1", 3L)
                .containsEntry("flt2", "India").containsEntry("flt3", country);
    }

    @Test
    void supportsContainsNullAndEscapes() {
        SqlFragment contains = parse("name : \"50%_off\\\"\"");
        assertThat(contains.sql()).isEqualTo("c.name ILIKE :flt0");
        assertThat(contains.params().get("flt0")).isEqualTo("%50\\%\\_off\"%");
        assertThat(parse("name = null").sql()).isEqualTo("c.name IS NULL");
        assertThat(parse("name != null").sql()).isEqualTo("c.name IS NOT NULL");
        assertThat(parse("sortOrder < -1").params()).containsEntry("flt0", -1L);
        assertThat(parse("sortOrder <= 1 AND sortOrder > 0").sql()).contains("<=", ">");
    }

    @Test
    void rejectsAnythingOutsideTheGrammar() {
        String[] bad = {
            "secret = 1", "name", "name = ", "name ~ \"x\"", "name = \"open", "(name = \"x\"", "name = \"x\")",
            "sortOrder = \"abc\"", "isActive > true", "sortOrder : \"1\"", "name < null", "name = x",
            "name ! \"x\"", "1 = 1", "name = \"x\" ; DROP TABLE x", "name = \"x\" OR"
        };
        for (String filter : bad) {
            assertThatThrownBy(() -> parse(filter)).as(filter).isInstanceOf(InvalidArgumentException.class)
                    .extracting("code").isEqualTo(InvalidArgumentException.INVALID_FILTER);
        }
    }

    @Test
    void boundsSizeDepthAndTerms() {
        assertThatThrownBy(() -> parse("name = \"" + "x".repeat(2001) + "\"")).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> parse("(".repeat(11) + "name = \"x\"" + ")".repeat(11)))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("deeply");
        String many = String.join(" OR ", java.util.Collections.nCopies(51, "sortOrder = 1"));
        assertThatThrownBy(() -> parse(many)).isInstanceOf(InvalidArgumentException.class).hasMessageContaining("50");
    }

    private static final Map<String, FieldSpec> DATED = Map.of(
            "name", new FieldSpec("name", "c.name", ValueType.STRING),
            "day", new FieldSpec("day", "c.day", ValueType.DATE),
            "at", new FieldSpec("at", "c.at", ValueType.TIMESTAMP));

    @Test
    void aPartitionedListMustBoundItsKeyOnBothSides() {
        FilterParser.requireRange("day >= \"2026-01-01\" AND day < \"2026-02-01\"", DATED, "day");
        FilterParser.requireRange("name = \"a\" AND day = \"2026-03-04\"", DATED, "day");
        FilterParser.requireRange("(name = \"a\" OR name = \"b\") AND at > \"2026-01-01T00:00:00Z\""
                + " AND at <= \"2026-12-31T23:00:00+05:30\"", DATED, "at");
        FilterParser.requireRange("day > \"2026-01-01\" AND day >= \"2025-01-01\" AND day <= \"2026-06-01\"",
                DATED, "day");
        for (String missing : new String[] {null, " ", "name = \"a\"", "day >= \"2026-01-01\"",
            "day >= \"2026-01-01\" OR day < \"2026-02-01\"", "(day >= \"2026-01-01\" OR name = \"a\") AND day "
                + "< \"2026-02-01\"", "NOT day < \"2026-01-01\" AND day >= \"2025-12-01\"",
            "day != \"2026-01-01\" AND day >= \"2025-12-01\"",
            "day >= \"2025-01-01\" AND day < \"2026-06-01\"", "day = null AND day >= \"2025-12-01\""}) {
            assertThatThrownBy(() -> FilterParser.requireRange(missing, DATED, "day"))
                    .as(String.valueOf(missing)).isInstanceOf(InvalidArgumentException.class);
        }
        assertThatThrownBy(() -> FilterParser.requireRange("day >= \"x\" AND day < \"y\"", DATED, "day"))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> FilterParser.requireRange("day = \"2026-01-01\"", DATED, "none"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FilterParser.requireRange("name >= \"a\" AND name <= \"b\"", DATED, "name"))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("date or a timestamp");
    }
}
