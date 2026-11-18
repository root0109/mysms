package com.mysms.lms.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * The attempt token StartQuizAttempt hands out and every later step of the attempt must send back
 * (SaveQuizResponses, SubmitQuiz): it binds a save to the delivery of this attempt, so a tab left open on an older
 * attempt of the same submission record can never write into it. It is derived from the attempt itself (id, number,
 * start time), so nothing is stored for it; it is not a credential — only the student who owns the attempt can use
 * the operations at all.
 */
public final class QuizAttemptToken {

    private QuizAttemptToken() {
    }

    /** The token of the attempt started at {@code startedAt} (compared at microsecond precision, as stored). */
    public static String of(UUID submissionId, int attemptNumber, OffsetDateTime startedAt) {
        String started = startedAt == null ? "-" : String.valueOf(
                ChronoUnit.MICROS.between(OffsetDateTime.parse("1970-01-01T00:00:00Z"), startedAt));
        String material = submissionId + "|" + attemptNumber + "|" + started;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    /** Constant-time comparison of a sent token with the attempt's own. */
    static boolean matches(String sent, String expected) {
        return sent != null && MessageDigest.isEqual(sent.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
