package com.mysms.lms.business;

import com.mysms.lms.model.CoursePackage.Option;
import com.mysms.lms.model.CoursePackage.Question;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a question file into bank questions, collecting every problem with its line: GIFT (Moodle's format: true/false,
 * multiple choice, short answer, numeric and essay questions; matching and missing-word questions are reported as
 * problems) and CSV with the header {@value #CSV_HEADER} (options and correct option numbers separated by {@code |}).
 */
final class QuestionFileParser {

    static final String CSV_HEADER = "questionType,title,questionText,marks,options,correct,acceptedAnswers,"
            + "trueFalseAnswer,numericAnswer,numericTolerance";
    static final int MAX_QUESTIONS = 2000;
    private static final Pattern TITLE = Pattern.compile("^::(.*?)::", Pattern.DOTALL);
    private static final Pattern NUMERIC_RANGE = Pattern.compile("^(-?[0-9.]+)\\.\\.(-?[0-9.]+)$");

    /** The questions read, or the problems that stop the import. */
    record Parsed(List<Question> questions, List<String> problems) {
    }

    private QuestionFileParser() {
    }

    static Parsed parse(String format, byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        List<String> problems = new ArrayList<>();
        List<Question> questions = "csv".equals(format) ? csv(text, problems) : gift(text, problems);
        if (questions.isEmpty() && problems.isEmpty()) {
            problems.add("the file holds no question");
        }
        if (questions.size() > MAX_QUESTIONS) {
            problems.add("at most " + MAX_QUESTIONS + " questions per file");
        }
        for (int i = 0; i < questions.size() && problems.size() < CoursePackageCodec.MAX_PROBLEMS; i++) {
            CoursePackageCodec.question(questions.get(i), "question " + (i + 1), problems);
        }
        return new Parsed(problems.isEmpty() ? questions : List.of(), problems.subList(0,
                Math.min(problems.size(), CoursePackageCodec.MAX_PROBLEMS)));
    }

    // ---- GIFT

    private static List<Question> gift(String text, List<String> problems) {
        List<Question> questions = new ArrayList<>();
        StringBuilder block = new StringBuilder();
        int blockLine = 1;
        String[] lines = text.split("\n", -1);
        for (int i = 0; i <= lines.length; i++) {
            String line = i < lines.length ? lines[i] : "";
            if (line.trim().startsWith("//") || line.trim().startsWith("$CATEGORY:")) {
                continue;
            }
            if (line.isBlank()) {
                if (!block.toString().isBlank()) {
                    Question question = giftQuestion(block.toString().trim(), blockLine, problems);
                    if (question != null) {
                        questions.add(question);
                    }
                }
                block.setLength(0);
                blockLine = i + 2;
                continue;
            }
            block.append(line).append('\n');
        }
        return questions;
    }

    private static Question giftQuestion(String block, int line, List<String> problems) {
        String at = "line " + line;
        String title = null;
        Matcher named = TITLE.matcher(block);
        if (named.find()) {
            title = unescape(named.group(1).trim());
            block = block.substring(named.end()).trim();
        }
        int open = unescaped(block, '{', 0);
        int close = open < 0 ? -1 : unescaped(block, '}', open);
        if (open < 0 || close < 0) {
            problems.add(at + ": a question needs its answers in { }");
            return null;
        }
        if (!block.substring(close + 1).isBlank()) {
            problems.add(at + ": missing-word questions (text after the answers) are not supported");
            return null;
        }
        String stem = unescape(block.substring(0, open).replaceFirst("^\\[\\w+\\]", "").trim());
        String answers = block.substring(open + 1, close).trim();
        if (answers.isEmpty()) {
            return question("open_ended", title, stem, null, List.of(), List.of(), null, null, null);
        }
        String upper = answers.toUpperCase(Locale.ROOT);
        if (List.of("T", "TRUE", "F", "FALSE").contains(upper.split("#")[0].trim())) {
            return question("true_false", title, stem, null, List.of(), List.of(), upper.startsWith("T"), null, null);
        }
        if (answers.startsWith("#")) {
            return numeric(title, stem, answers.substring(1).trim(), at, problems);
        }
        if (answers.contains("->")) {
            problems.add(at + ": matching questions are not supported");
            return null;
        }
        List<Option> options = new ArrayList<>();
        boolean anyWrong = false;
        for (String part : split(answers)) {
            char mark = part.charAt(0);
            String rest = part.substring(1).trim();
            String feedback = null;
            int hash = unescaped(rest, '#', 0);
            if (hash >= 0) {
                feedback = unescape(rest.substring(hash + 1).trim());
                rest = rest.substring(0, hash).trim();
            }
            boolean correct = mark == '=';
            Matcher weight = Pattern.compile("^%(-?[0-9.]+)%").matcher(rest);
            if (weight.find()) {
                correct = QuizGrader.number(weight.group(1)) != null && QuizGrader.number(weight.group(1)).signum() > 0;
                rest = rest.substring(weight.end()).trim();
            }
            anyWrong |= mark == '~';
            options.add(new Option(unescape(rest), correct, feedback));
        }
        if (!anyWrong) {
            return question("user_input", title, stem, null, List.of(),
                    options.stream().map(Option::text).toList(), null, null, null);
        }
        boolean multiple = options.stream().filter(Option::correct).count() > 1;
        return question("choices", title, stem, multiple, options, List.of(), null, null, null);
    }

    private static Question numeric(String title, String stem, String spec, String at, List<String> problems) {
        String first = spec.split("=")[spec.startsWith("=") ? 1 : 0].split("#")[0].trim();
        Matcher range = NUMERIC_RANGE.matcher(first);
        if (range.matches()) {
            return new Question("numeric", title, stem, "1", null, false, null, range.group(1), range.group(2), true,
                    null, List.of(), List.of());
        }
        String[] value = first.split(":");
        if (value.length > 2 || QuizGrader.number(value[0]) == null) {
            problems.add(at + ": a numeric answer is a number, number:tolerance or low..high");
            return null;
        }
        return question("numeric", title, stem, null, List.of(), List.of(), null, value[0].trim(),
                value.length == 2 ? value[1].trim() : null);
    }

    /** The answers split at unescaped {@code =} and {@code ~}, each starting with its mark. */
    private static List<String> split(String answers) {
        List<String> parts = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < answers.length(); i++) {
            char c = answers.charAt(i);
            boolean escaped = i > 0 && answers.charAt(i - 1) == '\\';
            if ((c == '=' || c == '~') && !escaped) {
                if (start >= 0) {
                    parts.add(answers.substring(start, i).trim());
                }
                start = i;
            }
        }
        if (start >= 0) {
            parts.add(answers.substring(start).trim());
        }
        return parts;
    }

    private static int unescaped(String text, char wanted, int from) {
        for (int i = from; i < text.length(); i++) {
            if (text.charAt(i) == wanted && (i == 0 || text.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String text) {
        return text.replaceAll("\\\\([:~=#{}])", "$1").replace("\\n", "\n");
    }

    // ---- CSV

    private static List<Question> csv(String text, List<String> problems) {
        List<List<String>> rows = csvRows(text, problems);
        List<Question> questions = new ArrayList<>();
        if (rows.isEmpty()) {
            return questions;
        }
        if (!CSV_HEADER.equals(String.join(",", rows.get(0)).trim())) {
            problems.add("line 1: the header must be " + CSV_HEADER);
            return questions;
        }
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.size() == 1 && row.get(0).isBlank()) {
                continue;
            }
            if (row.size() != 10) {
                problems.add("line " + (i + 1) + ": expected 10 columns, found " + row.size());
                continue;
            }
            Map<String, String> cells = Map.of("type", row.get(0).trim(), "options", row.get(4),
                    "correct", row.get(5), "accepted", row.get(6));
            List<String> optionTexts = cells.get("options").isBlank() ? List.of()
                    : List.of(cells.get("options").split("\\|"));
            List<String> correct = cells.get("correct").isBlank() ? List.of()
                    : List.of(cells.get("correct").split("\\|"));
            List<Option> options = new ArrayList<>();
            for (int o = 0; o < optionTexts.size(); o++) {
                options.add(new Option(optionTexts.get(o).trim(), correct.contains(String.valueOf(o + 1)), null));
            }
            List<String> accepted = cells.get("accepted").isBlank() ? List.of()
                    : List.of(cells.get("accepted").split("\\|")).stream().map(String::trim).toList();
            String trueFalse = row.get(7).trim().toLowerCase(Locale.ROOT);
            questions.add(new Question(cells.get("type"), blank(row.get(1)), row.get(2).trim(),
                    blank(row.get(3)) == null ? "1" : row.get(3).trim(), correct.size() > 1, false,
                    trueFalse.isEmpty() ? null : Boolean.valueOf("true".equals(trueFalse)), blank(row.get(8)),
                    blank(row.get(9)), false, null, options, accepted));
        }
        return questions;
    }

    /** RFC 4180 rows: quoted cells may hold commas, quotes ("") and line breaks. */
    static List<List<String>> csvRows(String text, List<String> problems) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(c);
            }
        }
        if (quoted) {
            problems.add("the file ends inside a quoted cell");
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Question question(String type, String title, String text, Boolean multiple, List<Option> options,
            List<String> accepted, Boolean trueFalse, String numeric, String tolerance) {
        return new Question(type, title, text, "1", multiple, false, trueFalse, numeric, tolerance, false, null,
                options, accepted);
    }
}
