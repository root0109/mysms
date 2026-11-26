package com.mysms.learning.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The minimal PDF writer's structure: valid header, xref and trailer, escaped text, page breaks. */
class PdfTest {

    @Test
    void writesAValidSinglePageDocument() {
        byte[] pdf = Pdf.render("Certificate", List.of("Awarded to Asha (Maths)", "Issued on 2026-11-12"));
        String text = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(text).startsWith("%PDF-1.4").endsWith("%%EOF")
                .contains("/Type /Catalog").contains("/Count 1")
                .contains("(Certificate) Tj").contains("(Awarded to Asha \\(Maths\\)) Tj")
                .contains("/BaseFont /Helvetica");
        int startxref = Integer.parseInt(text.substring(text.lastIndexOf("startxref") + 10,
                text.lastIndexOf("%%EOF")).strip());
        assertThat(text.substring(startxref)).startsWith("xref");
    }

    @Test
    void breaksLongBodiesOverPages() {
        List<String> lines = IntStream.range(0, Pdf.FIRST_PAGE_LINES + Pdf.PAGE_LINES + 1)
                .mapToObj(at -> "line " + at).toList();
        String text = new String(Pdf.render("Syllabus", lines), StandardCharsets.ISO_8859_1);
        assertThat(text).contains("/Count 3").contains("(line 0) Tj")
                .contains("(line " + (Pdf.FIRST_PAGE_LINES + Pdf.PAGE_LINES) + ") Tj");
    }

    @Test
    void escapesReservedAndNonLatinCharacters() {
        assertThat(Pdf.escape("a(b)c\\d")).isEqualTo("a\\(b\\)c\\\\d");
        assertThat(Pdf.escape("नमस्ते")).isEqualTo("??????");
    }
}
