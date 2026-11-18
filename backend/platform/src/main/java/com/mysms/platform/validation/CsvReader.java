package com.mysms.platform.validation;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads an uploaded CSV file (RFC 4180, as spreadsheets save it): UTF-8 with or without a byte-order mark, comma
 * separated, cells optionally in double quotes (a quoted cell may hold commas, line breaks and doubled quotes), records
 * ended by CRLF or LF. Each record is one spreadsheet row, so its 1-based index is the row number a user sees.
 */
public final class CsvReader {

    /** Why the bytes are not a CSV file; the message names the problem, never the content. */
    public static final class MalformedCsvException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        MalformedCsvException(String message) {
            super(message);
        }
    }

    private CsvReader() {
    }

    /**
     * The file's records, each a list of its cells (unquoted, not trimmed).
     *
     * @throws MalformedCsvException when the bytes are not UTF-8 text or a quoted cell is never closed
     */
    public static List<List<String>> read(byte[] content) {
        String text = decode(content);
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        int start = !text.isEmpty() && text.charAt(0) == '﻿' ? 1 : 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c != '"') {
                    cell.append(c);
                } else if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = false;
                }
                continue;
            }
            switch (c) {
                case '"' -> {
                    quoted = true;
                    any = true;
                }
                case ',' -> {
                    record.add(cell.toString());
                    cell.setLength(0);
                    any = true;
                }
                case '\r' -> {
                    // CRLF ends the record at the LF; a lone CR is kept as text
                    if (i + 1 >= text.length() || text.charAt(i + 1) != '\n') {
                        cell.append(c);
                        any = true;
                    }
                }
                case '\n' -> {
                    record.add(cell.toString());
                    records.add(List.copyOf(record));
                    record.clear();
                    cell.setLength(0);
                    any = false;
                }
                default -> {
                    cell.append(c);
                    any = true;
                }
            }
        }
        if (quoted) {
            throw new MalformedCsvException("a quoted cell is never closed");
        }
        if (any) {
            record.add(cell.toString());
            records.add(List.copyOf(record));
        }
        return records;
    }

    private static String decode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException notUtf8) {
            throw new MalformedCsvException("the file is not UTF-8 text; save it as CSV UTF-8");
        }
    }
}
