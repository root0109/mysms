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

    private static SortSpec sort(String... sort) {
        return SortSpec.parse(List.of(sort), FIELDS, "name", "c.id");
    }

    @Test
    void parsesSortAgainstTheAllowlist() {
        assertThat(sort().canonical()).isEqualTo("name,asc");
        assertThat(SortSpec.parse(null, FIELDS, "sortOrder desc", "c.id").canonical()).isEqualTo("sortOrder,desc");
        // one value is split at its commas by Spring, several are not: both read the same
        assertThat(sort("sortOrder", "desc", "name").orderByClause())
                .isEqualTo("COALESCE(c.sort_order, 2147483647) DESC, c.name ASC, c.id ASC");
        assertThat(sort("sortOrder,desc", "name,asc").canonical()).isEqualTo("sortOrder,desc;name,asc");
        assertThat(sort("sortOrder,desc", "name,desc").orderByClause()).as("a descending order breaks ties descending")
                .isEqualTo("COALESCE(c.sort_order, 2147483647) DESC, c.name DESC, c.id DESC");
        assertThat(sort("sortOrder,DESC,name").canonical()).isEqualTo("sortOrder,desc;name,asc");
        assertThatThrownBy(() -> sort("secret")).isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("not sortable");
        assertThatThrownBy(() -> sort("name", "sideways")).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> sort("name,asc,desc")).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> sort("desc")).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> sort("name", "name,desc")).isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("twice");
    }

    @Test
    void validatesPageAndSize() {
        PageRequest defaults = PageRequest.of(null, null, null, sort(), "", CLOCK);
        assertThat(defaults.page()).isEqualTo(1);
        assertThat(defaults.size()).isEqualTo(50);
        assertThat(defaults.limitParams()).containsEntry("limit", 51).containsEntry("offset", 0L);
        assertThat(defaults.limitClause()).isEqualTo(" LIMIT :limit OFFSET :offset");
        assertThat(PageRequest.of(3, 500, " ", sort(), "", CLOCK).limitParams())
                .containsEntry("limit", 501).containsEntry("offset", 1000L);
        assertThat(PageRequest.of(10, 10, null, sort(), "", CLOCK).keysetPredicate()).isSameAs(SqlFragment.TRUE);
        assertThatThrownBy(() -> PageRequest.of(0, null, null, sort(), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> PageRequest.of(1, 0, null, sort(), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> PageRequest.of(1, 501, null, sort(), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void capsOffsetPagesAndLeavesDeeperPagesToCursors() {
        assertThat(PageRequest.of(201, 50, null, sort(), "", CLOCK).limitParams()).containsEntry("offset", 10_000L);
        assertThatThrownBy(() -> PageRequest.of(202, 50, null, sort(), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("follow the next links");
        assertThatThrownBy(() -> PageRequest.of(Integer.MAX_VALUE, 500, null, sort(), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class);
        String deep = PageCursor.encode(5000, sort(), "", List.of("M"), ID, CLOCK);
        PageRequest followed = PageRequest.of(5000, 50, deep, sort(), "", CLOCK);
        assertThat(followed.limitParams()).containsEntry("offset", 0L);
        assertThat(followed.keysetPredicate().params()).containsEntry("ks0", "M");
    }

    @Test
    void issuesAndFollowsKeysetCursors() {
        SortSpec sort = sort("sortOrder,desc", "name");
        PageRequest first = PageRequest.of(null, 2, null, sort, "isActive = true", CLOCK);
        List<Row> fetched = List.of(new Row(ID, "A", 9), new Row(UUID.randomUUID(), "B", 9), new Row(UUID.randomUUID(), "C", 1));
        Page<Row> page = first.toPage(fetched, r -> List.of(r.sortOrder(), r.name()), Row::id);
        assertThat(page.items()).hasSize(2);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.hasNext()).isTrue();

        PageRequest second = PageRequest.of(2, 2, page.nextCursor(), sort, "isActive = true", CLOCK);
        SqlFragment keyset = second.keysetPredicate();
        assertThat(keyset.sql()).isEqualTo("(COALESCE(c.sort_order, 2147483647) < :ks0) OR "
                + "(COALESCE(c.sort_order, 2147483647) = :ks0 AND c.name > :ks1) OR "
                + "(COALESCE(c.sort_order, 2147483647) = :ks0 AND c.name = :ks1 AND c.id > :ksid)");
        assertThat(keyset.params()).containsEntry("ks0", 9L).containsEntry("ks1", "B").containsKey("ksid");
        assertThat(second.limitParams()).containsEntry("offset", 0L);

        Page<Row> last = second.toPage(List.of(new Row(ID, "C", 1)), r -> List.of(r.sortOrder(), r.name()), Row::id);
        assertThat(last.hasNext()).isFalse();
        assertThat(last.nextCursor()).isEmpty();
        assertThat(last.map(Row::name).items()).containsExactly("C");
        assertThat(last.map(Row::name).page()).isEqualTo(2);
    }

    @Test
    void aUniformOrderContinuesWithOneRowComparison() {
        String ascending = PageCursor.encode(2, sort("name"), "", List.of("B"), ID, CLOCK);
        assertThat(PageRequest.of(2, 10, ascending, sort("name"), "", CLOCK).keysetPredicate().sql())
                .isEqualTo("(c.name, c.id) > (:ks0, :ksid)");
        SortSpec descending = sort("sortOrder,desc", "name,desc");
        String cursor = PageCursor.encode(2, descending, "", List.of(3L, "B"), ID, CLOCK);
        SqlFragment after = PageRequest.of(2, 10, cursor, descending, "", CLOCK).keysetPredicate();
        assertThat(after.sql()).isEqualTo("(COALESCE(c.sort_order, 2147483647), c.name, c.id) < (:ks0, :ks1, :ksid)");
        assertThat(after.params()).containsEntry("ks0", 3L).containsEntry("ks1", "B").containsEntry("ksid", ID);
    }

    @Test
    void rejectsCursorsForAnotherQueryPageExpiredOrMalformed() {
        SortSpec sort = sort("name");
        String cursor = PageCursor.encode(2, sort, "", List.of("A"), ID, CLOCK);
        assertThat(PageRequest.of(2, 10, cursor, sort, "", CLOCK).page()).isEqualTo(2);
        assertThatThrownBy(() -> PageRequest.of(2, 10, cursor, sort("sortOrder"), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("different");
        assertThatThrownBy(() -> PageRequest.of(2, 10, cursor, sort("name,desc"), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("different");
        assertThatThrownBy(() -> PageRequest.of(2, 10, cursor, sort, "name = \"x\"", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("different");
        assertThatThrownBy(() -> PageRequest.of(3, 10, cursor, sort, "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("page 2");
        Clock later = Clock.offset(CLOCK, Duration.ofHours(25));
        assertThatThrownBy(() -> PageRequest.of(2, 10, cursor, sort, "", later))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("expired");
        assertThatThrownBy(() -> PageRequest.of(2, 10, "not-a-cursor", sort, "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("not valid");
        String partial = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"v\":2}".getBytes());
        assertThatThrownBy(() -> PageRequest.of(2, 10, partial, sort, "", CLOCK)).isInstanceOf(InvalidArgumentException.class);
        String version1 = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"v\":1,\"p\":2,\"s\":\"name,asc\",\"t\":0,\"k\":[\"A\"],\"i\":\"x\"}".getBytes());
        assertThatThrownBy(() -> PageRequest.of(2, 10, version1, sort, "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class).hasMessageContaining("not valid");
        String wrongArity = PageCursor.encode(2, sort("sortOrder,name"), "", List.of(1L, "A"), ID, CLOCK);
        assertThatThrownBy(() -> PageCursor.decode(wrongArity, 2, sort("name"), "", CLOCK))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void pagingInfoLinksKeepTheRequestAndReplaceOnlyPageSizeAndCursor() {
        Page<String> middle = new Page<>(List.of("a", "b"), 3, 2, "Q1w-_");
        PagingInfo<String> info = PagingUtil.pagingInfo(middle, "/v1/hr/staff",
                "size=2&filter=name%20%3D%20%22A%26B%22&page=3&sort=name,desc&cursor=old&fields=id");
        assertThat(info.data()).containsExactly("a", "b");
        assertThat(info.paging()).isEqualTo(new NavigationInfo(3, 2,
                "/v1/hr/staff?page=1&size=2&filter=name%20%3D%20%22A%26B%22&sort=name,desc&fields=id",
                "/v1/hr/staff?page=2&size=2&filter=name%20%3D%20%22A%26B%22&sort=name,desc&fields=id",
                "/v1/hr/staff?page=4&size=2&filter=name%20%3D%20%22A%26B%22&sort=name,desc&fields=id&cursor=Q1w-_"));

        PagingInfo<String> only = PagingUtil.pagingInfo(new Page<>(List.of("a"), 1, 50, ""), "/v1/x", null);
        assertThat(only.paging()).isEqualTo(new NavigationInfo(1, 1, "/v1/x?page=1&size=50", null, null));

        // a page reached through cursors beyond the OFFSET cap has no previous link
        PagingInfo<String> deep = PagingUtil.pagingInfo(new Page<>(List.of("a"), 300, 50, ""), "/v1/x", "%zz=1");
        assertThat(deep.paging().previous()).isNull();
        assertThat(deep.paging().first()).isEqualTo("/v1/x?page=1&size=50&%zz=1");

        assertThat(PagingUtil.cursorOf(info.paging().next())).contains("Q1w-_");
        assertThat(PagingUtil.cursorOf(info.paging().previous())).isEmpty();
        assertThat(PagingUtil.cursorOf(null)).isEmpty();
        assertThat(PagingUtil.cursorOf("/v1/x")).isEmpty();
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
