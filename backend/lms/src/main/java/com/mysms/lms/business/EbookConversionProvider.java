package com.mysms.lms.business;

import java.util.Map;
import java.util.Optional;

/**
 * A document-conversion service (the port; {@link CloudConvertConversionProvider} is the adapter). Calls are remote
 * and never made inside a database transaction; a refusal or an unreachable service is a {@link Failure}.
 */
public interface EbookConversionProvider {

    /** The provider name the configuration ({@code mysms.lms.conversion.provider}) and the callback path use. */
    String name();

    /** Starts converting the document behind {@code sourceUrl}; answers the provider's job reference. */
    String submit(Service service, Conversion conversion);

    /**
     * Waits for the job up to the service's wait time; empty when it is still running then. A finished job carries
     * the output's URL, a failed one its error.
     */
    Optional<Result> await(Service service, String jobRef);

    /** What a provider callback says about a job, from its parsed body; empty when it is not one of its events. */
    Optional<Event> event(Map<String, Object> body);

    /** The deployment's conversion service: base URL, API key and how long a synchronous wait may take. */
    record Service(String apiUrl, String apiKey, java.time.Duration waitFor) {
    }

    /** One document to convert, with the references the callback must echo. */
    record Conversion(String sourceUrl, String sourceName, String targetFormat, Map<String, String> metadata) {

        public Conversion {
            metadata = Map.copyOf(metadata);
        }
    }

    /** How a job ended: the converted file's URL, or the provider's error. */
    record Result(boolean succeeded, String outputUrl, String error) {
    }

    /** A callback: the job it is about, how it ended and the echoed references. */
    record Event(String jobRef, Result result, Map<String, String> metadata) {

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
