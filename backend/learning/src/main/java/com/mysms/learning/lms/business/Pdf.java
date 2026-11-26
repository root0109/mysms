package com.mysms.learning.lms.business;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimal PDF writer (PDF 1.4, Helvetica, uncompressed) for lms's generated documents, kept in place of a
 * rendering library the build does not carry. One title line, then body lines top to bottom over as many A4 pages
 * as they need.
 */
final class Pdf {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 72;
    private static final int TITLE_SIZE = 24;
    private static final int BODY_SIZE = 12;
    private static final int LEADING = 20;
    /** Body lines on the first page (below the title) and on every later page. */
    static final int FIRST_PAGE_LINES = (PAGE_HEIGHT - 120 - 2 * LEADING - MARGIN) / LEADING;
    static final int PAGE_LINES = (PAGE_HEIGHT - MARGIN - MARGIN) / LEADING;

    private Pdf() {
    }

    static byte[] render(String title, List<String> lines) {
        List<String> contents = new ArrayList<>();
        StringBuilder first = new StringBuilder("BT\n/F1 " + TITLE_SIZE + " Tf\n");
        first.append(MARGIN).append(' ').append(PAGE_HEIGHT - 120).append(" Td\n");
        first.append('(').append(escape(title)).append(") Tj\n");
        first.append("/F2 ").append(BODY_SIZE).append(" Tf\n0 ").append(-2 * LEADING).append(" Td\n");
        int at = body(first, lines, 0, FIRST_PAGE_LINES);
        contents.add(first.append("ET").toString());
        while (at < lines.size()) {
            StringBuilder next = new StringBuilder("BT\n/F2 " + BODY_SIZE + " Tf\n");
            next.append(MARGIN).append(' ').append(PAGE_HEIGHT - MARGIN).append(" Td\n");
            at = body(next, lines, at, PAGE_LINES);
            contents.add(next.append("ET").toString());
        }

        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        int pageCount = contents.size();
        for (int page = 0; page < pageCount; page++) {
            kids.append(3 + 2 * page).append(" 0 R ");
        }
        objects.add("<< /Type /Pages /Kids [" + kids.toString().strip() + "] /Count " + pageCount + " >>");
        int fontBold = 3 + 2 * pageCount;
        int fontBody = fontBold + 1;
        for (int page = 0; page < pageCount; page++) {
            byte[] stream = contents.get(page).getBytes(StandardCharsets.ISO_8859_1);
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "]"
                    + " /Resources << /Font << /F1 " + fontBold + " 0 R /F2 " + fontBody + " 0 R >> >>"
                    + " /Contents " + (4 + 2 * page) + " 0 R >>");
            objects.add("<< /Length " + stream.length + " >>\nstream\n"
                    + new String(stream, StandardCharsets.ISO_8859_1) + "\nendstream");
        }
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n");
        long[] offsets = new long[objects.size() + 1];
        for (int number = 1; number <= objects.size(); number++) {
            offsets[number] = out.size();
            write(out, number + " 0 obj\n" + objects.get(number - 1) + "\nendobj\n");
        }
        long xref = out.size();
        StringBuilder table = new StringBuilder("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int number = 1; number <= objects.size(); number++) {
            table.append(String.format("%010d 00000 n ", offsets[number])).append('\n');
        }
        table.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF");
        write(out, table.toString());
        return out.toByteArray();
    }

    private static int body(StringBuilder content, List<String> lines, int from, int capacity) {
        int to = Math.min(lines.size(), from + capacity);
        for (int at = from; at < to; at++) {
            content.append('(').append(escape(lines.get(at))).append(") Tj\n0 ").append(-LEADING).append(" Td\n");
        }
        return to;
    }

    /** Escapes the three characters PDF strings reserve; anything outside Latin-1 becomes '?'. */
    static String escape(String text) {
        StringBuilder safe = new StringBuilder(text.length());
        for (char symbol : text.toCharArray()) {
            switch (symbol) {
                case '(', ')', '\\' -> safe.append('\\').append(symbol);
                default -> safe.append(symbol <= 0xFF && symbol >= 0x20 ? symbol : '?');
            }
        }
        return safe.toString();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
    }
}
