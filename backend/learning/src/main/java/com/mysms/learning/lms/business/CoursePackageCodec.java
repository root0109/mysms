package com.mysms.learning.lms.business;

import com.mysms.learning.lms.model.CoursePackage;
import com.mysms.learning.lms.model.CoursePackage.Assignment;
import com.mysms.learning.lms.model.CoursePackage.Chapter;
import com.mysms.learning.lms.model.CoursePackage.Lesson;
import com.mysms.learning.lms.model.CoursePackage.Option;
import com.mysms.learning.lms.model.CoursePackage.Question;
import com.mysms.learning.lms.model.CoursePackage.Quiz;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import tools.jackson.core.JacksonException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads and writes the native course package (a ZIP with {@code manifest.json}, see {@link CoursePackage}). Reading is
 * defensive: the archive is bounded (entries, expanded bytes — a ZIP bomb stops at the bound), the manifest must state
 * the format and version, and every value is checked against the column it will land in. Every problem is reported
 * with its place in the manifest ({@code chapters[0].quizzes[1].questions[2].marks}), so the author can fix them all at
 * once; nothing is imported from a package with a problem.
 */
@Component
public final class CoursePackageCodec {

    static final String MANIFEST = "manifest.json";
    static final int MAX_ENTRIES = 1000;
    static final long MAX_EXPANDED_BYTES = 50L * 1024 * 1024;
    static final int MAX_MANIFEST_BYTES = 5 * 1024 * 1024;
    static final int MAX_PROBLEMS = 100;
    private static final Set<String> QUESTION_TYPES = Set.of("choices", "true_false", "user_input", "numeric",
            "open_ended");
    private static final Set<String> SUBMISSION_TYPES = Set.of("document", "pdf", "url", "image", "text");

    /** What reading a package found: the package when it is sound, else its problems. */
    public record Outcome(CoursePackage pack, List<String> problems) {

        public Outcome {
            problems = List.copyOf(problems);
        }

        public boolean valid() {
            return problems.isEmpty();
        }
    }

    private final JsonMapper json;

    public CoursePackageCodec(JsonMapper json) {
        this.json = json;
    }

    public Outcome read(byte[] zip) {
        byte[] manifest;
        try {
            manifest = manifest(zip);
        } catch (ZipException e) {
            return problem("the file is not a readable ZIP archive");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalStateException e) {
            return problem(e.getMessage());
        }
        if (manifest == null) {
            return problem("the archive has no " + MANIFEST);
        }
        CoursePackage pack;
        try {
            pack = json.readValue(manifest, CoursePackage.class);
        } catch (JacksonException e) {
            return problem(MANIFEST + " is not valid JSON of a course package");
        }
        if (pack == null) {
            return problem(MANIFEST + " is empty");
        }
        List<String> problems = new ArrayList<>();
        if (!CoursePackage.FORMAT.equals(pack.format())) {
            problems.add("format: must be \"" + CoursePackage.FORMAT + "\"");
        } else if (pack.version() == null || pack.version() != CoursePackage.VERSION) {
            problems.add("version: only version " + CoursePackage.VERSION + " is supported");
        } else {
            check(pack, problems);
        }
        return new Outcome(problems.isEmpty() ? pack : null, List.copyOf(problems.subList(0,
                Math.min(problems.size(), MAX_PROBLEMS))));
    }

    public byte[] write(CoursePackage pack) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(MANIFEST));
            zip.write(json.writerWithDefaultPrettyPrinter().writeValueAsBytes(pack));
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** The manifest's bytes, or null when the archive has none; the archive's bounds are enforced on the way. */
    private static byte[] manifest(byte[] zip) throws IOException {
        byte[] manifest = null;
        long expanded = 0;
        int entries = 0;
        boolean any = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = in.getNextEntry()) != null) {
                any = true;
                if (++entries > MAX_ENTRIES) {
                    throw new IllegalStateException("the archive holds more than " + MAX_ENTRIES + " entries");
                }
                boolean isManifest = MANIFEST.equals(entry.getName());
                ByteArrayOutputStream kept = isManifest ? new ByteArrayOutputStream() : null;
                int read;
                while ((read = in.read(buffer)) > 0) {
                    expanded += read;
                    if (expanded > MAX_EXPANDED_BYTES) {
                        throw new IllegalStateException("the archive expands beyond "
                                + MAX_EXPANDED_BYTES / (1024 * 1024) + " MB");
                    }
                    if (kept != null) {
                        if (kept.size() + read > MAX_MANIFEST_BYTES) {
                            throw new IllegalStateException(MANIFEST + " is larger than "
                                    + MAX_MANIFEST_BYTES / (1024 * 1024) + " MB");
                        }
                        kept.write(buffer, 0, read);
                    }
                }
                if (kept != null) {
                    manifest = kept.toByteArray();
                }
            }
        }
        if (!any) {
            throw new ZipException("no entries");
        }
        return manifest;
    }

    private static void check(CoursePackage pack, List<String> problems) {
        if (pack.course() != null) {
            text(pack.course().code(), "course.code", 50, false, problems);
            text(pack.course().title(), "course.title", 200, false, problems);
            text(pack.course().version(), "course.version", 20, false, problems);
        }
        if (pack.chapters().isEmpty()) {
            problems.add("chapters: the package holds no chapter");
        }
        for (int c = 0; c < pack.chapters().size(); c++) {
            Chapter chapter = pack.chapters().get(c);
            String at = "chapters[" + c + "]";
            if (chapter == null) {
                problems.add(at + ": must not be null");
                continue;
            }
            text(chapter.title(), at + ".title", 200, true, problems);
            for (int i = 0; i < chapter.lessons().size(); i++) {
                lesson(chapter.lessons().get(i), at + ".lessons[" + i + "]", problems);
            }
            for (int i = 0; i < chapter.quizzes().size(); i++) {
                quiz(chapter.quizzes().get(i), at + ".quizzes[" + i + "]", problems);
            }
            for (int i = 0; i < chapter.assignments().size(); i++) {
                assignment(chapter.assignments().get(i), at + ".assignments[" + i + "]", problems);
            }
        }
    }

    private static void lesson(Lesson lesson, String at, List<String> problems) {
        if (lesson == null) {
            problems.add(at + ": must not be null");
            return;
        }
        text(lesson.title(), at + ".title", 200, true, problems);
        text(lesson.youtubeVideoId(), at + ".youtubeVideoId", 60, false, problems);
    }

    private static void quiz(Quiz quiz, String at, List<String> problems) {
        if (quiz == null) {
            problems.add(at + ": must not be null");
            return;
        }
        text(quiz.title(), at + ".title", 200, true, problems);
        if (quiz.passingPercentage() != null && (quiz.passingPercentage() < 0 || quiz.passingPercentage() > 100)) {
            problems.add(at + ".passingPercentage: must be from 0 to 100");
        }
        positive(quiz.durationMinutes(), at + ".durationMinutes", problems);
        positive(quiz.maxAttempts(), at + ".maxAttempts", problems);
        for (int i = 0; i < quiz.questions().size(); i++) {
            question(quiz.questions().get(i), at + ".questions[" + i + "]", problems);
        }
    }

    /** Checks one question as the bank will hold it; adds each problem with its place. */
    static void question(Question question, String at, List<String> problems) {
        if (question == null) {
            problems.add(at + ": must not be null");
            return;
        }
        if (!QUESTION_TYPES.contains(question.questionType())) {
            problems.add(at + ".questionType: must be one of " + String.join(", ", QUESTION_TYPES.stream().sorted()
                    .toList()));
            return;
        }
        text(question.title(), at + ".title", 255, false, problems);
        if (question.questionText() == null || question.questionText().isBlank()) {
            problems.add(at + ".questionText: is required");
        }
        BigDecimal marks = QuizGrader.number(question.marks());
        if (marks == null || marks.signum() <= 0 || marks.scale() > 2 || marks.precision() - marks.scale() > 6) {
            problems.add(at + ".marks: must be a positive number with at most 2 decimals");
        }
        switch (question.questionType()) {
            case "choices" -> {
                long correct = question.options().stream().filter(o -> o != null && Boolean.TRUE.equals(o.correct()))
                        .count();
                if (question.options().size() < 2) {
                    problems.add(at + ".options: a choices question has at least 2 options");
                }
                if (correct == 0 || (correct > 1 && !Boolean.TRUE.equals(question.allowsMultiple()))) {
                    problems.add(at + ".options: mark one correct option (several only with allowsMultiple)");
                }
                for (int i = 0; i < question.options().size(); i++) {
                    Option option = question.options().get(i);
                    if (option == null || option.text() == null || option.text().isBlank()) {
                        problems.add(at + ".options[" + i + "].text: is required");
                    }
                }
            }
            case "true_false" -> {
                if (question.trueFalseAnswer() == null) {
                    problems.add(at + ".trueFalseAnswer: is required");
                }
            }
            case "user_input" -> {
                if (question.acceptedAnswers().isEmpty()
                        || question.acceptedAnswers().stream().anyMatch(a -> a == null || a.isBlank())) {
                    problems.add(at + ".acceptedAnswers: name at least one, none blank");
                }
            }
            case "numeric" -> {
                if (QuizGrader.number(question.numericAnswer()) == null) {
                    problems.add(at + ".numericAnswer: must be a number");
                }
                if (question.numericTolerance() != null && QuizGrader.number(question.numericTolerance()) == null) {
                    problems.add(at + ".numericTolerance: must be a number");
                }
            }
            default -> {
                // open-ended: marked by a teacher, no key
            }
        }
    }

    private static void assignment(Assignment assignment, String at, List<String> problems) {
        if (assignment == null) {
            problems.add(at + ": must not be null");
            return;
        }
        text(assignment.title(), at + ".title", 200, true, problems);
        if (assignment.question() == null || assignment.question().isBlank()) {
            problems.add(at + ".question: is required");
        }
        if (!SUBMISSION_TYPES.contains(assignment.submissionType())) {
            problems.add(at + ".submissionType: must be one of document, image, pdf, text, url");
        }
    }

    private static void text(String value, String at, int max, boolean required, List<String> problems) {
        if (value == null || value.isBlank()) {
            if (required) {
                problems.add(at + ": is required");
            }
        } else if (value.length() > max) {
            problems.add(at + ": must be at most " + max + " characters");
        }
    }

    private static void positive(Integer value, String at, List<String> problems) {
        if (value != null && value <= 0) {
            problems.add(at + ": must be positive");
        }
    }

    private static Outcome problem(String problem) {
        return new Outcome(null, List.of(problem));
    }
}
