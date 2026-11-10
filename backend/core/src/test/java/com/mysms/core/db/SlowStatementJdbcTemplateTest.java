package com.mysms.core.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlowStatementJdbcTemplateTest {

    @Test
    void aStatementIsLoggedOnOneLineAndCutToTheLimit() {
        assertThat(SlowStatementJdbcTemplate.abbreviate("  SELECT id\n    FROM sis.student\n   WHERE tenant_id = ?  "))
                .isEqualTo("SELECT id FROM sis.student WHERE tenant_id = ?");
        String cut = SlowStatementJdbcTemplate.abbreviate("SELECT " + "x, ".repeat(400) + "1");
        assertThat(cut).hasSize(SlowStatementJdbcTemplate.MAX_LOGGED + 1).endsWith("…");
        assertThat(SlowStatementJdbcTemplate.abbreviate(null)).isEqualTo("(statement text not available)");
    }
}
