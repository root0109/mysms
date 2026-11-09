package com.mysms.core.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.InvalidArgumentException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PagingTest {

    private static final Map<String, FieldSpec> FIELDS = Map.of(
            "name", new FieldSpec("name", "c.name", ValueType.STRING),
            "sortOrder", new FieldSpec("sortOrder", "COALESCE(c.sort_order, 2147483647)", ValueType.INTEGER));
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-06T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID ID = UUID.fromString("0190b0a0-0000-7000-8000-000000000001");

    record Row(UUID id, String name, long sortOrder) {
    }

    private static SortSpec sort(String orderBy) {
        return SortSpec.parse(orderBy, FIELDS, "name", "c.id");
    }

    @Test
    void parsesOrderByAgainstTheAllowlist() {
        assertThat(sort(null).canonical()).isEqualTo("name");
        assertThat(sort("sortOrder desc, name").orderByClause())
                .isEqualTo("COALESCE(c.sort_order, 2147483647) DESC, c.name ASC, c.id ASC");
        assertThat(sort("name asc").canonical()).isEqualTo("name");
        assertThatThrownBy(() -> sort("secret")).isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("not sortable");
        assertThatThrownBy(() -> sort("name sideways")).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> sort("name,name desc")).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> sort("name,")).isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void validatesPageSize() {
        assertThat(PageRequest.of(null, null, sort(null), "", CLOCK).pageSize()).isEqualTo(50);
        assertThat(PageRequest.of(500, null, sort(null), "", CLOCK).limit()).isEqualTo(501);
        assertThatThrownBy(() -> PageRequest.of(0, null, sort(null), "", CLOCK)).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> PageRequest.of(501, null, sort(null), "", CLOCK)).isInstanceOf(InvalidArgumentException.class);
        assertThat(PageRequest.of(10, " ", sort(null), "", CLOCK).keysetPredicate()).isSameAs(SqlFragment.TRUE);
    }

    @Test
    void issuesAndFollowsKeysetTokens() {
        SortSpec sort = sort("sortOrder desc,name");
        PageRequest first = PageRequest.of(2, null, sort, "isActive = true", CLOCK);
        List<Row> fetched = List.of(new Row(ID, "A", 9), new Row(UUID.randomUUID(), "B", 9), new Row(UUID.randomUUID(), "C", 1));
        Page<Row> page = first.toPage(fetched, r -> List.of(r.sortOrder(), r.name()), Row::id);
        assertThat(page.items()).hasSize(2);
        assertThat(page.nextPageToken()).isNotEmpty();

        PageRequest second = PageRequest.of(2, page.nextPageToken(), sort, "isActive = true", CLOCK);
        SqlFragment keyset = second.keysetPredicate();
        assertThat(keyset.sql()).isEqualTo("(COALESCE(c.sort_order, 2147483647) < :ks0) OR "
                + "(COALESCE(c.sort_order, 2147483647) = :ks0 AND c.name > :ks1) OR "
                + "(COALESCE(c.sort_order, 2147483647) = :ks0 AND c.name = :ks1 AND c.id > :ksid)");
        assertThat(keyset.params()).containsEntry("ks0", 9L).containsEntry("ks1", "B").containsKey("ksid");

        Page<Row> last = second.toPage(List.of(new Row(ID, "C", 1)), r -> List.of(r.sortOrder(), r.name()), Row::id);
        assertThat(last.nextPageToken()).isEmpty();
        assertThat(last.map(Row::name).items()).containsExactly("C");
    }

    @Test
    void rejectsTokensForAnotherQueryExpiredOrForged() {
        SortSpec sort = sort("name");
        String token = PageToken.encode(sort, "", List.of("A"), ID, CLOCK);
        assertThatThrownBy(() -> PageRequest.of(10, token, sort("sortOrder"), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("different");
        assertThatThrownBy(() -> PageRequest.of(10, token, sort, "name = \"x\"", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("different");
        Clock later = Clock.offset(CLOCK, Duration.ofHours(25));
        assertThatThrownBy(() -> PageRequest.of(10, token, sort, "", later))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("expired");
        assertThatThrownBy(() -> PageRequest.of(10, "not-a-token", sort, "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("not valid");
        String forged = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"v\":2}".getBytes());
        assertThatThrownBy(() -> PageRequest.of(10, forged, sort, "", CLOCK)).isInstanceOf(InvalidArgumentException.class);
        String wrongArity = PageToken.encode(sort("sortOrder,name"), "", List.of(1L, "A"), ID, CLOCK);
        assertThatThrownBy(() -> PageToken.decode(wrongArity, sort("name"), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void valueTypesParseAndFormat() {
        assertThat(ValueType.INTEGER.parse("42")).isEqualTo(42L);
        assertThat(ValueType.BOOLEAN.parse("true")).isEqualTo(true);
        assertThat(ValueType.BOOLEAN.parse("false")).isEqualTo(false);
        assertThat(ValueType.UUID.parse(ID.toString())).isEqualTo(ID);
        assertThat(ValueType.UUID.format(ID)).isEqualTo(ID.toString());
        assertThat(ValueType.DATE.parse("2026-11-06")).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(ValueType.DECIMAL.parse("1.50")).isEqualTo(new BigDecimal("1.50"));
        OffsetDateTime time = (OffsetDateTime) ValueType.TIMESTAMP.parse("2026-11-06T10:00:00Z");
        assertThat(ValueType.TIMESTAMP.format(time)).isEqualTo("2026-11-06T10:00Z");
        assertThat(ValueType.STRING.format("x")).isEqualTo("x");
        assertThatThrownBy(() -> ValueType.BOOLEAN.parse("yes")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValueType.INTEGER.parse("x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValueType.TIMESTAMP.parse("x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sqlFragmentsCombine() {
        SqlFragment a = new SqlFragment("a = :a", Map.of("a", 1));
        assertThat(SqlFragment.TRUE.and(a)).isEqualTo(a);
        assertThat(a.and(SqlFragment.TRUE)).isEqualTo(a);
        SqlFragment both = a.and(new SqlFragment("b = :b", Map.of("b", 2)));
        assertThat(both.sql()).isEqualTo("(a = :a) AND (b = :b)");
        assertThat(both.params()).containsOnlyKeys("a", "b");
        assertThatThrownBy(() -> a.and(a)).isInstanceOf(IllegalStateException.class);
    }
}
