package com.mysms.learning.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.learning.lms.business.CoursePackageCodec.Outcome;
import com.mysms.learning.lms.model.CoursePackage;
import com.mysms.learning.lms.model.CoursePackage.Assignment;
import com.mysms.learning.lms.model.CoursePackage.Chapter;
import com.mysms.learning.lms.model.CoursePackage.Lesson;
import com.mysms.learning.lms.model.CoursePackage.Option;
import com.mysms.learning.lms.model.CoursePackage.Question;
import com.mysms.learning.lms.model.CoursePackage.Quiz;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** A written course package reads back whole, and a broken archive is named rather than imported. */
class CoursePackageCodecTest {

    private final CoursePackageCodec codec = new CoursePackageCodec(JsonMapper.builder().build());

    @Test
    void aWrittenPackageReadsBackWhole() {
        CoursePackage pack = new CoursePackage(CoursePackage.FORMAT, CoursePackage.VERSION,
                new CoursePackage.Course("ALG-1", "Algebra", "2"),
                List.of(new Chapter("Unit 1", "Start here",
                        List.of(new Lesson("Primes", "<p>7 is prime.</p>", true, null)),
                        List.of(new Quiz("Check", null, 50, 20, 2, List.of(
                                new Question("choices", "Primes", "Pick the prime.", "6", false, false, null, null,
                                        null, false, null,
                                        List.of(new Option("7", true, "Right"), new Option("8", false, null)),
                                        List.of())))),
                        List.of(new Assignment("Essay", "Write about primes.", "text")))));
        Outcome outcome = codec.read(codec.write(pack));
        assertThat(outcome.valid()).as(String.join("; ", outcome.problems())).isTrue();
        assertThat(outcome.pack()).isEqualTo(pack);
    }

    @Test
    void aBrokenArchiveOrManifestIsNamedNotImported() {
        assertThat(codec.read("not a zip".getBytes(StandardCharsets.UTF_8)).problems())
                .containsExactly("the file is not a readable ZIP archive");
        CoursePackage wrongFormat = new CoursePackage("other-format", 1, null, List.of());
        assertThat(codec.read(codec.write(wrongFormat)).valid()).isFalse();
    }
}
