package com.mysms.lms.model;

import java.util.List;

/**
 * MySMS's native course package (package format {@code native}, version 1): a ZIP holding {@code manifest.json} with
 * this shape. It carries a course template's chapters with their lessons, quizzes (and their questions) and
 * assignments — what ExportCourse writes and ImportCoursePackage reads. Absent lists are empty; every text is
 * checked against the column it lands in before anything is written.
 */
public record CoursePackage(String format, Integer version, Course course, List<Chapter> chapters) {

    /** The format name a manifest must state. */
    public static final String FORMAT = "mysms-course-package";
    /** The one version this release reads and writes. */
    public static final int VERSION = 1;

    public CoursePackage {
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }

    /** The course the package was exported from, as recorded on the import job. */
    public record Course(String code, String title, String version) {
    }

    public record Chapter(String title, String description, List<Lesson> lessons, List<Quiz> quizzes,
                          List<Assignment> assignments) {

        public Chapter {
            lessons = lessons == null ? List.of() : List.copyOf(lessons);
            quizzes = quizzes == null ? List.of() : List.copyOf(quizzes);
            assignments = assignments == null ? List.of() : List.copyOf(assignments);
        }
    }

    public record Lesson(String title, String body, Boolean includeInPreview, String youtubeVideoId) {
    }

    public record Quiz(String title, String description, Integer passingPercentage, Integer durationMinutes,
                       Integer maxAttempts, List<Question> questions) {

        public Quiz {
            questions = questions == null ? List.of() : List.copyOf(questions);
        }
    }

    /** A question with its key: options (choices) or accepted answers (user input); other types carry fields. */
    public record Question(String questionType, String title, String questionText, String marks,
                           Boolean allowsMultiple, Boolean caseSensitive, Boolean trueFalseAnswer,
                           String numericAnswer, String numericTolerance, Boolean numericIsRange,
                           String generalFeedback, List<Option> options, List<String> acceptedAnswers) {

        public Question {
            options = options == null ? List.of() : List.copyOf(options);
            acceptedAnswers = acceptedAnswers == null ? List.of() : List.copyOf(acceptedAnswers);
        }
    }

    public record Option(String text, Boolean correct, String explanation) {
    }

    public record Assignment(String title, String question, String submissionType) {
    }
}
