package com.mysms.lms.business;

import java.util.Map;
import java.util.Optional;

/**
 * A plagiarism-checking service (the port; {@link TurnitinPlagiarismProvider} is the adapter). Calls are remote and
 * never made inside a database transaction; a refusal or an unreachable service is a {@link Failure}.
 */
public interface PlagiarismProvider {

    /** The provider name a plagiarism account names ({@code lms.plagiarism_account.provider}). */
    String name();

    /** Creates the submission at the provider and uploads the file; answers the provider's submission reference. */
    String submit(Account account, Upload upload);

    /** Asks for the similarity report of an uploaded submission. */
    void requestSimilarity(Account account, String submissionRef);

    /** A short-lived link to the provider's report viewer for one viewer. */
    String viewerUrl(Account account, String submissionRef, String viewerRef, boolean teacher);

    /** What a provider callback says about a submission, from its parsed body; empty when it is not one of its events. */
    Optional<Event> event(Map<String, Object> body);

    /** An account's credentials: the API base URL, the account reference and the decrypted key. */
    record Account(String apiUrl, String accountRef, String secret) {
    }

    /** A file to check, with who owns and submitted it and the references the callback must echo. */
    record Upload(String title, String fileName, byte[] content, String ownerRef, String submitterRef,
                  Map<String, String> metadata) {

        public Upload {
            metadata = Map.copyOf(metadata);
        }
    }

    /**
     * A callback: the submission it is about, whether the upload was processed ({@code uploaded}), whether the report
     * is ready with its overall match percentage, or a provider error code.
     */
    record Event(String submissionRef, boolean uploaded, boolean reportReady, String similarity, String errorCode,
                 Map<String, String> metadata) {

        public Event {
            metadata = Map.copyOf(metadata);
        }
    }

    /** The provider refused the request or could not be reached. */
    final class Failure extends RuntimeException {

        public Failure(String message) {
            super(message);
        }
    }
}
