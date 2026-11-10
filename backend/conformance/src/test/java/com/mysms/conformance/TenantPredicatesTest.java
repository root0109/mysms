package com.mysms.conformance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TenantPredicatesTest {

    private final TenantPredicates predicates = new TenantPredicates(Set.of("sis.student", "sis.guardian"));

    @Test
    void aFilteredStatementIsComplete() {
        assertThat(predicates.missing("SELECT id FROM sis.student WHERE tenant_id = :tenantId AND id = :id")).isEmpty();
        assertThat(predicates.missing("""
                SELECT s.id FROM sis.student s
                JOIN sis.guardian g ON g.tenant_id = :tenantId AND g.student_id = s.id
                WHERE s.tenant_id = :tenantId AND s.deleted_at IS NULL
                ORDER BY s.id""")).isEmpty();
        assertThat(predicates.missing("UPDATE sis.student SET name = :name WHERE tenant_id = :tenantId AND id = :id"))
                .isEmpty();
        // a global table needs no predicate
        assertThat(predicates.missing("SELECT id FROM master.country WHERE code = :code")).isEmpty();
    }

    @Test
    void aPredicateMustFilterTheTableItself() {
        assertThat(predicates.missing("SELECT id FROM sis.student WHERE id = :id")).containsExactly("sis.student");
        // only inside an OR branch: the other branch reads every tenant
        assertThat(predicates.missing("SELECT id FROM sis.student WHERE tenant_id = :tenantId OR id = :id"))
                .containsExactly("sis.student");
        // only inside a sub-select: the outer table is unfiltered
        assertThat(predicates.missing("""
                SELECT s.id FROM sis.student s
                WHERE s.id IN (SELECT g.student_id FROM sis.guardian g WHERE g.tenant_id = :tenantId)""")
        ).containsExactly("sis.student");
        // a joined table filtered only by the base table's predicate
        assertThat(predicates.missing("""
                SELECT s.id FROM sis.student s JOIN sis.guardian g ON g.student_id = s.id
                WHERE s.tenant_id = :tenantId""")).containsExactly("sis.guardian");
    }

    @Test
    void shapesTheCheckCannotPlaceAreRefused() {
        assertThatThrownBy(() -> predicates.missing(
                "SELECT id FROM sis.student WHERE tenant_id = :tenantId UNION SELECT id FROM sis.guardian"))
                .isInstanceOf(TenantPredicates.Unsupported.class).hasMessageContaining("compound");
        assertThatThrownBy(() -> predicates.missing("SELECT s.id FROM sis.student s, sis.guardian g WHERE 1 = 1"))
                .isInstanceOf(TenantPredicates.Unsupported.class).hasMessageContaining("comma join");
        assertThatThrownBy(() -> predicates.missing("SELECT id FROM sis.student ORDER BY id"))
                .isInstanceOf(TenantPredicates.Unsupported.class).hasMessageContaining("has no WHERE");
        assertThatThrownBy(() -> predicates.missing("SELECT id FROM sis.student WHERE id = :id)"))
                .isInstanceOf(TenantPredicates.Unsupported.class).hasMessageContaining("unbalanced");
    }
}
