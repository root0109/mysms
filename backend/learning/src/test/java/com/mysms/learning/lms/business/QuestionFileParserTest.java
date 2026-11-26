package com.mysms.learning.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.learning.lms.business.QuestionFileParser.Parsed;
import com.mysms.learning.lms.model.CoursePackage.Option;
import com.mysms.learning.lms.model.CoursePackage.Question;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** GIFT and CSV question files become bank questions; every problem is named with its line. */
class QuestionFileParserTest {

    private static Parsed gift(String text) {
        return QuestionFileParser.parse("gift", text.getBytes(StandardCharsets.UTF_8));
    }

    private static Parsed csv(String text) {
        return QuestionFileParser.parse("csv", text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsATitledMultipleChoiceQuestionWithFeedbackAndWeights() {
        Parsed parsed = gift("""
                // a comment the parser skips
                $CATEGORY: algebra

                ::Capitals::What is the capital of France? {
                =Paris #Well done
                ~London #No
                ~%-50%Berlin
                }
                """);
        assertThat(parsed.problems()).isEmpty();
        Question question = parsed.questions().get(0);
        assertThat(question.questionType()).isEqualTo("choices");
        assertThat(question.title()).isEqualTo("Capitals");
        assertThat(question.questionText()).isEqualTo("What is the capital of France?");
        assertThat(question.allowsMultiple()).isFalse();
        assertThat(question.options()).containsExactly(new Option("Paris", true, "Well done"),
                new Option("London", false, "No"), new Option("Berlin", false, null));
    }

    @Test
    void positivelyWeightedAnswersCountAsCorrectAndTwoCorrectMeansMultiple() {
        Parsed parsed = gift("Pick the primes {~%50%2 ~%50%3 ~%-100%4}");
        Question question = parsed.questions().get(0);
        assertThat(question.allowsMultiple()).isTrue();
        assertThat(question.options().stream().filter(Option::correct)).hasSize(2);
    }

    @Test
    void readsTrueFalseShortAnswerNumericAndEssayQuestions() {
        Parsed parsed = gift("""
                The sky is blue. {TRUE}

                Who wrote Hamlet? {=Shakespeare =William Shakespeare}

                What is 2 + 2? {#4}

                How warm is a healthy human? {#36.5:0.8}

                What is pi, roughly? {#3.1..3.2}

                Describe photosynthesis. {}
                """);
        assertThat(parsed.problems()).isEmpty();
        assertThat(parsed.questions()).extracting(Question::questionType)
                .containsExactly("true_false", "user_input", "numeric", "numeric", "numeric", "open_ended");
        assertThat(parsed.questions().get(0).trueFalseAnswer()).isTrue();
        assertThat(parsed.questions().get(1).acceptedAnswers()).containsExactly("Shakespeare", "William Shakespeare");
        assertThat(parsed.questions().get(2).numericAnswer()).isEqualTo("4");
        assertThat(parsed.questions().get(3).numericAnswer()).isEqualTo("36.5");
        assertThat(parsed.questions().get(3).numericTolerance()).isEqualTo("0.8");
        Question range = parsed.questions().get(4);
        assertThat(range.numericAnswer()).isEqualTo("3.1");
        assertThat(range.numericTolerance()).isEqualTo("3.2");
        assertThat(range.numericIsRange()).isTrue();
    }

    @Test
    void unescapesTheGiftEscapes() {
        Parsed parsed = gift("What is 1\\:1? {=a ratio\\=equality}");
        assertThat(parsed.questions().get(0).questionText()).isEqualTo("What is 1:1?");
        assertThat(parsed.questions().get(0).acceptedAnswers()).containsExactly("a ratio=equality");
    }

    @Test
    void namesTheUnsupportedAndMalformedQuestionsByLine() {
        assertThat(gift("Match these {=one -> 1 =two -> 2}").problems())
                .containsExactly("line 1: matching questions are not supported");
        assertThat(gift("The {=missing} word").problems())
                .containsExactly("line 1: missing-word questions (text after the answers) are not supported");
        assertThat(gift("No answers here").problems())
                .containsExactly("line 1: a question needs its answers in { }");
        assertThat(gift("First {TRUE}\n\nWhat? {#not-a-number}").problems())
                .containsExactly("line 3: a numeric answer is a number, number:tolerance or low..high");
        assertThat(gift("\n\n").problems()).containsExactly("the file holds no question");
    }

    @Test
    void aFileWithAnyProblemImportsNothing() {
        Parsed parsed = gift("Good {TRUE}\n\nBad {=one -> 1 =two -> 2}");
        assertThat(parsed.problems()).hasSize(1);
        assertThat(parsed.questions()).isEmpty();
    }

    @Test
    void readsTheCsvColumnsIntoAQuestion() {
        Parsed parsed = csv(QuestionFileParser.CSV_HEADER + "\n"
                + "choices,Capitals,\"What is the capital of France?\",2,Paris|London|Berlin,1,,,,\n"
                + "user_input,,Who wrote Hamlet?,,,,\"Shakespeare|William, the Bard\",,,\n"
                + "true_false,,The sky is blue.,1,,,,true,,\n"
                + "numeric,,What is 2 + 2?,1,,,,,4,0.5\n");
        assertThat(parsed.problems()).isEmpty();
        Question choices = parsed.questions().get(0);
        assertThat(choices.marks()).isEqualTo("2");
        assertThat(choices.options()).containsExactly(new Option("Paris", true, null),
                new Option("London", false, null), new Option("Berlin", false, null));
        assertThat(parsed.questions().get(1).acceptedAnswers()).containsExactly("Shakespeare", "William, the Bard");
        assertThat(parsed.questions().get(2).trueFalseAnswer()).isTrue();
        assertThat(parsed.questions().get(3).numericAnswer()).isEqualTo("4");
        assertThat(parsed.questions().get(3).numericTolerance()).isEqualTo("0.5");
    }

    @Test
    void refusesTheWrongCsvHeaderAndWrongColumnCounts() {
        assertThat(csv("type,title\nchoices,x").problems())
                .containsExactly("line 1: the header must be " + QuestionFileParser.CSV_HEADER);
        assertThat(csv(QuestionFileParser.CSV_HEADER + "\nchoices,only-two").problems())
                .containsExactly("line 2: expected 10 columns, found 2");
        assertThat(csv(QuestionFileParser.CSV_HEADER + "\n\"unterminated,a,b,c,d,e,f,g,h").problems())
                .contains("the file ends inside a quoted cell");
    }

    @Test
    void quotedCsvCellsKeepCommasQuotesAndLineBreaks() {
        var rows = QuestionFileParser.csvRows("a,\"b,\"\"c\"\"\nd\",e\n", new java.util.ArrayList<>());
        assertThat(rows).containsExactly(java.util.List.of("a", "b,\"c\"\nd", "e"));
    }
}
