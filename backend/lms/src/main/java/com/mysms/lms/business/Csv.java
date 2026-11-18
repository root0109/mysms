package com.mysms.lms.business;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV for lms's exports and member imports, written and read as platform's import files are: UTF-8 with a BOM,
 * CRLF rows, quoted cells, formula-opening cells defused with a leading apostrophe.
 */
final class Csv {

    private Csv() {
    }

    static byte[] write(List<List<String>> rows) {
        StringBuilder out = new StringBuilder("﻿");
        for (List<String> row : rows) {
            out.append(row.stream().map(Csv::cell).collect(Collectors.joining(","))).append("\r\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** The file's rows, split on commas with double-quote quoting; blank lines and '#' comment lines are dropped. */
    static List<List<String>> read(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int at = 0; at < text.length(); at++) {
            char symbol = text.charAt(at);
            if (quoted) {
                if (symbol == '"' && at + 1 < text.length() && text.charAt(at + 1) == '"') {
                    cell.append('"');
                    at++;
                } else if (symbol == '"') {
                    quoted = false;
                } else {
                    cell.append(symbol);
                }
            } else if (symbol == '"') {
                quoted = true;
            } else if (symbol == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (symbol == '\n' || symbol == '\r') {
                if (symbol == '\r' && at + 1 < text.length() && text.charAt(at + 1) == '\n') {
                    at++;
                }
                end(rows, row, cell);
                row = new ArrayList<>();
            } else {
                cell.append(symbol);
            }
        }
        end(rows, row, cell);
        return rows;
    }

    private static void end(List<List<String>> rows, List<String> row, StringBuilder cell) {
        row.add(cell.toString());
        cell.setLength(0);
        boolean blank = row.size() == 1 && row.get(0).isBlank();
        if (!blank && !row.get(0).stripLeading().startsWith("#")) {
            rows.add(List.copyOf(row));
        }
    }

    private static String cell(String value) {
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
