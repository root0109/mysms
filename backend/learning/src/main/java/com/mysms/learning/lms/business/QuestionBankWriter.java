package com.mysms.learning.lms.business;

import com.mysms.core.id.UuidV7;
import com.mysms.learning.lms.model.CoursePackage;
import com.mysms.learning.lms.repository.CoursePackageRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Writes a checked question with its key (options, accepted answers) into the bank, inside the caller's transaction:
 * shared by course package imports and question imports.
 */
@Component
public class QuestionBankWriter {

    private final CoursePackageRepository repository;
    private final Clock clock;

    public QuestionBankWriter(CoursePackageRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Answers the new question's id. */
    public UUID write(CoursePackage.Question question, OffsetDateTime now, UUID actor) {
        UUID id = UuidV7.next(clock);
        repository.insertQuestion(id, question.questionType(), question.title(), question.questionText(),
                QuizGrader.number(question.marks()), Boolean.TRUE.equals(question.allowsMultiple()),
                Boolean.TRUE.equals(question.caseSensitive()), question.trueFalseAnswer(), question.numericAnswer(),
                question.numericTolerance(), Boolean.TRUE.equals(question.numericIsRange()), question.generalFeedback(), now,
                actor);
        int order = 0;
        for (CoursePackage.Option option : question.options()) {
            repository.insertOption(UuidV7.next(clock), id, ++order, option.text(),
                    Boolean.TRUE.equals(option.correct()), option.explanation(), now, actor);
        }
        order = 0;
        for (String answer : question.acceptedAnswers()) {
            repository.insertAccepted(UuidV7.next(clock), id, ++order, answer, now, actor);
        }
        return id;
    }
}
