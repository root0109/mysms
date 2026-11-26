package com.mysms.learning.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The CSV writer and reader round-trip, quoting, formula defusal, BOM, comments and blank lines. */
class CsvTest {

    @Test
    void writesBomCrlfQuotedCells() {
        String text = new String(Csv.write(List.of(List.of("a", "b,c", "d\"e"), List.of("=SUM(A1)", ""))),
                StandardCharsets.UTF_8);
        assertThat(text).isEqualTo("﻿a,\"b,c\",\"d\"\"e\"\r\n'=SUM(A1),\r\n");
    }

    @Test
    void readsQuotesCommentsAndBlankLines() {
        String text = "﻿# how to fill this file\nuser_id,email\n\"u,1\",a@x.in\n\n\"says \"\"hi\"\"\",b\r\nlast,";
        assertThat(Csv.read(text.getBytes(StandardCharsets.UTF_8))).containsExactly(
                List.of("user_id", "email"),
                List.of("u,1", "a@x.in"),
                List.of("says \"hi\"", "b"),
                List.of("last", ""));
    }

    @Test
    void roundTrips() {
        List<List<String>> rows = List.of(List.of("x", "a,b", "c\nd"), List.of("y", "", "\"q\""));
        assertThat(Csv.read(Csv.write(rows))).isEqualTo(rows);
    }
}
